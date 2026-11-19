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
package com.percussion.delivery.client;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.error.PSExceptionUtils;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.IPSProxyConfigService;
import com.percussion.proxyconfig.service.PSProxyConfigServiceLocator;
import com.percussion.security.TLSSocketFactory;
import com.percussion.server.PSServer;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP client used to communicate with Percussion Delivery Tier Services (DTS).
 *
 * @author wesleyhirsch
 */
public class PSDeliveryClient implements IPSDeliveryClient, Closeable {

  public static final String PERC_VERSION_HEADER = "perc-version";
  public static final String TOMCAT_USER = "tomcat-user";
  public static final String TOMCAT_PASSWORD = "tomcat-password";

  /** License Override. */
  private String licenseOverride = "";

  /** Logger for this service. */
  public static final Logger log = LogManager.getLogger(PSDeliveryClient.class);

  /** Maximum number of consecutive transport failures before assuming DTS is offline. */
  private static final int MAX_FAILURES = 30;

  /**
   * The request message body content type used as a fallback when no explicit content type is set.
   */
  private static final String DEFAULT_REQUEST_CONTENT_TYPE = MediaType.APPLICATION_JSON;

  /** Shared Jackson ObjectMapper. Created once per class; thread-safe per Jackson docs. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** The number of times an idempotent request will be retried on I/O error. */
  private int retryCount = 3;

  /** Offline detection flag: stops error spam once failureCount exceeds MAX_FAILURES. */
  private boolean offline;

  /** Number of consecutive transport failures reported. */
  private int failureCount;

  /** Timeout until a connection is established in milliseconds. */
  private int connectionTimeout = 300000;

  /** Socket / response timeout in milliseconds. */
  private int operationTimeout = 300000;

