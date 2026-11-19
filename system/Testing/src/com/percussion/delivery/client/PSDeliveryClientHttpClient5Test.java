/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryClientException;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.proxyconfig.data.PSProxyConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Offline loopback unit tests verifying {@link PSDeliveryClient} on Apache HttpClient 5.6.3
 * (issue #310): JSON GET/POST, URL-encoded form push, null POST body handling, Basic auth
 * challenge-response, non-2xx {@link PSDeliveryClientException} mapping, 204 empty response,
 * transport failure tracking, and idempotent {@link PSDeliveryClient#close()}.
 */
public class PSDeliveryClientHttpClient5Test {

  private HttpServer server;
  private int port;
  private String baseUrl;
  private volatile String lastMethod;
  private volatile String lastContentType;
  private volatile String lastBody;

  @Before
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    port = server.getAddress().getPort();
    baseUrl = "http://127.0.0.1:" + port;

    server.createContext(
        "/json-obj",
        exchange -> {
          lastMethod = exchange.getRequestMethod();
          lastContentType = exchange.getRequestHeaders().getFirst("Content-Type");
          lastBody = readBody(exchange.getRequestBody());
          sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\",\"count\":2}");
        });

    server.createContext(
        "/json-arr", exchange -> sendResponse(exchange, 200, "application/json", "[\"a\",\"b\"]"));

    server.createContext(
        "/text", exchange -> sendResponse(exchange, 200, "text/plain", "hello-dts"));

    server.createContext(
        "/form",
        exchange -> {
          lastMethod = exchange.getRequestMethod();
          lastContentType = exchange.getRequestHeaders().getFirst("Content-Type");
          lastBody = readBody(exchange.getRequestBody());
          sendResponse(exchange, 200, "text/plain", "accepted");
        });

    server.createContext(
        "/null-post",
        exchange -> {
          lastMethod = exchange.getRequestMethod();
          lastBody = readBody(exchange.getRequestBody());
          sendResponse(exchange, 200, "application/json", "{\"ok\":true}");
        });

    server.createContext(
        "/auth",
        exchange -> {
          String auth = exchange.getRequestHeaders().getFirst("Authorization");
          String expected =
              "Basic "
                  + Base64.getEncoder()
                      .encodeToString("dtsuser:dtssecret".getBytes(StandardCharsets.UTF_8));
          if (!expected.equals(auth)) {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"dts-realm\"");
            sendResponse(exchange, 401, "text/plain", "Invalid credentials");
            return;
          }
          sendResponse(exchange, 200, "application/json", "{\"authenticated\":true}");
        });

    server.createContext(
        "/error500",
        exchange ->
            sendResponse(exchange, 500, "text/plain", "Internal Failure Details\nSecond line"));

    server.createContext("/nocontent", exchange -> sendResponse(exchange, 204, "text/plain", ""));

    server.start();
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  private PSDeliveryClient newStandaloneClient() {
    PSDeliveryClient client = new PSDeliveryClient(0, 2000, 2000);
    client.setProxyConfig(new PSProxyConfig());
    return client;
  }

  private PSDeliveryInfo newServerInfo(String user, String password, String realm) {
    PSDeliveryInfo info = new PSDeliveryInfo(baseUrl, user, password);
    info.setAdminUrl(baseUrl);
    info.setRealm(realm);
    return info;
  }

  @Test
  public void testGetJsonObjectArrayAndString() {
    try (PSDeliveryClient client = newStandaloneClient()) {
      PSDeliveryInfo info = newServerInfo(null, null, null);

      ObjectNode obj =
          client.getJsonObject(new PSDeliveryActionOptions(info, "/json-obj", HttpMethodType.GET));
      assertEquals("ok", obj.get("status").asText());
      assertEquals(2, obj.get("count").asInt());

      ArrayNode arr =
          client.getJsonArray(new PSDeliveryActionOptions(info, "/json-arr", HttpMethodType.GET));
      assertEquals(2, arr.size());
      assertEquals("a", arr.get(0).asText());

      String text =
          client.getString(new PSDeliveryActionOptions(info, "/text", HttpMethodType.GET));
      assertEquals("hello-dts", text);
    }
  }

  @Test
  public void testPostJsonFormAndNullBody() {
    try (PSDeliveryClient client = newStandaloneClient()) {
      PSDeliveryInfo info = newServerInfo(null, null, null);

      ObjectNode postResp =
          client.getJsonObject(
              new PSDeliveryActionOptions(info, "/json-obj", HttpMethodType.POST),
              "{\"hello\":\"world\"}");
      assertEquals("POST", lastMethod);
      assertEquals("{\"hello\":\"world\"}", lastBody);
      assertEquals("ok", postResp.get("status").asText());

      List<NameValuePair> form =
          Arrays.asList(
              new BasicNameValuePair("site", "TestSite"),
              new BasicNameValuePair("title", "Comment One"));
      client.push(new PSDeliveryActionOptions(info, "/form", HttpMethodType.POST), form);
      assertEquals("POST", lastMethod);
      assertTrue(
          "Expected form content type but got: " + lastContentType,
          lastContentType != null && lastContentType.contains("application/x-www-form-urlencoded"));
      assertTrue(lastBody.contains("site=TestSite"));
      assertTrue(lastBody.contains("title=Comment+One"));

      client.push(new PSDeliveryActionOptions(info, "/null-post", HttpMethodType.POST), null);
      assertEquals("POST", lastMethod);
      assertEquals("", lastBody);
    }
  }

  @Test
  public void testBasicAuthChallengeAndUnauthorizedError() {
    try (PSDeliveryClient client = newStandaloneClient()) {
      PSDeliveryInfo validInfo = newServerInfo("dtsuser", "dtssecret", "dts-realm");
      ObjectNode authResp =
          client.getJsonObject(new PSDeliveryActionOptions(validInfo, "/auth", HttpMethodType.GET));
      assertTrue(authResp.get("authenticated").asBoolean());

      PSDeliveryInfo badInfo = newServerInfo("dtsuser", "wrong", "dts-realm");
      try {
        client.getString(new PSDeliveryActionOptions(badInfo, "/auth", HttpMethodType.GET));
        fail("Expected PSDeliveryClientException for HTTP 401");
      } catch (PSDeliveryClientException ex) {
        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("401"));
      }
    }
  }

  @Test
  public void testNon2xxNoContentTransportFailureAndIdempotentClose() throws IOException {
    try (PSDeliveryClient client = newStandaloneClient()) {
      PSDeliveryInfo info = newServerInfo(null, null, null);

      try {
        client.getString(new PSDeliveryActionOptions(info, "/error500", HttpMethodType.GET));
        fail("Expected PSDeliveryClientException for HTTP 500");
      } catch (PSDeliveryClientException ex) {
        assertEquals(500, ex.getStatusCode());
        assertEquals("Internal Failure Details", ex.getResponseSnippet());
      }

      String noContent =
          client.getString(new PSDeliveryActionOptions(info, "/nocontent", HttpMethodType.GET));
      assertEquals("", noContent);

      int unusedPort;
      try (ServerSocket socket = new ServerSocket(0)) {
        unusedPort = socket.getLocalPort();
      }
      PSDeliveryInfo deadInfo = new PSDeliveryInfo("http://127.0.0.1:" + unusedPort);
      try {
        client.getString(new PSDeliveryActionOptions(deadInfo, "/ping", HttpMethodType.GET, false));
        fail("Expected PSDeliveryClientException on unreachable port");
      } catch (PSDeliveryClientException ex) {
        assertEquals(-1, ex.getStatusCode());
      }
      assertEquals(1, client.getFailureCount());

      client.close();
      client.close();
    }
  }

  private static void sendResponse(
      HttpExchange exchange, int status, String contentType, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    if (contentType != null && !contentType.isEmpty()) {
      exchange.getResponseHeaders().set("Content-Type", contentType);
    }
    if (status == 204) {
      exchange.sendResponseHeaders(status, -1);
    } else {
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    }
    exchange.close();
  }

  private static String readBody(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int n;
    while ((n = is.read(buf)) != -1) {
      baos.write(buf, 0, n);
    }
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }
}
