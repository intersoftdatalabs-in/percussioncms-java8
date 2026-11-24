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
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression tests for issue #413: verifies {@code percBlogIndexPage.xml} honors {@code
 * perc_hide_when_empty} on empty blogs instead of falling back to the sample content placeholder,
 * keeps {@code .perc-blog-list-container} and {@code .perc-no-post} reachable when posts are empty,
 * and emits balanced {@code <div>} tags across all branches.
 */
public class PSBlogListHideWhenEmptyTemplateTest {

  private static final String BLOG_INDEX_WIDGET_PATH =
      "system/Packages/perc.widget.blogIndexPage/sys__UserDependency--rxconfig/Widgets/percBlogIndexPage.xml";
  private static final String VELOCITY_PROPS_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Code>", Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  private static final Pattern CONTAINER_OPEN_TAG_PATTERN =
      Pattern.compile("<div\\b[^>]*class=\"[^\"]*perc-blog-list-container[^\"]*\"[^>]*>");

  private static final Pattern DIV_OPEN_PATTERN = Pattern.compile("<div\\b");
  private static final Pattern DIV_CLOSE_PATTERN = Pattern.compile("</div>");

  private static final String STUB_MACROS =
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\" title=\"$!{text}\"></div>#end\n"
          + "#macro(cleanClass $str)$!{str.trim()}#end\n";

  @Test
  public void emptyBlogWithHideWhenEmptyEnabledShouldHideListContainer() throws Exception {
    String html = renderBlogIndexWidget(true, true, Collections.emptyList(), false);

    String containerTag = extractContainerOpenTag(html);
    assertTrue(
        "Expected hidden attribute on .perc-blog-list-container: " + containerTag,
        containerTag.contains(" hidden ") || containerTag.endsWith(" hidden>"));
    assertFalse(
        "Did not expect sample content placeholder for a real empty blog: " + html,
        html.contains("bloglist-sample-content"));
    assertBalancedDivs(html);
  }

  @Test
  public void emptyBlogWithHideWhenEmptyDisabledShouldShowListContainerAndNoPostMessage()
      throws Exception {
    String html = renderBlogIndexWidget(true, false, Collections.emptyList(), false);

    String containerTag = extractContainerOpenTag(html);
    assertFalse(
        "Did not expect hidden attribute on .perc-blog-list-container: " + containerTag,
        containerTag.contains(" hidden ") || containerTag.endsWith(" hidden>"));
    assertTrue(
        "Expected .perc-no-post message in HTML: " + html,
        html.contains("<div class = \"perc-no-post\">No posts</div>"));
    assertTrue(
        "Expected .perc-blog-list-structure in HTML: " + html,
        html.contains("class=\"perc-blog-list-structure\""));
    assertFalse(
        "Did not expect sample content placeholder for a real empty blog: " + html,
        html.contains("bloglist-sample-content"));
    assertBalancedDivs(html);
  }

  @Test
  public void emptyBlogWithNullRelresultsShouldHandleBothHideWhenEmptySettingsSafely()
      throws Exception {
    String hiddenHtml = renderBlogIndexWidget(true, true, null, false);
    String hiddenContainerTag = extractContainerOpenTag(hiddenHtml);
    assertTrue(
        "Expected hidden attribute when relresults is null and hide_when_empty is true: "
            + hiddenContainerTag,
        hiddenContainerTag.contains(" hidden ") || hiddenContainerTag.endsWith(" hidden>"));
    assertTrue(
        "Expected .perc-no-post message when relresults is null: " + hiddenHtml,
        hiddenHtml.contains("<div class = \"perc-no-post\">No posts</div>"));
    assertFalse(
        "Did not expect sample content placeholder when relresults is null: " + hiddenHtml,
        hiddenHtml.contains("bloglist-sample-content"));
    assertBalancedDivs(hiddenHtml);

    String visibleHtml = renderBlogIndexWidget(true, false, null, false);
    String visibleContainerTag = extractContainerOpenTag(visibleHtml);
    assertFalse(
        "Did not expect hidden attribute when relresults is null and hide_when_empty is false: "
            + visibleContainerTag,
        visibleContainerTag.contains(" hidden ") || visibleContainerTag.endsWith(" hidden>"));
    assertTrue(
        "Expected .perc-no-post message when relresults is null: " + visibleHtml,
        visibleHtml.contains("<div class = \"perc-no-post\">No posts</div>"));
    assertFalse(
        "Did not expect sample content placeholder when relresults is null: " + visibleHtml,
        visibleHtml.contains("bloglist-sample-content"));
    assertBalancedDivs(visibleHtml);
  }

