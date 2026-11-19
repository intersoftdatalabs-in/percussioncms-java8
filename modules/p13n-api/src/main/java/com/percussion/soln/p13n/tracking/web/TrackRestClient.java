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

package com.percussion.soln.p13n.tracking.web;

import static com.percussion.soln.p13n.tracking.web.VisitorTrackingWebUtils.convertServletRequestToTrackingRequest;
import static com.percussion.soln.p13n.tracking.web.VisitorTrackingWebUtils.jsonToResponse;
import static com.percussion.soln.p13n.tracking.web.VisitorTrackingWebUtils.parameterizeTrackingRequest;
import static com.percussion.soln.p13n.tracking.web.VisitorTrackingWebUtils.setVisitorProfileToCookie;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.percussion.soln.p13n.tracking.VisitorTrackingActionRequest;
import com.percussion.soln.p13n.tracking.VisitorTrackingResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.ClientCookie;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

/**
 * An abstract class that provides REST client access to the tracking service. See the REST API
 * portion of the {@link com.percussion.soln.p13n.tracking Visitor Tracking Guide}.
 *
 * @author adamgent
 * @see #clientRequest(HttpServletRequest, HttpServletResponse, VisitorTrackingActionRequest)
 * @see com.percussion.soln.p13n.tracking
 */
public class TrackRestClient {

  private final String trackingPath = "/soln-p13n/track/track";
  private final String defaultTrackingURI = "http://localhost:8080" + trackingPath;

  private String trackingURI = null;
  private int timeOut = 0;

  /**
   * Makes a tracking request with the supplied parameters. This is a convenience method for {@link
   * #clientRequest(HttpServletRequest, HttpServletResponse, VisitorTrackingActionRequest)}.
   *
   * @param servletRequest Servlet request. Maybe null. If passed the tracking cookie will be
   *     retrieved.
   * @param servletResponse Servlet response. Maybe null. If passed cookie will be set.
   * @param visitorProfileId Visitor profile id if null will try servlet request for it.
   * @param action tracking action.
   * @param userName userName of profile. Maybe null.
   * @param label label is usually null.
   * @param segmentWeights segment weights.
   * @return the response.
   * @see #clientRequest(HttpServletRequest, HttpServletResponse, VisitorTrackingActionRequest)
   */
  protected VisitorTrackingResponse clientRequest(
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      String visitorProfileId,
      String action,
      String userName,
      String label,
      Map<String, Integer> segmentWeights) {

    VisitorTrackingActionRequest tr = new VisitorTrackingActionRequest();

    if (visitorProfileId != null) {
      tr.setVisitorProfileId(Long.parseLong(visitorProfileId));
    }

    tr.setSegmentWeights(segmentWeights);
    tr.setActionName(action);
    tr.setUserId(userName);
    tr.setLabel(label);

    return clientRequest(servletRequest, servletResponse, tr);
  }

  /**
   * Makes tracking action request based on the trackingRequest parameter.
   *
   * @param servletRequest Servlet request. Maybe null. If passed the tracking cookie will be
   *     retrieved.
   * @param servletResponse Servlet response. Maybe null. If passed cookie will be set.
   * @param trackingRequest {@link VisitorTrackingActionRequest#getActionName() actionName property}
   *     should never be <code>null</code>, empty, or blank.
   * @return never <code>null</code>.
   * @see VisitorTrackingActionRequest
   */
  public VisitorTrackingResponse clientRequest(
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      VisitorTrackingActionRequest trackingRequest) {
    BasicCookieStore cookieStore = new BasicCookieStore();
    final VisitorTrackingActionRequest tr = trackingRequest;

    if (servletRequest != null) {
      javax.servlet.http.Cookie[] cookies = servletRequest.getCookies();
      if (cookies != null) {
        for (javax.servlet.http.Cookie c : cookies) {
          String domain = c.getDomain();
          if (domain == null) {
            domain = servletRequest.getServerName();
          }
          String cpath = c.getPath();
          if (cpath == null || cpath.isEmpty()) {
            cpath = servletRequest.getContextPath();
          }
          if (cpath == null || cpath.isEmpty()) {
            cpath = "/";
          }
          log.debug(
              "forwarding cookie domain="
                  + c.getDomain()
                  + " name="
                  + c.getName()
                  + " value="
                  + c.getValue()
                  + " path="
                  + c.getPath());
          BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
          cookie.setDomain(domain);
          cookie.setAttribute(ClientCookie.DOMAIN_ATTR, domain);
          cookie.setPath(cpath);
          cookie.setAttribute(ClientCookie.PATH_ATTR, cpath);
          cookie.setSecure(c.getSecure());
          if (c.getMaxAge() > 0) {
            cookie.setExpiryDate(Instant.now().plusSeconds(c.getMaxAge()));
          } else if (c.getMaxAge() == 0) {
            cookie.setExpiryDate(Instant.EPOCH);
          }
          cookieStore.addCookie(cookie);
        }
      }
      convertServletRequestToTrackingRequest(servletRequest, tr);
    }

    Map<String, String> nvp = parameterizeTrackingRequest(tr);
    List<NameValuePair> paramList = nameValuePairs(nvp.entrySet());
    String baseUri = getTrackingURI(servletRequest);
    URI fullUri;
    try {
      fullUri = new URIBuilder(baseUri).addParameters(paramList).build();
    } catch (URISyntaxException e) {
      throw new TrackRestClientException("Problem connecting to " + baseUri, e);
    }

    Timeout timeout =
        getTimeOut() > 0 ? Timeout.ofMilliseconds(getTimeOut()) : Timeout.DISABLED;
    ConnectionConfig connConfig = ConnectionConfig.custom().setSocketTimeout(timeout).build();
    RequestConfig reqConfig =
        RequestConfig.custom()
            .setCookieSpec(StandardCookieSpec.RELAXED)
            .setResponseTimeout(timeout)
            .build();

    String responseBody;
    try (CloseableHttpClient client =
        HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(connConfig)
                    .build())
            .setDefaultCookieStore(cookieStore)
            .setDefaultRequestConfig(reqConfig)
            .build()) {
      HttpGet method = new HttpGet(fullUri);
      responseBody = executeMethod(client, method);
    } catch (IOException e) {
      throw new TrackRestClientException("Problem connecting to " + fullUri, e);
    }

