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
package com.percussion.linkmanagement.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.util.IPSHtmlParameters;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PSManagedJSONPayloadPathOutputTransformer} and {@link
 * PSManagedJSONPayloadPathInputTransformer} covering JSON {@code null} link IDs (issue #354).
 */
public class PSManagedJSONPayloadPathTransformersTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Mockery context;
  private IPSManagedLinkService managedLinkService;
  private IPSRequestContext request;

  @Before
  public void setUp() {
    context = new JUnit4Mockery();
    managedLinkService = context.mock(IPSManagedLinkService.class);
    request = context.mock(IPSRequestContext.class);
  }

  @Test
  public void outputTransformerShouldSkipJsonNullMissingAndBlankLinkIds() throws Exception {
    PSManagedJSONPayloadPathOutputTransformer transformer =
        new PSManagedJSONPayloadPathOutputTransformer();
    transformer.setService(managedLinkService);

    context.checking(
        new Expectations() {
          {
            allowing(request).getRequestPage();
            will(returnValue("percImageSlider.xml"));
            allowing(request).getParameter(IPSHtmlParameters.SYS_CONTENTID);
            will(returnValue("10500"));
            oneOf(managedLinkService).renderItemPath(null, "131");
            will(returnValue("/Assets/qa-assets/qa-image-18-updated.png"));
          }
        });

    String payload =
        "{\"sliderName\":\"hero\",\"percJSONConfig\":[{"
            + "\"percImagePath\":\"/Assets/qa-assets/qa-image-18.png\","
            + "\"percImagePathLinkId\":\"131\","
            + "\"percFilePath\":\"/Assets/doc.pdf\","
            + "\"percFilePathLinkId\":null,"
            + "\"percPagePath\":\"\","
            + "\"percPagePathLinkId\":null"
            + "}]}";

    String result = (String) transformer.processUdf(new Object[] {payload}, request);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    JsonNode entry = root.get(IPSManagedLinkService.PERC_CONFIG).get(0);

    assertEquals(
        "/Assets/qa-assets/qa-image-18-updated.png",
        entry.get(IPSManagedLinkService.PERC_IMAGEPATH).asText());
    assertEquals("/Assets/doc.pdf", entry.get(IPSManagedLinkService.PERC_FILEPATH).asText());
    assertEquals("", entry.get(IPSManagedLinkService.PERC_PAGEPATH).asText());
    context.assertIsSatisfied();
  }

  @Test
  public void inputTransformerShouldStoreEmptyStringForUnmanagedLinkAndNotPassLiteralNull()
      throws Exception {
    PSManagedJSONPayloadPathInputTransformer transformer =
        new PSManagedJSONPayloadPathInputTransformer();
    transformer.setService(managedLinkService);

    context.checking(
        new Expectations() {
          {
            allowing(request).getRequestPage();
            will(returnValue("percImageSlider.xml"));
            allowing(request).getParameter(IPSHtmlParameters.SYS_CONTENTID);
            will(returnValue(""));
            allowing(request)
                .setPrivateObject(PSManagedLinksPostProcessor.PERC_UPDATE_NEW_MANAGED_LINKS, true);
            allowing(managedLinkService).initNewItemLinks();
            oneOf(managedLinkService).manageItemPath(null, "/Assets/img.png", "");
            will(returnValue("131"));
            oneOf(managedLinkService).manageItemPath(null, "", "");
            will(returnValue(null));
          }
        });

    String payload =
        "{\"sliderName\":\"hero\",\"percJSONConfig\":[{"
            + "\"percImagePath\":\"/Assets/img.png\","
            + "\"percImagePathLinkId\":\"\","
            + "\"percPagePath\":\"\","
            + "\"percPagePathLinkId\":null"
            + "}]}";

    String result = (String) transformer.processUdf(new Object[] {payload}, request);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    JsonNode entry = root.get(IPSManagedLinkService.PERC_CONFIG).get(0);

    assertEquals("131", entry.get(IPSManagedLinkService.PERC_IMAGEPATH_LINKID).asText());
    JsonNode pageLinkIdNode = entry.get(IPSManagedLinkService.PERC_PAGEPATH_LINKID);
    assertFalse(pageLinkIdNode.isNull());
    assertEquals("", pageLinkIdNode.asText());
    context.assertIsSatisfied();
  }
}