  @Test
  public void nonEmptyBlogShouldNeverHideListContainerRegardlessOfHideWhenEmptyOption()
      throws Exception {
    List<Object> posts =
        Collections.<Object>singletonList(
            new ResultItemStub("<p>Post summary</p>", createTestCalendar()));

    for (boolean hideWhenEmpty : new boolean[] {true, false}) {
      String html = renderBlogIndexWidget(true, hideWhenEmpty, posts, false);
      String containerTag = extractContainerOpenTag(html);
      assertFalse(
          "Did not expect hidden attribute for non-empty blog (hideWhenEmpty="
              + hideWhenEmpty
              + "): "
              + containerTag,
          containerTag.contains(" hidden ") || containerTag.endsWith(" hidden>"));
      assertTrue(
          "Expected rendered post list <ol> in HTML: " + html,
          html.contains(
              "<ol style=\"visibility:hidden\" class=\"perc-blog-list perc-list-main\">"));
      assertFalse(
          "Did not expect .perc-no-post for non-empty blog: " + html,
          html.contains("perc-no-post"));
      assertFalse(
          "Did not expect sample content placeholder for non-empty blog: " + html,
          html.contains("bloglist-sample-content"));
      assertBalancedDivs(html);
    }
  }

  @Test
  public void templateInEditModeShouldRenderSampleContentWithBalancedDivs() throws Exception {
    String html = renderBlogIndexWidget(false, true, Collections.emptyList(), true);

    assertTrue(
        "Expected sample content placeholder in template edit mode: " + html,
        html.contains("bloglist-sample-content"));
    assertFalse(
        "Did not expect .perc-blog-list-container when navLoc is empty: " + html,
        html.contains("perc-blog-list-container"));
    assertBalancedDivs(html);
  }

  @Test
  public void templateInPublishModeShouldRenderNothingWithNoStrayDiv() throws Exception {
    String html = renderBlogIndexWidget(false, false, Collections.emptyList(), false);

    assertFalse(
        "Did not expect sample content placeholder outside edit mode: " + html,
        html.contains("bloglist-sample-content"));
    assertFalse(
        "Did not expect .perc-blog-list-container when navLoc is empty: " + html,
        html.contains("perc-blog-list-container"));
    assertFalse("Did not expect stray </div> in HTML: " + html, html.contains("</div>"));
    assertBalancedDivs(html);
  }

  private static String extractContainerOpenTag(String html) {
    Matcher matcher = CONTAINER_OPEN_TAG_PATTERN.matcher(html);
    assertTrue("Expected .perc-blog-list-container div in HTML: " + html, matcher.find());
    return matcher.group(0);
  }

  private static void assertBalancedDivs(String html) {
    int openCount = countMatches(DIV_OPEN_PATTERN, html);
    int closeCount = countMatches(DIV_CLOSE_PATTERN, html);
    assertEquals("Expected balanced <div> and </div> tags in HTML: " + html, openCount, closeCount);
  }

  private static int countMatches(Pattern pattern, String input) {
    Matcher matcher = pattern.matcher(input);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  private static String renderBlogIndexWidget(
      boolean hasNavLoc, boolean hideWhenEmpty, List<Object> posts, boolean editMode)
      throws Exception {
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
    props.put("blogPostReadMoreText", "more...");
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
    props.put("perc_hide_when_empty", Boolean.valueOf(hideWhenEmpty));
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

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub(hasNavLoc, posts, "Test Post"));
    rx.put("string", new StringToolStub());

    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new EscapeToolStub());
    tools.put("date", new DateToolStub(createTestCalendar().getTime()));
    tools.put("alternator", new AlternatorStub());

    Map<String, Object> bindings = new HashMap<>();
    bindings.put(
        "$perc", new PercHelper(props, cssProps, Collections.<Object>emptyList(), editMode));
    bindings.put("$sys", new SysHelper(""));
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
    private final boolean editMode;

    public PercHelper(
        Map<String, Object> props,
        Map<String, Object> cssProps,
        List<Object> widgetContents,
        boolean editMode) {
      this.widget = new WidgetHelper(props, cssProps);
      this.widgetContents = widgetContents;
      this.editMode = editMode;
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
      return editMode;
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
      return "index.html";
    }

    public String getTitle() {
      return "QA Empty Blog";
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
      return "//Sites/TestSite/EmptyBlog";
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
    private final boolean hasNavLoc;
    private final List<Object> posts;
    private final String linkTitle;

    public PageUtilsStub(boolean hasNavLoc, List<Object> posts, String linkTitle) {
      this.hasNavLoc = hasNavLoc;
      this.posts = posts;
      this.linkTitle = linkTitle;
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      if (finderName == null) {
        return Collections.emptyList();
      }
      return posts;
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params, boolean includeSubItems) {
      if (!hasNavLoc) {
        return Collections.emptyList();
      }
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
      return "/EmptyBlog/Post.html";
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
