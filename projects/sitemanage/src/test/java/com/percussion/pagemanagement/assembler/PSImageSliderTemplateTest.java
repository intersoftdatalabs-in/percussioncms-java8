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
package com.percussion.pagemanagement.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.percussion.services.assembly.IPSAssemblyItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #340: verifies {@code percImageSlider.xml} Velocity template parses and
 * renders cleanly against Jackson {@code ArrayNode} / {@code ObjectNode} data from {@link
 * PSPageUtils#createJsonArray}.
 */
public class PSImageSliderTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.imageSlider/sys__UserDependency--rxconfig/Widgets/percImageSlider.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!--.*?-->\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  @Test
  public void shouldRenderPublishedSliderWithApprovedSlidesAndExternalAndInternalLinks()
      throws Exception {
    StubPageUtils pageUtils = new StubPageUtils();
    pageUtils.registerAsset("1001", "Alt One", "5");
    pageUtils.registerAsset("1002", null, "4");
    pageUtils.registerAsset("1003", "Draft Alt", "1");
    pageUtils.registerAsset("2001", null, "5");

    String jsonPayload =
        "{\"sliderName\":\"hero-slider\",\"percJSONConfig\":["
            + "{\"percImagePath\":\"/Assets/uploads/slide1.png\",\"percImageContentId\":\"1001\","
            + "\"imageCaption\":\"Caption One\",\"imageCaption2\":\"Sub One\","
            + "\"linkSetting\":\"external\",\"externalLink\":\"https://example.com\","
            + "\"percPagePath\":\"\",\"percPageContentId\":\"\"},"
            + "{\"percImagePath\":\"/Assets/uploads/slide2.png\",\"percImageContentId\":\"1002\","
            + "\"imageCaption\":\"Caption Two\",\"imageCaption2\":\"\","
            + "\"linkSetting\":\"internal\",\"externalLink\":\"\","
            + "\"percPagePath\":\"/Sites/test/page.html\",\"percPageContentId\":\"2001\"},"
            + "{\"percImagePath\":\"/Assets/uploads/draft.png\",\"percImageContentId\":\"1003\","
            + "\"imageCaption\":\"Draft\",\"imageCaption2\":\"\","
            + "\"linkSetting\":\"\",\"externalLink\":\"\","
            + "\"percPagePath\":\"\",\"percPageContentId\":\"\"},"
            + "{\"percImagePath\":\"\",\"percImageContentId\":\"1004\","
            + "\"imageCaption\":\"Broken\",\"imageCaption2\":\"\","
            + "\"linkSetting\":\"\",\"externalLink\":\"\","
            + "\"percPagePath\":\"\",\"percPageContentId\":\"\"}"
            + "]}";

    String html = renderTemplate(pageUtils, jsonPayload, "standard", "above", "true");

    assertEquals(4, pageUtils.recordedQueries.size());
    assertEquals(
        "select * from rx:percImageAsset where rx:sys_contentid = 1001",
        pageUtils.recordedQueries.get(0));
    assertEquals(
        "select * from rx:percImageAsset where rx:sys_contentid = 1002",
        pageUtils.recordedQueries.get(1));
    assertEquals(
        "select * from rx:percPage where rx:sys_contentid = 2001",
        pageUtils.recordedQueries.get(2));
    assertEquals(
        "select * from rx:percImageAsset where rx:sys_contentid = 1003",
        pageUtils.recordedQueries.get(3));

    assertTrue(html.contains(".slick({"));
    assertTrue(html.contains("background-image: url('/Assets/uploads/slide1.png')"));
    assertTrue(html.contains("background-image: url('/Assets/uploads/slide2.png')"));
    assertTrue(html.contains("aria-label=\"Alt One\""));
    assertTrue(html.contains("Caption One"));
    assertTrue(html.contains("Sub One"));
    assertTrue(html.contains("Caption Two"));
    assertTrue(html.contains("href=\"https://example.com\""));
    assertTrue(html.contains("href=\"/Sites/test/page.html\""));

    assertFalse(html.contains("draft.png"));
    assertFalse(html.contains("${"));
    assertFalse(html.contains("$image."));
  }

  @Test
  public void shouldRenderAllCaptionLocationsImageLinkModesAndNavigationLayout() throws Exception {
    String jsonPayload =
        "{\"sliderName\":\"nav-slider\",\"percJSONConfig\":["
            + "{\"percImagePath\":\"/Assets/uploads/slide1.png\",\"percImageContentId\":\"1001\","
            + "\"imageCaption\":\"Cap A\",\"imageCaption2\":\"Cap B\","
            + "\"linkSetting\":\"external\",\"externalLink\":\"https://example.com\","
            + "\"percPagePath\":\"\",\"percPageContentId\":\"\"}"
            + "]}";

    String[] locations = new String[] {"above", "over", "below"};
    String[] imageLinkModes = new String[] {"true", "false"};

    for (String location : locations) {
      for (String imageLink : imageLinkModes) {
        StubPageUtils pageUtils = new StubPageUtils();
        pageUtils.registerAsset("1001", "Alt Nav", "5");

        String html = renderTemplate(pageUtils, jsonPayload, "navigation", location, imageLink);

        assertTrue(
            "Expected slick init for location=" + location + " imageLink=" + imageLink,
            html.contains(".slick({"));
        assertTrue(
            "Expected navigation slider for location=" + location + " imageLink=" + imageLink,
            html.contains("nav-slider-42-navigation"));
        assertTrue(html.contains("background-image: url('/Assets/uploads/slide1.png')"));
        assertTrue(html.contains("Cap A"));
        assertTrue(html.contains("Cap B"));
        assertFalse(html.contains("${"));
        assertFalse(html.contains("$image."));
      }
    }
  }

  @Test
  public void shouldSkipInternalPageQueryWhenPercPagePathIsEmpty() throws Exception {
    StubPageUtils pageUtils = new StubPageUtils();
    pageUtils.registerAsset("1001", "Alt Only", "5");

    String jsonPayload =
        "{\"sliderName\":\"no-page-slider\",\"percJSONConfig\":["
            + "{\"percImagePath\":\"/Assets/uploads/slide1.png\",\"percImageContentId\":\"1001\","
            + "\"imageCaption\":\"Cap\",\"imageCaption2\":\"\","
            + "\"linkSetting\":\"internal\",\"externalLink\":\"\","
            + "\"percPagePath\":\"\",\"percPageContentId\":\"\"}"
            + "]}";

    String html = renderTemplate(pageUtils, jsonPayload, "standard", "below", "true");

    assertEquals(1, pageUtils.recordedQueries.size());
    assertEquals(
        "select * from rx:percImageAsset where rx:sys_contentid = 1001",
        pageUtils.recordedQueries.get(0));
    assertTrue(html.contains("background-image: url('/Assets/uploads/slide1.png')"));
    assertFalse(html.contains("<a href="));
    assertFalse(html.contains("${"));
    assertFalse(html.contains("$image."));
  }

  private static String renderTemplate(
      StubPageUtils pageUtils,
      String jsonPayload,
      String layout,
      String captionLocation,
      String imageLink)
      throws Exception {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);
    Matcher matcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, matcher.find());
    String velocityTemplate = matcher.group(1);

    RuntimeInstance runtime = new RuntimeInstance();
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      props.load(in);
    }
    runtime.init(props);

    ObjectNode json = pageUtils.createJsonObject(jsonPayload);
    ArrayNode imageSliderArray = pageUtils.createJsonArray(json, "percJSONConfig");

    VelocityContext ctx = new VelocityContext();
    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", pageUtils);
    rx.put("string", new StringHelper());
    ctx.put("rx", rx);

    Map<String, Object> sys = new HashMap<>();
    sys.put("assemblyItem", null);
    ctx.put("sys", sys);

    ctx.put("perc", new PercHelper());

    ctx.put("rootclass", "percImageSlider");
    ctx.put("captionclass", "");
    ctx.put("validNumbers", "true");
    ctx.put("sliderName", json.path("sliderName").asText());
    ctx.put("sliderId", "42");
    ctx.put("perc_slide_layout", layout);
    ctx.put("perc_slide_fade", "false");
    ctx.put("perc_show_arrows", "true");
    ctx.put("perc_show_dots", "true");
    ctx.put("perc_slide_show_number_desktop", 1);
    ctx.put("perc_slide_show_number_tablet", 1);
    ctx.put("perc_slide_show_number_mobile", 1);
    ctx.put("perc_slides_to_scroll", 1);
    ctx.put("perc_slide_autoplay", "true");
    ctx.put("perc_autoplay_speed", 2000);
    ctx.put("perc_scroll_speed", 500);
    ctx.put("perc_enable_custom_arrows", false);
    ctx.put("perc_prev_arrow_class", "");
    ctx.put("perc_next_arrow_class", "");
    ctx.put("perc_slider_tablet_breakpoint", 1024);
    ctx.put("perc_slider_mobile_breakpoint", 480);
    ctx.put("perc_slide_theme", "dark");
    ctx.put("perc_slide_image_height", "250px");
    ctx.put("perc_thumbnail_height", "8vw");
    ctx.put("perc_slide_caption_tag_type", "p");
    ctx.put("perc_slide_caption_2_tag_type", "p");
    ctx.put("perc_slide_caption_location", captionLocation);
    ctx.put("perc_slide_caption_alignment", "left");
    ctx.put("perc_slide_caption_vertical_alignment", "top");
    ctx.put("perc_slide_targetblank", "_blank");
    ctx.put("perc_slide_image_link", imageLink);
    ctx.put("imageSliderArray", imageSliderArray);

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percImageSlider", velocityTemplate);
    return writer.toString();
  }

  private static File resolveRepoFile(String relativePath) throws IOException {
    File fromModule = new File(new File("../.."), relativePath);
    if (fromModule.exists()) {
      return fromModule;
    }
    File fromRoot = new File(relativePath);
    if (fromRoot.exists()) {
      return fromRoot;
    }
    throw new IOException(
        "Could not locate " + relativePath + " from " + new File(".").getAbsolutePath());
  }

  public static class StringHelper {
    public Map<String, Object> stringToMap(Object ignored) {
      return new HashMap<>();
    }
  }

  public static class PercHelper {
    public Object getWidget() {
      return new Object();
    }

    public List<Object> getWidgetContents() {
      return Collections.singletonList(new Object());
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return false;
    }
  }

  public static class StubPageUtils extends PSPageUtils {
    final List<String> recordedQueries = new ArrayList<>();
    private final Map<String, PSRenderAsset> assetsById = new HashMap<>();

    void registerAsset(String contentId, String altText, String contentStateId) {
      PSRenderAsset asset = new PSRenderAsset();
      Map<String, Object> fields = new HashMap<>();
      fields.put("alttext", altText);
      fields.put("sys_contentstateid", contentStateId);
      asset.setFields(fields);
      assetsById.put(contentId, asset);
    }

    @Override
    public List<PSRenderAsset> widgetContents(
        IPSAssemblyItem item, Object widget, String finderName, Map<String, Object> params) {
      String query = String.valueOf(params.get("query"));
      recordedQueries.add(query);
      int eqIdx = query.lastIndexOf('=');
      String id = eqIdx >= 0 ? query.substring(eqIdx + 1).trim() : "";
      PSRenderAsset asset = assetsById.get(id);
      if (asset == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(asset);
    }
  }
}
