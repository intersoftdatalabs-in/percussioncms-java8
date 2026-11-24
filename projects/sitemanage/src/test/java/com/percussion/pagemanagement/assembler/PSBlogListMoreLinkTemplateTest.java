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

import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression tests for issue #411: verifies {@code percBlogIndexPage.xml}, {@code
 * percPageAutoList.xml}, {@code sys_assembly.vm}, and TinyMCE's {@code percmorelink/plugin.js}
 * handle {@code perc-blog-more-link} markers that contain whitespace, non-breaking spaces,
 * single-quoted class attributes, or self-closing syntax.
 */
public class PSBlogListMoreLinkTemplateTest {

  private static final String BLOG_INDEX_WIDGET_PATH =
      "system/Packages/perc.widget.blogIndexPage/sys__UserDependency--rxconfig/Widgets/percBlogIndexPage.xml";
  private static final String PAGE_AUTO_LIST_WIDGET_PATH =
      "system/Packages/perc.PageAutoListWidget/sys__UserDependency--rxconfig/Widgets/percPageAutoList.xml";
  private static final String SYS_ASSEMBLY_VM_PATH =
      "system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm";
  private static final String PERCMORELINK_PLUGIN_PATH =
      "modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percmorelink/plugin.js";
  private static final String VELOCITY_PROPS_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Code>", Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  private static final String STUB_MACROS =
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end\n"
          + "#macro(cleanClass $str)$!{str.trim()}#end\n";

  private static final String[] MORE_LINK_MARKER_VARIANTS = {
    "<span class=\"perc-blog-more-link\"></span>",
    "<span class=\"perc-blog-more-link\"> </span>",
    "<span class=\"perc-blog-more-link\">  \t\n </span>",
    "<span class=\"perc-blog-more-link\">\u00A0</span>",
    "<span class=\"perc-blog-more-link\">&nbsp;</span>",
    "<span class=\"perc-blog-more-link\">&#160;</span>",
    "<span class=\"perc-blog-more-link\">&#xA0;</span>",
    "<span class=\"perc-blog-more-link\">&#x00a0;</span>",
    "<span class=\"perc-blog-more-link\"/>",
    "<span class=\"perc-blog-more-link\" />",
    "<span class='perc-blog-more-link'> </span>",
    "<SPAN CLASS=\"perc-blog-more-link\">&NBSP;</SPAN>"
  };

  @Test
  public void blogListShouldReplaceMoreLinkMarkerVariantsWithConfiguredText() throws Exception {
    for (String marker : MORE_LINK_MARKER_VARIANTS) {
      String summary = "Own summary of the Bravo post 1541357 " + marker;
      String html = renderBlogIndexWidget(summary, "Bravo Post", "Read on");
      String expectedLink =
          "<a href='/Blog/Bravo.html' aria-label='Read more about Bravo Post' title='Bravo Post'>Read on</a>";
      assertTrue(
          "Expected read-more link for marker [" + marker + "] in HTML: " + html,
          html.contains("Own summary of the Bravo post 1541357 " + expectedLink));
      assertFalse(
          "Did not expect raw marker [" + marker + "] to remain in HTML: " + html,
          html.toLowerCase().contains("perc-blog-more-link"));
    }
  }

  @Test
  public void blogListShouldSafelyHandleDollarAndBackslashInTitleAndReadMoreText()
      throws Exception {
    String summary = "Summary with space <span class=\"perc-blog-more-link\"> </span>";
    String html = renderBlogIndexWidget(summary, "Grant $50,000 \\ Award", "Read $1 more \\ now");
    String expectedLink =
        "<a href='/Blog/Bravo.html' aria-label='Read more about Grant $50,000 \\ Award'"
            + " title='Grant $50,000 \\ Award'>Read $1 more \\ now</a>";
    assertTrue("Expected literal $ and \\ in link, got HTML: " + html, html.contains(expectedLink));
  }

  @Test
  public void blogListShouldPreserveNonWhitespaceSpanTextAndUnrelatedSpans() throws Exception {
    String summary =
        "Intro <span class=\"perc-blog-more-link\">Keep this text</span>"
            + " <span class=\"other-span\"> </span>";
    String html = renderBlogIndexWidget(summary, "Bravo Post", "Read on");
    assertTrue(
        "Expected non-whitespace span text to be preserved in HTML: " + html,
        html.contains("<span class=\"perc-blog-more-link\">Keep this text</span>"));
    assertTrue(
        "Expected unrelated span to be preserved in HTML: " + html,
        html.contains("<span class=\"other-span\"> </span>"));
    assertFalse(
        "Did not expect read-more link to be injected in HTML: " + html,
        html.contains(">Read on</a>"));
  }

