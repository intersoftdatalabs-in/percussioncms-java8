/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.percussion.rx.delivery.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper that probes the Amazon EC2 instance metadata service (IMDS) to
 * determine whether the host is running inside EC2. Supports both IMDSv2 (the
 * default on Amazon Linux 2023+ and most current AMIs) and falls back to
 * IMDSv1 for older AMIs that have not enabled the token-based flow.
 *
 * <p>The helper is designed to be cheap to call repeatedly: the result of the
 * first successful probe is cached for the lifetime of the JVM so callers such
 * as {@code PSPubServerService.isEC2Instance()} and
 * {@link PSAmazonS3DeliveryHandler#isEC2Instance()} can keep their existing
 * JVM-lifetime cache semantics.
 *
 * <p>IMDSv2 flow:
 *
 * <ol>
 *   <li>{@code PUT /latest/api/token} with header
 *       {@code X-aws-ec2-metadata-token-ttl-seconds: 21600} to obtain a session
 *       token.
 *   <li>Subsequent calls to {@code /latest/meta-data/...} include the token via
 *       {@code X-aws-ec2-metadata-token}.
 * </ol>
 *
 * <p>IMDSv1 fallback: if the {@code PUT} for the token is not supported (HTTP
 * 405) or fails for any other reason, the helper retries the metadata read
 * using a plain {@code GET} without a token, mimicking the legacy probe.
 *
 * <p>The helper is intentionally test-friendly: the underlying HTTP transport
 * is injected via {@link MetadataTransport} so unit tests can exercise the
 * IMDSv2-success, IMDSv1-fallback and non-EC2 paths without binding a local
 * server.
 */
public final class PSEc2InstanceMetadataClient {

  private static final Logger log = LogManager.getLogger(PSEc2InstanceMetadataClient.class);

  /** Default IMDS endpoint base URL. */
  static final String DEFAULT_METADATA_ENDPOINT = "http://169.254.169.254";

  /** Connect timeout in milliseconds. Keep short so the probe never stalls. */
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 1500;

  /** Read timeout in milliseconds. The token PUT can be slow on cold-cache. */
  static final int DEFAULT_READ_TIMEOUT_MS = 1500;

  /** Token TTL requested from IMDSv2. 21600 seconds (6h) is the AWS default. */
  static final String DEFAULT_TOKEN_TTL_SECONDS = "21600";

  /** Token request header. */
  static final String HEADER_TOKEN_TTL = "X-aws-ec2-metadata-token-ttl-seconds";

  /** Token echo header used for subsequent metadata reads. */
  static final String HEADER_TOKEN = "X-aws-ec2-metadata-token";

  /** Path for the IMDSv2 token request. */
  static final String TOKEN_PATH = "/latest/api/token";

  /** Path probed for the IMDSv1 / IMDSv2 metadata listing. */
  static final String METADATA_PATH = "/latest/meta-data/";

  /** HTTP status family treated as success. */
  static final int HTTP_STATUS_OK = 200;

  static final AtomicReference<MetadataTransport> TRANSPORT_OVERRIDE =
      new AtomicReference<>();

  private static final AtomicReference<Boolean> CACHED_RESULT = new AtomicReference<>();

  private static final AtomicBoolean PROBE_IN_FLIGHT = new AtomicBoolean(false);

  private static final AtomicInteger ATTEMPT_COUNT = new AtomicInteger(0);

  private PSEc2InstanceMetadataClient() {
    // utility class
  }

  /**
   * Returns {@code true} if the current host is running on EC2, performing an
   * IMDSv2 probe with an IMDSv1 fallback on first call. The result is cached
   * for the JVM lifetime.
   *
   * @return {@code true} if EC2 was detected, {@code false} otherwise (including
   *     on any error / timeout / non-EC2 host).
   */
  public static boolean isEC2Instance() {
    Boolean cached = CACHED_RESULT.get();
    if (cached != null) {
      return cached;
    }
    if (!PROBE_IN_FLIGHT.compareAndSet(false, true)) {
      // Another thread is already probing; behave as not-yet-known until it
      // finishes so we don't race two IMDS probes.
      return false;
    }
    try {
      Boolean firstResult = probeOnce();
      CACHED_RESULT.compareAndSet(null, firstResult);
      return CACHED_RESULT.get();
    } finally {
      PROBE_IN_FLIGHT.set(false);
    }
  }

  /**
   * Resets the cached result. Intended for tests and operators that have
   * changed the runtime environment (e.g. moved a packaged EC2 image to a
   * non-EC2 host).
   */
  public static void resetCache() {
    CACHED_RESULT.set(null);
    PROBE_IN_FLIGHT.set(false);
    ATTEMPT_COUNT.set(0);
  }

  /** Test hook to override the transport. Pass {@code null} to restore. */
  static void setTransportForTests(MetadataTransport transport) {
    TRANSPORT_OVERRIDE.set(transport);
  }

  /**
   * Performs a single end-to-end probe using the configured transport. Tries
   * IMDSv2 first, then falls back to IMDSv1 if IMDSv2 is not supported.
   *
   * @return {@code true} if any probe variant succeeded with an HTTP 2xx, else
   *     {@code false}.
   */
  private static Boolean probeOnce() {
    MetadataTransport transport = TRANSPORT_OVERRIDE.get();
    if (transport == null) {
      transport = defaultTransport();
    }

    int attempt = ATTEMPT_COUNT.incrementAndGet();
    if (attempt > 1) {
      log.debug("IMDS EC2 probe attempt #{}", attempt);
    }

    String tokenEndpoint = DEFAULT_METADATA_ENDPOINT + TOKEN_PATH;
    String metadataEndpoint = DEFAULT_METADATA_ENDPOINT + METADATA_PATH;

    try {
      // Step 1: try IMDSv2 token PUT.
      TokenResponse tokenResponse =
          transport.putForToken(tokenEndpoint, DEFAULT_TOKEN_TTL_SECONDS);
      if (tokenResponse.status == HTTP_STATUS_OK
          && StringUtils.isNotBlank(tokenResponse.body)) {
        // Step 2: GET metadata with the token.
        MetadataResponse metadata =
            transport.getWithToken(metadataEndpoint, tokenResponse.body);
        if (metadata.status >= 200 && metadata.status < 300) {
          log.debug("IMDSv2 probe succeeded (status={}).", metadata.status);
          return Boolean.TRUE;
        }
        log.debug(
            "IMDSv2 metadata GET failed with status {}; falling back to IMDSv1.",
            metadata.status);
      } else {
        log.debug(
            "IMDSv2 token PUT returned status {}; falling back to IMDSv1.",
            tokenResponse.status);
      }
    } catch (IOException e) {
      // Token PUT is not supported (IMDSv1-only) or the endpoint is
      // unreachable (non-EC2 host). Either way, try IMDSv1 below.
      log.debug("IMDSv2 token PUT failed: {}", e.getMessage());
    }

    // Step 3: IMDSv1 fallback.
    try {
      MetadataResponse v1 = transport.getPlain(metadataEndpoint);
      if (v1.status >= 200 && v1.status < 300) {
        log.debug("IMDSv1 fallback probe succeeded (status={}).", v1.status);
        return Boolean.TRUE;
      }
    } catch (IOException e) {
      log.debug("IMDSv1 fallback probe failed: {}", e.getMessage());
    }

    return Boolean.FALSE;
  }

  private static MetadataTransport defaultTransport() {
    return new HttpUrlConnectionMetadataTransport(
        DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
  }

  // ------------------------------------------------------------------
  // Transport abstraction
  // ------------------------------------------------------------------

  /** Minimal HTTP transport abstraction used by the IMDS probe. */
  interface MetadataTransport {
    TokenResponse putForToken(String endpoint, String ttlSeconds) throws IOException;

    MetadataResponse getWithToken(String endpoint, String token) throws IOException;

    MetadataResponse getPlain(String endpoint) throws IOException;
  }

  /** Holder for the token PUT response. */
  static final class TokenResponse {
    final int status;
    final String body;

    TokenResponse(int status, String body) {
      this.status = status;
      this.body = body == null ? "" : body;
    }
  }

  /** Holder for a metadata GET response. */
  static final class MetadataResponse {
    final int status;
    final String body;

    MetadataResponse(int status, String body) {
      this.status = status;
      this.body = body == null ? "" : body;
    }
  }

  /**
   * Default {@link MetadataTransport} backed by {@link HttpURLConnection}. Kept package
   * private so tests can stub it.
   */
  static final class HttpUrlConnectionMetadataTransport implements MetadataTransport {
    final int connectTimeoutMs;
    final int readTimeoutMs;

    HttpUrlConnectionMetadataTransport(int connectTimeoutMs, int readTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs;
      this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public TokenResponse putForToken(String endpoint, String ttlSeconds) throws IOException {
      HttpURLConnection conn = openConnection(endpoint);
      try {
        conn.setRequestMethod("PUT");
        conn.setRequestProperty(HEADER_TOKEN_TTL, ttlSeconds);
        conn.setDoOutput(true);
        // Empty body.
        conn.getOutputStream().close();
        int status = conn.getResponseCode();
        String body = readBody(conn, status);
        return new TokenResponse(status, body);
      } finally {
        conn.disconnect();
      }
    }

    @Override
    public MetadataResponse getWithToken(String endpoint, String token) throws IOException {
      HttpURLConnection conn = openConnection(endpoint);
      try {
        conn.setRequestMethod("GET");
        conn.setRequestProperty(HEADER_TOKEN, token);
        int status = conn.getResponseCode();
        String body = readBody(conn, status);
        return new MetadataResponse(status, body);
      } finally {
        conn.disconnect();
      }
    }

    @Override
    public MetadataResponse getPlain(String endpoint) throws IOException {
      HttpURLConnection conn = openConnection(endpoint);
      try {
        conn.setRequestMethod("GET");
        int status = conn.getResponseCode();
        String body = readBody(conn, status);
        return new MetadataResponse(status, body);
      } finally {
        conn.disconnect();
      }
    }

    private HttpURLConnection openConnection(String endpoint) throws IOException {
      URL url = new URL(endpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(connectTimeoutMs);
      conn.setReadTimeout(readTimeoutMs);
      conn.setInstanceFollowRedirects(false);
      conn.setUseCaches(false);
      return conn;
    }

    private static String readBody(HttpURLConnection conn, int status) {
      InputStream stream = null;
      try {
        stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
          return "";
        }
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
          StringBuilder sb = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
            sb.append(line);
          }
          return sb.toString();
        }
      } catch (IOException ignored) {
        return "";
      } finally {
        if (stream != null) {
          try {
            stream.close();
          } catch (IOException ignored) {
            // ignore
          }
        }
      }
    }
  }
}
