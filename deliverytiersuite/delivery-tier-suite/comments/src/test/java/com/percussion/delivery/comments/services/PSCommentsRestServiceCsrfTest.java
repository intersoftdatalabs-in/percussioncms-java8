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
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

  @Test
  public void testDeferredHandlerDoesNotEmitCookieOnCsrfExemptPostListOrLikes() throws Exception {
    CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
    PSCsrfSecurityRequestMatcher matcher =
        new PSCsrfSecurityRequestMatcher(
            "^(GET|HEAD|TRACE|OPTIONS)$",
            "/comment/list,/comment/moderation/delete,/comment/moderation/defaultModerationState,"
                + "/comment/moderation/reject,/comment/moderation/approve,/comment/pageswithcomments/,"
                + "/comment/defaultModerationState,/comment/moderation/,/moderation/asmoderator,"
                + "/comment/updateOldSiteEntries/,/likes/",
            true);
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");

    CsrfFilter filter = new CsrfFilter(repo);
    filter.setRequireCsrfProtectionMatcher(matcher);
    filter.setRequestHandler(requestHandler);

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

    String token1 = headResp.getHeader("X-CSRF-TOKEN");
    assertNotNull(token1);
    Cookie headCookie = headResp.getCookie("XSRF-TOKEN");
    assertNotNull(headCookie);
    assertEquals(token1, headCookie.getValue());

    // 2. Concurrent POST /comment/list with NO cookies must NOT emit a competing XSRF-TOKEN cookie.
    MockHttpServletRequest listReq =
        new MockHttpServletRequest("POST", "/perc-comments-services/comment/list");
    MockHttpServletResponse listResp = new MockHttpServletResponse();
    AtomicBoolean listChainInvoked = new AtomicBoolean(false);
    filter.doFilter(
        listReq,
        listResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            listChainInvoked.set(true);
          }
        });

    assertTrue(listChainInvoked.get(), "Expected POST /comment/list to pass CsrfFilter");
    assertNull(
        listResp.getCookie("XSRF-TOKEN"),
        "CSRF-exempt POST /comment/list must not emit XSRF-TOKEN cookie");

    // 3. Concurrent POST /likes/total/... with NO cookies must NOT emit a competing XSRF-TOKEN
    // cookie.
    MockHttpServletRequest likesReq =
        new MockHttpServletRequest("POST", "/perc-comments-services/likes/total/site/PAGE/");
    MockHttpServletResponse likesResp = new MockHttpServletResponse();
    AtomicBoolean likesChainInvoked = new AtomicBoolean(false);
    filter.doFilter(
        likesReq,
        likesResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            likesChainInvoked.set(true);
          }
        });

    assertTrue(likesChainInvoked.get(), "Expected POST /likes/... to pass CsrfFilter");
    assertNull(
        likesResp.getCookie("XSRF-TOKEN"),
        "CSRF-exempt POST /likes/... must not emit XSRF-TOKEN cookie");

    // 4. Subsequent POST /comment/addcomment with token1 in cookie and _csrf parameter succeeds.
    MockHttpServletRequest addReq =
        new MockHttpServletRequest("POST", "/perc-comments-services/comment/addcomment");
    addReq.setCookies(new Cookie("XSRF-TOKEN", token1));
    addReq.setParameter("_csrf", token1);
    MockHttpServletResponse addResp = new MockHttpServletResponse();
    AtomicBoolean addChainInvoked = new AtomicBoolean(false);
    filter.doFilter(
        addReq,
        addResp,
        new MockFilterChain() {
          @Override
          public void doFilter(
              javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
            addChainInvoked.set(true);
          }
        });

    assertTrue(addChainInvoked.get(), "Expected POST /comment/addcomment with token1 to succeed");
  }

  @Test
  public void testCommentsSecurityXmlConfiguresDeferredCsrfRequestHandler() throws Exception {
    File securityXml = new File("src/main/java/webapp/WEB-INF/security.xml");
    assertTrue(securityXml.exists(), "Expected comments security.xml to exist");

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc = dbf.newDocumentBuilder().parse(securityXml);

    NodeList csrfNodes =
        doc.getElementsByTagNameNS("http://www.springframework.org/schema/security", "csrf");
    assertEquals(1, csrfNodes.getLength(), "Expected single <security:csrf> element");
    Element csrfEl = (Element) csrfNodes.item(0);
    assertEquals("csrfRequestHandler", csrfEl.getAttribute("request-handler-ref"));

    NodeList beanNodes =
        doc.getElementsByTagNameNS("http://www.springframework.org/schema/beans", "bean");
    Element handlerBean = null;
    for (int i = 0; i < beanNodes.getLength(); i++) {
      Element el = (Element) beanNodes.item(i);
      if ("csrfRequestHandler".equals(el.getAttribute("id"))) {
        handlerBean = el;
        break;
      }
    }
    assertNotNull(handlerBean, "Expected csrfRequestHandler bean in security.xml");
    assertEquals(
        CsrfTokenRequestAttributeHandler.class.getName(), handlerBean.getAttribute("class"));

    NodeList propNodes =
        handlerBean.getElementsByTagNameNS(
            "http://www.springframework.org/schema/beans", "property");
    boolean foundAttrName = false;
    for (int i = 0; i < propNodes.getLength(); i++) {
      Element prop = (Element) propNodes.item(i);
      if ("csrfRequestAttributeName".equals(prop.getAttribute("name"))
          && "_csrf".equals(prop.getAttribute("value"))) {
        foundAttrName = true;
        break;
      }
    }
    assertTrue(
        foundAttrName,
        "Expected csrfRequestHandler to configure csrfRequestAttributeName=\"_csrf\"");
  }
}
