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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.soln.p13n.tracking.web;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.soln.p13n.tracking.VisitorTrackingResponse;

/**
 * Utilities for JSON <-> POJO conversion of the p13n visitor tracking protocol.
 *
 * <p>T2.17.4a hardening (issue #145): migrated from json-lib to Jackson. The
 * 1 public static method is unchanged in signature; the implementation now
 * uses {@link ObjectMapper#readValue(String, Class)} instead of
 * {@code JSONObject.fromObject}/{@code JSONObject.toBean}.
 *
 * <p>The previous {@code JsonConfig} used a date processor
 * ({@code JsDateJsonValueProcessor}) that formatted dates as JavaScript
 * milliseconds; Jackson's default {@code Date} serialization/deserialization
 * also uses long milliseconds, so no custom date module is required. The
 * {@code setExcludes(["location", "request"])} call referenced field names
 * that do not exist on {@link VisitorTrackingResponse} (defensive no-op);
 * dropped from the new configuration. Transient fields are ignored by Jackson
 * by default (matching the previous {@code setIgnoreTransientFields(true)}).
 */
public class VisitorTrackingWebUtils {

    /**
     * T2.17.4a hardening (issue #145): shared Jackson ObjectMapper. Created
     * once per class; thread-safe per Jackson documentation.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * T2.17.4a hardening (issue #145): parses the JSON string from the
     * tracking service into a {@link VisitorTrackingResponse} POJO.
     *
     * @param json the JSON string, never <code>null</code>.
     * @return a fully populated {@link VisitorTrackingResponse}, never <code>null</code>.
     * @throws IOException if Jackson cannot parse or deserialize the input.
     */
    public static VisitorTrackingResponse jsonToTrackingResponse(String json) throws IOException {
        return MAPPER.readValue(json, VisitorTrackingResponse.class);
    }

    /**
     * T2.17.4a hardening (issue #145): serializes a {@link VisitorTrackingResponse}
     * POJO to a JSON string. Convenience method for symmetry with the
     * read side; no current callers in the project.
     *
     * @param response the POJO, never <code>null</code>.
     * @return the JSON representation, never <code>null</code>.
     * @throws JsonProcessingException if Jackson cannot serialize the POJO.
     */
    public static String trackingResponseToJson(VisitorTrackingResponse response) throws JsonProcessingException {
        return MAPPER.writeValueAsString(response);
    }
}
