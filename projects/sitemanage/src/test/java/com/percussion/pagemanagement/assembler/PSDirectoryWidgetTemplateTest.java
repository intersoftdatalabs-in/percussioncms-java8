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
import java.util.ArrayList;
import java.util.Arrays;
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
 * Regression tests for {@code percDirectory.xml}:
 *
 * <ul>
 *   <li>Issue #446: verifies that a Directory configured for a single organization and all
 *       departments ({@code $orgSearchId > 0 && $deptId == -1}) renders a visible {@code
 *       #perc-dpt-filter} dropdown without {@code #perc-org-filter}, while preserving dropdown
 *       hiding when {@code perc_hidefield_filter_dropdown} is enabled, omitting both dropdowns when
 *       a specific department is selected, and keeping the all-organizations dropdown behavior.
 * </ul>
 */
public class PSDirectoryWidgetTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percDirectory.xml";
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

  private static final Pattern DPT_SELECT_PATTERN =
      Pattern.compile("<select\\b[^>]*id=\"perc-dpt-filter\"[^>]*>", Pattern.DOTALL);

  private static final String STUB_MACROS =
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end";

  @Test
  public void shouldRenderVisibleDepartmentFilterWhenOneOrgAndAllDepartments() throws Exception {
    String html = renderTemplate(Boolean.FALSE, "601", "-1");

    assertFalse(
        "Did not expect #perc-org-filter when a single organization is selected: " + html,
        html.contains("id=\"perc-org-filter\""));

    Matcher dptSelect = DPT_SELECT_PATTERN.matcher(html);
    assertTrue(
        "Expected #perc-dpt-filter select when one organization and all departments are selected: "
            + html,
        dptSelect.find());
    assertFalse(
        "Expected #perc-dpt-filter to be visible (no display:none): " + dptSelect.group(),
        dptSelect.group().contains("display:none"));

    assertTrue(
        "Expected data-search-all-orgs=\"false\": " + html,
        html.contains("data-search-all-orgs=\"false\""));
    assertTrue(
        "Expected data-show-dpt-filter=\"true\": " + html,
        html.contains("data-show-dpt-filter=\"true\""));
    assertTrue(
        "Expected data-directory-org-name=\"Acme University\": " + html,
        html.contains("data-directory-org-name=\"Acme University\""));
    assertTrue(
        "Expected Engineering department cell in rendered directory: " + html,
        html.contains("<span class=\"perc-person-field perc-person-dpt\">Engineering</span>"));
    assertTrue(
        "Expected Admissions department cell in rendered directory: " + html,
        html.contains("<span class=\"perc-person-field perc-person-dpt\">Admissions</span>"));
  }

  @Test
  public void shouldRenderVisibleDepartmentFilterWhenOneOrgAndHideDropdownIsUnset()
      throws Exception {
    String html = renderTemplate(null, "601", "-1");

    assertFalse(
        "Did not expect #perc-org-filter when a single organization is selected: " + html,
        html.contains("id=\"perc-org-filter\""));

    Matcher dptSelect = DPT_SELECT_PATTERN.matcher(html);
    assertTrue(
        "Expected #perc-dpt-filter select when perc_hidefield_filter_dropdown is unset: " + html,
        dptSelect.find());
    assertFalse(
        "Expected #perc-dpt-filter to be visible (no display:none): " + dptSelect.group(),
        dptSelect.group().contains("display:none"));
  }

  @Test
  public void shouldHideDepartmentFilterWhenHideDropdownFilterMenuIsTrueForOneOrg()
      throws Exception {
    String html = renderTemplate(Boolean.TRUE, "601", "-1");

    assertFalse(
        "Did not expect #perc-org-filter when Hide dropdown filter menu is true: " + html,
        html.contains("id=\"perc-org-filter\""));
    assertFalse(
        "Did not expect #perc-dpt-filter when Hide dropdown filter menu is true: " + html,
        html.contains("id=\"perc-dpt-filter\""));
  }

  @Test
  public void shouldOmitFilterDropdownsWhenOneOrgAndSpecificDepartmentAreSelected()
      throws Exception {
    String html = renderTemplate(Boolean.FALSE, "601", "701");

    assertFalse(
        "Did not expect #perc-org-filter when a single organization is selected: " + html,
        html.contains("id=\"perc-org-filter\""));
    assertFalse(
        "Did not expect #perc-dpt-filter when a specific department is selected: " + html,
        html.contains("id=\"perc-dpt-filter\""));
    assertTrue(
        "Expected data-show-dpt-filter=\"false\": " + html,
        html.contains("data-show-dpt-filter=\"false\""));
  }

  @Test
  public void shouldRenderOrgFilterAndHiddenDepartmentFilterWhenAllOrgsAndAllDepartments()
      throws Exception {
    String html = renderTemplate(Boolean.FALSE, "-1", "-1");

    assertTrue(
        "Expected #perc-org-filter when all organizations are selected: " + html,
        html.contains("<select name=\"perc-org-filter\" id=\"perc-org-filter\">"));
    assertTrue(
        "Expected organization option in #perc-org-filter: " + html,
        html.contains("<option value=\"Acme University\">Acme University</option>"));
    assertTrue(
        "Expected second organization option in #perc-org-filter: " + html,
        html.contains("<option value=\"Beta College\">Beta College</option>"));

    Matcher dptSelect = DPT_SELECT_PATTERN.matcher(html);
    assertTrue(
        "Expected #perc-dpt-filter select in DOM for JS to show after org selection: " + html,
        dptSelect.find());
    assertTrue(
        "Expected #perc-dpt-filter to be initially hidden when searching all orgs: "
            + dptSelect.group(),
        dptSelect.group().contains("style=\"display:none;\""));

    assertTrue(
        "Expected data-search-all-orgs=\"true\": " + html,
        html.contains("data-search-all-orgs=\"true\""));
    assertTrue(
        "Expected data-show-dpt-filter=\"true\": " + html,
        html.contains("data-show-dpt-filter=\"true\""));
  }

  @Test
  public void shouldHideAllFilterDropdownsWhenHideDropdownFilterMenuIsTrueForAllOrgs()
      throws Exception {
    String html = renderTemplate(Boolean.TRUE, "-1", "-1");

    assertFalse(
        "Did not expect #perc-org-filter when Hide dropdown filter menu is true: " + html,
        html.contains("id=\"perc-org-filter\""));
    assertFalse(
        "Did not expect #perc-dpt-filter when Hide dropdown filter menu is true: " + html,
        html.contains("id=\"perc-dpt-filter\""));
  }

  private static String renderTemplate(
      Boolean hideFilterDropdown, String organizationSearch, String departmentId) throws Exception {
    Map<String, Object> props = new HashMap<>();
    props.put("dirTitleFormat", "h2");
    props.put("directoryLayoutFormat", "table");
    props.put("perc_hidefield_textSearch", Boolean.FALSE);
    if (hideFilterDropdown != null) {
      props.put("perc_hidefield_filter_dropdown", hideFilterDropdown);
    }
    props.put("perc_hidefield_personOrganization", Boolean.FALSE);
    props.put("perc_hidefield_personDepartment", Boolean.FALSE);
    props.put("perc_hidefield_personTitle", Boolean.FALSE);
    props.put("perc_hidefield_personPhoneField", Boolean.TRUE);
    props.put("perc_hidefield_personOfficeLocation", Boolean.TRUE);
    props.put("perc_hidefield_personEmailField", Boolean.TRUE);
    props.put("perc_hidefield_personImageField", Boolean.TRUE);
    props.put("perc_display_full_dir", Boolean.FALSE);

    Map<String, String> assetProps = new HashMap<>();
    assetProps.put("sys_contentid", "500");
    assetProps.put("directoryTitle", "Campus Directory");
    assetProps.put("customPlaceholderImg_linkId", "");
    assetProps.put("organizationSearch", organizationSearch);
    assetProps.put("departmentID", departmentId);

    Map<String, Map<String, String>> orgsById = new LinkedHashMap<>();
    orgsById.put("601", Collections.singletonMap("orgName", "Acme University"));
    orgsById.put("602", Collections.singletonMap("orgName", "Beta College"));

    Map<String, Map<String, String>> deptsById = new LinkedHashMap<>();
    deptsById.put("701", Collections.singletonMap("dptName", "Engineering"));
    deptsById.put("702", Collections.singletonMap("dptName", "Admissions"));

    List<Map<String, String>> people =
        Arrays.asList(
            personProps("801", "Alice", "Adams", "601", "701", "Engineer"),
            personProps("802", "Bob", "Brown", "601", "702", "Counselor"));

    return renderWidget(props, assetProps, orgsById, deptsById, people);
  }

  private static Map<String, String> personProps(
      String contentId,
      String firstName,
      String lastName,
      String orgId,
      String deptId,
      String title) {
    Map<String, String> props = new HashMap<>();
    props.put("sys_contentid", contentId);
    props.put("personNamePrefix", "");
    props.put("personFirstName", firstName);
    props.put("personLastName", lastName);
    props.put("personOrganization", orgId);
    props.put("personDepartment", deptId);
    props.put("personTitle", title);
    props.put("personPhone", "");
    props.put("personOfficeLocation", "");
    props.put("personEmail", "");
    props.put("personImage_linkId", "");
    props.put("personPage_linkId", "");
    props.put("personCaption", "");
    return props;
  }

  private static String renderWidget(
      Map<String, Object> props,
      Map<String, String> assetProps,
      Map<String, Map<String, String>> orgsById,
      Map<String, Map<String, String>> deptsById,
      List<Map<String, String>> people)
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
    rx.put("pageutils", new PageUtilsStub(assetProps, orgsById, deptsById, people));
    rx.put("string", new StringToolStub());

    Map<String, Object> tools = new HashMap<>();
    tools.put("math", new MathToolStub());

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
    runtime.evaluate(ctx, writer, "percDirectory", velocityTemplate);
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
    private final PageStub page = new PageStub("101", "directory-page");
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
    private final AssemblyItemStub assemblyItem = new AssemblyItemStub();

    public AssemblyItemStub getAssemblyItem() {
      return assemblyItem;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return 1L;
    }
  }

  public static class PageUtilsStub {
    private final Map<String, String> assetProps;
    private final Map<String, Map<String, String>> orgsById;
    private final Map<String, Map<String, String>> deptsById;
    private final List<Map<String, String>> people;

    public PageUtilsStub(
        Map<String, String> assetProps,
        Map<String, Map<String, String>> orgsById,
        Map<String, Map<String, String>> deptsById,
        List<Map<String, String>> people) {
      this.assetProps = assetProps;
      this.orgsById = orgsById;
      this.deptsById = deptsById;
      this.people = people;
    }

    @SuppressWarnings("unchecked")
    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object paramsObj) {
      if (finderName == null) {
        return Collections.singletonList(new AssetItemStub(assetProps));
      }

      Map<String, Object> params = (Map<String, Object>) paramsObj;
      String query = String.valueOf(params.get("query"));

      if (query.contains("rx:percPerson")) {
        Object orgSearchId = params.get("orgSearchId");
        Object deptSearchId = params.get("deptSearchId");
        List<Object> results = new ArrayList<>();
        for (Map<String, String> person : people) {
          boolean matchesOrg =
              orgSearchId == null
                  || String.valueOf(orgSearchId).equals(person.get("personOrganization"));
          boolean matchesDept =
              deptSearchId == null
                  || String.valueOf(deptSearchId).equals(person.get("personDepartment"));
          if (matchesOrg && matchesDept) {
            results.add(new AssetItemStub(person));
          }
        }
        return results;
      }

      if (query.contains("rx:percOrganization")) {
        Object orgId =
            params.containsKey("orgSearchId") ? params.get("orgSearchId") : params.get("orgId");
        if (orgId != null) {
          Map<String, String> org = orgsById.get(String.valueOf(orgId));
          return org == null
              ? Collections.emptyList()
              : Collections.singletonList(new AssetItemStub(org));
        }
        List<Object> allOrgs = new ArrayList<>();
        for (Map<String, String> org : orgsById.values()) {
          allOrgs.add(new AssetItemStub(org));
        }
        return allOrgs;
      }

      if (query.contains("rx:percDepartment")) {
        Object deptId = params.get("deptId");
        Map<String, String> dept = deptId == null ? null : deptsById.get(String.valueOf(deptId));
        return dept == null
            ? Collections.emptyList()
            : Collections.singletonList(new AssetItemStub(dept));
      }

      return Collections.emptyList();
    }

    public String renderManagedItemPath(Object linkContext, String linkId) {
      return "";
    }

    public String renderManagedItemPath(Object linkContext, String linkId, Object pubServerId) {
      return "";
    }

    public String getManagedLinkDependentId(String linkId) {
      return "";
    }

    public Map<String, String> findItemFieldValues(
        String contentType, String fields, String contentId) {
      return Collections.emptyMap();
    }
  }

  public static class StringToolStub {
    public Map<String, Object> stringToMap(String input) {
      return new LinkedHashMap<>();
    }
  }

  public static class MathToolStub {
    public Integer toInteger(Object value) {
      if (value == null) {
        return null;
      }
      String text = String.valueOf(value).trim();
      if (text.isEmpty()) {
        return null;
      }
      return Integer.valueOf(text);
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

    public boolean hasProperty(String name) {
      return props.containsKey(name);
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
