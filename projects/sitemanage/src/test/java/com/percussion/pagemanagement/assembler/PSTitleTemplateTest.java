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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #387: verifies {@code percTitle.xml} renders a {@code <p>} element when
 * the {@code wrapper} property is {@code "paragraph"}.
 */
public class PSTitleTemplateTest {

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

  private static final String TEST_MACROS =
      "#macro(loadRelatedWidgetContents)$perc.setWidgetContents($assets)#end\n"
          + "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\" title=\"$!{text}\"></div>"
          + "#end\n";

  @Test
  public void shouldRenderParagraphElementWhenWrapperIsParagraph() throws Exception {
    String xml = readWidgetXml();
    assertTrue(
        "percTitle.xml should preserve <EnumValue value=\"paragraph\" display_value=\"Paragraph\" />",
        xml.contains("<EnumValue value=\"paragraph\" display_value=\"Paragraph\" />"));

    String html = renderTitle("paragraph", null, "Page Title", null);
    assertTrue("Expected <p>Page Title</p> in: " + html, html.contains("<p>Page Title</p>"));
    assertFalse("Did not expect <paragraph in: " + html, html.contains("<paragraph"));
    assertFalse("Did not expect </paragraph in: " + html, html.contains("</paragraph"));

    String htmlWithClass = renderTitle("paragraph", "custom-title", "Page Title", null);
    assertTrue(
        "Expected <p class=\"custom-title\">Page Title</p> in: " + htmlWithClass,
        htmlWithClass.contains("<p class=\"custom-title\">Page Title</p>"));
    assertFalse(htmlWithClass.contains("<paragraph"));
  }

  @Test
  public void shouldRenderParagraphElementForTitleOverrideAssetWhenWrapperIsParagraph()
      throws Exception {
    String htmlOverride = renderTitle("paragraph", null, "Page Title", "Custom Override");
    assertTrue(
        "Expected <p>Custom Override</p> in: " + htmlOverride,
        htmlOverride.contains("<p>Custom Override</p>"));
    assertFalse(htmlOverride.contains("<paragraph"));
    assertFalse(htmlOverride.contains("</paragraph"));

    String htmlEmptyOverride = renderTitle("paragraph", null, "Page Title", "");
    assertTrue(
        "Expected fallback <p>Page Title</p> in: " + htmlEmptyOverride,
        htmlEmptyOverride.contains("<p>Page Title</p>"));
    assertFalse(htmlEmptyOverride.contains("<paragraph"));
  }

  @Test
  public void shouldRenderStandardWrapperElementsUnchanged() throws Exception {
    String[] tags = {"h1", "h2", "h3", "h4", "h5", "h6", "div", "p"};
    for (String tag : tags) {
      String html = renderTitle(tag, null, "Page Title", null);
      assertTrue(
          "Expected <" + tag + ">Page Title</" + tag + "> in: " + html,
          html.contains("<" + tag + ">Page Title</" + tag + ">"));
      assertFalse(html.contains("<paragraph"));
    }
  }

  @Test
  public void shouldDefaultToH1WhenWrapperIsEmptyOrMissing() throws Exception {
    String htmlEmpty = renderTitle("", null, "Page Title", null);
    assertTrue(
        "Expected <h1>Page Title</h1> in: " + htmlEmpty, htmlEmpty.contains("<h1>Page Title</h1>"));

    String htmlMissing = renderTitle(null, null, "Page Title", null);
    assertTrue(
        "Expected <h1>Page Title</h1> in: " + htmlMissing,
        htmlMissing.contains("<h1>Page Title</h1>"));
  }

  @Test
  public void shouldEscapeHtmlInLinkTitleWhenRenderedInParagraph() throws Exception {
    String html = renderTitle("paragraph", null, "News & <Updates>", null);
    assertTrue(
        "Expected <p>News &amp; &lt;Updates&gt;</p> in: " + html,
        html.contains("<p>News &amp; &lt;Updates&gt;</p>"));
  }

