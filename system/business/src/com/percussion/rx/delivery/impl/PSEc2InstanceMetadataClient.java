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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Probes the EC2 instance metadata service (IMDS) to determine whether the
 * host is running inside EC2. Tries IMDSv2 first (the default on Amazon Linux
 * 2023+ and most current AMIs) and falls back to IMDSv1 only when the token
 * endpoint is not available.
 *
 * <p>Concurrency: the first probe is performed by exactly one thread; all
 * concurrent callers block on a {@link CountDownLatch} until the result is
 * cached. {@link #resetCache()} clears the cache for tests / ops.
 */
public final class PSEc2InstanceMetadataClient {

  private static final Logger log = LogManager.getLogger(PSEc2InstanceMetadataClient.class);

  static final String DEFAULT_METADATA_ENDPOINT = "http://169.254.169.254";
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 1500;
  static final int DEFAULT_READ_TIMEOUT_MS = 1500;
  static final String DEFAULT_TOKEN_TTL_SECONDS = "21600";
  static final String HEADER_TOKEN_TTL = "X-aws-ec2-metadata-token-ttl-seconds";
  static final String HEADER_TOKEN = "X-aws-ec2-metadata-token";
  static final String TOKEN_PATH = "/latest/api/token";
  static final String METADATA_PATH = "/latest/meta-data/";
  static final int HTTP_STATUS_OK = 200;

  static final AtomicReference<MetadataTransport> TRANSPORT_OVERRIDE =
      new AtomicReference<>();

  private static final Object PROBE_LOCK = new Object();
  private static volatile Boolean cachedResult = null;
  private static volatile CountDownLatch probeLatch = new CountDownLatch(0);
  private static final AtomicInteger attemptCount = new AtomicInteger(0);

  private PSEc2InstanceMetadataClient() {
    // utility class
  }

  /**
   * @return {@code true} if the host is EC2; {@code false} otherwise
   *     (including on any error, timeout, or non-EC2 host).
   */
  public static boolean isEC2Instance() {
    Boolean cached = cachedResult;
    if (cached != null) {
      return cached;
    }
    CountDownLatch latch = probeLatch;
    try {
      synchronized (PROBE_LOCK) {
        if (cachedResult == null) {
          cachedResult = probeOnce();
        }
        latch.countDown();
      }
    } catch (Throwable t) {
      synchronized (PROBE_LOCK) {
        if (cachedResult == null) {
          cachedResult = Boolean.FALSE;
        }
        latch.countDown();
      }
      log.debug("IMDS EC2 probe failed: {}", t.getMessage());
    }
    // Other concurrent callers block on the original latch until this thread
    // publishes the result above.
    try {
      latch.await();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    return cachedResult != null ? cachedResult : false;
  }

  /** Clears the cached result. For tests / ops. */
  public static void resetCache() {
    synchronized (PROBE_LOCK) {
      cachedResult = null;
      probeLatch = new CountDownLatch(1);
    }
  }

  /** Test hook to override the transport. Pass {@code null} to restore. */
  static void setTransportForTests(MetadataTransport transport) {
    synchronized (PROBE_LOCK) {
      cachedResult = null;
      probeLatch = new CountDownLatch(1);
      TRANSPORT_OVERRIDE.set(transport);
    }
  }

  private static Boolean probeOnce() {
    MetadataTransport transport = TRANSPORT_OVERRIDE.get();
    if (transport == null) {
      transport = new HttpUrlConnectionMetadataTransport(
          DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    int attempt = attemptCount.incrementAndGet();
    if (attempt > 1) {
      log.debug("IMDS EC2 probe attempt #{}", attempt);
    }

    String tokenEndpoint = DEFAULT_METADATA_ENDPOINT + TOKEN_PATH;
    String metadataEndpoint = DEFAULT_METADATA_ENDPOINT + METADATA_PATH;

    try {
      TokenResponse tokenResponse =
          transport.putForToken(tokenEndpoint, DEFAULT_TOKEN_TTL_SECONDS);
      if (tokenResponse.status == HTTP_STATUS_OK
          && StringUtils.isNotBlank(tokenResponse.body)) {
        MetadataResponse metadata =
            transport.getWithToken(metadataEndpoint, tokenResponse.body);
        if (metadata.status >= 200 && metadata.status < 300) {
          log.debug("IMDSv2 probe succeeded (status={}).", metadata.status);
          return Boolean.TRUE;
        }
        log.debug("IMDSv2 metadata GET failed with status {}; falling back to IMDSv1.",
            metadata.status);
      } else {
        log.debug("IMDSv2 token PUT returned status {}; falling back to IMDSv1.",
            tokenResponse.status);
      }
    } catch (IOException e) {
      log.debug("IMDSv2 token PUT failed: {}", e.getMessage());
    }

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

  // ------------------------------------------------------------------
  // Transport abstraction
  // ------------------------------------------------------------------

  interface MetadataTransport {
    TokenResponse putForToken(String endpoint, String ttlSeconds) throws IOException;

    MetadataResponse getWithToken(String endpoint, String token) throws IOException;

    MetadataResponse getPlain(String endpoint) throws IOException;
  }

  static final class TokenResponse {
    final int status;
    final String body;

    TokenResponse(int status, String body) {
      this.status = status;
      this.body = body == null ? "" : body;
    }
  }

  static final class MetadataResponse {
    final int status;
    final String body;

    MetadataResponse(int status, String body) {
      this.status = status;
      this.body = body == null ? "" : body;
    }
  }

  /**
   * Default {@link MetadataTransport} backed by {@link HttpURLConnection}.
   * Package private so tests can stub it.
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
