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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Regression tests for {@code percDepartment.xml}:
 *
 * <ul>
 *   <li>Issue #438: verifies {@code perc_hidefield_dptPageField} ("Hide Department Page") renders
 *       the department name without an anchor link when enabled.
 *   <li>Issue #439: verifies {@code perc_hidefield_dptOrganizationField} ("Hide Organization")
 *       hides {@code .perc-org-name} alongside {@code perc_hidefield_orgNameField} ("Hide
 *       Organization Name").
 *   <li>Issue #440: verifies the JSON-LD schema includes the resolved organization {@code name} on
 *       the outer {@code Organization} object and omits it when no organization is resolved.
 *   <li>Issue #441: verifies {@code department.address.streetAddress} separates {@code dptAddress1}
 *       and {@code dptAddress2} with a single space and omits empty lines cleanly.
 * </ul>
 */
public class PSDepartmentWidgetTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percDepartment.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">\\s*<!--.*?-->\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Code>",
          Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!--.*?-->\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  private static final Pattern JSON_LD_SCRIPT_PATTERN =
      Pattern.compile(
          "<script[^>]*type=['\"]application/ld\\+json['\"][^>]*>(.*?)</script>", Pattern.DOTALL);

  private static final String STUB_MACROS =
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end";

  @Test
  public void shouldOmitLinkWhenHideDepartmentPageIsTrueWithExternalWebsite() throws Exception {
    String html =
        renderTemplate(
            Boolean.TRUE, "h2", "Engineering", "", "", "", "https://example.edu/engineering");

    String dptNameHtml = extractDepartmentNameBlock(html);
    assertTrue(
        "Expected formatted department name in .perc-dpt-name: " + dptNameHtml,
        dptNameHtml.contains("<h2>Engineering</h2>"));
    assertFalse(
        "Did not expect <a> link in .perc-dpt-name when Hide Department Page is true: "
            + dptNameHtml,
        dptNameHtml.contains("<a "));
  }

  @Test
  public void shouldOmitLinkWhenHideDepartmentPageIsTrueWithManagedPage() throws Exception {
    String html =
        renderTemplate(
            Boolean.TRUE,
            "div",
            "Engineering",
            "42",
            "/departments/engineering.html",
            "Engineering Page",
            "");

    String dptNameHtml = extractDepartmentNameBlock(html);
    assertTrue(
        "Expected formatted department name in .perc-dpt-name: " + dptNameHtml,
        dptNameHtml.contains("<div>Engineering</div>"));
    assertFalse(
        "Did not expect <a> link in .perc-dpt-name when Hide Department Page is true: "
            + dptNameHtml,
        dptNameHtml.contains("<a "));
  }

  @Test
  public void shouldRenderLinkWhenHideDepartmentPageIsFalseWithExternalWebsite() throws Exception {
    String html =
        renderTemplate(
            Boolean.FALSE, "div", "Engineering", "", "", "", "https://example.edu/engineering");

    String dptNameHtml = extractDepartmentNameBlock(html);
    assertTrue(
        "Expected external website link in .perc-dpt-name: " + dptNameHtml,
        dptNameHtml.contains(
            "<a href=\"https://example.edu/engineering\" title=\"https://example.edu/engineering\">"));
    assertTrue(
        "Expected formatted department name inside link: " + dptNameHtml,
        dptNameHtml.contains("<div>Engineering</div>"));
  }

  @Test
  public void shouldRenderLinkWhenHideDepartmentPageIsFalseWithManagedPage() throws Exception {
    String html =
        renderTemplate(
            Boolean.FALSE,
            "h3",
            "Engineering",
            "42",
            "/departments/engineering.html",
            "Engineering Page",
            "");

    String dptNameHtml = extractDepartmentNameBlock(html);
    assertTrue(
        "Expected managed page link in .perc-dpt-name: " + dptNameHtml,
        dptNameHtml.contains(
            "<a href=\"/departments/engineering.html\" title=\"Engineering Page\">"));
    assertTrue(
        "Expected formatted department name inside link: " + dptNameHtml,
        dptNameHtml.contains("<h3>Engineering</h3>"));
  }

  @Test
  public void shouldRenderLinkByDefaultWhenHideDepartmentPagePropertyIsUnset() throws Exception {
    String html =
        renderTemplate(null, "div", "Engineering", "", "", "", "https://example.edu/engineering");

    String dptNameHtml = extractDepartmentNameBlock(html);
    assertTrue(
        "Expected website link when perc_hidefield_dptPageField is unset: " + dptNameHtml,
        dptNameHtml.contains("<a href=\"https://example.edu/engineering\""));
  }

  @Test
  public void shouldOmitLinkWhenWebsitePathIsEmptyOrHash() throws Exception {
    String emptyHtml = renderTemplate(Boolean.FALSE, "div", "Engineering", "", "", "", "");
    String emptyNameHtml = extractDepartmentNameBlock(emptyHtml);
    assertTrue(
        "Expected department name without website: " + emptyNameHtml,
        emptyNameHtml.contains("<div>Engineering</div>"));
    assertFalse(
        "Did not expect <a> when website path is empty: " + emptyNameHtml,
        emptyNameHtml.contains("<a "));

    String hashHtml = renderTemplate(Boolean.FALSE, "div", "Engineering", "42", "#", "", "");
    String hashNameHtml = extractDepartmentNameBlock(hashHtml);
    assertTrue(
        "Expected department name when path is '#': " + hashNameHtml,
        hashNameHtml.contains("<div>Engineering</div>"));
    assertFalse(
        "Did not expect <a> when website path is '#': " + hashNameHtml,
        hashNameHtml.contains("<a "));
  }

  @Test
  public void shouldHideOrganizationWhenHideOrganizationIsTrue() throws Exception {
    String html =
        renderTemplate(
            Boolean.FALSE,
            Boolean.TRUE,
            Boolean.FALSE,
            "div",
            "Engineering",
            "Acme University",
            "",
            "",
            "",
            "");

    assertFalse(
        "Did not expect .perc-org-name when Hide Organization is true: " + html,
        html.contains("perc-org-name"));
    assertFalse(
        "Did not expect organization name when Hide Organization is true: " + html,
        html.contains("Acme University"));
    assertTrue(
        "Expected department name to still render: " + html,
        html.contains("<div>Engineering</div>"));
  }

  @Test
  public void shouldHideOrganizationWhenHideOrganizationIsTrueAndHideOrgNameIsUnset()
      throws Exception {
    String html =
        renderTemplate(
            null,
            Boolean.TRUE,
            Boolean.FALSE,
            "div",
            "Engineering",
            "Acme University",
            "",
            "",
            "",
            "");

    assertFalse(
        "Did not expect .perc-org-name when Hide Organization is true and Hide Organization Name"
            + " is unset: "
            + html,
        html.contains("perc-org-name"));
  }

  @Test
  public void shouldHideOrganizationWhenHideOrgNameIsTrue() throws Exception {
    String html =
        renderTemplate(
            Boolean.TRUE,
            Boolean.FALSE,
            Boolean.FALSE,
            "div",
            "Engineering",
            "Acme University",
            "",
            "",
            "",
            "");

    assertFalse(
        "Did not expect .perc-org-name when Hide Organization Name is true: " + html,
        html.contains("perc-org-name"));
  }

  @Test
  public void shouldHideOrganizationWhenBothHideOrganizationOptionsAreTrue() throws Exception {
    String html =
        renderTemplate(
            Boolean.TRUE,
            Boolean.TRUE,
            Boolean.FALSE,
            "div",
            "Engineering",
            "Acme University",
            "",
            "",
            "",
            "");

    assertFalse(
        "Did not expect .perc-org-name when both organization hide options are true: " + html,
        html.contains("perc-org-name"));
  }

  @Test
  public void shouldRenderOrganizationWhenBothHideOrganizationOptionsAreFalse() throws Exception {
    String html =
        renderTemplate(
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE,
            "div",
            "Engineering",
            "Acme University",
            "",
            "",
            "",
            "");

    assertTrue(
        "Expected .perc-org-name with organization name when both hide options are false: " + html,
        html.contains("<div class=\"perc-org-name\">Acme University</div>"));
  }

  @Test
  public void shouldRenderOrganizationByDefaultWhenHideOrganizationOptionsAreUnset()
      throws Exception {
    String html =
        renderTemplate(
            null, null, Boolean.FALSE, "div", "Engineering", "Acme University", "", "", "", "");

    assertTrue(
        "Expected .perc-org-name with organization name when hide options are unset: " + html,
        html.contains("<div class=\"perc-org-name\">Acme University</div>"));
  }

  @Test
  public void shouldIncludeOrganizationNameInSchemaWhenOrganizationIsResolved() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.FALSE,
            "Dept X",
            "601",
            "Child Org",
            "9 Side Road",
            "Floor 3",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "555-0200",
            "555-0201",
            "dept@example.com");

    assertEquals("Organization", schema.getString("@type"));
    assertEquals("Child Org", schema.getString("name"));
    JSONObject department = schema.getJSONObject("department");
    assertEquals("Dept X", department.getString("name"));
    assertEquals("555-0200", department.getString("telephone"));
    assertEquals("555-0201", department.getString("faxNumber"));
    assertEquals("mailto:dept@example.com", department.getString("email"));
  }

  @Test
  public void shouldOmitOrganizationNameInSchemaWhenOrganizationIsAbsent() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.TRUE,
            "Dept X",
            "",
            "",
            "9 Side Road",
            "Floor 3",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "",
            "",
            "");

    assertFalse(
        "Did not expect outer schema name when dptOrganization is empty: " + schema,
        schema.has("name"));
  }

  @Test
  public void shouldOmitOrganizationNameInSchemaWhenOrganizationLookupReturnsEmpty()
      throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.TRUE,
            "Dept X",
            "601",
            "",
            "9 Side Road",
            "Floor 3",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "",
            "",
            "");

    assertFalse(
        "Did not expect outer schema name when organization lookup returns no results: " + schema,
        schema.has("name"));
  }

  @Test
  public void shouldJoinStreetAddressLinesWithSpaceInSchema() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.FALSE,
            "Dept X",
            "601",
            "Child Org",
            "9 Side Road",
            "Floor 3",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "555-0200",
            "555-0201",
            "dept@example.com");

    JSONObject address = schema.getJSONObject("department").getJSONObject("address");
    assertEquals("9 Side Road Floor 3", address.getString("streetAddress"));
    assertEquals("Shelbyville", address.getString("addressLocality"));
    assertEquals("KY", address.getString("addressRegion"));
    assertEquals("40065", address.getString("postalCode"));
    assertEquals("USA", address.getString("addressCountry"));
  }

  @Test
  public void shouldNotAddTrailingSpaceWhenOnlyAddress1IsPresentInSchema() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.TRUE,
            "Dept X",
            "601",
            "Child Org",
            "9 Side Road",
            "",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "",
            "",
            "");

    JSONObject address = schema.getJSONObject("department").getJSONObject("address");
    assertEquals("9 Side Road", address.getString("streetAddress"));
  }

  @Test
  public void shouldNotAddLeadingSpaceWhenOnlyAddress2IsPresentInSchema() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.TRUE,
            "Dept X",
            "601",
            "Child Org",
            "",
            "Floor 3",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "",
            "",
            "");

    JSONObject address = schema.getJSONObject("department").getJSONObject("address");
    assertEquals("Floor 3", address.getString("streetAddress"));
  }

  @Test
  public void shouldOmitStreetAddressInSchemaWhenBothAddressLinesAreEmpty() throws Exception {
    JSONObject schema =
        renderSchema(
            Boolean.TRUE,
            "Dept X",
            "601",
            "Child Org",
            "",
            "",
            "Shelbyville",
            "KY",
            "40065",
            "USA",
            "",
            "",
            "");

    JSONObject address = schema.getJSONObject("department").getJSONObject("address");
    assertFalse(
        "Did not expect streetAddress when both address lines are empty: " + address,
        address.has("streetAddress"));
  }

  private static String extractDepartmentNameBlock(String html) {
    int start = html.indexOf("<div class=\"perc-dpt-name\">");
    assertTrue("Expected .perc-dpt-name block in rendered HTML: " + html, start >= 0);
    int nextField = html.indexOf("<div class=\"perc-dpt-address\">", start);
    if (nextField < 0) {
      nextField = html.indexOf("</div>\n       </div>", start);
    }
    if (nextField < 0) {
      nextField = html.length();
    }
    return html.substring(start, nextField);
  }

  private static String renderTemplate(
      Boolean hideDepartmentPage,
      String dptNameFormat,
      String dptName,
      String dptWebsiteLinkId,
      String managedWebsitePath,
      String managedWebsiteTitle,
      String dptWebsiteExt)
      throws Exception {
    return renderTemplate(
        Boolean.TRUE,
        Boolean.FALSE,
        hideDepartmentPage,
        dptNameFormat,
        dptName,
        "",
        dptWebsiteLinkId,
        managedWebsitePath,
        managedWebsiteTitle,
        dptWebsiteExt);
  }

  private static String renderTemplate(
      Boolean hideOrgName,
      Boolean hideDptOrganization,
      Boolean hideDepartmentPage,
      String dptNameFormat,
      String dptName,
      String orgName,
      String dptWebsiteLinkId,
      String managedWebsitePath,
      String managedWebsiteTitle,
      String dptWebsiteExt)
      throws Exception {
    Map<String, Object> props = new HashMap<>();
    props.put("dptNameFormat", dptNameFormat);
    if (hideOrgName != null) {
      props.put("perc_hidefield_orgNameField", hideOrgName);
    }
    props.put("perc_hidefield_dptAddressField", Boolean.TRUE);
    props.put("perc_hidefield_dptPhoneField", Boolean.TRUE);
    props.put("perc_hidefield_dptFaxField", Boolean.TRUE);
    props.put("perc_hidefield_dptEmailField", Boolean.TRUE);
    if (hideDepartmentPage != null) {
      props.put("perc_hidefield_dptPageField", hideDepartmentPage);
    }
    if (hideDptOrganization != null) {
      props.put("perc_hidefield_dptOrganizationField", hideDptOrganization);
    }

    Map<String, String> assetProps = defaultAssetProps(dptName, orgName.isEmpty() ? "" : "601");
    assetProps.put("dptWebsite_linkId", dptWebsiteLinkId);
    assetProps.put("dptWebsiteExt", dptWebsiteExt);

    Map<String, String> orgProps = defaultOrgProps(orgName);

    return renderWidget(props, assetProps, orgProps, managedWebsitePath, managedWebsiteTitle).html;
  }

  private static JSONObject renderSchema(
      Boolean hideEmail,
      String dptName,
      String dptOrganization,
      String orgName,
      String dptAddress1,
      String dptAddress2,
      String dptCity,
      String dptState,
      String dptPostCode,
      String dptCountry,
      String dptPhone,
      String dptFax,
      String dptEmail)
      throws Exception {
    Map<String, Object> props = new HashMap<>();
    props.put("dptNameFormat", "div");
    props.put("perc_hidefield_orgNameField", Boolean.FALSE);
    props.put("perc_hidefield_dptAddressField", Boolean.FALSE);
    props.put("perc_hidefield_dptPhoneField", Boolean.FALSE);
    props.put("perc_hidefield_dptFaxField", Boolean.FALSE);
    props.put("perc_hidefield_dptEmailField", hideEmail);
    props.put("perc_hidefield_dptPageField", Boolean.FALSE);
    props.put("perc_hidefield_dptOrganizationField", Boolean.FALSE);

    Map<String, String> assetProps = defaultAssetProps(dptName, dptOrganization);
    assetProps.put("dptAddress1", dptAddress1);
    assetProps.put("dptAddress2", dptAddress2);
    assetProps.put("dptCity", dptCity);
    assetProps.put("dptState", dptState);
    assetProps.put("dptPostCode", dptPostCode);
    assetProps.put("dptCountry", dptCountry);
    assetProps.put("dptPhone", dptPhone);
    assetProps.put("dptFax", dptFax);
    assetProps.put("dptEmail", dptEmail);

    Map<String, String> orgProps = defaultOrgProps(orgName);

    RenderResult result = renderWidget(props, assetProps, orgProps, "", "");
    Matcher matcher = JSON_LD_SCRIPT_PATTERN.matcher(result.additionalHeadContent);
    assertTrue(
        "Expected JSON-LD script in additional head content: " + result.additionalHeadContent,
        matcher.find());
    return new JSONObject(matcher.group(1));
  }

  private static Map<String, String> defaultAssetProps(String dptName, String dptOrganization) {
    Map<String, String> assetProps = new HashMap<>();
    assetProps.put("sys_contentid", "501");
    assetProps.put("dptName", dptName);
    assetProps.put("dptOrganization", dptOrganization);
    assetProps.put("dptAddress1", "");
    assetProps.put("dptAddress2", "");
    assetProps.put("dptCity", "");
    assetProps.put("dptState", "");
    assetProps.put("dptPostCode", "");
    assetProps.put("dptCountry", "");
    assetProps.put("dptEmail", "");
    assetProps.put("dptPhone", "");
    assetProps.put("dptFax", "");
    assetProps.put("dptWebsite_linkId", "");
    assetProps.put("dptWebsiteExt", "");
    return assetProps;
  }

  private static Map<String, String> defaultOrgProps(String orgName) {
    Map<String, String> orgProps = new HashMap<>();
    if (!orgName.isEmpty()) {
      orgProps.put("orgName", orgName);
      orgProps.put("orgPage_linkId", "");
      orgProps.put("orgPageExt", "");
    }
    return orgProps;
  }

  private static RenderResult renderWidget(
      Map<String, Object> props,
      Map<String, String> assetProps,
      Map<String, String> orgProps,
      String managedWebsitePath,
      String managedWebsiteTitle)
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

    PercHelper percHelper = new PercHelper(props);
    SysHelper sysHelper = new SysHelper();

    Map<String, Object> rx = new HashMap<>();
    rx.put(
        "pageutils",
        new PageUtilsStub(assetProps, orgProps, managedWebsitePath, managedWebsiteTitle));
    rx.put("string", new StringToolStub());

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$perc", percHelper);
    bindings.put("$sys", sysHelper);
    bindings.put("$rx", rx);

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
    runtime.evaluate(ctx, writer, "percDepartment", velocityTemplate);
    return new RenderResult(writer.toString(), percHelper.getPage().getAdditionalHeadContent());
  }

  private static final class RenderResult {
    private final String html;
    private final String additionalHeadContent;

    private RenderResult(String html, String additionalHeadContent) {
      this.html = html;
      this.additionalHeadContent = additionalHeadContent;
    }
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
    private final PageStub page = new PageStub("101", "department-page");
    private final LinkContextStub linkContext = new LinkContextStub();
    private List<Object> widgetContents = Collections.emptyList();

    public PercHelper(Map<String, Object> properties) {
      this.widget = new WidgetStub(properties);
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
  }

  public static class PageStub {
    private final String id;
    private final String name;
    private String additionalHeadContent = "";

    public PageStub(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public List<String> getFolderPaths() {
      return Collections.singletonList("//Sites/example.edu/departments");
    }

    public String getAdditionalHeadContent() {
      return additionalHeadContent;
    }

    public void setAdditionalHeadContent(String additionalHeadContent) {
      this.additionalHeadContent = additionalHeadContent;
    }
  }

  public static class LinkContextStub {}

  public static class WidgetStub {
    private final WidgetItemStub item;

    public WidgetStub(Map<String, Object> properties) {
      this.item = new WidgetItemStub(properties);
    }

    public WidgetItemStub getItem() {
      return item;
    }
  }

  public static class WidgetItemStub {
    private final Map<String, Object> properties;

    public WidgetItemStub(Map<String, Object> properties) {
      this.properties = properties;
    }

    public String getId() {
      return "widget-1";
    }

    public Map<String, Object> getProperties() {
      return properties;
    }
  }

  public static class SysHelper {
    private final Object assemblyItem = new Object();

    public Object getAssemblyItem() {
      return assemblyItem;
    }
  }

  public static class PageUtilsStub {
    private final Map<String, String> assetProps;
    private final Map<String, String> orgProps;
    private final String managedWebsitePath;
    private final String managedWebsiteTitle;

    public PageUtilsStub(
        Map<String, String> assetProps,
        Map<String, String> orgProps,
        String managedWebsitePath,
        String managedWebsiteTitle) {
      this.assetProps = assetProps;
      this.orgProps = orgProps;
      this.managedWebsitePath = managedWebsitePath;
      this.managedWebsiteTitle = managedWebsiteTitle;
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      if (finderName != null) {
        if (orgProps.isEmpty()) {
          return Collections.emptyList();
        }
        return Collections.singletonList(new AssetItemStub(orgProps));
      }
      return Collections.singletonList(new AssetItemStub(assetProps));
    }

    public String renderManagedItemPath(Object linkContext, String linkId) {
      if (linkId == null || linkId.isEmpty()) {
        return "";
      }
      return managedWebsitePath;
    }

    public String getManagedLinkDependentId(String linkId) {
      if (linkId == null || linkId.isEmpty()) {
        return "";
      }
      return "900";
    }

    public Map<String, String> findItemFieldValues(
        String contentType, String fields, String contentId) {
      Map<String, String> result = new HashMap<>();
      result.put("resource_link_title", managedWebsiteTitle);
      return result;
    }
  }

  public static class StringToolStub {
    public Map<String, Object> stringToMap(String input) {
      return new HashMap<>();
    }

    public JSONObject getJSONObject() {
      return new JSONObject();
    }
  }

  public static class AssetItemStub {
    private final AssetNodeStub node;

    public AssetItemStub(Map<String, String> props) {
      this.node = new AssetNodeStub(props);
    }

    public AssetNodeStub getNode() {
      return node;
    }
  }

  public static class AssetNodeStub {
    private final Map<String, String> props;

    public AssetNodeStub(Map<String, String> props) {
      this.props = props;
    }

    public PropertyStub getProperty(String name) {
      return new PropertyStub(props.getOrDefault(name, ""));
    }
  }

  public static class PropertyStub {
    private final String stringValue;

    public PropertyStub(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getString() {
      return stringValue;
    }
  }
}
