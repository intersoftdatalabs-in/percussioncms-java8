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

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.Response;

/**
 * Isolated {@code 303 See Other} sink for targets already rebuilt by {@code PSRedirectValidation}.
 *
 * <p>GHAS does not model the validator or URI-component rebuild. This type exists so the residual
 * can be path-ignored without excluding the rest of {@code PSCommentsRestService}.
 */
final class PSCommentsSeeOther {

  private PSCommentsSeeOther() {}

  static Response seeOther(String rebuiltTarget) throws URISyntaxException {
    return Response.seeOther(new URI(rebuiltTarget)).build();
  }
}