  @Test
  public void pageAutoListShouldReplaceMoreLinkMarkerVariants() throws Exception {
    for (String marker : MORE_LINK_MARKER_VARIANTS) {
      String summary = "Auto list summary " + marker;
      String html = renderPageAutoListWidget(summary, "Grant $50,000 Post");
      String expectedLink =
          "<a href='/Blog/Bravo.html' aria-label='Read more about  Grant $50,000 Post'"
              + " title='Grant $50,000 Post'>...more</a>";
      assertTrue(
          "Expected auto-list read-more link for marker [" + marker + "] in HTML: " + html,
          html.contains("Auto list summary " + expectedLink));
      assertFalse(
          "Did not expect raw marker [" + marker + "] to remain in HTML: " + html,
          html.toLowerCase().contains("perc-blog-more-link"));
    }
  }

  @Test
  public void sysAssemblyVmShouldConvertMoreLinkMarkerVariantsIntoDctermsAbstractAnchor()
      throws Exception {
    String abstractBlock = extractSysAssemblyAbstractBlock();
    for (String marker : MORE_LINK_MARKER_VARIANTS) {
      String summary = "<p>Own summary of the Bravo post 1541357 " + marker + "</p>";
      String html = renderSysAssemblyAbstract(abstractBlock, summary);
      assertTrue(
          "Expected <a class=\"perc-more-link\"></a> in dcterms:abstract for marker ["
              + marker
              + "], got: "
              + html,
          html.contains(
              "<div property='dcterms:abstract' style='display:none' datatype='rdf:XMLLiteral'>"
                  + "<p>Own summary of the Bravo post 1541357 <a class=\"perc-more-link\"></a></p></div>"));
      assertFalse(
          "Did not expect raw marker [" + marker + "] to remain in dcterms:abstract: " + html,
          html.toLowerCase().contains("perc-blog-more-link"));
    }

    String nonEmptySummary = "<p><span class=\"perc-blog-more-link\">Keep me</span></p>";
    String nonEmptyHtml = renderSysAssemblyAbstract(abstractBlock, nonEmptySummary);
    assertTrue(
        "Expected non-whitespace span text to remain intact: " + nonEmptyHtml,
        nonEmptyHtml.contains("<span class=\"perc-blog-more-link\">Keep me</span>"));
    assertFalse(
        "Did not expect perc-more-link anchor for non-whitespace span: " + nonEmptyHtml,
        nonEmptyHtml.contains("perc-more-link"));
  }

  @Test
  public void tinyMcePercMoreLinkPluginShouldEmitValidHtmlSpanAndRestoreAllMarkerVariants()
      throws Exception {
    File pluginFile = resolveRepoFile(PERCMORELINK_PLUGIN_PATH);
    String pluginSource =
        new String(Files.readAllBytes(pluginFile.toPath()), StandardCharsets.UTF_8);

    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    String harness =
        "var handlers = {};\n"
            + "var tinymce = { PluginManager: { add: function(name, fn) {\n"
            + "  var editor = {\n"
            + "    addCommand: function() {},\n"
            + "    ui: { registry: { addMenuItem: function() {}, addButton: function() {} } },\n"
            + "    on: function(ev, cb) { handlers[ev] = cb; },\n"
            + "    dom: { hasClass: function() { return false; }, loadCSS: function() {} },\n"
            + "    settings: { content_css: false },\n"
            + "    theme: {}\n"
            + "  };\n"
            + "  fn(editor, '/sys_resources/tinymce/plugins/percmorelink');\n"
            + "} } };\n";
    engine.eval(harness);
    engine.eval(pluginSource);

    // PostProcess should convert placeholder <img> to valid HTML5 <span></span> (not <span/>)
    String postProcessed =
        (String)
            engine.eval(
                "(function() {"
                    + " var e = { get: true, content: '<p>Summary <img src=\"/sys_resources/tinymce/plugins/percmorelink/img/ReadMoreInsert-1.png\" class=\"perc-blog-more-link mceItemNoResize\" /></p>' };"
                    + " handlers['PostProcess'](e);"
                    + " return e.content;"
                    + "})()");
    assertEquals("<p>Summary <span class=\"perc-blog-more-link\"></span></p>", postProcessed);

    // BeforeSetContent should restore all stored marker variants into the placeholder <img>
    for (String marker : MORE_LINK_MARKER_VARIANTS) {
      engine.put("testMarker", marker);
      String restored =
          (String)
              engine.eval(
                  "(function() {"
                      + " var e = { content: '<p>Summary ' + testMarker + '</p>' };"
                      + " handlers['BeforeSetContent'](e);"
                      + " return e.content;"
                      + "})()");
      assertTrue(
          "Expected placeholder <img> for stored marker [" + marker + "], got: " + restored,
          restored.contains("ReadMoreInsert-1.png")
              && restored.contains("class=\"perc-blog-more-link mceItemNoResize\""));
      assertFalse(
          "Did not expect raw <span> marker [" + marker + "] after BeforeSetContent: " + restored,
          restored.toLowerCase().contains("<span"));
    }
  }

