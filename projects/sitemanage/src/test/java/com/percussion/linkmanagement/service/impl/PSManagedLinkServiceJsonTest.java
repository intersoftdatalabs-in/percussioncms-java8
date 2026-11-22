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
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PSManagedLinkService#renderLinksInJSON} covering Jackson {@code NullNode}
 * and missing/blank link ID handling (issue #354).
 */
public class PSManagedLinkServiceJsonTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Mockery context;
  private IPSManagedLinkDao dao;
  private IPSContentWs contentWs;
  private IPSGuidManager guidMgr;
  private IPSNotificationService notificationService;
  private PSManagedLinkService service;

  @Before
  public void setUp() {
    context = new JUnit4Mockery();
    dao = context.mock(IPSManagedLinkDao.class);
    contentWs = context.mock(IPSContentWs.class);
    guidMgr = context.mock(IPSGuidManager.class);
    notificationService = context.mock(IPSNotificationService.class);

    context.checking(
        new Expectations() {
          {
            allowing(notificationService)
                .addListener(with(any(EventType.class)), with(any(IPSNotificationListener.class)));
          }
        });

    service =
        new PSManagedLinkService(
            dao, null, contentWs, null, notificationService, guidMgr, null, null, null, null);
  }

  @Test
  public void shouldSkipJsonNullPageLinkIdAndUpdateValidImageLinkId() throws Exception {
    final PSManagedLink imageLink = createManagedLink(131L, 10561);
    final IPSGuid imageGuid = context.mock(IPSGuid.class, "imageGuid");

    context.checking(
        new Expectations() {
          {
            oneOf(dao).findLinkByLinkId(131L);
            will(returnValue(imageLink));
            oneOf(guidMgr).makeGuid(with(any(PSLocator.class)));
            will(returnValue(imageGuid));
            oneOf(contentWs).findItemPaths(imageGuid);
            will(returnValue(new String[] {"//Assets/qa-assets/qa-image-18-updated.png"}));
          }
        });

    String payload =
        "{\"sliderName\":\"hero\",\"percJSONConfig\":[{"
            + "\"percImagePath\":\"/Assets/qa-assets/qa-image-18.png\","
            + "\"linkSetting\":\"none\","
            + "\"percImageContentId\":\"10561\","
            + "\"percPageContentId\":\"\","
            + "\"percImagePathLinkId\":\"131\","
            + "\"percPagePath\":\"\","
            + "\"percPagePathLinkId\":null"
            + "}]}";

    String result = service.renderLinksInJSON(null, payload, Boolean.FALSE);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    ArrayNode entries = (ArrayNode) root.get(IPSManagedLinkService.PERC_CONFIG);
    JsonNode entry = entries.get(0);

    assertEquals(
        "/Assets/qa-assets/qa-image-18-updated.png",
        entry.get(IPSManagedLinkService.PERC_IMAGEPATH).asText());
    assertEquals("", entry.get(IPSManagedLinkService.PERC_PAGEPATH).asText());
    context.assertIsSatisfied();
  }

  @Test
  public void shouldSkipJsonNullAndMissingAndBlankLinkIdsForImageFileAndPage() throws Exception {
    context.checking(
        new Expectations() {
          {
            never(dao).findLinkByLinkId(with(any(Long.class)));
          }
        });

    String payload =
        "{\"percJSONConfig\":["
            + "{\"percImagePath\":\"/Assets/img.png\",\"percImagePathLinkId\":null,"
            + "\"percFilePath\":\"/Assets/doc.pdf\",\"percFilePathLinkId\":null,"
            + "\"percPagePath\":\"/Sites/site/page.html\",\"percPagePathLinkId\":null},"
            + "{\"percImagePath\":\"/Assets/img2.png\","
            + "\"percFilePath\":\"/Assets/doc2.pdf\",\"percFilePathLinkId\":\"   \","
            + "\"percPagePath\":\"/Sites/site/page2.html\",\"percPagePathLinkId\":\"\"},"
            + "{\"percImagePath\":\"/Assets/img3.png\",\"percImagePathLinkId\":\"null\","
            + "\"percFilePath\":\"/Assets/doc3.pdf\",\"percFilePathLinkId\":\"not-a-number\","
            + "\"percPagePath\":\"/Sites/site/page3.html\",\"percPagePathLinkId\":\"null\"}"
            + "]}";

    String result = service.renderLinksInJSON(null, payload, Boolean.FALSE);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    ArrayNode entries = (ArrayNode) root.get(IPSManagedLinkService.PERC_CONFIG);

    assertEquals(
        "/Assets/img.png", entries.get(0).get(IPSManagedLinkService.PERC_IMAGEPATH).asText());
    assertEquals(
        "/Assets/doc.pdf", entries.get(0).get(IPSManagedLinkService.PERC_FILEPATH).asText());
    assertEquals(
        "/Sites/site/page.html", entries.get(0).get(IPSManagedLinkService.PERC_PAGEPATH).asText());
    assertEquals(
        "/Assets/img3.png", entries.get(2).get(IPSManagedLinkService.PERC_IMAGEPATH).asText());
    context.assertIsSatisfied();
  }

  @Test
  public void shouldUpdateImageFileAndPageLinksForStringAndNumericLinkIds() throws Exception {
    final PSManagedLink imageLink = createManagedLink(131L, 1001);
    final PSManagedLink fileLink = createManagedLink(132L, 1002);
    final PSManagedLink pageLink = createManagedLink(133L, 1003);
    final IPSGuid imageGuid = context.mock(IPSGuid.class, "imageGuid");
    final IPSGuid fileGuid = context.mock(IPSGuid.class, "fileGuid");
    final IPSGuid pageGuid = context.mock(IPSGuid.class, "pageGuid");

    context.checking(
        new Expectations() {
          {
            oneOf(dao).findLinkByLinkId(131L);
            will(returnValue(imageLink));
            oneOf(dao).findLinkByLinkId(132L);
            will(returnValue(fileLink));
            oneOf(dao).findLinkByLinkId(133L);
            will(returnValue(pageLink));
            exactly(3).of(guidMgr).makeGuid(with(any(PSLocator.class)));
            will(
                onConsecutiveCalls(
                    returnValue(imageGuid), returnValue(fileGuid), returnValue(pageGuid)));
            oneOf(contentWs).findItemPaths(imageGuid);
            will(returnValue(new String[] {"//Assets/new/img.png"}));
            oneOf(contentWs).findItemPaths(fileGuid);
            will(returnValue(new String[] {"//Assets/new/doc.pdf"}));
            oneOf(contentWs).findItemPaths(pageGuid);
            will(returnValue(new String[] {"//Sites/test/new-page.html"}));
          }
        });

    String payload =
        "{\"percJSONConfig\":[{"
            + "\"percImagePath\":\"/Assets/old/img.png\",\"percImagePathLinkId\":\"131\","
            + "\"percFilePath\":\"/Assets/old/doc.pdf\",\"percFilePathLinkId\":132,"
            + "\"percPagePath\":\"/Sites/test/old-page.html\",\"percPagePathLinkId\":\"133\""
            + "}]}";

    String result = service.renderLinksInJSON(null, payload, Boolean.FALSE);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    JsonNode entry = root.get(IPSManagedLinkService.PERC_CONFIG).get(0);

    assertEquals("/Assets/new/img.png", entry.get(IPSManagedLinkService.PERC_IMAGEPATH).asText());
    assertEquals("/Assets/new/doc.pdf", entry.get(IPSManagedLinkService.PERC_FILEPATH).asText());
    assertEquals(
        "/Sites/test/new-page.html", entry.get(IPSManagedLinkService.PERC_PAGEPATH).asText());
    context.assertIsSatisfied();
  }

  @Test
  public void shouldWriteEmptyStringWhenRenderedHrefIsNull() throws Exception {
    context.checking(
        new Expectations() {
          {
            oneOf(dao).findLinkByLinkId(999L);
            will(returnValue(null));
          }
        });

    String payload =
        "{\"percJSONConfig\":[{\"percImagePath\":\"/Assets/deleted.png\","
            + "\"percImagePathLinkId\":\"999\"}]}";

    String result = service.renderLinksInJSON(null, payload, Boolean.FALSE);
    ObjectNode root = (ObjectNode) MAPPER.readTree(result);
    JsonNode imagePathNode =
        root.get(IPSManagedLinkService.PERC_CONFIG)
            .get(0)
            .get(IPSManagedLinkService.PERC_IMAGEPATH);

    assertFalse(imagePathNode.isNull());
    assertEquals("", imagePathNode.asText());
    context.assertIsSatisfied();
  }

  @Test
  public void shouldReturnNullWhenPercConfigMissingAndOriginalOnMalformedJson() {
    assertNull(service.renderLinksInJSON(null, "{\"sliderName\":\"hero\"}", Boolean.FALSE));

    String malformed = "{not-valid-json";
    assertEquals(malformed, service.renderLinksInJSON(null, malformed, Boolean.FALSE));
  }

  private static PSManagedLink createManagedLink(long linkId, int childId) {
    PSManagedLink link = new PSManagedLink();
    link.setLinkId(linkId);
    link.setChildId(childId);
    return link;
  }
}
