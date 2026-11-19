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

package com.percussion.integrations.ems.rest;

import static org.apache.commons.lang3.Validate.notNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.security.TLSSocketFactory;
import com.percussion.util.PSSiteManageBean;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Functions as a proxy on the CMS side for calling the remote DTS EMS Api client. Used from within
 * content editors and content type fields.
 *
 * @author natechadwick
 */
@Path("/ems")
@PSSiteManageBean("emsAPIService")
public class PSEmsRestService {

  private static final String BUILDINGS_PATH = "/perc-integrations/integrations/ems/buildings";
  private static final String GROUPS_PATH = "/perc-integrations/integrations/ems/groups";
  private static final String EVENTTYPES_PATH = "/perc-integrations/integrations/ems/eventtypes";
  private static final String BOOKINGS_PATH = "/perc-integrations/integrations/ems/bookings";
  private static final String MC_CALENDARS_PATH =
      "/perc-integrations/integrations/ems/mc/calendars";
  private static final String MC_LOCATIONS_PATH =
      "/perc-integrations/integrations/ems/mc/locations";
  private static final String MC_GROUPINGS_PATH =
      "/perc-integrations/integrations/ems/mc/groupings";
  private static final String MC_EVENTTYPES_PATH =
      "/perc-integrations/integrations/ems/mc/eventtypes";
  private static final String MC_EVENTS_PATH = "/perc-integrations/integrations/ems/mc/events";
  private static final String MC_FEATUREDEVENTS_PATH =
      "/perc-integrations/integrations/ems/mc/featuredevents";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** The delivery service initialized by constructor, never <code>null</code>. */
  private IPSDeliveryInfoService deliveryService;

  private static final Logger log = LogManager.getLogger(PSEmsRestService.class);

  // Generic client-facing error message used to avoid leaking internal exception details
  // (CWE-209 / CodeQL java/error-message-exposure). The detailed exception is always logged
  // server-side via PSExceptionUtils.getMessageForLog before this generic message is returned.
  private static final String GENERIC_EMS_ERROR =
      "An error occurred while communicating with the EMS integration service";

  /** The license Override if any. */
  private String licenseId = "";

  @Autowired
  public PSEmsRestService(IPSDeliveryInfoService deliveryService) {
    notNull(deliveryService);
    this.deliveryService = deliveryService;
  }

  public void setLicenseOverride(String licenseId) {
    this.licenseId = licenseId;
  }

  public String getLicenseOverride() {
    return this.licenseId;
  }

  @GET
  @Path("/buildings")
  @Produces(MediaType.APPLICATION_XML)
  public Response getBuildings() {
    return executeEmsGet(
        BUILDINGS_PATH, "buildings", "Error pulling buildings from DTS. Error: {}");
  }

  @GET
  @Path("/eventtypes")
  @Produces(MediaType.APPLICATION_XML)
  public Response getEventTypes() {
    return executeEmsGet(
        EVENTTYPES_PATH, "eventtypes", "Error pulling Event Types from DTS. Error: {}");
  }

  @GET
  @Path("/groups")
  @Produces(MediaType.APPLICATION_XML)
  public Response getGroups() {
    return executeEmsGet(GROUPS_PATH, "groups", "Error pulling Groups from DTS. Error: {}");
  }

  @POST
  @Path("/bookings")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getBookings(PSBookingsQuery query) {
    log.debug("Entering getBookings...");
    return executeEmsPost(
        BOOKINGS_PATH, query, "bookings", "Error pulling Groups from DTS. Error: {}");
  }

  @GET
  @Path("/mc/groupings")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCGroupings() {
    return executeEmsGet(
        MC_GROUPINGS_PATH, "MC groupings", "Error pulling Groups from DTS. Error: {}");
  }

  @GET
  @Path("/mc/locations")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCLocations() {
    return executeEmsGet(
        MC_LOCATIONS_PATH, "MC locations", "Error pulling MC Locations from DTS. Error: {}");
  }

  @GET
  @Path("/mc/eventtypes")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCEventTypes() {
    return executeEmsGet(
        MC_EVENTTYPES_PATH, "MC eventtypes", "Error pulling Event Types from DTS. Error: {}");
  }

