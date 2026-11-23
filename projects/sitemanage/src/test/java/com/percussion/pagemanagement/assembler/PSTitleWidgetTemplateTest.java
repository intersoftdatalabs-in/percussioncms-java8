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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
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
 * Regression test for issue #387: verifies {@code percTitle.xml} renders {@code <p>} instead of
 * {@code <paragraph>} when {@code wrapper} is {@code "paragraph"}, while preserving {@code p},
 * empty/null fallback to {@code h1}, and heading/{@code div} formats.
 */
public class PSTitleWidgetTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.title/sys__UserDependency--rxconfig/Widgets/percTitle.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Code>", Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  private static final String STUB_MACROS =
      "#macro(loadRelatedWidgetContents)$perc.setWidgetContents($assets)#end"
          + "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end";

  @Test
  public void shouldRenderParagraphTagWhenWrapperIsParagraphUsingLinkTitle() throws Exception {
    String html = renderTemplate("paragraph", "", "Sample Page Title", null);

    assertTrue("Expected <p> element in HTML: " + html, html.contains("<p>Sample Page Title</p>"));
    assertFalse("Did not expect <paragraph in HTML: " + html, html.contains("<paragraph"));
    assertFalse("Did not expect </paragraph> in HTML: " + html, html.contains("</paragraph>"));
  }

  @Test
  public void shouldRenderParagraphTagWithRootClassAndTitleOverride() throws Exception {
    String html = renderTemplate("paragraph", "hero-title", "Sample Page Title", "Custom Override");

    assertTrue(
        "Expected <p class=\"hero-title\"> element in HTML: " + html,
        html.contains("<p class=\"hero-title\">Custom Override</p>"));
    assertFalse("Did not expect <paragraph in HTML: " + html, html.contains("<paragraph"));
  }

  @Test
  public void shouldRenderParagraphTagWhenWrapperIsP() throws Exception {
    String html = renderTemplate("p", "", "Sample Page Title", null);

    assertTrue("Expected <p> element in HTML: " + html, html.contains("<p>Sample Page Title</p>"));
  }

  @Test
  public void shouldDefaultToH1WhenWrapperIsEmptyOrNull() throws Exception {
    String emptyHtml = renderTemplate("", "", "Sample Page Title", null);
    assertTrue(
        "Expected <h1> fallback for empty wrapper: " + emptyHtml,
        emptyHtml.contains("<h1>Sample Page Title</h1>"));

    String nullHtml = renderTemplate(null, "", "Sample Page Title", null);
    assertTrue(
        "Expected <h1> fallback for null wrapper: " + nullHtml,
        nullHtml.contains("<h1>Sample Page Title</h1>"));
  }

  @Test
  public void shouldPreserveHeadingAndDivFormats() throws Exception {
    List<String> tags = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6", "div");
    for (String tag : tags) {
      String html = renderTemplate(tag, "", "Sample Page Title", null);
      assertTrue(
          "Expected <" + tag + "> element in HTML: " + html,
          html.contains("<" + tag + ">Sample Page Title</" + tag + ">"));
    }
  }

  private static String renderTemplate(
      String wrapper, String rootclass, String linkTitle, String overrideText) throws Exception {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);

    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(xml);
    assertTrue("JEXL <Code> section not found in " + widgetFile, jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, velocityMatcher.find());
    String velocityTemplate = STUB_MACROS + velocityMatcher.group(1);

    Map<String, Object> props = new HashMap<>();
    if (wrapper != null) {
      props.put("wrapper", wrapper);
    }

    Map<String, Object> cssProps = new HashMap<>();
    if (rootclass != null) {
      cssProps.put("rootclass", rootclass);
    }

    PercHelper percHelper = new PercHelper(props, cssProps, linkTitle);

    Map<String, Object> sys = new HashMap<>();
    sys.put("assemblyItem", new AssemblyItemStub());

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub(overrideText));

    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new EscapeToolStub());

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
    runtime.evaluate(ctx, writer, "percTitle", velocityTemplate);
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

  public static class PercHelper {
    private final WidgetStub widget;
    private final PageStub page;
    private List<Object> widgetContents = Collections.emptyList();

    public PercHelper(
        Map<String, Object> properties, Map<String, Object> cssProperties, String linkTitle) {
      this.widget = new WidgetStub(properties, cssProperties);
      this.page = new PageStub(linkTitle);
    }

    public WidgetStub getWidget() {
      return widget;
    }

    public PageStub getPage() {
      return page;
    }

    public List<Object> getWidgetContents() {
      return widgetContents;
    }

    public void setWidgetContents(List<Object> widgetContents) {
      this.widgetContents = widgetContents;
    }
  }

  public static class PageStub {
    private final String linkTitle;

    public PageStub(String linkTitle) {
      this.linkTitle = linkTitle;
    }

    public String getLinkTitle() {
      return linkTitle;
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
    private final NodeStub node = new NodeStub("");

    public NodeStub getNode() {
      return node;
    }
  }

  public static class PageUtilsStub {
    private final String overrideText;

    public PageUtilsStub(String overrideText) {
      this.overrideText = overrideText;
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      if (overrideText == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(new AssetItemStub(overrideText));
    }

    public String html(NodeStub node, String fieldName) {
      return node.getProperty(fieldName).getValue();
    }
  }

  public static class AssetItemStub {
    private final NodeStub node;

    public AssetItemStub(String overrideText) {
      this.node = new NodeStub(overrideText);
    }

    public NodeStub getNode() {
      return node;
    }
  }

  public static class NodeStub {
    private final String overrideText;

    public NodeStub(String overrideText) {
      this.overrideText = overrideText;
    }

    public boolean hasProperty(String name) {
      return "resource_link_title".equals(name);
    }

    public PropertyStub getProperty(String name) {
      if ("rx:text".equals(name)) {
        return new PropertyStub(overrideText);
      }
      return new PropertyStub("");
    }
  }

  public static class PropertyStub {
    private final String value;

    public PropertyStub(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static class EscapeToolStub {
    public String html(String input) {
      if (input == null) {
        return "";
      }
      return input
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;");
    }
  }
}
