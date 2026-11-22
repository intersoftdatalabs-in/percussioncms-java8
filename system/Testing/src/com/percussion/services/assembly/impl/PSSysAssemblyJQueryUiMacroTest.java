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
package com.percussion.services.assembly.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.tools.generic.EscapeTool;
import org.junit.Test;

/**
 * Regression test for issue #355: verifies {@code sys_assembly.vm} renders {@code percJQueryUIWidget}
 * scripts in {@code <head>} when Script Location is {@code additionalHeadContent} ("Head"), and
 * preserves {@code afterBodyStart}, {@code beforeBodyClose}, {@code includeUIOnPublishedPage},
 * deferral propagation, widget location coordination, edit mode, and default footer fallback behavior.
 */
public class PSSysAssemblyJQueryUiMacroTest {

  private static final String VM_DIR_REL_PATH =
      "system/cms/content/applications/sys_resources/ApplicationFiles/vm";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final String SYSTEM_JQUERY_PATH = "/web_resources/cm/jslib/jquery.js";
  private static final String SYSTEM_JQUERY_UI_PATH = "/web_resources/cm/jslib/jquery-ui.js";
  private static final String EDIT_JQUERY_UI_PATH =
      "/cm/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js";

  @Test
  public void shouldRenderJQueryUiInHeadAfterJQueryWhenLocationIsHead() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue("Expected jquery-ui.js in <head>, got:\n" + head, head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery.js before jquery-ui.js in <head>",
        head.indexOf(SYSTEM_JQUERY_PATH) < head.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery-ui.js before page additionalHeadContent",
        head.indexOf(SYSTEM_JQUERY_UI_PATH) < head.indexOf("<!-- page-head -->"));
    assertFalse("Did not expect jquery-ui.js in <body>, got:\n" + body, body.contains(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldRenderFallbackJQueryUiInHeadWhenCdnLinkUnreachable() throws Exception {
    String unreachableUrl = "https://cdn.example.invalid/jquery-ui.min.js";
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.markLinkBad(unreachableUrl);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(unreachableUrl, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue(
        "Expected fallback comment in <head>",
        head.contains("Unable to verify CDN link - defaulting to system jquery version"));
    assertTrue("Expected fallback jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertFalse("Did not expect unreachable URL in output", html.contains(unreachableUrl));
    assertFalse("Did not expect fallback jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldRenderCdnJQueryUiWithSriAndCrossOriginInHead() throws Exception {
    String cdnUrl = "https://code.jquery.com/ui/1.13.2/jquery-ui.min.js";
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(
            cdnUrl,
            "additionalHeadContent",
            "no",
            "yes",
            "sha256-testSriHash",
            "anonymous"));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue("Expected CDN URL in <head>", head.contains(cdnUrl));
    assertTrue("Expected integrity attribute in <head>", head.contains("integrity=\"sha256-testSriHash\""));
    assertTrue("Expected crossorigin attribute in <head>", head.contains("crossorigin=\"anonymous\""));
    assertFalse("Did not expect CDN URL in <body>", body.contains(cdnUrl));
    assertEquals(1, countOccurrences(html, cdnUrl));
  }

  @Test
  public void shouldRenderJQueryUiAfterBodyStartWhenConfigured() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "afterBodyStart", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery-ui.js before main body content",
        body.indexOf(SYSTEM_JQUERY_UI_PATH) < body.indexOf("<div id=\"main\">Body</div>"));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldRenderJQueryUiBeforeBodyCloseWhenConfiguredWithDefer() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "beforeBodyClose", "yes", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery-ui.js after main body content",
        body.indexOf(SYSTEM_JQUERY_UI_PATH) > body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected defer attribute on jquery-ui.js script",
        body.contains("<script src=\"" + SYSTEM_JQUERY_UI_PATH + "\"\ndefer"));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldOmitJQueryUiWhenIncludeOnPublishedPageIsNo() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "no", "", ""));

    String html = renderTemplate(ctx);

