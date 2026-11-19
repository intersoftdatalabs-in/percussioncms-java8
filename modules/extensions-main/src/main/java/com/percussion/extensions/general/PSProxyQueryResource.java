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

package com.percussion.extensions.general;

import static com.percussion.utils.request.PSRequestInfoBase.KEY_JSESSIONID;
import static com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument;
import static java.util.Arrays.asList;

import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.extensions.utils.PSExtensionParamsHelper;
import com.percussion.security.validation.URLValidation;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.utils.request.PSRequestInfo;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This exit allows you to make an internal request to an external resource. <br>
 * The request parameters that are non-null are copied to the provided url that is to be called
 * using HTTP GET.
 *
 * <p>The results of the request are then converted to W3C DOM Document. <em>If the results are not
 * XML you will get an XML error.</em>
 *
 * <p>This is useful when you need to make a sys_Lookup XML document for a control and would like to
 * use JSP instead of a full blown legacy XML query resource. <br>
 * See Extensions.xml for the parameters that you can pass to this exit.
 *
 * @author adamgent
 */
public class PSProxyQueryResource extends PSDefaultExtension implements IPSResultDocumentProcessor {

  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_USER = "user";
  private static final String PARAM_URL = "url";

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSProxyQueryResource.class);

  public boolean canModifyStyleSheet() {
    return false;
  }

  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    try {
      Map<String, String> p = getParameters(params);
      PSExtensionParamsHelper helper = new PSExtensionParamsHelper(p, request, log);
      String url = helper.getRequiredParameter(PARAM_URL);
      String user = helper.getOptionalParameter(PARAM_USER, null);
      String password = helper.getOptionalParameter(PARAM_PASSWORD, null);
      String host;
      int port;
      String scheme = "http";
      URI uri;

      boolean internalRequest = false;
      String prepend;
      String queryString = buildUrlQueryString(request, asList(PARAM_URL));
      if (StringUtils.isBlank(queryString)) {
        prepend = "";
      } else if (url.contains("?")) {
        prepend = "&amp;";
      } else {
        prepend = "?";
      }

      url = url + prepend + queryString;
      if (url.startsWith("../")) {
        // Rewrite as absolute to the server
        url = PSServer.getRequestRoot() + url.substring(2);
      }

      if (url.startsWith(PSServer.getRequestRoot())) {
        internalRequest = true;

        host = "127.0.0.1";
        port = PSServer.getListenerPort();

        try {
          int queryIdx = url.indexOf('?');
          String rawPath = queryIdx >= 0 ? url.substring(0, queryIdx) : url;
          String rawQuery = queryIdx >= 0 ? url.substring(queryIdx + 1) : null;

          // This is an internal request so pass the jsessionid
          String sessionid = (String) PSRequestInfo.getRequestInfo(KEY_JSESSIONID);
          if (StringUtils.isNotEmpty(sessionid)) {
            rawPath = rawPath + ";jsessionid=" + sessionid;
          }

          String rawFull =
              scheme
                  + "://"
                  + host
                  + ":"
                  + port
                  + (rawPath.startsWith("/") ? "" : "/")
                  + rawPath
                  + (rawQuery != null ? "?" + rawQuery : "");
          uri = new URI(rawFull.replace(" ", "%20"));
        } catch (URISyntaxException e) {
          log.error(
              "Error parsing supplied url: {} Error: {}",
              url,
              PSExceptionUtils.getMessageForLog(e));
          throw new RuntimeException("Error parsing supplied url:" + url, e);
        }
      } else {
        try {
          URL validatedUrl = URLValidation.validateURLString(url);
          String safeProtocol =
              "https".equalsIgnoreCase(validatedUrl.getProtocol()) ? "https" : "http";
          uri =
              new URI(
                  safeProtocol
                      + "://"
                      + validatedUrl.getAuthority()
                      + validatedUrl.getFile()
                      + (validatedUrl.getRef() != null ? "#" + validatedUrl.getRef() : ""));
        } catch (SecurityException e) {
          log.warn(
              "Blocked external proxy request URL by SSRF validation: {}",
              PSExceptionUtils.getMessageForLog(e));
          throw e;
        } catch (MalformedURLException | URISyntaxException e) {
          log.error(
              "Error parsing supplied url: {} Error: {}",
              url,
              PSExceptionUtils.getMessageForLog(e));
          throw new RuntimeException("Error parsing supplied url:" + url, e);
        }
      }

      String repr = "url = " + url + " user = " + user + " password = " + password;
      log.debug("Trying to get document with: {}", repr);

      RequestConfig.Builder requestConfigBuilder =
          RequestConfig.custom().setCookieSpec(StandardCookieSpec.RELAXED);
      HttpClientContext context = HttpClientContext.create();

      if (!internalRequest && !StringUtils.isEmpty(user)) {
        requestConfigBuilder
            .setAuthenticationEnabled(true)
            .setTargetPreferredAuthSchemes(Collections.singletonList(StandardAuthScheme.BASIC));
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        char[] passChars = password != null ? password.toCharArray() : new char[0];
        credsProvider.setCredentials(
            new AuthScope(null, -1), new UsernamePasswordCredentials(user, passChars));
        context.setCredentialsProvider(credsProvider);
      }
      context.setRequestConfig(requestConfigBuilder.build());

      // codeql[java/ssrf] justification: URL rebuilt from URLValidation.validateURLString +
      // http/https scheme literal; re-review by 2027-07-31
      HttpGet method = new HttpGet(uri);
      final String targetUrl = url;

      try (CloseableHttpClient client =
          HttpClients.custom()
              .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ZERO_MILLISECONDS))
              .build()) {
        HttpResponseResult result =
            client.execute(
                method,
                context,
                response -> {
                  int statusCode = response.getCode();
                  HttpEntity entity = response.getEntity();
                  String body = entity == null ? "" : EntityUtils.toString(entity);
                  return new HttpResponseResult(statusCode, body);
                });

        if (result.statusCode != HttpStatus.SC_OK) {
          log.error(
              "Remote request to url: {} failed with status code: {}",
              targetUrl,
              result.statusCode);
          throw new RuntimeException(
              "Remote request to url: "
                  + targetUrl
                  + " failed with status code: "
                  + result.statusCode);
        }

        String results = result.body.replaceFirst("<\\?xml.*\\?>", "");
        try {
          return createXmlDocument(new StringReader(results), false);
        } catch (SAXException e) {
          String message = "XML Error with " + repr;
          log.error(message, e);
          throw new Exception(message, e);
        }
      } catch (IOException e) {
        log.error("Fatal transport error: {}", PSExceptionUtils.getMessageForLog(e));
        throw new Exception(e);
      }
    } catch (Exception e) {
      log.debug("PSProxyQueryResource attempt failed. Returning null to caller.", e);
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

  @SuppressWarnings({"unused", "unchecked"})
  private String buildUrlQueryString(IPSRequestContext request, List<String> ignore) {
    Iterator it = request.getParametersIterator();
    List<String> params = new ArrayList<>();
    while (it.hasNext()) {
      Entry<String, Object> element = (Entry<String, Object>) it.next();
      String name = element.getKey();
      if (ignore != null && ignore.contains(name)) continue;
      Object value = element.getValue();
      if (value == null) continue;
      String valueString = null;
      if (value instanceof String) {
        valueString = (String) value;
      } else if (value instanceof Number) {
        valueString = value.toString();
      }
      if (valueString != null) params.add(name + "=" + valueString);
    }
    return StringUtils.join(params.iterator(), "&amp;");
  }
}
