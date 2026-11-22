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
package com.percussion.delivery.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

public class PSCsrfTokenHelperTest {

  @Test
  public void testResolveFromCsrfFilterFirstVisitWithoutCookie() throws Exception {
    CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
    CsrfFilter filter = new CsrfFilter(repo);
    MockHttpServletRequest request =
        new MockHttpServletRequest("HEAD", "/perc-comments-services/comment/csrf");
    MockHttpServletResponse response = new MockHttpServletResponse();

    AtomicReference<CsrfToken> resolved = new AtomicReference<>();
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            resolved.set(
                PSCsrfTokenHelper.resolveCsrfToken((javax.servlet.http.HttpServletRequest) req));
          }
        };

    filter.doFilter(request, response, chain);

    CsrfToken token = resolved.get();
    assertNotNull(token, "Expected CsrfToken on first visit without incoming cookie");
    assertEquals("X-XSRF-TOKEN", token.getHeaderName());
    assertTrue(
        token.getToken() != null && !token.getToken().trim().isEmpty(),
        "Expected non-blank CSRF token");

    Cookie xsrfCookie = response.getCookie("XSRF-TOKEN");
    assertNotNull(xsrfCookie, "Expected CsrfFilter to emit XSRF-TOKEN cookie");
    assertEquals(token.getToken(), xsrfCookie.getValue());
  }

  @Test
  public void testResolveFromCsrfFilterExistingCookie() throws Exception {
    CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
    CsrfFilter filter = new CsrfFilter(repo);
    MockHttpServletRequest request =
        new MockHttpServletRequest("HEAD", "/perc-comments-services/comment/csrf");
    request.setCookies(new Cookie("XSRF-TOKEN", "existing-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    AtomicReference<CsrfToken> resolved = new AtomicReference<>();
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            resolved.set(
                PSCsrfTokenHelper.resolveCsrfToken((javax.servlet.http.HttpServletRequest) req));
          }
        };

    filter.doFilter(request, response, chain);

    CsrfToken token = resolved.get();
    assertNotNull(token);
    assertEquals("existing-token", token.getToken());
  }

  @Test
  public void testResolveFromRequestAttributeFallbackToParamName() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(
        "_csrf", new DefaultCsrfToken("X-CUSTOM-CSRF", "_csrf", "param-attr-token"));

    CsrfToken token = PSCsrfTokenHelper.resolveCsrfToken(request);
    assertNotNull(token);
    assertEquals("X-CUSTOM-CSRF", token.getHeaderName());
    assertEquals("param-attr-token", token.getToken());
  }

  @Test
  public void testResolveFallsBackToCookieWhenNoAttribute() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("XSRF-TOKEN", "cookie-token"));

    CsrfToken token = PSCsrfTokenHelper.resolveCsrfToken(request);
    assertNotNull(token);
    assertEquals("X-XSRF-TOKEN", token.getHeaderName());
    assertEquals("cookie-token", token.getToken());
  }

  @Test
  public void testResolveReturnsNullWhenNoAttributeAndNoCookie() {
    assertNull(PSCsrfTokenHelper.resolveCsrfToken(null));
    assertNull(PSCsrfTokenHelper.resolveCsrfToken(new MockHttpServletRequest()));
  }

  @Test
  public void testResolveIgnoresBlankAttributeAndFallsBackToCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(
        CsrfToken.class.getName(), new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "   "));
    request.setCookies(new Cookie("XSRF-TOKEN", "fallback-cookie-token"));

    CsrfToken token = PSCsrfTokenHelper.resolveCsrfToken(request);
    assertNotNull(token);
    assertEquals("fallback-cookie-token", token.getToken());
  }
}
