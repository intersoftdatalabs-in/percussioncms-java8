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
package com.percussion.delivery.comments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.spring.PSCsrfSecurityRequestMatcher;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

public class PSCommentsRestServiceCsrfTest {

  @Test
  public void testFirstVisitCsrfHeadAndSubsequentPostSucceeds() throws Exception {
    CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
    PSCsrfSecurityRequestMatcher matcher =
        new PSCsrfSecurityRequestMatcher(
            "^(GET|HEAD|TRACE|OPTIONS)$",
            "/comment/list,/comment/moderation/delete,/comment/moderation/defaultModerationState,"
                + "/comment/moderation/reject,/comment/moderation/approve,/comment/pageswithcomments/,"
                + "/comment/defaultModerationState,/comment/moderation/,/moderation/asmoderator,"
                + "/comment/updateOldSiteEntries/,/likes/",
            true);
    CsrfFilter filter = new CsrfFilter(repo);
    filter.setRequireCsrfProtectionMatcher(matcher);

    PSCommentsRestService service = new PSCommentsRestService(null);

    // 1. First-time visitor sends HEAD /perc-comments-services/comment/csrf with NO cookies.
    MockHttpServletRequest headReq =
        new MockHttpServletRequest("HEAD", "/perc-comments-services/comment/csrf");
    MockHttpServletResponse headResp = new MockHttpServletResponse();

    filter.doFilter(
        headReq,
        headResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            service.csrf((HttpServletRequest) req, (HttpServletResponse) res);
          }
        });

    assertEquals("X-XSRF-TOKEN", headResp.getHeader("X-CSRF-HEADER"));
    String token = headResp.getHeader("X-CSRF-TOKEN");
    assertNotNull(token, "Expected X-CSRF-TOKEN header on first visit");
    assertFalse(token.trim().isEmpty(), "Expected non-blank X-CSRF-TOKEN header");

    Cookie xsrfCookie = headResp.getCookie("XSRF-TOKEN");
    assertNotNull(xsrfCookie, "Expected XSRF-TOKEN cookie on first visit");
    assertEquals(token, xsrfCookie.getValue());

    // 2. Submitting POST /comment/addcomment with the cookie and _csrf parameter passes CsrfFilter.
    MockHttpServletRequest postReq =
        new MockHttpServletRequest("POST", "/perc-comments-services/comment/addcomment");
    postReq.setCookies(new Cookie("XSRF-TOKEN", token));
    postReq.setParameter("_csrf", token);
    MockHttpServletResponse postResp = new MockHttpServletResponse();

    AtomicBoolean chainInvoked = new AtomicBoolean(false);
    filter.doFilter(
        postReq,
        postResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            chainInvoked.set(true);
          }
        });

    assertTrue(chainInvoked.get(), "Expected POST with valid _csrf token to pass CsrfFilter");

    // 3. Submitting POST /comment/addcomment WITHOUT _csrf is rejected with 403.
    MockHttpServletRequest missingTokenReq =
        new MockHttpServletRequest("POST", "/perc-comments-services/comment/addcomment");
    missingTokenReq.setCookies(new Cookie("XSRF-TOKEN", token));
    MockHttpServletResponse missingTokenResp = new MockHttpServletResponse();

    AtomicBoolean rejectedChainInvoked = new AtomicBoolean(false);
    filter.doFilter(
        missingTokenReq,
        missingTokenResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            rejectedChainInvoked.set(true);
          }
        });

    assertFalse(rejectedChainInvoked.get(), "Expected POST without _csrf token to be blocked");
    assertEquals(403, missingTokenResp.getStatus());
  }

  @Test
  public void testCsrfStripsCrLfFromCookieFallback() {
    PSCommentsRestService service = new PSCommentsRestService(null);
    MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/comment/csrf");
    request.setCookies(new Cookie("XSRF-TOKEN", "tok\r\nInjected: yes"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.csrf(request, response);

    assertEquals("X-XSRF-TOKEN", response.getHeader("X-CSRF-HEADER"));
    assertEquals("tokInjected: yes", response.getHeader("X-CSRF-TOKEN"));
  }

  @Test
  public void testCsrfSetsNoHeadersWhenDisabledAndNoCookie() {
    PSCommentsRestService service = new PSCommentsRestService(null);
    MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/comment/csrf");
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.csrf(request, response);

    assertNull(response.getHeader("X-CSRF-HEADER"));
    assertNull(response.getHeader("X-CSRF-TOKEN"));
  }
}
