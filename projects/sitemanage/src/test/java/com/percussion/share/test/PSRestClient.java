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
package com.percussion.share.test;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Wrapper around Apache HttpClient 5 for REST services.
 *
 * <p>Most of the methods are protected as this class should be extended to provide more specific
 * behavior.
 *
 * @author adamgent
 * @see #GET(String)
 * @see #POST(String, String)
 * @see #DELETE(String)
 */
public class PSRestClient {
  private String url;
  private Map<String, String> requestHeaders = new HashMap<>();
  private String postContentType = "text/xml";

  private final BasicCookieStore cookieStore = new BasicCookieStore();
  private final CloseableHttpClient client = createHttpClient();

  private CloseableHttpClient createHttpClient() {
    try {
      SSLContext sslContext =
          SSLContexts.custom().loadTrustMaterial(TrustAllStrategy.INSTANCE).build();
      DefaultClientTlsStrategy tlsStrategy =
          new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
      PoolingHttpClientConnectionManager cm =
          PoolingHttpClientConnectionManagerBuilder.create()
              .setTlsSocketStrategy(tlsStrategy)
              .build();
      RequestConfig defaultConfig =
          RequestConfig.custom().setCookieSpec(StandardCookieSpec.RELAXED).build();
      return HttpClients.custom()
          .setConnectionManager(cm)
          .setDefaultCookieStore(cookieStore)
          .setDefaultRequestConfig(defaultConfig)
          .build();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to initialize PSRestClient TLS context", e);
    }
  }

  public List<String> parseAcceptHeader(String acceptHeader) {
    return new ArrayList<>(asList(acceptHeader.split(",")));
  }

  public String outputAcceptHeader(List<String> accepts) {
    return StringUtils.join(accepts, ",");
  }

  protected void addAccept(String mime) {
    List<String> accepts = parseAcceptHeader(getAcceptHeader());
    accepts.add(mime);
    setAcceptHeader(outputAcceptHeader(accepts));
  }

  protected String getAcceptHeader() {
    String accept = getRequestHeaders().get("Accept");
    if (accept == null) return "";
    return accept;
  }

