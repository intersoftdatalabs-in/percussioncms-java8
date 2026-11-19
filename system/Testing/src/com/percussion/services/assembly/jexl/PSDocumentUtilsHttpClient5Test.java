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
package com.percussion.services.assembly.jexl;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Offline loopback unit tests verifying {@link PSDocumentUtils#getDocument(String, String, String)}
 * external HTTP requests on Apache HttpClient 5.6.3 (issue #310): plain GET, preemptive Basic
 * Authorization header, and non-200 status handling.
 */
public class PSDocumentUtilsHttpClient5Test {

  private HttpServer server;
  private String baseUrl;
  private volatile String capturedAuthHeader;

  @Before
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    int port = server.getAddress().getPort();
    baseUrl = "http://127.0.0.1:" + port;

    server.createContext(
        "/doc", exchange -> sendResponse(exchange, 200, "<html><body>external-doc</body></html>"));

    server.createContext(
        "/protected",
        exchange -> {
          capturedAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
          String expected =
              "Basic "
                  + Base64.getEncoder()
                      .encodeToString("jexluser:jexlpass".getBytes(StandardCharsets.UTF_8));
          if (!expected.equals(capturedAuthHeader)) {
            sendResponse(exchange, 403, "Forbidden");
            return;
          }
          sendResponse(exchange, 200, "authorized-doc");
        });

    server.createContext("/missing", exchange -> sendResponse(exchange, 404, "Not Found"));

    server.start();
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  public void testGetExternalDocumentPlainBasicAuthAndNon200() throws Exception {
    PSDocumentUtils utils = new PSDocumentUtils();

    assertEquals(
        "<html><body>external-doc</body></html>", utils.getDocument(baseUrl + "/doc"));

    String authResult = utils.getDocument(baseUrl + "/protected", "jexluser", "jexlpass");
    assertEquals("authorized-doc", authResult);
    String expectedHeader =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("jexluser:jexlpass".getBytes(StandardCharsets.UTF_8));
    assertEquals(expectedHeader, capturedAuthHeader);

    assertEquals("", utils.getDocument(baseUrl + "/missing"));
  }

  private static void sendResponse(HttpExchange exchange, int status, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
    exchange.close();
  }
}
