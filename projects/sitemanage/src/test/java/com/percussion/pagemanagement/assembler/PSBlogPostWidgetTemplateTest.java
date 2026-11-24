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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #397: verifies {@code percBlogPost.xml} formats the post date using the
 * configured {@code locale} UserPref via {@link PSPageUtils#getDateFormatFromLocale(String,
 * String)} rather than always formatting in the JVM default locale.
 */
public class PSBlogPostWidgetTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widgets.blog/sys__UserDependency--rxconfig/Widgets/percBlogPost.xml";
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
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end";

  private static Calendar createTestCalendar() {
    Calendar cal = Calendar.getInstance();
    cal.set(2026, Calendar.NOVEMBER, 24, 14, 30, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal;
  }

  @Test
  public void shouldFormatDateInFrenchWhenLocaleIsFrenchEvenWithDifferentJvmDefault()
      throws Exception {
    Locale previousDefault = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      String html =
          renderTemplate("fr-FR", "EEEE d MMMM yyyy", createTestCalendar(), "Default", false);
      assertTrue(
          "Expected French date 'mardi 24 novembre 2026' in HTML: " + html,
          html.contains("mardi 24 novembre 2026"));
      assertFalse(
          "Did not expect English day 'Tuesday' in HTML: " + html, html.contains("Tuesday"));
      assertFalse(
          "Did not expect English month 'November' in HTML: " + html, html.contains("November"));
    } finally {
      Locale.setDefault(previousDefault);
    }
  }

  @Test
  public void shouldFormatDateInEnglishForEnUsNullAndEmptyLocales() throws Exception {
    Locale previousDefault = Locale.getDefault();
    Locale.setDefault(Locale.FRANCE);
    try {
      String[] locales = {"en-US", null, ""};
      for (String locale : locales) {
        String html =
            renderTemplate(locale, "EEEE d MMMM yyyy", createTestCalendar(), "Default", false);
        assertTrue(
            "Expected English date 'Tuesday 24 November 2026' for locale="
                + locale
                + " in HTML: "
                + html,
            html.contains("Tuesday 24 November 2026"));
      }
    } finally {
      Locale.setDefault(previousDefault);
    }
  }

  @Test
  public void shouldFormatDateInOtherConfiguredLocales() throws Exception {
    String deHtml =
        renderTemplate("de-DE", "EEEE d MMMM yyyy", createTestCalendar(), "Default", false);
    assertTrue(
        "Expected German date 'Dienstag 24 November 2026' in HTML: " + deHtml,
        deHtml.contains("Dienstag 24 November 2026"));

    String esHtml =
        renderTemplate("es-ES", "EEEE d MMMM yyyy", createTestCalendar(), "Default", false);
    assertTrue(
        "Expected Spanish date 'martes 24 noviembre 2026' in HTML: " + esHtml,
        esHtml.contains("martes 24 noviembre 2026"));
  }

  @Test
  public void shouldFallbackToDefaultFormatInConfiguredLocaleWhenFormatIsInvalidOrBlank()
      throws Exception {
    Calendar cal = createTestCalendar();
    String expectedFrench =
        new SimpleDateFormat("EEE MMM d, yyyy 'at' hh:mm a", Locale.FRANCE).format(cal.getTime());

    String invalidHtml = renderTemplate("fr-FR", "invalid-pattern-QQQ", cal, "Default", false);
    assertTrue(
        "Expected fallback format in French '" + expectedFrench + "' in HTML: " + invalidHtml,
        invalidHtml.contains(expectedFrench));

    String blankHtml = renderTemplate("fr-FR", "", cal, "Default", false);
    assertTrue(
        "Expected fallback format in French '"
            + expectedFrench
            + "' for blank format in HTML: "
            + blankHtml,
        blankHtml.contains(expectedFrench));
  }

  @Test
  public void shouldFormatFallbackCurrentDateInConfiguredLocaleWhenPostDateMissing()
      throws Exception {
    String html = renderTemplate("fr-FR", "EEEE d MMMM yyyy", null, "Default", false);
    assertTrue(
        "Expected French fallback date 'mardi 24 novembre 2026' in HTML: " + html,
        html.contains("mardi 24 novembre 2026"));
  }

  @Test
  public void shouldAppendNonDefaultTimezoneAndHideDateWhenConfigured() throws Exception {
    String htmlWithTz =
        renderTemplate("fr-FR", "EEEE d MMMM yyyy", createTestCalendar(), "EST", false);
    assertTrue(
        "Expected French date with timezone 'mardi 24 novembre 2026 EST' in HTML: " + htmlWithTz,
        htmlWithTz.contains("mardi 24 novembre 2026 EST"));

    String hiddenHtml =
        renderTemplate("fr-FR", "EEEE d MMMM yyyy", createTestCalendar(), "EST", true);
    assertFalse(
        "Did not expect .perc-blog-date when hidden in HTML: " + hiddenHtml,
        hiddenHtml.contains("class=\"perc-blog-date\""));
  }

  private static String renderTemplate(
      String locale,
      String dateTimeFormat,
      Calendar postDate,
      String postDateTz,
      boolean hideBlogDate)
      throws Exception {
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
    props.put("titleFormat", "h1");
    props.put("blogPostDateTimeFormat", dateTimeFormat);
    if (locale != null) {
      props.put("locale", locale);
    }
    props.put("perc_hidefield_blog_date", hideBlogDate);
    props.put("blogPostBylineText", "by");
    props.put("perc_hidefield_byline", Boolean.FALSE);
    props.put("perc_hidefield_tags", Boolean.TRUE);
    props.put("perc_tags_label", "Tags:");
    props.put("perc_hidefield_categories", Boolean.TRUE);
    props.put("perc_categories_label", "Categories:");
    props.put("blogNavigationLocation", "none");
    props.put("blogNavigationContext", "preNext");
    props.put("blogPostNextPost", "Newer Post");
    props.put("blogPostPrePost", "Older Post");
    props.put("trackBlogPost", Boolean.FALSE);

    Map<String, Object> cssProps = new HashMap<>();
    cssProps.put("rootclass", "");

    PercHelper percHelper = new PercHelper(props, cssProps);
    SysHelper sysHelper = new SysHelper(postDate, postDateTz);

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub());

    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new EscapeToolStub());
    tools.put("date", new DateToolStub(createTestCalendar().getTime()));

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$perc", percHelper);
    bindings.put("$sys", sysHelper);
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
    runtime.evaluate(ctx, writer, "percBlogPost", velocityTemplate);
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
    private final PageStub page = new PageStub("101");
    private final LinkContextStub linkContext = new LinkContextStub();
    private List<Object> widgetContents = Collections.emptyList();

    public PercHelper(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.widget = new WidgetStub(properties, cssProperties);
    }

    public WidgetStub getWidget() {
      return widget;
    }

    public PageStub getPage() {
      return page;
    }

    public LinkContextStub getLinkContext() {
      return linkContext;
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

  public static class PageStub {
    private final String id;

    public PageStub(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
  }

  public static class LinkContextStub {}

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

  public static class SysHelper {
    private final AssemblyItemStub assemblyItem = new AssemblyItemStub();
    private final PageItemStub item;

    public SysHelper(Calendar postDate, String postDateTz) {
      this.item = new PageItemStub(postDate, postDateTz);
    }

    public AssemblyItemStub getAssemblyItem() {
      return assemblyItem;
    }

    public PageItemStub getItem() {
      return item;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return 1L;
    }
  }

  public static class PageItemStub {
    private final Calendar postDate;
    private final String postDateTz;

    public PageItemStub(Calendar postDate, String postDateTz) {
      this.postDate = postDate;
      this.postDateTz = postDateTz;
    }

    public boolean hasProperty(String name) {
      if ("rx:sys_contentpostdate".equals(name)) {
        return postDate != null;
      }
      return false;
    }

    public PropertyStub getProperty(String name) {
      if ("rx:sys_contentpostdatetz".equals(name)) {
        return new PropertyStub(postDateTz, null);
      }
      if ("rx:sys_contentpostdate".equals(name)) {
        return new PropertyStub(postDate != null ? "2026-11-24T14:30:00" : "", postDate);
      }
      return new PropertyStub("", null);
    }
  }

  public static class PageUtilsStub {
    private final PSPageUtils realPageUtils = new PSPageUtils();

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      return Collections.singletonList(new AssetItemStub());
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params, boolean includeSubItems) {
      return Collections.emptyList();
    }

    public Object itemLink(Object linkContext, Object page) {
      return "/Blog/Post1.html";
    }

    public String getItemPath(String pageId) {
      return "/Sites/TestSite/Blog/Post1";
    }

    public String getDeliveryServer(Object pubServerId) {
      return "http://localhost:9980";
    }

    public String parseDateFormat(String format, String defaultFormat) {
      return realPageUtils.parseDateFormat(format, defaultFormat);
    }

    public SimpleDateFormat getDateFormatFromLocale(String format, String locale) {
      return realPageUtils.getDateFormatFromLocale(format, locale);
    }
  }

  public static class AssetItemStub {
    private final AssetNodeStub node = new AssetNodeStub();

    public AssetNodeStub getNode() {
      return node;
    }
  }

  public static class AssetNodeStub {
    public PropertyStub getProperty(String name) {
      if ("authorname".equals(name)) {
        return new PropertyStub("QA Author", null);
      }
      if ("rx:displaytitle".equals(name)) {
        return new PropertyStub("Sample Blog Post", null);
      }
      if ("rx:postbody".equals(name)) {
        return new PropertyStub("<p>Post body</p>", null);
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
