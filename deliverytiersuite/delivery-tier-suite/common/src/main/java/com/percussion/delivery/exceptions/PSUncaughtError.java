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

package com.percussion.delivery.exceptions;

import com.percussion.error.PSExceptionUtils;
import com.percussion.security.utils.PSRedirectValidation;
import com.percussion.security.utils.PSValidatedRedirect;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
public class PSUncaughtError extends Throwable implements ExceptionMapper<Throwable> {
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_ERROR_PATH = "/error.html";
  private static final String SERVLET_ERROR_STATUS_CODE = "javax.servlet.error.status_code";

  private static final Logger log = LogManager.getLogger(PSUncaughtError.class);

  @Context private HttpServletRequest request;

  @Context private HttpServletResponse response;

  @Override
  public Response toResponse(Throwable exception) {
    try {
      if (request != null && response != null) {
        log.warn(
            "Page redirecting to error {} : . Error code {} : . Error message {} : ",
            request.getHeader("referer"),
            response.getStatus(),
            exception != null ? exception.getLocalizedMessage() : null);
        // CWE-601 / java/unvalidated-url-redirection #598: never build the error redirect
        // from the Referer header (open-redirect vector). Always target the root error page
        // (webapps/ROOT/error.html), because per-service webapps map /* to Jersey and have no
        // /error.html endpoint of their own.
        String safe = PSRedirectValidation.validateInternalRedirectUrl(DEFAULT_ERROR_PATH);
        String rebuilt = safe == null ? null : PSRedirectValidation.rebuildInternalRedirect(safe);
        if (rebuilt == null) {
          rebuilt = DEFAULT_ERROR_PATH;
        }
        // Guard against an infinite self-redirect loop if a service is ever mounted at root ("").
        if (rebuilt.equals(request.getRequestURI())) {
          return Response.status(resolveErrorStatus(exception))
              .entity("A server error happened. Please try your request again.")
              .type(MediaType.TEXT_PLAIN)
              .build();
        }
        if (!response.isCommitted()) {
          PSValidatedRedirect.send(response, rebuilt);
        }
      } else {
        logErrorMessage(exception);
      }
    } catch (Exception e) {
      logErrorMessage(exception);
    }
    return null;
  }

  private int resolveErrorStatus(Throwable exception) {
    Object attr = request.getAttribute(SERVLET_ERROR_STATUS_CODE);
    if (attr instanceof Integer) {
      int code = (Integer) attr;
      if (code >= 400 && code < 600) {
        return code;
      }
    }
    if (exception instanceof WebApplicationException) {
      Response errResp = ((WebApplicationException) exception).getResponse();
      if (errResp != null && errResp.getStatus() >= 400) {
        return errResp.getStatus();
      }
    }
    return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }

  private void logErrorMessage(Throwable exception) {
    String errorMessage = exception == null ? "Unknown error" : exception.getMessage();
    if (exception instanceof Exception) {
      errorMessage = PSExceptionUtils.getMessageForLog((Exception) exception);
      log.debug(PSExceptionUtils.getDebugMessageForLog((Exception) exception));
    } else {
      log.debug(exception);
    }
    log.error(
        "Exception occurred while redirecting to error.html, Original Error is : {}", errorMessage);
  }
}