  private static String readWidgetXml() throws IOException {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    return new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);
  }

  private static String renderTitle(
      String wrapper, String rootclass, String linkTitle, String overrideText) throws Exception {
    String xml = readWidgetXml();
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(xml);
    assertTrue("JEXL <Code> section not found in percTitle.xml", jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in percTitle.xml", velocityMatcher.find());
    String velocityTemplate = velocityMatcher.group(1);

    Map<String, String> widgetProps = new HashMap<>();
    if (wrapper != null) {
      widgetProps.put("wrapper", wrapper);
    }

    Map<String, String> cssProps = new HashMap<>();
    if (rootclass != null) {
      cssProps.put("rootclass", rootclass);
    }

    List<AssetItemStub> assets =
        overrideText != null
            ? Collections.singletonList(new AssetItemStub(overrideText))
            : Collections.emptyList();

    PercHelper percHelper = new PercHelper(widgetProps, cssProps, linkTitle);
    SysHelper sysHelper = new SysHelper();

    Map<String, Object> rxMap = new HashMap<>();
    rxMap.put("pageutils", new PageUtilsStub(assets));

    PSJexlEvaluator evaluator = new PSJexlEvaluator();
    evaluator.bind("$perc", percHelper);
    evaluator.bind("$sys", sysHelper);
    evaluator.bind("$rx", rxMap);

    String wrappedScript = "if ( 1 == 1 ) {" + jexlCode + "} else { ; }";
    IPSScript script = PSJexlEvaluator.createScript(wrappedScript);
    evaluator.evaluate(script);

    RuntimeInstance runtime = new RuntimeInstance();
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      props.load(in);
    }
    runtime.init(props);

    Map<String, Object> toolsMap = new HashMap<>();
    toolsMap.put("esc", new EscapeToolStub());

    VelocityContext ctx = new VelocityContext();
    ctx.put("perc", percHelper);
    ctx.put("sys", sysHelper);
    ctx.put("rx", rxMap);
    ctx.put("tools", toolsMap);
    ctx.put("assets", evaluator.getVars().get("$assets"));
    Object classAttribute = evaluator.getVars().get("$classAttribute");
    if (classAttribute != null) {
      ctx.put("classAttribute", classAttribute);
    }

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percTitle", TEST_MACROS + velocityTemplate);
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
    private List<AssetItemStub> widgetContents = Collections.emptyList();

    public PercHelper(
        Map<String, String> properties, Map<String, String> cssProperties, String linkTitle) {
      this.widget = new WidgetStub(properties, cssProperties);
      this.page = new PageStub(linkTitle);
    }

    public WidgetStub getWidget() {
      return widget;
    }

    public PageStub getPage() {
      return page;
    }

    public List<AssetItemStub> getWidgetContents() {
      return widgetContents;
    }

    public void setWidgetContents(List<AssetItemStub> widgetContents) {
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

    public WidgetStub(Map<String, String> properties, Map<String, String> cssProperties) {
      this.item = new WidgetItemStub(properties, cssProperties);
    }

    public WidgetItemStub getItem() {
      return item;
    }
  }

  public static class WidgetItemStub {
    private final Map<String, String> properties;
    private final Map<String, String> cssProperties;

    public WidgetItemStub(Map<String, String> properties, Map<String, String> cssProperties) {
      this.properties = properties;
      this.cssProperties = cssProperties;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public Map<String, String> getCssProperties() {
      return cssProperties;
    }
  }

  public static class SysHelper {
    private final AssemblyItemStub assemblyItem = new AssemblyItemStub();

    public AssemblyItemStub getAssemblyItem() {
      return assemblyItem;
    }
  }

  public static class AssemblyItemStub {
    private final PageNodeStub node = new PageNodeStub();

    public PageNodeStub getNode() {
      return node;
    }
  }

  public static class PageNodeStub {
    public boolean hasProperty(String name) {
      return "resource_link_title".equals(name);
    }
  }

  public static class PageUtilsStub {
    private final List<AssetItemStub> assets;

    public PageUtilsStub(List<AssetItemStub> assets) {
      this.assets = assets;
    }

    public List<AssetItemStub> widgetContents(
        Object assemblyItem, Object widget, Object finderName, Object params) {
      return assets;
    }

    public String html(AssetNodeStub node, String field) {
      return node.getProperty(field).getValue();
    }
  }

  public static class AssetItemStub {
    private final AssetNodeStub node;

    public AssetItemStub(String overrideText) {
      this.node = new AssetNodeStub(overrideText);
    }

    public AssetNodeStub getNode() {
      return node;
    }
  }

  public static class AssetNodeStub {
    private final String overrideText;

    public AssetNodeStub(String overrideText) {
      this.overrideText = overrideText;
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
    public String html(String value) {
      return value == null ? "" : StringEscapeUtils.escapeHtml4(value);
    }
  }
}
