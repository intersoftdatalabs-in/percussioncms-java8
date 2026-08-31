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
package com.percussion.soln.p13n.delivery.web;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.soln.p13n.delivery.data.DeliveryRequest;
import com.percussion.soln.p13n.delivery.data.DeliveryResponse;

/**
 * Utilities for JSON <-> POJO conversion of the p13n delivery tier protocol.
 *
 * <p>T2.17.4a hardening (issue #145): migrated from json-lib to Jackson. The
 * 4 public static methods are unchanged in signature; the implementation
 * now uses {@link ObjectMapper#readValue(String, Class)} and
 * {@link ObjectMapper#writeValueAsString(Object)} instead of
 * {@code JSONObject.fromObject}/{@code JSONObject.toBean} and
 * {@code JSONArray.toCollection}.
 *
 * <p>Behavioral notes for the migration:
 * <ul>
 *   <li>Jackson's default {@code Date} serialization is long milliseconds
 *       (since epoch), which matches the json-lib {@code JsDateJsonValueProcessor}
 *       behavior; no custom date module is required.</li>
 *   <li>Jackson's nested-array deserialization handles {@code listItem.snippets}
 *       in a single pass, so the manual {@code optJSONObject}/{@code optJSONArray}/
 *       {@code JSONArray.toCollection} walk in the previous {@code jsonToRequest}
 *       implementation is no longer needed. The {@code DeliveryRequest.listItem.snippets}
 *       field is populated as part of the standard Jackson tree walk.</li>
 *   <li>Transient fields are ignored by Jackson by default (matching the previous
 *       {@code setIgnoreTransientFields(true)} setting).</li>
 *   <li>The previous {@code setExcludes([...])} call referenced field names that
 *       did not exist on {@code VisitorTrackingResponse} (defensive no-op);
 *       dropped from the new configuration.</li>
 * </ul>
 */
public class DeliveryWebUtils {

    /**
     * T2.17.4a hardening (issue #145): shared Jackson ObjectMapper. Created once
     * per class; thread-safe per Jackson documentation.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * T2.17.4a hardening (issue #145): parses the JSON string from the
     * delivery tier into a {@link DeliveryRequest} POJO. Returns a
     * deserialized POJO with the standard Jackson nested-array handling
     * (the {@code listItem.snippets} field is populated as part of the
     * same readValue call).
     *
     * @param json the JSON string, never <code>null</code>.
     * @return a fully populated {@link DeliveryRequest}, never <code>null</code>.
     * @throws IOException if Jackson cannot parse or deserialize the input.
     */
    public static DeliveryRequest jsonToRequest(String json) throws IOException {
        return MAPPER.readValue(json, DeliveryRequest.class);
    }

    /**
     * T2.17.4a hardening (issue #145): serializes the {@link DeliveryRequest}
     * POJO to a JSON string. {@code Date} fields are emitted as long
     * milliseconds (matching the json-lib {@code JsDateJsonValueProcessor}
     * output that the delivery tier was historically consuming).
     *
     * @param request the POJO, never <code>null</code>.
     * @return the JSON representation, never <code>null</code>.
     * @throws JsonProcessingException if Jackson cannot serialize the POJO.
     */
    public static String requestToJson(DeliveryRequest request) throws JsonProcessingException {
        return MAPPER.writeValueAsString(request);
    }

    /**
     * T2.17.4a hardening (issue #145): parses the JSON string from the
     * delivery tier into a {@link DeliveryResponse} POJO.
     *
     * @param json the JSON string, never <code>null</code>.
     * @return a fully populated {@link DeliveryResponse}, never <code>null</code>.
     * @throws IOException if Jackson cannot parse or deserialize the input.
     */
    public static DeliveryResponse jsonToResponse(String json) throws IOException {
        return MAPPER.readValue(json, DeliveryResponse.class);
    }

    /**
     * T2.17.4a hardening (issue #145): serializes the {@link DeliveryResponse}
     * POJO to a JSON string. {@code Date} fields are emitted as long
     * milliseconds.
     *
     * @param response the POJO, never <code>null</code>.
     * @return the JSON representation, never <code>null</code>.
     * @throws JsonProcessingException if Jackson cannot serialize the POJO.
     */
    public static String responseToJson(DeliveryResponse response) throws JsonProcessingException {
        return MAPPER.writeValueAsString(response);
    }
}
