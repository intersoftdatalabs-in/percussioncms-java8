/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.util.Timeout;

/**
 * Apache HttpClient 5 replacement for Shindig's {@code BasicHttpFetcher}, removing runtime reliance
 * on EOL {@code commons-httpclient:3.1}.
 */
@Singleton
public class BasicHttpFetcher implements HttpFetcher {

  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
  private static final int DEFAULT_MAX_OBJECT_SIZE = 0;
  private static final int BUFFER_SIZE = 4096;

  private static final Logger LOG = Logger.getLogger(BasicHttpFetcher.class.getName());

  private volatile int maxObjSize;
  private volatile int connectionTimeoutMs;

  public BasicHttpFetcher(int maxObjSize, int connectionTimeoutMs) {
    setMaxObjectSizeBytes(maxObjSize);
    setConnectionTimeoutMs(connectionTimeoutMs);
  }

  public BasicHttpFetcher() {
    this(DEFAULT_MAX_OBJECT_SIZE, DEFAULT_CONNECT_TIMEOUT_MS);
  }

  @Inject(optional = true)
  public void setMaxObjectSizeBytes(
      @Named("shindig.http.client.max-object-size-bytes") int maxObjSize) {
    this.maxObjSize = maxObjSize;
  }

  @Inject(optional = true)
  public void setConnectionTimeoutMs(
      @Named("shindig.http.client.connection-timeout-ms") int connectionTimeoutMs) {
    if (connectionTimeoutMs <= 0) {
      throw new IllegalArgumentException("connection-timeout-ms must be greater than 0");
    }
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  @Override
  public HttpResponse fetch(HttpRequest request) {
    String methodType = request.getMethod();
    URI targetUri = request.getUri().toJavaUri();
    HttpUriRequestBase httpMethod = new HttpUriRequestBase(methodType, targetUri);

    RequestConfig.Builder reqConfigBuilder =
        RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(connectionTimeoutMs))
            .setRedirectsEnabled(request.getFollowRedirects());

    ProxySelector proxySelector = ProxySelector.getDefault();
    if (proxySelector != null) {
      List<Proxy> proxies = proxySelector.select(targetUri);
      if (proxies != null && !proxies.isEmpty()) {
        Proxy proxy = proxies.get(0);
        if (proxy.type() == Proxy.Type.HTTP && proxy.address() instanceof InetSocketAddress) {
          InetSocketAddress address = (InetSocketAddress) proxy.address();
          reqConfigBuilder.setProxy(new HttpHost(address.getHostString(), address.getPort()));
        }
      }
    }

    for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
      String headerName = entry.getKey();
      if ("Host".equalsIgnoreCase(headerName) || "Content-Length".equalsIgnoreCase(headerName)) {
        continue;
      }
      if (entry.getValue() != null) {
        for (String value : entry.getValue()) {
          httpMethod.addHeader(headerName, value);
        }
      }
    }

    if ("POST".equalsIgnoreCase(methodType) || "PUT".equalsIgnoreCase(methodType)) {
      InputStream postBody = request.getPostBody();
      if (postBody != null || request.getPostBodyLength() > 0) {
        try {
          byte[] payload = readBoundedBytes(postBody, 0);
          ContentType contentType = null;
          if (request.getContentType() != null && !request.getContentType().isEmpty()) {
            contentType = ContentType.parseLenient(request.getContentType());
          }
          httpMethod.setEntity(new ByteArrayEntity(payload, contentType));
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Failed to read Shindig request body", e);
          return HttpResponse.error();
        }
      }
    }

    ConnectionConfig connConfig =
        ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeoutMs))
            .setSocketTimeout(Timeout.ofMilliseconds(connectionTimeoutMs))
            .build();

    try (CloseableHttpClient client =
        HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(connConfig)
                    .build())
            .setDefaultRequestConfig(reqConfigBuilder.build())
            .build()) {
      return client.execute(
          httpMethod,
          response -> {
            int statusCode = response.getCode();
            HttpResponseBuilder builder = new HttpResponseBuilder().setHttpStatusCode(statusCode);
            for (Header header : response.getHeaders()) {
              builder.addHeader(header.getName(), header.getValue());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
              try (InputStream content = entity.getContent()) {
                if (content != null) {
                  byte[] bytes = readBoundedBytes(content, maxObjSize);
                  if (bytes == null) {
                    return HttpResponse.error();
                  }
                  builder.setResponseNoCopy(bytes);
                }
              }
            }
            return builder.create();
          });
    } catch (SocketTimeoutException | SocketException e) {
      LOG.log(Level.FINE, "Timeout or socket error fetching " + targetUri, e);
      return HttpResponse.timeout();
    } catch (IOException e) {
      LOG.log(Level.INFO, "Error fetching " + targetUri, e);
      return HttpResponse.error();
    }
  }

  private byte[] readBoundedBytes(InputStream input, int limit) throws IOException {
    if (input == null) {
      return new byte[0];
    }
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[BUFFER_SIZE];
    int total = 0;
    int read;
    while ((read = input.read(chunk)) != -1) {
      total += read;
      if (limit > 0 && total > limit) {
        return null;
      }
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
  }
}