    VisitorTrackingResponse trackResponse = jsonToResponse(responseBody);
    if (servletRequest != null
        && servletResponse != null
        && "OK".equals(trackResponse.getStatus())) {
      setVisitorProfileToCookie(
          servletRequest, servletResponse, trackResponse.getVisitorProfileId());
    }
    return trackResponse;
  }

  private List<NameValuePair> nameValuePairs(Collection<Entry<String, String>> params) {
    List<NameValuePair> list = new ArrayList<>();
    for (Entry<String, String> e : params) {
      if (isNotBlank(e.getValue())) {
        list.add(new BasicNameValuePair(e.getKey(), e.getValue()));
      }
    }
    return list;
  }

  private String executeMethod(CloseableHttpClient client, HttpGet method)
      throws TrackRestClientException {
    String uri = "";
    try {
      uri = method.getUri().toString();
      HttpResult result =
          client.execute(
              method,
              response -> {
                int stat = response.getCode();
                HttpEntity entity = response.getEntity();
                String body =
                    entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                return new HttpResult(stat, body);
              });
      log.debug("HTTP return code: " + result.status);
      if (!(200 <= result.status && result.status < 300)) {
        String error = "HTTP Error: " + result.status + " Response: \n" + result.body;
        log.error(error);
        throw new TrackRestClientException(error);
      }
      if (log.isDebugEnabled()) {
        log.debug("Response: " + result.body);
      }
      return result.body;
    } catch (URISyntaxException | IOException e) {
      String error = "Problem connecting to " + uri;
      throw new TrackRestClientException(error, e);
    }
  }

  private static final class HttpResult {
    final int status;
    final String body;

    HttpResult(int status, String body) {
      this.status = status;
      this.body = body;
    }
  }

  /**
   * Indicates an error happened while trying to connect and use a REST service.
   *
   * @author adamgent
   */
  public static class TrackRestClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TrackRestClientException(String message) {
      super(message);
    }

    public TrackRestClientException(String message, Throwable cause) {
      super(message, cause);
    }

    public TrackRestClientException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Gets the tracking request from the servlet request.
   *
   * @param request maybe <code>null</code>.
   * @return never <code>null</code>, empty, or blank.
   */
  public String getTrackingURI(HttpServletRequest request) {
    if (getTrackingURI() != null) {
      return getTrackingURI();
    }
    if (request == null) {
      return defaultTrackingURI;
    }
    return createUrlFromRequest(request);
  }

  /**
   * Creates the REST URL end-point from the servlet request.
   *
   * @param request never <code>null</code>.
   * @return never <code>null</code>, empty, or blank.
   */
  protected String createUrlFromRequest(HttpServletRequest request) {
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int serverPort = request.getServerPort();
    return createAbsURL(trackingPath, scheme, serverName, serverPort);
  }

  private String createAbsURL(String resource, String scheme, String serverName, int port) {
    URI siteURI = URI.create(scheme + "://" + serverName + ":" + port);
    URI origURI = URI.create(resource);

    if (origURI.isAbsolute()) {
      return origURI.toString();
    }
    return siteURI.resolve(resource).toString();
  }

  public int getTimeOut() {
    return timeOut;
  }

  public void setTimeOut(int timeOut) {
    this.timeOut = timeOut;
  }

  /**
   * The REST end-point for the tracking service as a configurable property.
   *
   * @see com.percussion.soln.p13n.tracking Visitor Tracking Developer Guide
   * @return if <code>null</code> the servlet request's server will be used.
   */
  public String getTrackingURI() {
    return trackingURI;
  }

  /**
   * See getter.
   *
   * @param trackingURI maybe <code>null</code>.
   * @see #getTrackingURI()
   */
  public void setTrackingURI(String trackingURI) {
    this.trackingURI = trackingURI;
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Log log = LogFactory.getLog(TrackLoginRestClient.class);
}
