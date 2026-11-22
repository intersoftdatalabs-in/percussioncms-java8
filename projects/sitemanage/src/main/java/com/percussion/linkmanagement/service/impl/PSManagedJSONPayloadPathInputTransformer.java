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
package com.percussion.linkmanagement.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.data.PSConversionException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSFieldInputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.util.IPSHtmlParameters;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A field input transformer to process/update an item path. Expects a JSON object with an array
 * named percJSONConfigas an input, calls the managedlink service to manage any links in the
 * payload, the payload is updated and returned with the resulting link id's updated.
 *
 * <p>For new items the item id is not yet created, so PSManagedItemPathPreProcessor should be added
 * to initialized new item links in the service to track that and set a request private object as a
 * marker. The managed link post processor exit checks the private object and if it is there and if
 * its value is true then updates the parent id.
 *
 * @author NateChadwick
 */
public class PSManagedJSONPayloadPathInputTransformer extends PSDefaultExtension
    implements IPSFieldInputTransformer {

  private static final Logger log =
      LogManager.getLogger(PSManagedJSONPayloadPathInputTransformer.class);

  /**
   * Jackson ObjectMapper for in-memory JSON construction. T2.x.7 hardening (issue #111): replaced
   * jettison {@code JSONObject} / {@code JSONArray} with Jackson {@code ObjectNode} / {@code
   * ArrayNode}.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private IPSManagedLinkService service;

  /* (non-Javadoc)
   * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef, java.io.File)
   */
  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    // This is for wiring the services
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    PSExtensionParams ep = new PSExtensionParams(params);
    String jsonPayload = ep.getStringParam(0, null, true);

    if (StringUtils.isBlank(jsonPayload)) {
      return "";
    }

    // Fix Old Data for Image Slider
    if (request != null && "percImageSlider.xml".equalsIgnoreCase(request.getRequestPage())) {
      if (jsonPayload != null) {
        jsonPayload =
            jsonPayload.replaceAll(
                IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_CONFIG_ATTR,
                IPSManagedLinkService.PERC_CONFIG);
        jsonPayload =
            jsonPayload.replaceAll(
                IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_IMAGEPATH_ATTR,
                IPSManagedLinkService.PERC_IMAGEPATH);
        log.info("Updated Old data in ImageSlider");
      }
    }

    ObjectNode object = null;

    try {
      log.debug("Parsing JSONPayload: " + jsonPayload);
      object = (ObjectNode) MAPPER.readTree(jsonPayload);
      log.debug("Returned from parsing JSONPayload.");
      log.debug("Parsing for " + IPSManagedLinkService.PERC_CONFIG);
      ArrayNode objectArray;
      // T2.x.7: PERC_CONFIG may be missing or not an array. In jettison,
      // getJSONArray threw JSONException for both; in Jackson, get returns
      // null for missing, and a non-array JsonNode for type-mismatch.
      // Both cases mean "no managed links in this payload", which the
      // caller treats as a skip signal (returns null).
      if (object.get(IPSManagedLinkService.PERC_CONFIG) instanceof ArrayNode) {
        objectArray = (ArrayNode) object.get(IPSManagedLinkService.PERC_CONFIG);
        log.debug("Found " + IPSManagedLinkService.PERC_CONFIG + " array.");
      } else {
        log.error(
            "An error occurred while trying to manage links in a JSONPayload field.",
            "PERC_CONFIG is missing or not an array");
        return null;
      }

      for (int i = 0; i < objectArray.size(); i++) {
        ObjectNode entry = (ObjectNode) objectArray.get(i);
        log.debug("Processing array entry " + i);
        manageJsonPath(
            request,
            entry,
            IPSManagedLinkService.PERC_IMAGEPATH,
            IPSManagedLinkService.PERC_IMAGEPATH_LINKID,
            "Image");
        manageJsonPath(
            request,
            entry,
            IPSManagedLinkService.PERC_FILEPATH,
            IPSManagedLinkService.PERC_FILEPATH_LINKID,
            "File");
        manageJsonPath(
            request,
            entry,
            IPSManagedLinkService.PERC_PAGEPATH,
            IPSManagedLinkService.PERC_PAGEPATH_LINKID,
            "Page");
      }
      log.debug("Updating JSONPayload to use updated array");
      object.set(IPSManagedLinkService.PERC_CONFIG, objectArray);
      log.debug("Done updating.");

    } catch (JsonProcessingException ex) {
      log.error(
          "An error occurred while trying to manage links in a JSONPayload field.",
          PSExceptionUtils.getMessageForLog(ex));
      log.debug("Error occurred. Returning original payload:" + jsonPayload, ex);
      return jsonPayload;
    }

    log.debug("Returning updated payload:" + object.toString());
    return object.toString();
  }

  private static String getJsonText(ObjectNode entry, String fieldName) {
    JsonNode node = entry.get(fieldName);
    if (node == null || node.isNull()) {
      return "";
    }
    return node.asText("");
  }

  private void manageJsonPath(
      IPSRequestContext request,
      ObjectNode entry,
      String pathKey,
      String linkIdKey,
      String entryType) {
    if (!entry.has(pathKey) || !entry.has(linkIdKey)) {
      return;
    }
    String path = getJsonText(entry, pathKey);
    String linkId = getJsonText(entry, linkIdKey);
    log.debug("Processing " + entryType + " entry with path " + path + " and Link Id" + linkId);
    String newLinkId = manageLinks(request, path, linkId);
    log.debug("Updating " + entryType + " JSONPayload entry:" + path + " with Link Id" + newLinkId);
    entry.put(linkIdKey, StringUtils.defaultString(newLinkId));
    log.debug("Done updating.");
  }

  private String manageLinks(IPSRequestContext request, String path, String linkId) {

    String result = "";
    String cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
    if (StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid)) {
      service.initNewItemLinks();
      result = service.manageItemPath(null, path, linkId);
      request.setPrivateObject(PSManagedLinksPostProcessor.PERC_UPDATE_NEW_MANAGED_LINKS, true);
    } else {
      cid = PSGuidManagerLocator.getGuidMgr().makeGuid(new PSLocator(cid)).toString();
      result = service.manageItemPath(cid, path, linkId);
    }
    return result;
  }

  /**
   * Setter for dependency injection
   *
   * @param service the service to set
   */
  public void setService(IPSManagedLinkService service) {
    this.service = service;
  }
}