    assertFalse("Did not expect jquery-ui.js when includeUIOnPublishedPage=no", html.contains(SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldPropagateDeferFromJQueryWidgetToHeadJQueryUi() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "yes"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue(
        "Expected jquery.js before jquery-ui.js in <head>",
        head.indexOf(SYSTEM_JQUERY_PATH) < head.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected defer on jquery.js in <head>",
        head.contains("<script src=\"" + SYSTEM_JQUERY_PATH + "\"\ndefer"));
    assertTrue(
        "Expected defer propagated to jquery-ui.js in <head>",
        head.contains("<script src=\"" + SYSTEM_JQUERY_UI_PATH + "\"\ndefer"));
    assertFalse("Did not expect jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldRenderJQueryUiInHeadInPreviewMode() throws Exception {
    String cdnUrl = "https://code.jquery.com/ui/1.13.2/jquery-ui.min.js";
    StubContext ctx = new StubContext("PREVIEW", false, true);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(cdnUrl, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue("Expected jquery-ui.js in <head> in PREVIEW mode", head.contains(cdnUrl));
    assertFalse("Did not expect jquery-ui.js in <body> in PREVIEW mode", body.contains(cdnUrl));
    assertEquals(1, countOccurrences(html, cdnUrl));
  }

  @Test
  public void shouldRenderSystemJQueryUiInFooterInEditMode() throws Exception {
    String cdnUrl = "https://code.jquery.com/ui/1.13.2/jquery-ui.min.js";
    StubContext ctx = new StubContext("PREVIEW", true, true);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("additionalHeadContent", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(cdnUrl, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect CDN jquery-ui.js in <head> in edit mode", head.contains(cdnUrl));
    assertFalse("Did not expect edit jquery-ui.js in <head> in edit mode", head.contains(EDIT_JQUERY_UI_PATH));
    assertTrue("Expected edit jquery-ui.js in <body> footer in edit mode", body.contains(EDIT_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, EDIT_JQUERY_UI_PATH));
  }

  @Test
  public void shouldPreserveDefaultFooterSystemJQueryUiWhenNoWidgetPresent() throws Exception {
    StubContext ctxAllowed = new StubContext("PUBLISH", false, false);
    ctxAllowed.site.overrideSystemJQueryUI = false;
    String htmlAllowed = renderTemplate(ctxAllowed);
    assertFalse("Did not expect jquery-ui.js in <head>", extractHead(htmlAllowed).contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected default jquery-ui.js in <body>", extractBody(htmlAllowed).contains(SYSTEM_JQUERY_UI_PATH));

    StubContext ctxOverridden = new StubContext("PUBLISH", false, false);
    ctxOverridden.site.overrideSystemJQueryUI = true;
    String htmlOverridden = renderTemplate(ctxOverridden);
    assertFalse(
        "Did not expect jquery-ui.js when overrideSystemJQueryUI=true",
        htmlOverridden.contains(SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldEmitHeadJQueryUiAfterBodyStartWhenJQueryIsAfterBodyStart() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("afterBodyStart", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery.js in <head>", head.contains(SYSTEM_JQUERY_PATH));
    assertFalse("Did not expect jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected jquery.js in <body>", body.contains(SYSTEM_JQUERY_PATH));
    assertTrue("Expected jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery.js before main body content",
        body.indexOf(SYSTEM_JQUERY_PATH) < body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery-ui.js before main body content",
        body.indexOf(SYSTEM_JQUERY_UI_PATH) < body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery.js before jquery-ui.js in <body>",
        body.indexOf(SYSTEM_JQUERY_PATH) < body.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldEmitHeadJQueryUiBeforeBodyCloseWhenJQueryIsBeforeBodyClose() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("beforeBodyClose", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery.js in <head>", head.contains(SYSTEM_JQUERY_PATH));
    assertFalse("Did not expect jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected jquery.js in <body>", body.contains(SYSTEM_JQUERY_PATH));
    assertTrue("Expected jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery.js after main body content",
        body.indexOf(SYSTEM_JQUERY_PATH) > body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery-ui.js after main body content",
        body.indexOf(SYSTEM_JQUERY_UI_PATH) > body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery.js before jquery-ui.js in <body>",
        body.indexOf(SYSTEM_JQUERY_PATH) < body.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldEmitAfterBodyStartJQueryUiBeforeBodyCloseWhenJQueryIsBeforeBodyClose()
      throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget("percJQueryWidget", defaultJQueryProps("beforeBodyClose", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "afterBodyStart", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery.js in <head>", head.contains(SYSTEM_JQUERY_PATH));
    assertFalse("Did not expect jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue("Expected jquery.js in <body>", body.contains(SYSTEM_JQUERY_PATH));
    assertTrue("Expected jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery.js after main body content",
        body.indexOf(SYSTEM_JQUERY_PATH) > body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery-ui.js after main body content",
        body.indexOf(SYSTEM_JQUERY_UI_PATH) > body.indexOf("<div id=\"main\">Body</div>"));
    assertTrue(
        "Expected jquery.js before jquery-ui.js in <body>",
        body.indexOf(SYSTEM_JQUERY_PATH) < body.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldKeepHeadJQueryUiInHeadWhenJQueryWidgetExcludesPublishedPage() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget(
        "percJQueryWidget", defaultJQueryProps("beforeBodyClose", "no", "no"));
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertFalse("Did not expect jquery.js when includeOnPublishedPage=no", html.contains(SYSTEM_JQUERY_PATH));
    assertTrue("Expected jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertFalse("Did not expect jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  @Test
  public void shouldRenderHeadJQueryUiInHeadWhenOnlyJQueryUiWidgetPresent() throws Exception {
    StubContext ctx = new StubContext("PUBLISH", false, false);
    ctx.pageUtils.addWidget(
        "percJQueryUIWidget",
        jQueryUiProps(SYSTEM_JQUERY_UI_PATH, "additionalHeadContent", "no", "yes", "", ""));

    String html = renderTemplate(ctx);
    String head = extractHead(html);
    String body = extractBody(html);

    assertTrue("Expected jquery.js in <head>", head.contains(SYSTEM_JQUERY_PATH));
    assertTrue("Expected jquery-ui.js in <head>", head.contains(SYSTEM_JQUERY_UI_PATH));
    assertTrue(
        "Expected jquery.js before jquery-ui.js in <head>",
        head.indexOf(SYSTEM_JQUERY_PATH) < head.indexOf(SYSTEM_JQUERY_UI_PATH));
    assertFalse("Did not expect jquery-ui.js in <body>", body.contains(SYSTEM_JQUERY_UI_PATH));
    assertEquals(1, countOccurrences(html, SYSTEM_JQUERY_UI_PATH));
  }

  private static String renderTemplate(StubContext stub) throws Exception {
    File vmDir = resolveRepoFile(VM_DIR_REL_PATH);
    File propsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(propsFile)) {
      props.load(in);
    }
    props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, vmDir.getAbsolutePath());
    props.setProperty(RuntimeConstants.VM_LIBRARY, "sys_assembly.vm");

    RuntimeInstance runtime = new RuntimeInstance();
    runtime.init(props);

    VelocityContext vc = new VelocityContext();
    vc.put("perc", stub.perc);
    vc.put("sys", stub.sys);
    vc.put("rx", stub.rx);
    vc.put("tools", stub.tools);

    String template =
        "#macro(cssPreload)#end#perc_templateHeader()\n<div id=\"main\">Body</div>\n#perc_templateFooter()";
    StringWriter writer = new StringWriter();
    runtime.evaluate(vc, writer, "sys_assembly_test", template);
    return writer.toString();
  }

  private static File resolveRepoFile(String relativePath) throws IOException {
    File fromModule = new File(new File(".."), relativePath);
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

  private static String extractHead(String html) {
    int start = html.indexOf("<head>");
    int end = html.indexOf("</head>");
    assertTrue("Missing <head>...</head> in:\n" + html, start >= 0 && end > start);
    return html.substring(start, end + "</head>".length());
  }

  private static String extractBody(String html) {
    int start = html.indexOf("<body>");
    int end = html.indexOf("</body>");
    assertTrue("Missing <body>...</body> in:\n" + html, start >= 0 && end > start);
    return html.substring(start, end + "</body>".length());
  }

  private static int countOccurrences(String text, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = text.indexOf(needle, idx)) >= 0) {
      count++;
      idx += needle.length();
    }
    return count;
  }

  private static Map<String, Object> defaultJQueryProps(String scriptLocation, String isDeferred) {
    return defaultJQueryProps(scriptLocation, isDeferred, "yes");
  }

  private static Map<String, Object> defaultJQueryProps(
      String scriptLocation, String isDeferred, String includeOnPublishedPage) {
    Map<String, Object> props = new HashMap<>();
    props.put("jQueryLocation", SYSTEM_JQUERY_PATH);
    props.put("jQueryLocationIntegrity", "");
    props.put("jQueryLocationCrossOrigin", "");
    props.put("jQueryMigrateLocation", "");
    props.put("jQueryMigrateLocationIntegrity", "");
    props.put("jQueryMigrateLocationCrossOrigin", "");
    props.put("isDeferred", isDeferred);
    props.put("scriptLocation", scriptLocation);
    props.put("includeOnPublishedPage", includeOnPublishedPage);
    return props;
  }

  private static Map<String, Object> jQueryUiProps(
      String location,
      String scriptLocation,
      String isDeferred,
      String includeOnPublishedPage,
      String sriHash,
      String crossOrigin) {
    Map<String, Object> props = new HashMap<>();
    props.put("jQueryUILocation", location);
    props.put("jQueryUILocationIntegrity", sriHash);
    props.put("jQueryUILocationCrossOrigin", crossOrigin);
    props.put("isUIDeferred", isDeferred);
    props.put("scriptUILocation", scriptLocation);
    props.put("includeUIOnPublishedPage", includeOnPublishedPage);
    return props;
  }

  public static class StubContext {
    final StubSite site = new StubSite();
    final StubPageUtils pageUtils = new StubPageUtils();
    final StubPerc perc;
    final Map<String, Object> sys = new HashMap<>();
    final Map<String, Object> rx = new HashMap<>();
    final Map<String, Object> tools = new HashMap<>();

    StubContext(String mode, boolean editMode, boolean previewMode) {
      StubLinkContext linkContext = new StubLinkContext(mode, site);
      this.perc = new StubPerc(linkContext, editMode, previewMode);
      this.sys.put("assemblyItem", new StubAssemblyItem());
      this.sys.put("item", new StubItem());
      this.rx.put("pageutils", pageUtils);
      this.tools.put("esc", new EscapeTool());
      this.tools.put("date", new StubDateTool());
    }
  }

  public static class StubPerc {
    private final StubLinkContext linkContext;
    private final boolean editMode;
    private final boolean previewMode;
    private final StubPage page = new StubPage();
    private final StubTemplate template = new StubTemplate();

    StubPerc(StubLinkContext linkContext, boolean editMode, boolean previewMode) {
      this.linkContext = linkContext;
      this.editMode = editMode;
      this.previewMode = previewMode;
    }

    public StubLinkContext getLinkContext() {
      return linkContext;
    }

    public boolean isEditMode() {
      return editMode;
    }

    public boolean isPreviewMode() {
      return previewMode;
    }

    public boolean isScriptsOff() {
      return false;
    }

    public StubPage getPage() {
      return page;
    }

    public StubTemplate getTemplate() {
      return template;
    }

    public String getEditMode() {
      return Boolean.toString(editMode);
    }

    public String getEditType() {
      return "";
    }
  }

  public static class StubLinkContext {
    private final String mode;
    private final StubSite site;

    StubLinkContext(String mode, StubSite site) {
      this.mode = mode;
      this.site = site;
    }

    public String getMode() {
      return mode;
    }

    public StubSite getSite() {
      return site;
    }
  }

  public static class StubSite {
    boolean overrideSystemJQuery = false;
    boolean overrideSystemJQueryUI = false;

    public String getName() {
      return "TestSite";
    }

    public boolean isCanonical() {
      return false;
    }

    public boolean isOverrideSystemJQuery() {
      return overrideSystemJQuery;
    }

    public boolean isOverrideSystemJQueryUI() {
      return overrideSystemJQueryUI;
    }

    public boolean isOverrideSystemFoundation() {
      return true;
    }

    public String getSiteAdditionalHeadContent() {
      return "";
    }

    public String getSiteAfterBodyOpenContent() {
      return "";
    }

    public String getSiteBeforeBodyCloseContent() {
      return "";
    }
  }

  public static class StubPage {
    public String getId() {
      return "100";
    }

    public String getTitle() {
      return "Test Page";
    }

    public String getDescription() {
      return "";
    }

    public String getLinkTitle() {
      return "Test Page";
    }

    public String getNoindex() {
      return "false";
    }

    public String getName() {
      return "";
    }

    public String getAdditionalHeadContent() {
      return "<!-- page-head -->";
    }

    public String getAfterBodyStartContent() {
      return "";
    }

    public String getBeforeBodyCloseContent() {
      return "";
    }
  }

  public static class StubTemplate {
    public String getName() {
      return "TestTemplate";
    }

    public String getSourceTemplateName() {
      return "perc.base.plain";
    }

    public String getTheme() {
      return "percussion";
    }

    public String getCssRegion() {
      return "";
    }

    public String getCssOverride() {
      return "";
    }

    public String getAdditionalHeadContent() {
      return "";
    }

    public String getAfterBodyStartContent() {
      return "";
    }

    public String getBeforeBodyCloseContent() {
      return "";
    }

    public StubDocType getDocType() {
      return new StubDocType();
    }
  }

  public static class StubDocType {
    public String getSelected() {
      return "html5";
    }

    public List<StubDocTypeOption> getOptions() {
      return Collections.singletonList(new StubDocTypeOption());
    }
  }

  public static class StubDocTypeOption {
    public String getOption() {
      return "html5";
    }

    public String getValue() {
      return "<!DOCTYPE html>";
    }
  }

  public static class StubAssemblyItem {
    public String getPubServerId() {
      return "1";
    }
  }

  public static class StubItem {
    public boolean hasProperty(String name) {
      return false;
    }

    public StubProperty getProperty(String name) {
      return new StubProperty();
    }
  }

  public static class StubProperty {
    public String getString() {
      return "";
    }
  }

  public static class StubDateTool {
    public Date getDate() {
      return new Date(0L);
    }

    public String format(String format, Object date) {
      return "2026-01-01T00:00:00";
    }
  }

  public static class StubWidgetInstance {
    private final StubWidgetItem item;

    StubWidgetInstance(Map<String, Object> properties) {
      this.item = new StubWidgetItem(properties);
    }

    public StubWidgetItem getItem() {
      return item;
    }
  }

  public static class StubWidgetItem {
    private final Map<String, Object> properties;

    StubWidgetItem(Map<String, Object> properties) {
      this.properties = properties;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }
  }

  public static class StubJsLink {
    private final String url;

    StubJsLink(String url) {
      this.url = url;
    }

    public String getUrl() {
      return url;
    }

    @Override
    public String toString() {
      return url;
    }
  }

  public static class StubPageUtils {
    private final Map<String, List<StubWidgetInstance>> widgetsByName = new HashMap<>();
    private final Set<String> badLinks = new HashSet<>();

    void addWidget(String widgetName, Map<String, Object> props) {
      widgetsByName.computeIfAbsent(widgetName, k -> new ArrayList<>()).add(new StubWidgetInstance(props));
    }

    void markLinkBad(String url) {
      badLinks.add(url);
    }

    public List<StubWidgetInstance> findWidgetInstances(
        Object assemblyItem, Object regionName, String widgetName) {
      return widgetsByName.getOrDefault(widgetName, Collections.emptyList());
    }

    public List<Object> cssLinks(Object linkContext, Object assemblyItem) {
      return Collections.emptyList();
    }

    public List<StubJsLink> javascriptLinks(Object linkContext, Object assemblyItem) {
      List<StubJsLink> links = new ArrayList<>();
      links.add(new StubJsLink(SYSTEM_JQUERY_PATH));
      links.add(new StubJsLink(SYSTEM_JQUERY_UI_PATH));
      return links;
    }

    public String themeLink(Object linkContext, String theme) {
      return "/web_resources/themes/percussion/theme.css";
    }

    public String themeRegionCssLink(
        Object linkContext, String theme, Object editMode, Object editType) {
      return "";
    }

    public String processFrameworkContent(String content) {
      return content == null ? "" : content;
    }

    public boolean isLinkGood(String link, String mode, boolean dontCache) {
      return !badLinks.contains(link);
    }

    public String parseDateFormat(String format, String fallback) {
      return format;
    }

    public String getDeliveryServer(Object pubServerId) {
      return "";
    }

    public String productVersion() {
      return "8.1.8";
    }
  }
}
