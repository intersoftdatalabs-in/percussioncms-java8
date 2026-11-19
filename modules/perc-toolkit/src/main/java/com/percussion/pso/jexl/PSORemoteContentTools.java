/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.pso.jexl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Provide tools to work with remote XML/HTML/JSON content
 *
 * @author justinraines
 */
@SuppressWarnings("unused")
public class PSORemoteContentTools extends PSJexlUtilBase implements IPSJexlExpression {

  private static final Logger log = LogManager.getLogger(PSORemoteContentTools.class);

  public PSORemoteContentTools() {
    super();
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  // content
  @IPSJexlMethod(
      description = "Returns a status code for a url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns an integer status code")
  public int getHTTPStatusCode(String urlString) throws IllegalArgumentException, IOException {
    return executeGetStatus(urlString, null, null, null);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns a status code for a url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a integer status code")
  public int getHTTPStatusCode(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGetStatus(urlString, null, username, password);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @param headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns status code based on url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set")
      },
      returns = "Returns a integer status code")
  public int getHTTPStatusCode(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    return executeGetStatus(urlString, headers, null, null);
  }

  /**
   * This gets a status code for a url.
   *
   * @param urlString
   * @param headers
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns status code based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a integer status code")
  public int getHTTPStatusCode(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGetStatus(urlString, headers, username, password);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JsonNode based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns a com.fasterxml.jackson.databind.JsonNode")
  public JsonNode getRemoteJSONContent(String urlString)
      throws IllegalArgumentException, IOException {
    return executeGetJson(urlString, null, null, null, true);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JsonNode based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a com.fasterxml.jackson.databind.JsonNode")
  public JsonNode getRemoteJSONContent(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGetJson(urlString, null, username, password, true);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @param headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JsonNode based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set")
      },
      returns = "Returns a com.fasterxml.jackson.databind.JsonNode")
  public JsonNode getRemoteJSONContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    return executeGetJson(urlString, headers, null, null, false);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString
   * @param headers
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JsonNode based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a com.fasterxml.jackson.databind.JsonNode")
  public JsonNode getRemoteJSONContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGetJson(urlString, headers, username, password, true);
  }

  private int executeGetStatus(
      String urlString, Map<String, String> headers, String username, String password)
      throws IOException {
    return executeGet(urlString, headers, username, password, false).statusCode;
  }

  private JsonNode executeGetJson(
      String urlString,
      Map<String, String> headers,
      String username,
      String password,
      boolean allowNotModified)
      throws IOException {
    GetResult result = executeGet(urlString, headers, username, password, true);
    if (allowNotModified) {
      if (result.statusCode != HttpStatus.SC_OK
          && result.statusCode != HttpStatus.SC_NOT_MODIFIED) {
        log.warn(
            "JEXL: getRemoteJSONContent was not 200/304. URL: {} Status Code: {} Status Message: {}",
            urlString,
            result.statusCode,
            result.statusLine);
      }
    } else if (result.statusCode != HttpStatus.SC_OK) {
      log.debug("Get failed" + result.statusLine + ": " + urlString);
    }
    return MAPPER.readTree(result.body);
  }

  private GetResult executeGet(
      String urlString,
      Map<String, String> headers,
      String username,
      String password,
      boolean readBody)
      throws IOException {
    HttpGet get = new HttpGet(urlString);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        get.setHeader(entry.getKey(), entry.getValue());
      }
    }

    HttpClientContext context = HttpClientContext.create();
    if (username != null) {
      URI uriObject;
      try {
        uriObject = new URI(urlString);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid URL: " + urlString, e);
      }
      BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
      char[] passChars = password != null ? password.toCharArray() : new char[0];
      credsProvider.setCredentials(
          new AuthScope(uriObject.getHost(), uriObject.getPort()),
          new UsernamePasswordCredentials(username, passChars));
      context.setCredentialsProvider(credsProvider);
    }

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      return client.execute(
          get,
          context,
          response -> {
            int code = response.getCode();
            String statusLine = code + " " + response.getReasonPhrase();
            String body = "";
            if (readBody) {
              HttpEntity entity = response.getEntity();
              body = entity == null ? "" : EntityUtils.toString(entity);
            }
            return new GetResult(code, statusLine, body);
          });
    }
  }

  private static final class GetResult {
    final int statusCode;
    final String statusLine;
    final String body;

    GetResult(int statusCode, String statusLine, String body) {
      this.statusCode = statusCode;
      this.statusLine = statusLine;
      this.body = body;
    }
  }

  /**
   * This gets remote XML content and returns a JSOUP Document object.
   *
   * @param urlString
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteXMLContent(String urlString)
      throws IllegalArgumentException, IOException {

    return Jsoup.connect(urlString).get();
  }

  /**
   * This gets remote XML content with basic authentication and returns a JSOUP Document object.
   *
   * @param urlString
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  public org.jsoup.nodes.Document getRemoteXMLContent(
      String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    String login = username + ":" + password;
    String base64login = new String(Base64.encodeBase64(login.getBytes(StandardCharsets.UTF_8)));

    return Jsoup.connect(urlString).header("Authorization", "Basic " + base64login).get();
  }

  /**
   * This gets remote XML content with map of headers and returns a JSOUP Document object.
   *
   * @param urlString
   * @param headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteXMLContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    Connection connection = Jsoup.connect(urlString);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      connection = connection.header(entry.getKey(), entry.getValue());
    }
    return connection.get();
  }

  /**
   * This gets remote XML content with map of headers, username, and password then returns a JSOUP
   * Document object.
   *
   * @param urlString
   * @param headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteXMLContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    String login = username + ":" + password;
    String base64login = new String(Base64.encodeBase64(login.getBytes(StandardCharsets.UTF_8)));
    Connection connection =
        Jsoup.connect(urlString).header("Authorization", "Basic " + base64login);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      connection = connection.header(entry.getKey(), entry.getValue());
    }
    return connection.get();
  }

  /* ALIAS METHODS FOR HTML CONTENT, THESE SIMPLY CALL THEIR XML COUNTERPARTS */
  /**
   * This is an aliased method for getRemoteXMLContent
   *
   * @param urlString
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description =
          "Returns JSOUP document with xml content, returns a JSoup Document element. Alias for getRemoteXMLContent",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteHTMLContent(String urlString)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with basic authorization
   *
   * @param urlString
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteHTMLContent(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, username, password);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with headers
   *
   * @param urlString
   * @param headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteHTMLContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, headers);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with headers & basic authorization
   *
   * @param urlString
   * @param headers
   * @param username
   * @param password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  public Document getRemoteHTMLContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, headers, username, password);
  }

  /** T2.17.4c hardening (issue #149): shared Jackson ObjectMapper. */
  private static final ObjectMapper MAPPER = new ObjectMapper();
}
