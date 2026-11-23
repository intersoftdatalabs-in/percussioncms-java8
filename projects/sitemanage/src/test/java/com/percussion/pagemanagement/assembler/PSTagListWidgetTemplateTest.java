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
 * Regression test for issue #383: verifies {@code percTagList.xml} writes {@code orderBy} into the
 * rendered {@code data-query} JSON attribute when {@code tag_sort_option} is set (e.g., {@code
 * count} or {@code alpha}), and omits it when empty or null.
 */
public class PSTagListWidgetTemplateTest {

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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void shouldWriteCountOrderByIntoDataQueryWhenSortMethodIsCount() throws Exception {
    JsonNode json = renderAndParseDataQuery("count", "list");

    assertEquals("count", json.path("orderBy").asText());
    assertEquals("list", json.path("tag_render_option").asText());
    assertEquals("false", json.path("isEditMode").asText());
    assertEquals("http://localhost:9980", json.path("deliveryurl").asText());

    JsonNode criteria = json.path("criteria");
    assertTrue("Expected criteria array in data-query", criteria.isArray());
    assertEquals(2, criteria.size());
    assertEquals("type = 'page'", criteria.get(0).asText());
    assertEquals("site = 'TestSite'", criteria.get(1).asText());
  }

  @Test
  public void shouldWriteAlphaOrderByIntoDataQueryWhenSortMethodIsAlpha() throws Exception {
    JsonNode json = renderAndParseDataQuery("alpha", "commaSeparated");

    assertEquals("alpha", json.path("orderBy").asText());
    assertEquals("commaSeparated", json.path("tag_render_option").asText());
  }

  @Test
  public void shouldOmitOrderByFromDataQueryWhenSortMethodIsEmptyOrNull() throws Exception {
    JsonNode emptyJson = renderAndParseDataQuery("", "list");
    assertFalse(
        "Expected orderBy to be omitted when tag_sort_option is empty", emptyJson.has("orderBy"));

    JsonNode nullJson = renderAndParseDataQuery(null, "list");
    assertFalse(
        "Expected orderBy to be omitted when tag_sort_option is null", nullJson.has("orderBy"));
  }

  private static JsonNode renderAndParseDataQuery(String tagSortOption, String tagRenderOption)
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

    Map<String, Object> props = new HashMap<>();
    props.put("tag_render_option", tagRenderOption);
    if (tagSortOption != null) {
      props.put("tag_sort_option", tagSortOption);
    }

    Map<String, Object> cssProps = new HashMap<>();
    cssProps.put("rootclass", "");

    PercHelper percHelper = new PercHelper(props, cssProps);

    Map<String, Object> sys = new HashMap<>();
    sys.put("assemblyItem", new AssemblyItemStub());

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub());
    rx.put("string", new StringHelper());

    Map<String, Object> tools = new HashMap<>();
    tools.put("alternator", new AlternatorStub());

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$perc", percHelper);
    bindings.put("$sys", sys);
    bindings.put("$rx", rx);
    bindings.put("$tools", tools);

    IPSScript script = PSJexlEvaluator.createScript("if ( 1 == 1 ) {" + jexlCode + "} else { ; }");
    PSJexlEvaluator evaluator = new PSJexlEvaluator(bindings);
    evaluator.evaluate(script);

    RuntimeInstance runtime = new RuntimeInstance();
    Properties velocityProps = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      velocityProps.load(in);
    }
    runtime.init(velocityProps);

    VelocityContext ctx = new VelocityContext();
    for (Map.Entry<String, Object> entry : evaluator.getVars().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("$")) {
        key = key.substring(1);
      }
      ctx.put(key, entry.getValue());
    }

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percTagList", velocityTemplate);
    String html = writer.toString();

    Matcher dataQueryMatcher = DATA_QUERY_ATTR_PATTERN.matcher(html);
    assertTrue("data-query attribute not found in rendered HTML: " + html, dataQueryMatcher.find());
    String decodedJson = dataQueryMatcher.group(1).replace("&quot;", "\"");
    return OBJECT_MAPPER.readTree(decodedJson);
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
    private List<Object> widgetContents = Collections.emptyList();

    public PercHelper(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.widget = new WidgetStub(properties, cssProperties);
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

    public WidgetStub(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.item = new WidgetItemStub(properties, cssProperties);
    }

    public WidgetItemStub getItem() {
      return item;
    }
  }

  public static class WidgetItemStub {
    private final Map<String, Object> properties;
    private final Map<String, Object> cssProperties;

    public WidgetItemStub(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.properties = properties;
      this.cssProperties = cssProperties;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public Map<String, Object> getCssProperties() {
      return cssProperties;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return 1L;
    }
  }

  public static class PageUtilsStub {
    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      return Collections.singletonList(new AssetItemStub());
    }

    public List<Object> getProcessedTags(Object relresults, Object sortOption) {
      return Collections.emptyList();
    }

    public String html(Object assetItem, String fieldName) {
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
    private final Map<String, String> props = new HashMap<>();

    public NodeStub() {
      props.put("query", "select rx:sys_contentid from rx:percPage");
      props.put("title_contains", "");
      props.put("start_date", "");
      props.put("end_date", "");
      props.put("site_path", "//Sites/TestSite");
      props.put("page_templates_list", "");
      props.put("tag_page_result", "/tag-results");
    }

    public PropertyStub getProperty(String name) {
      return new PropertyStub(props.get(name));
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

  public static class StringHelper {
    public Map<String, Object> stringToMap(Object ignored) {
      return new HashMap<>();
    }
  }

  public static class AlternatorStub {
    public String auto(String first, String second) {
      return first;
    }
  }
}