  @GET
  @Path("/mc/calendars")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCCalendars() {
    return executeEmsGet(
        MC_CALENDARS_PATH, "MC Calendars", "Error pulling MC Calendars from DTS. Error: {}");
  }

  @POST
  @Path("/mc/events")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getMCEvents(PSEventQuery query) {
    log.debug("Entering getMCEvents...");
    return executeEmsPost(
        MC_EVENTS_PATH, query, "MC Events", "Error pulling MC Events from DTS. Error: {}");
  }

  @POST
  @Path("/mc/featuredevents")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getMCFutureEvents(PSFeaturedEventsQuery query) {
    log.debug("Entering getMCFeaturedEvents...");
    return executeEmsPost(
        MC_FEATUREDEVENTS_PATH,
        query,
        "MC Featured Events",
        "Error pulling MC Featured Events from DTS. Error: {}");
  }

  private Response executeEmsGet(String path, String resourceLabel, String errorLogPattern) {
    PSDeliveryInfo server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INTEGRATIONS);
    if (server == null) {
      String msg =
          "Unable to locate delivery server for perc-integration service.  Verify the service is registered in the delivery-servers.xml and try again";
      log.error(msg);
      return Response.serverError().entity(msg).build();
    }

    try {
      URI targetUri = URI.create(server.getUrl()).resolve(path);
      HttpGet get = new HttpGet(targetUri);
      get.setHeader("Content-Type", MediaType.APPLICATION_XML);
      get.setHeader("Accept", MediaType.APPLICATION_XML);

      String body = executeEmsRequest(targetUri, get, resourceLabel);
      return Response.status(Status.OK).entity(body).build();
    } catch (Exception e) {
      log.error(errorLogPattern, PSExceptionUtils.getMessageForLog(e));
      return Response.serverError().entity(GENERIC_EMS_ERROR).build();
    }
  }

  private Response executeEmsPost(
      String path, Object query, String resourceLabel, String errorLogPattern) {
    PSDeliveryInfo server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INTEGRATIONS);
    if (server == null) {
      String msg =
          "Unable to locate delivery server for perc-integration service.  Verify the service is registered in the delivery-servers.xml and try again";
      log.error(msg);
      return Response.serverError().entity(msg).build();
    }

    try {
      URI targetUri = URI.create(server.getUrl()).resolve(path);
      log.debug("Building Request Entity...");
      String payload = MAPPER.writeValueAsString(query);
      log.debug("Built Request Entity");

      HttpPost post = new HttpPost(targetUri);
      post.setHeader("Content-Type", MediaType.APPLICATION_JSON);
      post.setHeader("Accept", MediaType.APPLICATION_JSON);
      post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

      String body = executeEmsRequest(targetUri, post, resourceLabel);
      return Response.ok(body, MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      log.error(errorLogPattern, PSExceptionUtils.getMessageForLog(e));
      return Response.serverError().entity(GENERIC_EMS_ERROR).build();
    }
  }

  private String executeEmsRequest(URI targetUri, HttpUriRequestBase request, String resourceLabel)
      throws Exception {
    PoolingHttpClientConnectionManagerBuilder cmBuilder =
        PoolingHttpClientConnectionManagerBuilder.create();
    if ("https".equalsIgnoreCase(targetUri.getScheme())) {
      cmBuilder.setTlsSocketStrategy(new TLSSocketFactory().createTlsSocketStrategy());
    }

    try (CloseableHttpClient client =
        HttpClients.custom().setConnectionManager(cmBuilder.build()).build()) {
      HttpResult result =
          client.execute(
              request,
              response -> {
                int code = response.getCode();
                HttpEntity entity = response.getEntity();
                String body = "";
                if (entity != null) {
                  try (InputStream is = entity.getContent()) {
                    body = is == null ? "" : IOUtils.toString(is, StandardCharsets.UTF_8);
                  }
                }
                return new HttpResult(code, body);
              });
      if (result.code != HttpStatus.SC_OK) {
        throw new IllegalStateException(
            "Invalid response code:"
                + result.code
                + " was received when listing "
                + resourceLabel
                + ".");
      }
      return result.body;
    }
  }

  private static final class HttpResult {
    final int code;
    final String body;

    HttpResult(int code, String body) {
      this.code = code;
      this.body = body;
    }
  }
}
