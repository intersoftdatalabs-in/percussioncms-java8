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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet filter that sets standard security response headers on every request. Defense in depth
 * for several of the 45+ CVEs in Spring 5.3.x + Spring Security 5.8.x (parent epic #73, T2.4
 * sub-task).
 *
 * <p>Headers set:
 *
 * <ul>
 *   <li>{@code X-Frame-Options: SAMEORIGIN} - clickjacking (CWE-1021)
 *   <li>{@code X-Content-Type-Options: nosniff} - MIME-sniffing (CWE-79 follow-up)
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin} - referrer-leak
 *   <li>{@code Strict-Transport-Security: max-age=31536000; includeSubDomains} - HTTPS downgrade
 *       (only emitted on secure (HTTPS) requests, so a misconfigured HTTP-only deployment does not
 *       get a stale HSTS header that browsers might honor on a future HTTPS port)
 *   <li>{@code X-XSS-Protection: 0} - the deprecated XSS auditor is disabled (per the modern
 *       recommendation; \`0\` rather than \`1; mode=block\` is correct)
 * </ul>
 *
 * <p>All values are constants on the class. Future tuning is a one-line change.
 *
 * <p>To register in web.xml:
 *
 * <pre>
 *   &lt;filter&gt;
 *     &lt;filter-name&gt;PSSecurityHeadersFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;com.percussion.security.servlet.PSSecurityHeadersFilter&lt;/filter-class&gt;
 *   &lt;/filter&gt;
 *   &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;PSSecurityHeadersFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *   &lt;/filter-mapping&gt;
 * </pre>
 */
public class PSSecurityHeadersFilter implements Filter {

  private static final String X_FRAME_OPTIONS = "X-Frame-Options";
  private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  private static final String REFERRER_POLICY = "Referrer-Policy";
  private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
  private static final String X_XSS_PROTECTION = "X-XSS-Protection";

  private static final String VALUE_SAMEORIGIN = "SAMEORIGIN";
  private static final String VALUE_NOSNIFF = "nosniff";
  private static final String VALUE_REFERRER_POLICY = "strict-origin-when-cross-origin";
  private static final String VALUE_HSTS = "max-age=31536000; includeSubDomains";
  private static final String VALUE_XSS_PROTECTION_DISABLED = "0";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No init params required; values are class constants.
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      // SetHeader (not addHeader) so a downstream filter / handler can override.
      httpResponse.setHeader(X_FRAME_OPTIONS, VALUE_SAMEORIGIN);
      httpResponse.setHeader(X_CONTENT_TYPE_OPTIONS, VALUE_NOSNIFF);
      httpResponse.setHeader(REFERRER_POLICY, VALUE_REFERRER_POLICY);
      httpResponse.setHeader(X_XSS_PROTECTION, VALUE_XSS_PROTECTION_DISABLED);

      // HSTS only on HTTPS — see class javadoc.
      if (request instanceof HttpServletRequest && ((HttpServletRequest) request).isSecure()) {
        httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, VALUE_HSTS);
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Nothing to clean up.
  }
}
