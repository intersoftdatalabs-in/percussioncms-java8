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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.integrations;

import com.percussion.delivery.spring.PSCsrfTokenHelper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Path("/integrations")
@Component
public class IntegrationsRestService {

  public IntegrationsRestService() {
    // NOOP
  }

  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    CsrfToken csrfToken = PSCsrfTokenHelper.resolveCsrfToken(request);
    if (csrfToken == null) {
      return;
    }
    // CR/LF strip before writing header values into the response
    // (CWE-113 / CodeQL java/http-response-splitting).
    response.setHeader(
        PSCsrfTokenHelper.RESPONSE_CSRF_HEADER,
        csrfToken.getHeaderName().replaceAll("[\\r\\n]", ""));
    response.setHeader(
        PSCsrfTokenHelper.RESPONSE_CSRF_TOKEN, csrfToken.getToken().replaceAll("[\\r\\n]", ""));
  }
}
