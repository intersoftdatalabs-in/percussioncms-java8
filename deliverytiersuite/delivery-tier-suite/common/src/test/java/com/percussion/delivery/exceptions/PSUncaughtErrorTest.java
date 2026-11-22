/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.delivery.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class PSUncaughtErrorTest {

  private static PSUncaughtError createMapper(
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    PSUncaughtError mapper = new PSUncaughtError();
    Field reqField = PSUncaughtError.class.getDeclaredField("request");
    reqField.setAccessible(true);
    reqField.set(mapper, request);
    Field respField = PSUncaughtError.class.getDeclaredField("response");
    respField.setAccessible(true);
    respField.set(mapper, response);
    return mapper;
  }

  @Test
  public void testRedirectsToRootErrorPageNotContextPath() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/perc-comments-services/error.html");
    request.setContextPath("/perc-comments-services");
    request.addHeader("referer", "http://localhost:9982/widgets/comments.html");
    MockHttpServletResponse response = new MockHttpServletResponse();

    PSUncaughtError mapper = createMapper(request, response);
    Response result = mapper.toResponse(new NotFoundException());

    assertNull(result);
    assertEquals("/error.html", response.getRedirectedUrl());
  }

  @Test
  public void testIgnoresExternalRefererHeader() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/perc-comments-services/comment/addcomment");
    request.setContextPath("/perc-comments-services");
    request.addHeader("referer", "https://evil.example/phish");
    MockHttpServletResponse response = new MockHttpServletResponse();

    PSUncaughtError mapper = createMapper(request, response);
    Response result = mapper.toResponse(new RuntimeException("boom"));

    assertNull(result);
    assertEquals("/error.html", response.getRedirectedUrl());
  }

  @Test
  public void testBreaksSelfRedirectLoopWhenAlreadyAtRootErrorPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error.html");
    request.setContextPath("");
    request.setAttribute("javax.servlet.error.status_code", 403);
    MockHttpServletResponse response = new MockHttpServletResponse();

    PSUncaughtError mapper = createMapper(request, response);
    Response result = mapper.toResponse(new NotFoundException());

    assertNull(response.getRedirectedUrl());
    assertNotNull(result);
    assertEquals(403, result.getStatus());
    assertEquals(MediaType.TEXT_PLAIN_TYPE, result.getMediaType());
  }

  @Test
  public void testBreaksSelfRedirectLoopFallsBackToWebApplicationExceptionStatus()
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error.html");
    request.setContextPath("");
    MockHttpServletResponse response = new MockHttpServletResponse();

    PSUncaughtError mapper = createMapper(request, response);
    Response result = mapper.toResponse(new NotFoundException());

    assertNull(response.getRedirectedUrl());
    assertNotNull(result);
    assertEquals(404, result.getStatus());
  }

  @Test
  public void testHandlesNullRequestOrResponseGracefully() throws Exception {
    PSUncaughtError mapper = createMapper(null, null);
    Response result = mapper.toResponse(new RuntimeException("err"));
    assertNull(result);
  }
}
