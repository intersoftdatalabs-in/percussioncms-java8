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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.services.integrations.siteimprove;

import com.percussion.error.PSExceptionUtils;
import com.percussion.security.TLSSocketFactory;
import com.percussion.server.PSServer;
import com.percussion.services.integrations.IPSIntegrationProviderService;
import com.percussion.util.PSURLEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/** Services for the REST endpoint for our Siteimprove integration. */
public class PSSiteImproveProviderService implements IPSIntegrationProviderService {

  // The api endpoints for the Siteimprove api.
  private static final String NEW_SITEIMPROVE_BASE_URL =
      "https://api-gateway.siteimprove.com/cms-recheck";

  private static final String PERCUSSION_CM1_VERSION = "Percussion CMS " + PSServer.getVersion();
  private static final String SITEIMPROVE_TOKEN_URL =
      "https://my2.siteimprove.com/auth/token?cms=";
  private static final String SITEIMPROVE_RECRAWL_SITE = "recrawl";
  private static final String SITEIMPROVE_RECHECK_PAGE = "recheck";
  private static final String SITEIMPROVE_TOKEN = "token";

  // Header strings
  private static final String ACCEPTS = "Accept";
  private static final String APPLICATION_JSON = "application/json";
  private static final String CONTENT_TYPE = "Content-Type";
  private static ExecutorService pool = Executors.newFixedThreadPool(1);
  private static final Logger logger = LogManager.getLogger(PSSiteImproveProviderService.class);

  private static final String DEFAULT_PROTOCOL = "http";

  /**
   * Gets a new Siteimprove token for the site. It is to be saved/persisted in PSMetadata object
   * unless the feature is disabled/re-enabled for a site.
   *
   * @return the token. empty string if not set
   */
  public String getNewSiteImproveToken() {
    String tokenQueryParam = PSURLEncoder.encodeQuery(PERCUSSION_CM1_VERSION);
    HttpGet getMethod = new HttpGet(SITEIMPROVE_TOKEN_URL + tokenQueryParam);
    HttpResponseResult result;
    try {
      result = executeMethod(getMethod);
    } catch (Exception e1) {
      logger.error(
          "Unable to get new Siteimprove token with message: {}",
          PSExceptionUtils.getMessageForLog(e1));
      return "";
    }

    String token = "";
    try {
      JSONObject jsonObjectItems = new JSONObject(result.body);
      token = jsonObjectItems.getString(SITEIMPROVE_TOKEN);
    } catch (JSONException e) {
      logger.error(
          "Failed to get new Siteimprove token with message: {}",
          PSExceptionUtils.getMessageForLog(e));
      logger.debug(e);
    }

    return token;
  }

  /**
   * Request a site check from siteimprove. Site is determined by id.
   *
   * @param siteId Site id of the site we wish to check.
   * @param credentials The credentials allowing us to access the siteimprove api.
   * @throws Exception Siteimprove rejected our request or the site id was bad.
   */
  @Override
  public void updateSiteInfo(final String siteId, final Map<String, String> credentials)
      throws Exception {

    if (siteId == null || siteId.isEmpty()) {
      throw new NullPointerException("siteURL cannot be null or empty.");
    }

    pool.submit(
        () -> {
          try {
            HttpPost postMethod = new HttpPost(NEW_SITEIMPROVE_BASE_URL);

            JSONObject object = new JSONObject();
            object.accumulate("url", siteId);
            object.accumulate(SITEIMPROVE_TOKEN, credentials.get(SITEIMPROVE_TOKEN));
            object.accumulate("type", SITEIMPROVE_RECRAWL_SITE);

            logger.debug("JSON Object body: {}", object);

            postMethod.setEntity(new StringEntity(object.toString(), ContentType.APPLICATION_JSON));

            boolean responseStatus = executeMethod(postMethod).isSuccess();

            if (!responseStatus) {
              throw new PSSiteImproveProviderException(
                  "Failed to request a page check from siteimprove with id:  " + siteId);
            }

          } catch (Exception e) {
            logger.error(e);
          }
        });
  }

