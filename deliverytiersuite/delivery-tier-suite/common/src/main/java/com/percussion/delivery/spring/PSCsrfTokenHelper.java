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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

/**
 * Resolves the active Spring Security {@link CsrfToken} for delivery-tier {@code /csrf} endpoints.
 *
 * <p>On a first-time visit (no incoming {@code XSRF-TOKEN} cookie), Spring Security's {@code
 * CsrfFilter} exposes a deferred {@link CsrfToken} via request attributes and writes the {@code
 * Set-Cookie: XSRF-TOKEN=...} header lazily when {@link CsrfToken#getToken()} is invoked. Reading
 * from the request attribute first ensures first-time visitors receive both the cookie and the
 * {@code X-CSRF-HEADER}/{@code X-CSRF-TOKEN} response headers.
 */
public final class PSCsrfTokenHelper {

  public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  public static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  public static final String DEFAULT_CSRF_PARAM_NAME = "_csrf";
  public static final String RESPONSE_CSRF_HEADER = "X-CSRF-HEADER";
  public static final String RESPONSE_CSRF_TOKEN = "X-CSRF-TOKEN";

  private PSCsrfTokenHelper() {}

  /**
   * Resolves the CSRF token from the request's Spring Security {@link CsrfToken} attributes,
   * falling back to the {@code XSRF-TOKEN} cookie when the filter attribute is absent.
   *
   * @param request the incoming HTTP request, may be {@code null}
   * @return the resolved {@link CsrfToken}, or {@code null} if unavailable
   */
  public static CsrfToken resolveCsrfToken(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    Object attr = request.getAttribute(CsrfToken.class.getName());
    if (!(attr instanceof CsrfToken)) {
      attr = request.getAttribute(DEFAULT_CSRF_PARAM_NAME);
    }

    if (attr instanceof CsrfToken) {
      CsrfToken token = (CsrfToken) attr;
      String tokenValue = token.getToken();
      if (!isBlank(tokenValue)) {
        String headerName =
            isBlank(token.getHeaderName()) ? DEFAULT_CSRF_HEADER_NAME : token.getHeaderName();
        String paramName =
            isBlank(token.getParameterName()) ? DEFAULT_CSRF_PARAM_NAME : token.getParameterName();
        return new DefaultCsrfToken(headerName, paramName, tokenValue);
      }
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (CSRF_COOKIE_NAME.equals(cookie.getName()) && !isBlank(cookie.getValue())) {
          return new DefaultCsrfToken(
              DEFAULT_CSRF_HEADER_NAME, DEFAULT_CSRF_PARAM_NAME, cookie.getValue());
        }
      }
    }

    return null;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