  private static String renderBlogIndexWidget(
      String pageSummary, String linkTitle, String readMoreText) throws Exception {
    File widgetFile = resolveRepoFile(BLOG_INDEX_WIDGET_PATH);
    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);

    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(xml);
    assertTrue("JEXL <Code> section not found in " + widgetFile, jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, velocityMatcher.find());
    String velocityTemplate = STUB_MACROS + velocityMatcher.group(1);

    Map<String, Object> props = new HashMap<>();
    props.put("titleFormat", "h2");
    props.put("blogPostDateTimeFormat", "yyyy-MM-dd");
    props.put("enableCalenderIcon", Boolean.FALSE);
    props.put("blogPostReadMoreText", readMoreText);
    props.put("perc_hidefield_title", Boolean.FALSE);
    props.put("perc_hidefield_summary", Boolean.FALSE);
    props.put("perc_hidefield_blog_date", Boolean.FALSE);
    props.put("perc_hidefield_byline", Boolean.TRUE);
    props.put("perc_hidefield_comments", Boolean.TRUE);
    props.put("perc_hidefield_tags", Boolean.TRUE);
    props.put("perc_hidefield_categories", Boolean.TRUE);
    props.put("resultEntriesTextPlural", "entries");
    props.put("resultEntriesTextSingular", "entry");
    props.put("autoListDivId", "blog_list_test");
    props.put("blogPostCommentsText", "Comments");
    props.put("totalMaxResults", Integer.valueOf(500));
    props.put("blogPostMaxResults", Integer.valueOf(10));
    props.put("pagingPagesText", "pages");
    props.put("listAriaLabel", "Blog list");
    props.put("listAriaRole", "region");
    props.put("perc_hide_when_empty", Boolean.FALSE);
    props.put("pagingOfText", "of");
    props.put("blogPostNoContentText", "No posts");
    props.put("rssLinkText", "RSS Link");
    props.put("perc_tag_label", "Tags:");
    props.put("perc_category_label", "Categories:");
    props.put("blogPostBylineText", "by");
    props.put("blogListHeaderText", "Results for");
    props.put("sortby", "rx:sys_contentpostdate");
    props.put("sortDirection", "desc");
    props.put("enable_rss_icon", Boolean.FALSE);
    props.put("rssIconPosition", "perc-list-top");
    props.put("locale", "en-US");

    Map<String, Object> cssProps = new HashMap<>();
    cssProps.put("rootclass", "");

