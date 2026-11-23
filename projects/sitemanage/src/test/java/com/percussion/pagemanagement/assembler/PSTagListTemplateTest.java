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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * Regression test for issue #383: verifies {@code percTagList.xml} writes {@code tag_sort_option}
 * into the rendered {@code data-query} {@code orderBy} field when configured.
 */
public class PSTagListTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.taglist/sys__UserDependency--rxconfig/Widgets/percTagList.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Code>", Pattern.DOTALL);
  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);
  private static final Pattern DATA_QUERY_ATTR_PATTERN = Pattern.compile("data-query=\"([^\"]*)\"");

  @Test
  public void shouldIncludeOrderByCountInDataQueryWhenTagSortOptionIsCount() throws Exception {
    JsonNode json = renderAndParseDataQuery("count", true);

    assertTrue("Expected orderBy in data-query", json.has("orderBy"));
    assertEquals("count", json.get("orderBy").asText());
    assertTrue(json.has("criteria"));
    assertEquals("list", json.get("tag_render_option").asText());
    assertEquals("/results", json.get("tag_page_result").asText());
    assertEquals("http://localhost:9980", json.get("deliveryurl").asText());
    assertEquals("false", json.get("isEditMode").asText());
  }

  @Test
  public void shouldIncludeOrderByAlphaInDataQueryWhenTagSortOptionIsAlpha() throws Exception {
    JsonNode json = renderAndParseDataQuery("alpha", true);

    assertTrue("Expected orderBy in data-query", json.has("orderBy"));
    assertEquals("alpha", json.get("orderBy").asText());
  }

  @Test
  public void shouldOmitOrderByFromDataQueryWhenTagSortOptionIsEmptyOrNull() throws Exception {
    JsonNode emptyOptionJson = renderAndParseDataQuery("", true);
    assertFalse("Expected orderBy to be omitted when empty", emptyOptionJson.has("orderBy"));

    JsonNode missingOptionJson = renderAndParseDataQuery(null, false);
    assertFalse(
        "Expected orderBy to be omitted when null/absent", missingOptionJson.has("orderBy"));
  }

  private static JsonNode renderAndParseDataQuery(String tagSortOption, boolean includeSortKey)
      throws Exception {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);
    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);

    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(xml);
    assertTrue("JEXL <Code> section not found in " + widgetFile, jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, velocityMatcher.find());
    String velocityTemplate = velocityMatcher.group(1);

    Map<String, String> widgetProps = new HashMap<>();
    widgetProps.put("tag_render_option", "list");
    if (includeSortKey) {
      widgetProps.put("tag_sort_option", tagSortOption);
    }

    PSJexlEvaluator evaluator = new PSJexlEvaluator();
    PercHelper percHelper = new PercHelper(widgetProps);
    evaluator.bind("$perc", percHelper);
    evaluator.bind("$sys", new SysHelper());

    Map<String, Object> rxMap = new HashMap<>();
    rxMap.put("pageutils", new PageUtilsStub());
    rxMap.put("string", new StringUtilsStub());
    evaluator.bind("$rx", rxMap);

    Map<String, Object> toolsMap = new HashMap<>();
    toolsMap.put("alternator", new AlternatorStub());
    evaluator.bind("$tools", toolsMap);

    String wrappedScript = "if ( 1 == 1 ) {" + jexlCode + "} else { ; }";
    IPSScript script = PSJexlEvaluator.createScript(wrappedScript);
    evaluator.evaluate(script);

    String dynamicListData = (String) evaluator.getVars().get("$dynamicListData");
    assertNotNull("Expected $dynamicListData to be populated by JEXL script", dynamicListData);

    RuntimeInstance runtime = new RuntimeInstance();
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      props.load(in);
    }
    runtime.init(props);

    VelocityContext ctx = new VelocityContext();
    ctx.put("perc", percHelper);
    ctx.put("rootclass", "");
    ctx.put("dynamicListData", dynamicListData);
    ctx.put("title", "Tags");
    ctx.put("processedTags", Collections.emptyList());
    ctx.put("tagRenderOption", "list");

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percTagList", velocityTemplate);
    String html = writer.toString();

    Matcher attrMatcher = DATA_QUERY_ATTR_PATTERN.matcher(html);
    assertTrue("Expected data-query attribute in rendered HTML: " + html, attrMatcher.find());
    String unescapedJson = attrMatcher.group(1).replace("&quot;", "\"");

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(unescapedJson);
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

  public static class PercHelper {
    private final WidgetStub widget;
    private List<Object> widgetContents = Collections.singletonList(new Object());

    public PercHelper(Map<String, String> properties) {
      this.widget = new WidgetStub(properties);
    }

    public String getLinkContext() {
      return "";
    }

    public WidgetStub getWidget() {
      return widget;
    }

    public List<Object> getWidgetContents() {
      return widgetContents;
    }

    public void setWidgetContents(List<Object> widgetContents) {
      this.widgetContents = widgetContents;
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return false;
    }
  }

  public static class WidgetStub {
    private final WidgetItemStub item;
    private final Map<String, String> cssProperties = new HashMap<>();

    public WidgetStub(Map<String, String> properties) {
      this.item = new WidgetItemStub(properties);
    }

    public WidgetItemStub getItem() {
      return item;
    }

    public Map<String, String> getCssProperties() {
      return cssProperties;
    }
  }

  public static class WidgetItemStub {
    private final Map<String, String> properties;

    public WidgetItemStub(Map<String, String> properties) {
      this.properties = properties;
    }

    public Map<String, String> getProperties() {
      return properties;
    }
  }

  public static class SysHelper {
    private final AssemblyItemStub assemblyItem = new AssemblyItemStub();

    public AssemblyItemStub getAssemblyItem() {
      return assemblyItem;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return 1L;
    }
  }

  public static class PageUtilsStub {
    public List<Object> widgetContents(
        Object assemblyItem, Object widget, Object finderName, Object params) {
      return Collections.singletonList(new AssetItemStub());
    }

    public List<Object> getProcessedTags(Object relresults, Object sortOption) {
      return Collections.emptyList();
    }

    public String html(Object assetItem, String field) {
      return "Tags";
    }

    public String getDeliveryServer(Object pubServerId) {
      return "http://localhost:9980";
    }
  }

  public static class AssetItemStub {
    private final NodeStub node = new NodeStub();

    public NodeStub getNode() {
      return node;
    }
  }

  public static class NodeStub {
    public PropertyStub getProperty(String name) {
      if ("site_path".equals(name)) {
        return new PropertyStub("//Sites/TestSite");
      }
      if ("tag_page_result".equals(name)) {
        return new PropertyStub("/results");
      }
      return new PropertyStub("");
    }
  }

  public static class PropertyStub {
    private final String value;

    public PropertyStub(String value) {
      this.value = value;
    }

    public String getString() {
      return value;
    }
  }

  public static class StringUtilsStub {
    public Map<String, Object> stringToMap(Object input) {
      return new HashMap<>();
    }
  }

  public static class AlternatorStub {
    public String auto(String odd, String even) {
      return odd;
    }
  }
}
