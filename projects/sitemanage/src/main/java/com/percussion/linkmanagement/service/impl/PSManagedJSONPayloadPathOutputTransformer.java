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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSFieldOutputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.util.IPSHtmlParameters;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Field output transformer to update the managed item paths on edit. This is a thin wrapper, calls
 * the managedlink service to do the actual work. This version follows the JSONPayload spec.
 *
 * @author Nate Chadwick
 */
public class PSManagedJSONPayloadPathOutputTransformer extends PSDefaultExtension
    implements IPSFieldOutputTransformer {
  private static final Logger log =
      LogManager.getLogger(PSManagedJSONPayloadPathOutputTransformer.class);

  /**
   * Jackson ObjectMapper for in-memory JSON construction. T2.x.7 hardening (issue #111): replaced
   * jettison {@code JSONObject} / {@code JSONArray} with Jackson {@code ObjectNode} / {@code
   * ArrayNode}.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private IPSManagedLinkService service;

  /*
   * (non-Javadoc)
   * @see com.percussion.extension.IPSUdfProcessor#processUdf(java.lang.Object[], com.percussion.server.IPSRequestContext)
   */
  @Override
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    PSExtensionParams ep = new PSExtensionParams(params);
    String jsonPayload = ep.getStringParam(0, null, true);

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
    String cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);

    if (log.isDebugEnabled()) {
      log.debug("Processing with Content Id:" + cid);
    }
    try {
      if (log.isDebugEnabled()) {
        log.debug("Parsing JSON Payload" + jsonPayload);
      }

      if (StringUtils.isEmpty(jsonPayload)) {
        return "";
      }

      object = (ObjectNode) MAPPER.readTree(jsonPayload);
      if (log.isDebugEnabled()) {
        log.debug("Done parsing payload, parsing " + IPSManagedLinkService.PERC_CONFIG + " array.");
      }

      ArrayNode objectArray = (ArrayNode) object.get(IPSManagedLinkService.PERC_CONFIG);
      if (log.isDebugEnabled()) {
        log.debug("Done parsing payload array");
      }

      String newPath = "";

      for (int i = 0; i < objectArray.size(); i++) {
        ObjectNode entry = (ObjectNode) objectArray.get(i);
        if (log.isDebugEnabled()) {
          log.debug("Processing payload entry " + i);
        }

        // Images
        if (entry.has(IPSManagedLinkService.PERC_IMAGEPATH)) {
          if (entry.has(IPSManagedLinkService.PERC_IMAGEPATH_LINKID)) {
            if (!StringUtils.isBlank(
                    entry.path(IPSManagedLinkService.PERC_IMAGEPATH_LINKID).asText())
                && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
              if (log.isDebugEnabled()) {
                log.debug(
                    "Getting updated path for Image entry: "
                        + entry.path(IPSManagedLinkService.PERC_IMAGEPATH_LINKID).asText()
                        + " with current path of "
                        + entry.path(IPSManagedLinkService.PERC_IMAGEPATH).asText());
              }
              newPath =
                  renderItemPath(entry.path(IPSManagedLinkService.PERC_IMAGEPATH_LINKID).asText());
              if (log.isDebugEnabled()) {
                log.debug(
                    "Updating payload for Image entry: "
                        + entry.path(IPSManagedLinkService.PERC_IMAGEPATH_LINKID).asText()
                        + " with new path of "
                        + newPath);
              }

              entry.put(IPSManagedLinkService.PERC_IMAGEPATH, newPath);
              objectArray.set(i, entry);
              if (log.isDebugEnabled()) {
                log.debug("Done updating.");
              }
            }
          }
        }

        // Files
        if (entry.has(IPSManagedLinkService.PERC_FILEPATH)) {
          if (entry.has(IPSManagedLinkService.PERC_FILEPATH_LINKID)) {
            if (!StringUtils.isBlank(
                    entry.path(IPSManagedLinkService.PERC_FILEPATH_LINKID).asText())
                && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
              if (log.isDebugEnabled()) {
                log.debug(
                    "Getting updated path for File entry: "
                        + entry.path(IPSManagedLinkService.PERC_FILEPATH_LINKID).asText()
                        + " with current path of "
                        + entry.path(IPSManagedLinkService.PERC_FILEPATH).asText());
              }
              newPath =
                  renderItemPath(entry.path(IPSManagedLinkService.PERC_FILEPATH_LINKID).asText());
              if (log.isDebugEnabled()) {
                log.debug(
                    "Updating payload for File entry: "
                        + entry.path(IPSManagedLinkService.PERC_FILEPATH_LINKID).asText()
                        + " with new path of "
                        + newPath);
              }

              entry.put(IPSManagedLinkService.PERC_FILEPATH, newPath);
              objectArray.set(i, entry);
              if (log.isDebugEnabled()) {
                log.debug("Done updating.");
              }
            }
          }
        }

        // Pages
        if (entry.has(IPSManagedLinkService.PERC_PAGEPATH)) {
          if (entry.has(IPSManagedLinkService.PERC_PAGEPATH_LINKID)) {
            if (!StringUtils.isBlank(
                    entry.path(IPSManagedLinkService.PERC_PAGEPATH_LINKID).asText())
                && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
              if (log.isDebugEnabled()) {
                log.debug(
                    "Getting updated path for Page entry: "
                        + entry.path(IPSManagedLinkService.PERC_PAGEPATH_LINKID).asText()
                        + " with current path of "
                        + entry.path(IPSManagedLinkService.PERC_PAGEPATH).asText());
              }
              newPath =
                  renderItemPath(entry.path(IPSManagedLinkService.PERC_PAGEPATH_LINKID).asText());
              if (log.isDebugEnabled()) {
                log.debug(
                    "Updating payload for File entry: "
                        + entry.path(IPSManagedLinkService.PERC_PAGEPATH_LINKID).asText()
                        + " with new path of "
                        + newPath);
              }
              entry.put(IPSManagedLinkService.PERC_PAGEPATH, newPath);
              objectArray.set(i, entry);
              if (log.isDebugEnabled()) {
                log.debug("Done updating.");
              }
            }
          }
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("Updating return payload.");
      }
      object.set(IPSManagedLinkService.PERC_CONFIG, objectArray);
      if (log.isDebugEnabled()) {
        log.debug("Done updating.");
      }
    } catch (JsonProcessingException ex) {
      log.error("An error occurred while trying to manage links in a JSONPayload field.");
      if (log.isDebugEnabled()) {
        log.debug("Error occurred.  Returning original payload: " + jsonPayload, ex);
      }
      return jsonPayload;
    }

    if (log.isDebugEnabled()) {
      log.debug("Returning updated payload with any managed path updates: " + object.toString());
    }
    return object.toString();
  }

  private String renderItemPath(String linkId) {
    return service.renderItemPath(null, linkId);
  }

  /* (non-Javadoc)
   * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef, java.io.File)
   */
  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    // This is for wiring the services
    PSSpringWebApplicationContextUtils.injectDependencies(this);
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
