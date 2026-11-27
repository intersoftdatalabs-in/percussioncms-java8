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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.tools.generic.EscapeTool;
import org.junit.Test;

/**
 * Regression test for issue #466: verifies {@code sys_assembly.vm} renders valid JSON in the
 * {@code #protectedRegionInformation} {@code data} attribute, closes the {@code <div>}, omits any
 * stray {@code )} before {@code <style>}, and prefixes the protected region CSS selector with
 * {@code #}.
 */
public class PSSysAssemblyProtectedRegionTest {

  private static final String VM_DIR_REL_PATH =
      "system/cms/content/applications/sys_resources/ApplicationFiles/vm";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";
  private static final Pattern PROTECTED_REGION_DIV =
      Pattern.compile(
          "<div\\s+id=\"protectedRegionInformation\"\\s+data=\"([^\"]*)\"></div>");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void shouldRenderValidProtectedRegionJsonInPublishMode() throws Exception {
    StubContext ctx = new StubContext("PUBLISH");
    ctx.template.protectedRegion = "test-protected";
    ctx.template.protectedRegionText = "Test Protected Region Text - calendar validation";
    ctx.pageUtils.siteLoginPage = "/login.html";

    String html = renderTemplate(ctx);
    Map<String, String> parsed = extractAndParseProtectedRegionData(html);

    assertEquals("test-protected", parsed.get("protectedRegion"));
    assertEquals(
        "Test Protected Region Text - calendar validation", parsed.get("protectedRegionText"));
    assertEquals("/login.html", parsed.get("siteLoginPage"));
    assertFalse("Did not expect stray ')' before <style> in:\n" + html, html.contains(")<style>"));
    assertTrue(
        "Expected '#test-protected{' CSS selector in:\n" + html,
        html.contains("#test-protected{"));
  }

  @Test
  public void shouldEscapeQuotesAndBackslashesInProtectedRegionJson() throws Exception {
    StubContext ctx = new StubContext("PUBLISH");
    ctx.template.protectedRegion = "members-only";
    ctx.template.protectedRegionText = "Members \"VIP\" & O'Connor \\ Section";
    ctx.pageUtils.siteLoginPage = "/site/login?next=\"home\"";

    String html = renderTemplate(ctx);
    Map<String, String> parsed = extractAndParseProtectedRegionData(html);

    assertEquals("members-only", parsed.get("protectedRegion"));
    assertEquals("Members \"VIP\" & O'Connor \\ Section", parsed.get("protectedRegionText"));
    assertEquals("/site/login?next=\"home\"", parsed.get("siteLoginPage"));
  }

  @Test
  public void shouldRenderValidJsonWhenLoginPageIsEmpty() throws Exception {
    StubContext ctx = new StubContext("PUBLISH");
    ctx.template.protectedRegion = "test-protected";
    ctx.template.protectedRegionText = "Please log in";
    ctx.pageUtils.siteLoginPage = "";

    String html = renderTemplate(ctx);
    Map<String, String> parsed = extractAndParseProtectedRegionData(html);

    assertEquals("test-protected", parsed.get("protectedRegion"));
    assertEquals("Please log in", parsed.get("protectedRegionText"));
    assertEquals("", parsed.get("siteLoginPage"));
  }

  @Test
  public void shouldNotRenderProtectedRegionInPreviewOrWhenEmpty() throws Exception {
    StubContext previewCtx = new StubContext("PREVIEW");
    previewCtx.template.protectedRegion = "test-protected";
    previewCtx.template.protectedRegionText = "Please log in";
    assertFalse(renderTemplate(previewCtx).contains("protectedRegionInformation"));

    StubContext emptyCtx = new StubContext("PUBLISH");
    emptyCtx.template.protectedRegion = "";
    assertFalse(renderTemplate(emptyCtx).contains("protectedRegionInformation"));
  }

  private static Map<String, String> extractAndParseProtectedRegionData(String html)
      throws IOException {
    Matcher matcher = PROTECTED_REGION_DIV.matcher(html);
    assertTrue("Expected <div id=\"protectedRegionInformation\" ...></div> in:\n" + html, matcher.find());
    String rawAttr = matcher.group(1);
    String json = StringEscapeUtils.unescapeHtml4(rawAttr);
    return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
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
    runtime.evaluate(vc, writer, "sys_assembly_protected_region_test", template);
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

  public static class StubContext {
    final StubSite site = new StubSite();
    final StubPageUtils pageUtils = new StubPageUtils();
    final StubTemplate template = new StubTemplate();
    final StubPerc perc;
    final Map<String, Object> sys = new HashMap<>();
    final Map<String, Object> rx = new HashMap<>();
    final Map<String, Object> tools = new HashMap<>();

    StubContext(String mode) {
      StubLinkContext linkContext = new StubLinkContext(mode, site);
      this.perc = new StubPerc(linkContext, template);
      this.sys.put("assemblyItem", new StubAssemblyItem());
      this.sys.put("item", new StubItem());
      this.rx.put("pageutils", pageUtils);
      this.tools.put("esc", new EscapeTool());
      this.tools.put("date", new StubDateTool());
    }
  }

  public static class StubPerc {
    private final StubLinkContext linkContext;
    private final StubPage page = new StubPage();
    private final StubTemplate template;

    StubPerc(StubLinkContext linkContext, StubTemplate template) {
      this.linkContext = linkContext;
      this.template = template;
    }

    public StubLinkContext getLinkContext() {
      return linkContext;
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return "PREVIEW".equals(linkContext.getMode());
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
      return "false";
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
    public String getName() {
      return "TestSite";
    }

    public boolean isCanonical() {
      return false;
    }

    public boolean isOverrideSystemJQuery() {
      return false;
    }

    public boolean isOverrideSystemJQueryUI() {
      return false;
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
      return "";
    }

    public String getAfterBodyStartContent() {
      return "";
    }

    public String getBeforeBodyCloseContent() {
      return "";
    }
  }

  public static class StubTemplate {
    String protectedRegion = "";
    String protectedRegionText = "";

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

    public String getProtectedRegion() {
      return protectedRegion;
    }

    public String getProtectedRegionText() {
      return protectedRegionText;
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

  public static class StubPageUtils {
    String siteLoginPage = "";

    public String getSiteLoginPage(String siteName) {
      return siteLoginPage;
    }

    public List<Map<String, Object>> findWidgets(Object assemblyItem, String widgetName) {
      return Collections.emptyList();
    }

    public String processFrameworkContent(String content) {
      return content == null ? "" : content;
    }

    public String parseThemeCSS(String theme, String cssRegion, boolean isEdit, boolean isPreview) {
      return "";
    }

    public boolean isValidLink(String url) {
      return true;
    }
  }

  public static class StubDateTool {
    public String getYear() {
      return "2026";
    }
  }
}