    ResultItemStub postItem = new ResultItemStub(pageSummary, createTestCalendar());

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub(postItem, linkTitle));
    rx.put("string", new StringToolStub());

    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new EscapeToolStub());
    tools.put("date", new DateToolStub(createTestCalendar().getTime()));
    tools.put("alternator", new AlternatorStub());

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$perc", new PercHelper(props, cssProps, Collections.singletonList(postItem)));
    bindings.put("$sys", new SysHelper(pageSummary));
    bindings.put("$rx", rx);
    bindings.put("$tools", tools);

    IPSScript script = PSJexlEvaluator.createScript("if ( 1 == 1 ) {" + jexlCode + "} else { ; }");
    PSJexlEvaluator evaluator = new PSJexlEvaluator(bindings);
    evaluator.evaluate(script);

    RuntimeInstance runtime = createVelocityRuntime();
    VelocityContext ctx = new VelocityContext();
    for (Map.Entry<String, Object> entry : evaluator.getVars().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("$")) {
        key = key.substring(1);
      }
      ctx.put(key, entry.getValue());
    }

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percBlogIndexPage", velocityTemplate);
    return writer.toString();
  }

  private static String renderPageAutoListWidget(String pageSummary, String linkTitle)
      throws Exception {
    File widgetFile = resolveRepoFile(PAGE_AUTO_LIST_WIDGET_PATH);
    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, velocityMatcher.find());
    String velocityTemplate = STUB_MACROS + velocityMatcher.group(1);

    ResultItemStub postItem = new ResultItemStub(pageSummary, createTestCalendar());
    List<Object> results = Collections.<Object>singletonList(postItem);

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub(postItem, linkTitle));
    rx.put("string", new StringToolStub());

    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new EscapeToolStub());
    tools.put("date", new DateToolStub(createTestCalendar().getTime()));

    VelocityContext ctx = new VelocityContext();
    ctx.put(
        "perc",
        new PercHelper(
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            results));
    ctx.put("rx", rx);
    ctx.put("tools", tools);
    ctx.put("relresults", results);
    ctx.put("velocityCount", Integer.valueOf(1));
    ctx.put("maxlength", Integer.valueOf(10));
    ctx.put("summary", Boolean.TRUE);
    ctx.put("rendering", "unordered");
    ctx.put("categoryList", "");
    ctx.put("target", "");
    ctx.put("rowtag", "perc-list-odd");
    ctx.put("locale", "en-US");
    ctx.put("postDateTimeFormat", "yyyy-MM-dd");
    ctx.put("showPostDate", Boolean.FALSE);
    ctx.put("enableCalenderIcon", Boolean.FALSE);
    ctx.put("enableRssIcon", Boolean.FALSE);
    ctx.put("listItemFormatStartTag", "");
    ctx.put("listItemFormatEndTag", "");
    ctx.put("summaryclass", "");

    RuntimeInstance runtime = createVelocityRuntime();
    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percPageAutoList", velocityTemplate);
    return writer.toString();
  }

  private static String extractSysAssemblyAbstractBlock() throws IOException {
    File vmFile = resolveRepoFile(SYS_ASSEMBLY_VM_PATH);
    String vm = new String(Files.readAllBytes(vmFile.toPath()), StandardCharsets.UTF_8);
    Pattern blockPattern =
        Pattern.compile(
            "(#if\\(\\$perc\\.page\\.name && \\$sys\\.item\\.getProperty\\(\"rx:page_summary\"\\)\\.String != \"\"\\)##.*?#end##)",
            Pattern.DOTALL);
    Matcher matcher = blockPattern.matcher(vm);
    assertTrue("Expected dcterms:abstract block in " + vmFile, matcher.find());
    return matcher.group(1);
  }

  private static String renderSysAssemblyAbstract(String abstractBlock, String pageSummary)
      throws Exception {
    RuntimeInstance runtime = createVelocityRuntime();
    VelocityContext ctx = new VelocityContext();
    ctx.put(
        "perc",
        new PercHelper(
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            Collections.emptyList()));
    ctx.put("sys", new SysHelper(pageSummary));

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "sys_assembly", abstractBlock);
    return writer.toString();
  }

  private static RuntimeInstance createVelocityRuntime() throws Exception {
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_PATH);
    RuntimeInstance runtime = new RuntimeInstance();
    Properties velocityProps = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      velocityProps.load(in);
    }
    runtime.init(velocityProps);
    return runtime;
  }

  private static Calendar createTestCalendar() {
    Calendar cal = Calendar.getInstance();
    cal.set(2026, Calendar.NOVEMBER, 24, 14, 30, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal;
  }

  private static File resolveRepoFile(String relativePath) throws IOException {
    File dir = new File(System.getProperty("user.dir")).getCanonicalFile();
    while (dir != null) {
      File candidate = new File(dir, relativePath);
      if (candidate.isFile()) {
        return candidate;
      }
      dir = dir.getParentFile();
    }
    throw new IOException(
        "Could not locate " + relativePath + " from user.dir=" + System.getProperty("user.dir"));
  }

  public static class PercHelper {
    private final WidgetHelper widget;
    private final PageHelper page = new PageHelper();
    private List<Object> widgetContents;

    public PercHelper(
        Map<String, Object> props, Map<String, Object> cssProps, List<Object> widgetContents) {
      this.widget = new WidgetHelper(props, cssProps);
      this.widgetContents = widgetContents;
    }

    public WidgetHelper getWidget() {
      return widget;
    }

    public PageHelper getPage() {
      return page;
    }

    public List<Object> getWidgetContents() {
      return widgetContents;
    }

    public void setWidgetContents(List<Object> contents) {
      this.widgetContents = contents;
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return false;
    }

    public Object getLinkContext() {
      return new Object();
    }
  }

  public static class WidgetHelper {
    private final WidgetItemHelper item;

    public WidgetHelper(Map<String, Object> props, Map<String, Object> cssProps) {
      this.item = new WidgetItemHelper(props, cssProps);
    }

    public WidgetItemHelper getItem() {
      return item;
    }
  }

  public static class WidgetItemHelper {
    private final Map<String, Object> properties;
    private final Map<String, Object> cssProperties;

    public WidgetItemHelper(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.properties = properties;
      this.cssProperties = cssProperties;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public Map<String, Object> getCssProperties() {
      return cssProperties;
    }

    public String getId() {
      return "42";
    }
  }

  public static class PageHelper {
    public String getId() {
      return "100";
    }

    public String getName() {
      return "bravo.html";
    }

    public String getTitle() {
      return "QA Blog";
    }
  }

  public static class SysHelper {
    private final AssemblyItemStub assemblyItem = new AssemblyItemStub();
    private final PageNodeStub item;

    public SysHelper(String pageSummary) {
      this.item = new PageNodeStub(pageSummary, createTestCalendar());
    }

    public AssemblyItemStub getAssemblyItem() {
      return assemblyItem;
    }

    public PageNodeStub getItem() {
      return item;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return Long.valueOf(1L);
    }
  }

  public static class NavItemStub {
    public String getFolderPath() {
      return "//Sites/TestSite/Blog";
    }
  }

  public static class ResultItemStub {
    private final PageNodeStub node;

    public ResultItemStub(String pageSummary, Calendar postDate) {
      this.node = new PageNodeStub(pageSummary, postDate);
    }

    public PageNodeStub getNode() {
      return node;
    }
  }

  public static class PageNodeStub {
    private final String pageSummary;
    private final Calendar postDate;

    public PageNodeStub(String pageSummary, Calendar postDate) {
      this.pageSummary = pageSummary;
      this.postDate = postDate;
    }

    public boolean hasProperty(String name) {
      return "rx:page_summary".equals(name) || "rx:sys_contentpostdate".equals(name);
    }

    public PropertyStub getProperty(String name) {
      if ("rx:page_summary".equals(name)) {
        return new PropertyStub(pageSummary, null);
      }
      if ("rx:sys_contentpostdate".equals(name)) {
        return new PropertyStub("2026-11-24T14:30:00", postDate);
      }
      if ("rx:sys_contentpostdatetz".equals(name)) {
        return new PropertyStub("Default", null);
      }
      return new PropertyStub("", null);
    }
  }

  public static class PropertyStub {
    private final String stringValue;
    private final Calendar dateValue;

    public PropertyStub(String stringValue, Calendar dateValue) {
      this.stringValue = stringValue;
      this.dateValue = dateValue;
    }

    public String getString() {
      return stringValue;
    }

    public Calendar getDate() {
      return dateValue;
    }
  }

  public static class PageUtilsStub {
    private final PSPageUtils realPageUtils = new PSPageUtils();
    private final ResultItemStub postItem;
    private final String linkTitle;

    public PageUtilsStub(ResultItemStub postItem, String linkTitle) {
      this.postItem = postItem;
      this.linkTitle = linkTitle;
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      if (finderName == null) {
        return Collections.emptyList();
      }
      return Collections.<Object>singletonList(postItem);
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params, boolean includeSubItems) {
      return Collections.<Object>singletonList(new NavItemStub());
    }

    public String getBlogPostTemplateId(String navLoc) {
      return "1001";
    }

    public List<String> templateNames(String templateId) {
      return Collections.singletonList("Blog-Post");
    }

    public String getDeliveryServer(Object pubServerId) {
      return "http://localhost:9980";
    }

    public String itemLink(Object linkContext, Object page) {
      return "/Blog/Bravo.html";
    }

    public String html(Object node, String fields, String defaultVal) {
      return linkTitle;
    }

    public SimpleDateFormat getDateFormatFromLocale(String format, String locale) {
      return realPageUtils.getDateFormatFromLocale(format, locale);
    }
  }

  public static class StringToolStub {
    public Map<String, Object> stringToMap(String value) {
      return new HashMap<>();
    }

    public List<Object> getArrayList() {
      return new ArrayList<>();
    }
  }

  public static class AlternatorStub {
    public String auto(String first, String second) {
      return first;
    }
  }

  public static class DateToolStub {
    private final Date fallbackDate;

    public DateToolStub(Date fallbackDate) {
      this.fallbackDate = fallbackDate;
    }

    public Date getDate() {
      return fallbackDate;
    }
  }

  public static class EscapeToolStub {
    public String html(String value) {
      return value == null ? "" : StringEscapeUtils.escapeHtml4(value);
    }
  }
}