  /**
   * Request a page check from siteimprove.
   *
   * @param siteId Id of our site that the page lives on
   * @param pageURL The live URL of a page, must pre-exist from a site crawl on siteimprove's side.
   *     Otherwise do a new site crawl.
   * @param credentials The sitename/token allowing us to access the siteimprove api.
   */
  @Override
  public void updatePageInfo(
      final String siteId, final String pageURL, final Map<String, String> credentials) {

    if (pageURL == null || pageURL.isEmpty()) {
      throw new NullPointerException("pageUrl cannot be null or empty.");
    }

    pool.submit(
        () -> {
          try {
            int retries = 0;
            while (retries < 4) {
              HttpPost postMethod = new HttpPost(NEW_SITEIMPROVE_BASE_URL);

              JSONObject object = new JSONObject();

              String finalURL = pageURL;

              logger.debug("canonicalDist: {}", credentials.get("canonicalDist"));
              logger.debug("siteProtocol: {}", credentials.get("siteProtocol"));
              logger.debug("defaultDocument: {}", credentials.get("defaultDocument"));
              logger.debug("token: {}", credentials.get(SITEIMPROVE_TOKEN));
              logger.debug("siteName: {}", credentials.get("sitename"));

              if ("sections".equals(credentials.get("canonicalDist"))) {
                finalURL = StringUtils.replace(pageURL, credentials.get("defaultDocument"), "");
              }

              object.accumulate("url", finalURL);
              object.accumulate(SITEIMPROVE_TOKEN, credentials.get(SITEIMPROVE_TOKEN));
              object.accumulate("type", SITEIMPROVE_RECHECK_PAGE);

              logger.debug("JSON Object body: {}", object);

              postMethod.setEntity(
                  new StringEntity(object.toString(), ContentType.APPLICATION_JSON));

              boolean responseStatus = executeMethod(postMethod).isSuccess();
              if (responseStatus) {
                return;
              }
              Thread.sleep(3000);
              retries++;
            }
            throw new PSSiteImproveProviderException(
                "Failed to notify siteimprove to check page with url: "
                    + pageURL
                    + " .  Site id is: "
                    + siteId
                    + " .  Exceeded retry count.");
          } catch (NoSuchAlgorithmException
              | KeyStoreException
              | KeyManagementException
              | PSSiteImproveProviderException
              | IOException e) {
            logger.error(PSExceptionUtils.getMessageForLog(e));
          } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
          }
        });
  }

  /**
   * Executes the prepared HTTP request against Siteimprove using {@link TLSSocketFactory}'s TLS
   * strategy and JSON headers, returning status code and body.
   */
  private HttpResponseResult executeMethod(HttpUriRequestBase httpMethod)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
    httpMethod.addHeader(ACCEPTS, APPLICATION_JSON);
    httpMethod.addHeader(CONTENT_TYPE, APPLICATION_JSON);

    TLSSocketFactory tlsFactory = new TLSSocketFactory();
    try (PoolingHttpClientConnectionManager cm =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsFactory.createTlsSocketStrategy())
                .build();
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build()) {
      return httpClient.execute(
          httpMethod,
          response -> {
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
            return new HttpResponseResult(response.getCode(), body);
          });
    }
  }

  private static final class HttpResponseResult {
    private final int statusCode;
    private final String body;

    private HttpResponseResult(int statusCode, String body) {
      this.statusCode = statusCode;
      this.body = body;
    }

    private boolean isSuccess() {
      return statusCode >= 200 && statusCode <= 300;
    }
  }

  @Override
  public Boolean validateCredentials(Map<String, String> credentials) throws Exception {
    if ("".equals(credentials.get(SITEIMPROVE_TOKEN))) {
      return false;
    } else if ("".equals(credentials.get("sitename"))) {
      return false;
    }

    if ("".equals(credentials.get("siteProtocol")) || null == credentials.get("siteProtocol")) {
      credentials.put("siteProtocol", DEFAULT_PROTOCOL); // default protocol to http if empty
    }
    if ("".equals(credentials.get("defaultDocument"))
        || null == credentials.get("defaultDocument")) {
      credentials.put("defaultDocument", "index.html");
    }
    if ("".equals(credentials.get("canonicalDist")) || null == credentials.get("canonicalDist")) {
      credentials.put("canonicalDist", "pages");
    }
    return true;
  }

  /**
   * No need to implement as we don't need to retreive site info from backend. It's all done from
   * the front end Siteimprove plugin.
   */
  @Override
  public String retrieveSiteInfo(String siteName, Map<String, String> credentials)
      throws Exception {
    throw new NotImplementedException();
  }

  /**
   * No need to implement as we don't need to retreive site info from backend. It's all done from
   * the front end Siteimprove plugin.
   */
  @Override
  public String retrievePageInfo(String siteName, String pageURL, Map<String, String> credentials) {
    throw new NotImplementedException();
  }
}
