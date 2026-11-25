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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
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

    Map<String, String> assetProps = new HashMap<>();
    assetProps.put("sys_contentid", "501");
    assetProps.put("dptName", dptName);
    assetProps.put("dptOrganization", orgName.isEmpty() ? "" : "601");
    assetProps.put("dptAddress1", "");
    assetProps.put("dptAddress2", "");
    assetProps.put("dptCity", "");
    assetProps.put("dptState", "");
    assetProps.put("dptPostCode", "");
    assetProps.put("dptCountry", "");
    assetProps.put("dptEmail", "");
    assetProps.put("dptPhone", "");
    assetProps.put("dptFax", "");
    assetProps.put("dptWebsite_linkId", dptWebsiteLinkId);
    assetProps.put("dptWebsiteExt", dptWebsiteExt);

    Map<String, String> orgProps = new HashMap<>();
    if (!orgName.isEmpty()) {
      orgProps.put("orgName", orgName);
      orgProps.put("orgPage_linkId", "");
      orgProps.put("orgPageExt", "");
    }

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

    public Map<String, Object> getJSONObject() {
      return new LinkedHashMap<>();
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