  protected void setAcceptHeader(String header) {
    notNull(header, "header");
    getRequestHeaders().put("Accept", header);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  protected CloseableHttpClient getClient() {
    return client;
  }

  /**
   * Performs an HTTP GET.
   *
   * @param path relative or fully qualified.
   * @param params never <code>null</code>.
   * @return never <code>null</code>.
   */
  protected String GET(String path, Collection<Entry<String, String>> params) {
    try {
      URI uri = new URIBuilder(getUri(path)).addParameters(nameValuePairs(params)).build();
      HttpGet method = new HttpGet(uri);
      setRequestHeaders(method);
      return executeMethod(method);
    } catch (URISyntaxException e) {
      throw new RestClientException("Bad url " + getUrl() + path, e);
    }
  }

  protected String GET(String path) {
    HttpGet method = new HttpGet(getUri(path));
    setRequestHeaders(method);
    return executeMethod(method);
  }

  protected InputStream GET_BINARY(String path) {
    HttpGet method = new HttpGet(getUri(path));
    setRequestHeaders(method);
    return executeBinaryMethod(method);
  }

  private InputStream executeBinaryMethod(HttpUriRequestBase method) throws RestClientException {
    try {
      String uri = method.getUri().toString();
      String name = method.getMethod();
      BinaryResult result =
          client.execute(
              method,
              response -> {
                int stat = response.getCode();
                HttpEntity entity = response.getEntity();
                byte[] bytes = entity == null ? new byte[0] : EntityUtils.toByteArray(entity);
                return new BinaryResult(stat, bytes);
              });
      log.trace("HTTP return code: " + result.status);
      if (log.isDebugEnabled()) {
        log.debug(format("{0} {1}  HTTP Stat:{2}", name, uri, "" + result.status));
      }
      if (!(200 <= result.status && result.status < 305)) {
        String errorBody = new String(result.bytes, StandardCharsets.UTF_8);
        String error =
            "URI: " + uri + " HTTP Error: " + result.status + " Response: \n" + errorBody;
        log.error(error);
        throw new RestClientException(result.status, uri, errorBody);
      }
      return new ByteArrayInputStream(result.bytes);
    } catch (URISyntaxException | IOException e) {
      throw new RestClientException(e);
    }
  }

  protected String POST(String path, String body) {
    return POST(path, body, getPostContentType());
  }

  protected String POST(String path, String body, String contentType) {
    HttpPost method = new HttpPost(getUri(path));
    setRequestHeaders(method);
    if (log.isTraceEnabled() && body != null) {
      log.trace("POST Body: " + body);
    }
    if (body != null) {
      ContentType ct = ContentType.create(contentType, StandardCharsets.UTF_8);
      method.setEntity(new StringEntity(body, ct));
    }
    return executeMethod(method);
  }

  protected String PUT(String path, String body) {
    return PUT(path, body, getPostContentType());
  }

  protected String PUT(String path, String body, String contentType) {
    HttpPut method = new HttpPut(getUri(path));
    setRequestHeaders(method);
    if (log.isTraceEnabled()) {
      log.trace("PUT Body: " + body);
    }
    ContentType ct = ContentType.create(contentType, StandardCharsets.UTF_8);
    method.setEntity(new StringEntity(body, ct));
    return executeMethod(method);
  }

  protected String POST(String path, Collection<Entry<String, String>> params) {
    HttpPost method = new HttpPost(getUri(path));
    setRequestHeaders(method);
    method.setEntity(new UrlEncodedFormEntity(nameValuePairs(params), StandardCharsets.UTF_8));
    return executeMethod(method);
  }

  protected String DELETE(String path) {
    HttpDelete method = new HttpDelete(getUri(path));
    setRequestHeaders(method);
    return executeMethod(method);
  }

  protected URI getUri(String relativePath) {
    notNull(relativePath);
    try {
      URI base = new URI(getUrl().replace(" ", "%20"));
      return base.resolve(relativePath.replace(" ", "%20"));
    } catch (URISyntaxException | NullPointerException e) {
      throw new RestClientException("Bad url " + getUrl() + relativePath, e);
    }
  }

  private List<NameValuePair> nameValuePairs(Collection<Entry<String, String>> params) {
    List<NameValuePair> list = new ArrayList<>();
    for (Entry<String, String> e : params) {
      list.add(new BasicNameValuePair(e.getKey(), e.getValue()));
    }
    return list;
  }

  private void setRequestHeaders(HttpUriRequestBase method) {
    for (Entry<String, String> e : getRequestHeaders().entrySet()) {
      method.setHeader(e.getKey(), e.getValue());
    }
  }

  public String concatPath(String start, String... end) {
    isTrue(isNotBlank(start), "start cannot be blank");
    notEmpty(end, "Must have end paths.");
    String path = start;
    for (String p : end) {
      path = removeEnd(path, "/") + "/" + removeStart(p, "/");
    }
    return path;
  }

  public String escapePath(String path) {
    try {
      return URLEncoder.encode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Should never happen", e);
    }
  }

  public String getRequestContentType() {
    return getRequestHeaders().get("Content-Type");
  }

  public void setRequestContentType(String contentType) {
    getRequestHeaders().put("Content-Type", contentType);
  }

  private String executeMethod(HttpUriRequestBase method) throws RestClientException {
    try {
      String uri = method.getUri().toString();
      String name = method.getMethod();
      StringResult result =
          client.execute(
              method,
              response -> {
                int stat = response.getCode();
                HttpEntity entity = response.getEntity();
                String body =
                    entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                return new StringResult(stat, body);
              });
      log.trace("HTTP return code: " + result.status);
      if (log.isDebugEnabled()) {
        log.debug(format("{0} {1}  HTTP Stat:{2}", name, uri, "" + result.status));
      }
      if (log.isTraceEnabled()) {
        log.trace("Response: " + result.body);
      }
      if (!(200 <= result.status && result.status < 305)) {
        String error =
            "URI: " + uri + " HTTP Error: " + result.status + " Response: \n" + result.body;
        log.error(error);
        throw new RestClientException(result.status, uri, result.body);
      }
      return result.body;
    } catch (URISyntaxException | IOException e) {
      throw new RestClientException(e);
    }
  }

  private static final class StringResult {
    final int status;
    final String body;

    StringResult(int status, String body) {
      this.status = status;
      this.body = body;
    }
  }

  private static final class BinaryResult {
    final int status;
    final byte[] bytes;

    BinaryResult(int status, byte[] bytes) {
      this.status = status;
      this.bytes = bytes;
    }
  }

  /**
   * Base exception for a REST failure.
   *
   * @author adamgent
   */
  public static class RestClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private int status = 0;
    private String uri = "";
    private String responseBody;
    private String message = null;

    public RestClientException(String message) {
      super(message);
    }

    public RestClientException(String message, Throwable cause) {
      super(message, cause);
    }

    public RestClientException(Throwable cause) {
      super(cause);
    }

    public RestClientException() {}

    public RestClientException(RestClientException cause) {
      setStatus(cause.getStatus());
      setUri(cause.getUri());
      setResponseBody(cause.getResponseBody());
      setMessage(cause.getMessage());
    }

    public RestClientException(int status, String uri, InputStream responseBody) {
      init(status, uri, null);
      setMessage(getRestErrorMessage());
      fillInStackTrace();
    }

    public RestClientException(int status, String uri, String responseBody) {
      init(status, uri, responseBody);
      setMessage(getRestErrorMessage());
      fillInStackTrace();
    }

    @Override
    public String getMessage() {
      if (message != null) {
        return this.message;
      }
      return super.getMessage();
    }

    protected void setMessage(String message) {
      this.message = message;
    }

    protected String getRestErrorMessage() {
      return format(
          "HTTP Error code: {0}\nURI: {1}\nResponse: {2}",
          Integer.valueOf(getStatus()), getUri(), getResponseBody());
    }

    protected void init(int status, String uri, String responseBody) {
      this.status = status;
      this.uri = uri;
      this.responseBody = responseBody;
    }

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    public int getStatus() {
      return status;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public String getResponseBody() {
      return responseBody;
    }

    public void setResponseBody(String responseBody) {
      this.responseBody = responseBody;
    }
  }

  /** The log instance to use for this class, never <code>null</code>. */
  protected static final Logger log = LogManager.getLogger(PSRestClient.class);

  public String getPostContentType() {
    return postContentType;
  }

  public void setPostContentType(String postContentType) {
    this.postContentType = postContentType;
  }

  public Map<String, String> getRequestHeaders() {
    return requestHeaders;
  }

  public void setRequestHeaders(Map<String, String> requestHeaders) {
    this.requestHeaders = requestHeaders;
  }
}