  /** HTTP status codes treated as successful for the current request. */
  private Set<Integer> successfulHttpStatusCodes =
      new HashSet<>(Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT));

  private HttpMethodType requestType;

  private String userName;

  private String authSecret;

  private Object requestMessageBody;

  private String requestUrl;

  private URI targetUri;

  private String targetRealm;

  private boolean allowSelfSignedCertificate;

  private boolean sslEnabled;

  private String responseMessageBodyContentType = MediaType.APPLICATION_JSON;

  private final String requestMessageBodyEncoding = StandardCharsets.UTF_8.name();

  private IPSProxyConfigService proxyConfigService;

  private PSProxyConfig proxyConfig = null;

  private CloseableHttpClient plainHttpClient;
  private CloseableHttpClient strictTlsHttpClient;
  private CloseableHttpClient selfSignedTlsHttpClient;

  /**
   * Creates an instance with default options for connection and operation timeouts.
   *
   * <p>The instance should be closed when it is no longer necessary. See {@link
   * PSDeliveryClient#close} for more information.
   */
  public PSDeliveryClient() {
    this(3, 300000, 300000);
  }

  /**
   * Creates an instance with the specified retry count, connection timeout, and operation timeout.
   *
   * <p>The instance should be closed when it is no longer necessary. See {@link
   * PSDeliveryClient#close} for more information.
   *
   * @param retryCount the number of retries on failure, must not be less than zero.
   * @param connectionTimeout the timeout until a connection is established in milliseconds. Must
   *     not be less than zero.
   * @param operationTimeout the socket timeout for each operation in milliseconds. Must not be less
   *     than zero.
   */
  public PSDeliveryClient(int retryCount, int connectionTimeout, int operationTimeout) {
    if (retryCount < 0) {
      throw new IllegalArgumentException("retryCount must not be < 0.");
    }
    if (connectionTimeout < 0) {
      throw new IllegalArgumentException("connectionTimeout must not be < 0.");
    }
    if (operationTimeout < 0) {
      throw new IllegalArgumentException("operationTimeout must not be < 0.");
    }

    this.retryCount = retryCount;
    this.connectionTimeout = connectionTimeout;
    this.operationTimeout = operationTimeout;
  }

  /**
   * @return the offline flag
   */
  public boolean isOffline() {
    return offline;
  }

  /**
   * @param offline the offline flag to set
   */
  public void setOffline(boolean offline) {
    this.offline = offline;
  }

  /**
   * @return the failureCount
   */
  public int getFailureCount() {
    return failureCount;
  }

  /**
   * @param failureCount the failureCount to set
   */
  public void setFailureCount(int failureCount) {
    this.failureCount = failureCount;
  }

  /**
   * Shuts down the HTTP connection pools managed by this {@code PSDeliveryClient} instance. This
   * method should be called when this instance is no longer used.
   *
   * <p>Calling this method more than once will have no effect.
   */
  @Override
  public void close() {
    closeQuietly(plainHttpClient);
    plainHttpClient = null;
    closeQuietly(strictTlsHttpClient);
    strictTlsHttpClient = null;
    closeQuietly(selfSignedTlsHttpClient);
    selfSignedTlsHttpClient = null;
  }

  private void closeQuietly(CloseableHttpClient client) {
    if (client != null) {
      try {
        client.close();
      } catch (IOException e) {
        log.debug("Error closing delivery HTTP client: {}", PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  /**
   * @return A JsonNode of the received data.
   */
  private JsonNode getJson() {
    try {
      return MAPPER.readTree(pushOrGet(MediaType.APPLICATION_JSON));
    } catch (JsonProcessingException e) {
      throw new PSDeliveryClientException("Error parsing delivery response: " + e.getMessage(), e);
    }
  }

  /**
   * A low level function that sends some content using the corresponding HTTP method (according to
   * {@code requestType}), gets the response and returns it.
   *
   * @param contentType Content Type
   * @return A string returned when the HTTP method is executed.
   */
  private String pushOrGet(String contentType) {
    String response;
    this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;

    switch (this.requestType) {
      case GET:
        response = executeGetMethod();
        break;
      case DELETE:
        response = executeDeleteMethod();
        break;
      case POST:
        if (this.requestMessageBody == null) {
          log.warn("Executing Post Method with null body.  This is probably not what you intended.");
        }
        response = executePostMethod(contentType);
        break;
      case PUT:
        if (this.requestMessageBody == null) {
          log.warn("Executing Put Method with null body.  This is probably not what you intended.");
        }
        response = executePutMethod(contentType);
        break;
      default:
        throw new PSDeliveryClientException("Method " + this.requestType + " not implemented.");
    }

    if (isBlank(response)) {
      response = StringUtils.EMPTY;
    }

    return response;
  }

  private ObjectNode getJsonObject() {
    JsonNode obj;
    try {
      obj = getJson();
    } catch (Exception ex) {
      throw new PSDeliveryClientException("Error in executing the HTTP method: " + ex.getMessage());
    }

    if (obj == null || obj.isNull() || obj.isMissingNode()) {
      return MAPPER.createObjectNode();
    }
    if (!(obj instanceof ObjectNode)) {
      throw new PSDeliveryClientException(
          "Expected JSONObject, got ".concat(obj.getClass().toString()));
    }

    return (ObjectNode) obj;
  }

  @Override
  public ObjectNode getJsonObject(PSDeliveryActionOptions actionOptions) {
    prepare(actionOptions, null);
    return getJsonObject();
  }

  @Override
  public ObjectNode getJsonObject(
      PSDeliveryActionOptions actionOptions, Object requestMessageBody) {
    prepare(actionOptions, requestMessageBody);
    return getJsonObject();
  }

  private ArrayNode getJsonArray() {
    JsonNode obj;
    try {
      obj = getJson();
    } catch (Exception ex) {
      throw new PSDeliveryClientException("Error in executing the HTTP method: " + ex.getMessage());
    }

    if (obj == null || obj.isNull() || obj.isMissingNode()) {
      return MAPPER.createArrayNode();
    }
    if (!(obj instanceof ArrayNode)) {
      throw new PSDeliveryClientException(
          "Expected JSONArray, got ".concat(obj.getClass().toString()));
    }

    return (ArrayNode) obj;
  }

  @Override
  public ArrayNode getJsonArray(PSDeliveryActionOptions actionOptions) {
    prepare(actionOptions, null);
    return getJsonArray();
  }

  @Override
  public ArrayNode getJsonArray(PSDeliveryActionOptions actionOptions, Object requestMessageBody) {
    prepare(actionOptions, requestMessageBody);
    return getJsonArray();
  }

  @Override
  public void push(
      PSDeliveryActionOptions actionOptions,
      String requestMessageBodyContentType,
      Object requestMessageBody) {
    prepare(actionOptions, requestMessageBody);

    String mediaType =
        isNotBlank(requestMessageBodyContentType)
            ? requestMessageBodyContentType
            : MediaType.APPLICATION_JSON;

    pushOrGet(mediaType);
  }

  @Override
  public void push(PSDeliveryActionOptions actionOptions, Object requestMessageBody) {
    push(actionOptions, null, requestMessageBody);
  }

  @Override
  public String getString(PSDeliveryActionOptions actionOptions) {
    prepare(actionOptions, null);
    return pushOrGet(MediaType.APPLICATION_JSON);
  }

  /**
   * Prepares state to run the query against the remote delivery server service.
   *
   * @param actionOptions The PSDeliveryActionOptions object.
   * @param requestMessageBody The request message body.
   */
  private void prepare(PSDeliveryActionOptions actionOptions, Object requestMessageBody) {
    if (actionOptions.getDeliveryInfo() == null) {
      String error = "Error getting info from delivery config file";
      log.error(error);
      throw new PSDeliveryClientException(error);
    }

    this.sslEnabled = isSslEnabled(actionOptions);
    PSDeliveryInfo server = actionOptions.getDeliveryInfo();
    this.allowSelfSignedCertificate = Boolean.TRUE.equals(server.getAllowSelfSignedCertificate());
    this.targetRealm = server.getRealm();

    this.requestType = actionOptions.getHttpMethod();
    this.requestMessageBody = requestMessageBody;

    this.successfulHttpStatusCodes =
        new HashSet<>(Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT));
    if (actionOptions.getSuccessfullHttpStatusCodes() != null
        && !actionOptions.getSuccessfullHttpStatusCodes().isEmpty()) {
      this.successfulHttpStatusCodes.addAll(actionOptions.getSuccessfullHttpStatusCodes());
    }

    if (HttpMethodType.GET.equals(this.requestType) && requestMessageBody != null) {
      throw new IllegalArgumentException(
          "Attempting to execute GET method with message body.  Body is: " + requestMessageBody);
    }

    this.requestUrl = processUrl(actionOptions);
    this.targetUri = parseConfigUri(this.requestUrl);

    String authUser = server.getUsername();
    String serverSecret = server.getPassword();
    this.userName = authUser;
    this.authSecret = serverSecret;

    if (isNotBlank(authUser) && isBlank(serverSecret)) {
      log.warn(
          "Executing HTTP request with username but blank password.  This is probably not what you intended.");
    }

    String protocol = this.targetUri.getScheme();
    if (proxyConfig == null) {
      if (this.proxyConfigService == null) {
        this.proxyConfigService = getProxyConfigService();
      }
      if (this.proxyConfigService != null) {
        this.proxyConfig = proxyConfigService.findByProtocol(protocol);
      } else {
        this.proxyConfig = new PSProxyConfig();
      }
    }
  }

  private URI parseConfigUri(String rawUri) {
    try {
      return new URI(rawUri.replace(" ", "%20"));
    } catch (URISyntaxException | NullPointerException e) {
      log.error("Error getting info from delivery config file");
      throw new PSDeliveryClientException("Error getting info from delivery config file", e);
    }
  }

  /**
   * Process the delivery server host URL and returns the target request URL according to admin vs
   * public selection.
   *
   * @param actionOptions The PSDeliveryActionOptions object.
   * @return The processed URL of the delivery service.
   */
  private String processUrl(PSDeliveryActionOptions actionOptions) {
    PSDeliveryInfo server = actionOptions.getDeliveryInfo();
    String actionUrl = actionOptions.getActionUrl();
    String finalUrl = "";

    try {
      String baseRaw = actionOptions.isAdminOperation() ? server.getAdminUrl() : server.getUrl();
      URI baseUri = new URI(baseRaw.replace(" ", "%20"));

      String deliveryHost = baseUri.getHost();
      String protocol = baseUri.getScheme();
      String port = baseUri.getPort() <= 0 ? "" : ":" + baseUri.getPort();

      finalUrl =
          protocol
              + "://"
              + deliveryHost
              + port
              + (actionUrl.startsWith("/") ? StringUtils.EMPTY : "/")
              + actionUrl;

      URI parsed = new URI(finalUrl.replace(" ", "%20"));
      return parsed.toASCIIString();
    } catch (URISyntaxException | NullPointerException e) {
      log.error("Error parsing URL: {}", finalUrl);
      throw new PSDeliveryClientException("Error parsing URL: " + finalUrl, e);
    }
  }

  private String executeGetMethod() {
    if (isBlank(this.responseMessageBodyContentType)) {
      this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;
    }

    HttpGet getMethod = new HttpGet(this.requestUrl);
    getMethod.setHeader(HttpHeaders.CONTENT_TYPE, this.responseMessageBodyContentType);

    return this.executeHttpMethod(getMethod);
  }

  private String executeDeleteMethod() {
    if (isBlank(this.responseMessageBodyContentType)) {
      this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;
    }

    HttpDelete deleteMethod = new HttpDelete(this.requestUrl);
    return this.executeHttpMethod(deleteMethod);
  }

  private String executePutMethod(String contentType) {
    HttpPut putMethod = new HttpPut(this.requestUrl);
    return this.executeEntityEnclosingMethod(putMethod, contentType);
  }

  private String executePostMethod(String contentType) {
    HttpPost postMethod = new HttpPost(this.requestUrl);

    if (this.requestMessageBody instanceof Collection<?>) {
      try {
        List<NameValuePair> pairs = new ArrayList<>();
        for (Object item : (Collection<?>) this.requestMessageBody) {
          pairs.add((NameValuePair) item);
        }
        postMethod.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
      } catch (Exception ex) {
        String errorMessage = "Error in trying to set the request body for the POST method";
        log.error(errorMessage);
        throw new PSDeliveryClientException(errorMessage);
      }
      return this.executeHttpMethod(postMethod);
    }

    return this.executeEntityEnclosingMethod(postMethod, contentType);
  }

  private String executeEntityEnclosingMethod(HttpUriRequestBase httpMethod, String contentType) {
    try {
      if (this.requestMessageBody != null) {
        ContentType resolvedType = parseContentType(contentType);
        if (this.requestMessageBody instanceof String) {
          httpMethod.setEntity(
              new StringEntity((String) this.requestMessageBody, resolvedType));
        } else if (this.requestMessageBody instanceof InputStream) {
          httpMethod.setEntity(
              new InputStreamEntity((InputStream) this.requestMessageBody, resolvedType));
        } else {
          httpMethod.setEntity(
              new StringEntity(this.requestMessageBody.toString(), ContentType.APPLICATION_JSON));
        }
      }
    } catch (Exception e) {
      String errorMessage = "Error in trying to set the request body for the HTTP method";
      log.error(errorMessage, e);
      throw new PSDeliveryClientException(errorMessage);
    }

    return this.executeHttpMethod(httpMethod);
  }

  private ContentType parseContentType(String mediaType) {
    if (isBlank(mediaType)) {
      return ContentType.APPLICATION_JSON;
    }
    try {
      ContentType parsed = ContentType.parse(mediaType);
      return parsed.getCharset() != null ? parsed : parsed.withCharset(StandardCharsets.UTF_8);
    } catch (Exception e) {
      return ContentType.create(mediaType, requestMessageBodyEncoding);
    }
  }

  private CloseableHttpClient resolveHttpClient() {
    if (!sslEnabled) {
      if (plainHttpClient == null) {
        plainHttpClient = buildHttpClient(null);
      }
      return plainHttpClient;
    }
    if (allowSelfSignedCertificate) {
      if (selfSignedTlsHttpClient == null) {
        selfSignedTlsHttpClient = buildHttpClient(createSelfSignedTlsStrategy());
      }
      return selfSignedTlsHttpClient;
    }
    if (strictTlsHttpClient == null) {
      try {
        strictTlsHttpClient = buildHttpClient(new TLSSocketFactory().createTlsSocketStrategy());
      } catch (GeneralSecurityException e) {
        throw new PSDeliveryClientException("Error initializing TLS socket factory", e);
      }
    }
    return strictTlsHttpClient;
  }

  private CloseableHttpClient buildHttpClient(TlsSocketStrategy tlsSocketStrategy) {
    ConnectionConfig connConfig =
        ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
            .setSocketTimeout(Timeout.ofMilliseconds(operationTimeout))
            .build();

    PoolingHttpClientConnectionManagerBuilder cmBuilder =
        PoolingHttpClientConnectionManagerBuilder.create().setDefaultConnectionConfig(connConfig);
    if (tlsSocketStrategy != null) {
      cmBuilder.setTlsSocketStrategy(tlsSocketStrategy);
    }

    return HttpClients.custom()
        .setConnectionManager(cmBuilder.build())
        .setRetryStrategy(
            new DefaultHttpRequestRetryStrategy(retryCount, TimeValue.ZERO_MILLISECONDS))
        .build();
  }

  private TlsSocketStrategy createSelfSignedTlsStrategy() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] {new EasyX509TrustManager(null)}, null);
      ClientTlsStrategyBuilder builder =
          ClientTlsStrategyBuilder.create()
              .setSslContext(sslContext)
              .setHostnameVerifier(NoopHostnameVerifier.INSTANCE);

      String protString = System.getProperty("jdk.tls.client.protocols");
      if (protString == null) {
        protString = System.getProperty("https.protocols");
      }
      if (protString != null) {
        String[] raw = protString.split(",");
        String[] cleaned = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
          cleaned[i] = raw[i].trim();
        }
        builder.setTlsVersions(cleaned);
      }
      return builder.buildClassic();
    } catch (GeneralSecurityException e) {
      throw new PSDeliveryClientException("Error initializing self-signed TLS context", e);
    }
  }

  /**
   * Executes a prepared HC5 request using configured credentials, proxy settings, headers, and
   * offline failure tracking.
   *
   * @param httpMethod The prepared request object.
   * @return The response body string, empty string for {@code 204}, or {@code null} when offline.
   */
  private String executeHttpMethod(HttpUriRequestBase httpMethod) {
    if (httpMethod.getFirstHeader(HttpHeaders.CONTENT_TYPE) != null
        && isBlank(httpMethod.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue())) {
      httpMethod.setHeader(HttpHeaders.CONTENT_TYPE, DEFAULT_REQUEST_CONTENT_TYPE);
    }

    RequestConfig.Builder requestConfigBuilder =
        RequestConfig.custom().setResponseTimeout(Timeout.ofMilliseconds(operationTimeout));
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

    if (StringUtils.isNotEmpty(this.userName)) {
      char[] passChars = this.authSecret != null ? this.authSecret.toCharArray() : new char[0];
      AuthScope authScope =
          (this.targetUri != null && this.targetUri.getHost() != null)
              ? new AuthScope(
                  null,
                  this.targetUri.getHost(),
                  this.targetUri.getPort(),
                  this.targetRealm,
                  null)
              : new AuthScope(null, -1);
      credsProvider.setCredentials(
          authScope, new UsernamePasswordCredentials(this.userName, passChars));
    }

    if (proxyConfig != null && proxyConfig.getHost() != null && proxyConfig.getPort() != null) {
      int proxyPort = Integer.parseInt(proxyConfig.getPort());
      requestConfigBuilder.setProxy(new HttpHost(proxyConfig.getHost(), proxyPort));

      if (proxyConfig.getUser() != null && proxyConfig.getPassword() != null) {
        credsProvider.setCredentials(
            new AuthScope(proxyConfig.getHost(), proxyPort),
            new UsernamePasswordCredentials(
                proxyConfig.getUser(), proxyConfig.getPassword().toCharArray()));
      }
    }

    httpMethod.setHeader(PERC_VERSION_HEADER, PSServer.getVersion());
    httpMethod.setHeader(TOMCAT_USER, this.userName);
    httpMethod.setHeader(TOMCAT_PASSWORD, this.authSecret);

    HttpClientContext context = HttpClientContext.create();
    context.setRequestConfig(requestConfigBuilder.build());
    context.setCredentialsProvider(credsProvider);

    try {
      HttpResponseResult result =
          resolveHttpClient()
              .execute(
                  httpMethod,
                  context,
                  response -> {
                    int statusCode = response.getCode();
                    HttpEntity entity = response.getEntity();
                    String responseData = "";
                    if (entity != null) {
                      try (InputStream is = entity.getContent()) {
                        responseData =
                            is == null ? "" : IOUtils.toString(is, StandardCharsets.UTF_8);
                      }
                    }
                    return new HttpResponseResult(statusCode, responseData);
                  });

      if (!successfulHttpStatusCodes.contains(result.statusCode)) {
        failureCount = 0;
        offline = false;
        String methodLabel = this.requestType == null ? "?" : this.requestType.name();
        String currentUrl = this.requestUrl == null ? "?" : this.requestUrl;
        String snippet =
            PSDeliveryHttpErrorSupport.firstLineSnippet(
                result.body, PSDeliveryHttpErrorSupport.DEFAULT_BODY_SNIPPET_MAX);
        String msg;
        if (result.statusCode == HttpStatus.SC_UNAUTHORIZED) {
          msg =
              String.format(
                  "Authentication error. Check user and password for this delivery server. %s %s (HTTP %d)%s",
                  methodLabel,
                  currentUrl,
                  result.statusCode,
                  snippet.isEmpty() ? "" : ": " + snippet);
        } else {
          msg =
              PSDeliveryHttpErrorSupport.formatExecutionError(
                  methodLabel, currentUrl, result.statusCode, result.body);
        }
        log.error(msg);
        throw new PSDeliveryClientException(
            msg, result.statusCode, methodLabel, currentUrl, snippet);
      }

      if (result.statusCode == HttpStatus.SC_NO_CONTENT) {
        return "";
      }
      return result.body;
    } catch (IOException ex) {
      failureCount++;
      if (failureCount > MAX_FAILURES) {
        offline = true;
        log.info("Delivery Services are unavailable.  Suppressing further messages.");
      }
      if (!offline) {
        log.error("Fatal transport error: {}", ex.getMessage());
      }

      String methodLabel = this.requestType == null ? "?" : this.requestType.name();
      String fullRequestUrl = this.requestUrl == null ? "?" : this.requestUrl;
      String reqUrl = fullRequestUrl;
      try {
        URL url = new URL(reqUrl);
        reqUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
      } catch (MalformedURLException e) {
        if (!offline) {
          log.error(e.getLocalizedMessage());
        }
      }
      if (!offline) {
        throw new PSDeliveryClientException(
            "Unable to connect to delivery server at: " + reqUrl + ".",
            -1,
            methodLabel,
            fullRequestUrl,
            null);
      }
      return null;
    }
  }

  private static final class HttpResponseResult {
    final int statusCode;
    final String body;

    HttpResponseResult(int statusCode, String body) {
      this.statusCode = statusCode;
      this.body = body;
    }
  }

  private boolean isSslEnabled(PSDeliveryActionOptions actionOptions) {
    PSDeliveryInfo server = actionOptions.getDeliveryInfo();
    try {
      String raw = isNotBlank(server.getAdminUrl()) ? server.getAdminUrl() : server.getUrl();
      URI uri = new URI(raw.replace(" ", "%20"));
      return "https".equalsIgnoreCase(uri.getScheme());
    } catch (URISyntaxException | NullPointerException e) {
      log.error("Error getting info from delivery config file");
      throw new PSDeliveryClientException("Error getting info from delivery config file", e);
    }
  }

  /**
   * When set, requests sent to the delivery tier use the supplied license number instead of the
   * primary instance license id.
   *
   * <p>This property will be automatically cleared after method execution to prevent accidental
   * override.
   *
   * @param licenseOverride the licenseOverride to set
   */
  public void setLicenseOverride(String licenseOverride) {
    this.licenseOverride = licenseOverride;
  }

  /**
   * Gets the corresponding proxy config bean for the service.
   *
   * @return ProxyConfigService bean. May be {@code null} if bean is not found.
   */
  private IPSProxyConfigService getProxyConfigService() {
    try {
      return PSProxyConfigServiceLocator.getProxyConfigService();
    } catch (PSMissingBeanConfigurationException e) {
      return null;
    }
  }

  public void setProxyConfig(PSProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  public PSProxyConfig getProxyConfig() {
    return this.proxyConfig;
  }
}
