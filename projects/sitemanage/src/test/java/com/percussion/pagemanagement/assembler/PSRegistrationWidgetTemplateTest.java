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

import com.percussion.pagemanagement.data.PSRenderLinkContext;
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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #393: verifies {@code percRegistration.xml} keeps {@code
 * #perc-password-reset} hidden on published pages until {@code PercRegistrationView.js} validates
 * {@code resetkey}, while preserving preview-mode visibility and {@code #perc-password-request}
 * behavior.
 */
public class PSRegistrationWidgetTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.registration/sys__UserDependency--rxconfig/Widgets/percRegistration.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">(?:\\s|<!--.*?-->)*<!\\[CDATA\\[(.*?)\\]\\]>(?:\\s|<!--.*?-->)*</Code>",
          Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">(?:\\s|<!--.*?-->)*<!\\[CDATA\\[(.*?)\\]\\]>(?:\\s|<!--.*?-->)*</Content>",
          Pattern.DOTALL);

  private static final Pattern SCRIPT_PATTERN =
      Pattern.compile("<script>(.*?)</script>", Pattern.DOTALL);

  private static final String STUB_MACROS =
      "#macro(createEmptyWidgetContent $class $text)"
          + "<div class=\"$!{class}\">$!{text}</div>#end";

  @Test
  public void shouldKeepPasswordResetFormHiddenOnPublishedPage() throws Exception {
    String html = renderTemplate("password_reset", false, false, PSRenderLinkContext.Mode.PUBLISH);

    assertTrue(
        "Expected hidden #perc-password-reset form in HTML: " + html,
        html.contains("<form name=\"perc-password-reset\" id=\"perc-password-reset\"")
            && html.contains("style = \"display:none\""));

    String script = extractScript(html);
    assertFalse(
        "Published page script must not reference #perc-password-reset: " + script,
        script.contains("#perc-password-reset"));
    assertTrue(
        "Published page script should show #perc-password-request: " + script,
        script.contains("jQuery(\"#perc-password-request\").show();"));

    ScriptEngine engine = runDomScript(script);
    assertFalse(
        "#perc-password-reset must remain hidden on published page",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-reset'].shown")));
    assertTrue(
        "#perc-password-request.show() should be invoked",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-request'].shown")));
  }

  @Test
  public void shouldShowDisabledPasswordResetFormInPreviewMode() throws Exception {
    String html = renderTemplate("password_reset", true, false, PSRenderLinkContext.Mode.PREVIEW);
    String script = extractScript(html);

    ScriptEngine engine = runDomScript(script);
    assertTrue(
        "#perc-password-reset should be shown in preview mode",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-reset'].shown")));
    assertTrue(
        "#perc-password-reset submit should be prevented in preview mode",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-reset'].submitPrevented")));
    assertTrue(
        "#perc-password-reset submit button should be disabled in preview mode",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-reset'].submitDisabled")));
  }

  @Test
  public void shouldShowPasswordRequestFormOnPublishedPage() throws Exception {
    String html =
        renderTemplate("password_request", false, false, PSRenderLinkContext.Mode.PUBLISH);

    assertTrue(
        "Expected #perc-password-request form in HTML: " + html,
        html.contains("<form name=\"perc-password-request\" id=\"perc-password-request\""));

    String script = extractScript(html);
    ScriptEngine engine = runDomScript(script);
    assertTrue(
        "#perc-password-request should be shown on published page",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-request'].shown")));
    assertFalse(
        "#perc-password-reset should not be shown on password_request page",
        Boolean.TRUE.equals(engine.eval("__elements['#perc-password-reset'].shown")));
  }

  @Test
  public void shouldNotEmitScriptInEditMode() throws Exception {
    String html = renderTemplate("password_reset", false, true, PSRenderLinkContext.Mode.PREVIEW);

    assertFalse("Edit mode must not emit <script>: " + html, html.contains("<script>"));
  }

  private static String extractScript(String html) {
    Matcher matcher = SCRIPT_PATTERN.matcher(html);
    assertTrue("Expected <script> block in HTML: " + html, matcher.find());
    return matcher.group(1);
  }

  private static ScriptEngine runDomScript(String script) throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    String harness =
        "var __listeners = {};\n"
            + "var window = {\n"
            + "  addEventListener: function(ev, fn) {\n"
            + "    if (!__listeners[ev]) { __listeners[ev] = []; }\n"
            + "    __listeners[ev].push(fn);\n"
            + "  }\n"
            + "};\n"
            + "function __fireDomContentLoaded() {\n"
            + "  var fns = __listeners['DOMContentLoaded'] || [];\n"
            + "  for (var i = 0; i < fns.length; i++) { fns[i](); }\n"
            + "}\n"
            + "var __elements = {\n"
            + "  '#perc-password-reset': { shown: false, submitPrevented: false, submitDisabled: false },\n"
            + "  '#perc-password-request': { shown: false, submitPrevented: false, submitDisabled: false },\n"
            + "  '#perc-registration-form': { shown: false, submitPrevented: false, submitDisabled: false },\n"
            + "  '#perc-registration-cform': { shown: false, submitPrevented: false, submitDisabled: false }\n"
            + "};\n"
            + "function jQuery(selector) {\n"
            + "  var keys = [];\n"
            + "  var parts = String(selector).split(',');\n"
            + "  for (var i = 0; i < parts.length; i++) {\n"
            + "    var k = parts[i].replace(/^\\s+|\\s+$/g, '');\n"
            + "    if (__elements[k]) { keys.push(k); }\n"
            + "  }\n"
            + "  return {\n"
            + "    show: function() {\n"
            + "      for (var i = 0; i < keys.length; i++) { __elements[keys[i]].shown = true; }\n"
            + "      return this;\n"
            + "    },\n"
            + "    on: function(ev, fn) {\n"
            + "      if (ev === 'submit') {\n"
            + "        for (var i = 0; i < keys.length; i++) {\n"
            + "          if (fn() === false) { __elements[keys[i]].submitPrevented = true; }\n"
            + "        }\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sub) {\n"
            + "      return {\n"
            + "        prop: function(name, val) {\n"
            + "          if (name === 'disabled' && val === true) {\n"
            + "            for (var i = 0; i < keys.length; i++) {\n"
            + "              __elements[keys[i]].submitDisabled = true;\n"
            + "            }\n"
            + "          }\n"
            + "          return this;\n"
            + "        }\n"
            + "      };\n"
            + "    }\n"
            + "  };\n"
            + "}\n";
    engine.eval(harness);
    engine.eval(script);
    engine.eval("__fireDomContentLoaded();");
    return engine;
  }

  private static String renderTemplate(
      String registrationMode,
      boolean previewMode,
      boolean editMode,
      PSRenderLinkContext.Mode linkMode)
      throws Exception {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.newDocumentBuilder().parse(widgetFile);

    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);

    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(xml);
    assertTrue("JEXL <Code> section not found in " + widgetFile, jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, velocityMatcher.find());
    String velocityTemplate = STUB_MACROS + velocityMatcher.group(1);

    Map<String, Object> props = new HashMap<>();
    props.put("perc-label-layout", "align_above");
    props.put("perc-registration-mode", registrationMode);
    props.put("perc-email-verification-required", "true");

    Map<String, Object> cssProps = new HashMap<>();

    PercHelper percHelper = new PercHelper(props, cssProps, previewMode, editMode, linkMode);

    Map<String, Object> sys = new HashMap<>();
    sys.put("assemblyItem", new AssemblyItemStub());

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", new PageUtilsStub());

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
    runtime.evaluate(ctx, writer, "percRegistration", velocityTemplate);
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
    private final PageStub page = new PageStub("16777215-101-1001");
    private final LinkContextStub linkContext;
    private final boolean previewMode;
    private final boolean editMode;
    private List<Object> widgetContents = Collections.emptyList();

    public PercHelper(
        Map<String, Object> properties,
        Map<String, Object> cssProperties,
        boolean previewMode,
        boolean editMode,
        PSRenderLinkContext.Mode linkMode) {
      this.widget = new WidgetStub(properties, cssProperties);
      this.previewMode = previewMode;
      this.editMode = editMode;
      this.linkContext = new LinkContextStub(linkMode);
    }

    public PageStub getPage() {
      return page;
    }

    public WidgetStub getWidget() {
      return widget;
    }

    public LinkContextStub getLinkContext() {
      return linkContext;
    }

    public boolean isPreviewMode() {
      return previewMode;
    }

    public boolean isEditMode() {
      return editMode;
    }

    public List<Object> getWidgetContents() {
      return widgetContents;
    }

    public void setWidgetContents(List<Object> widgetContents) {
      this.widgetContents = widgetContents;
    }
  }

  public static class LinkContextStub {
    private final PSRenderLinkContext.Mode mode;

    public LinkContextStub(PSRenderLinkContext.Mode mode) {
      this.mode = mode;
    }

    public PSRenderLinkContext.Mode getMode() {
      return mode;
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
    private final String id = "100";
    private final Map<String, Object> properties;
    private final Map<String, Object> cssProperties;

    public WidgetItemStub(Map<String, Object> properties, Map<String, Object> cssProperties) {
      this.properties = properties;
      this.cssProperties = cssProperties;
    }

    public String getId() {
      return id;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public Map<String, Object> getCssProperties() {
      return cssProperties;
    }
  }

  public static class AssemblyItemStub {
    public Long getPubServerId() {
      return 1L;
    }
  }

  public static class PageUtilsStub {
    public String getItemPath(String pageId) {
      return "/Sites/TestSite/reset-password";
    }

    public String getSiteRegistrationConfirmationPage(String siteName) {
      return "/confirm.html";
    }

    public String getSiteResetPage(String siteName) {
      return "/reset-password.html";
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params) {
      return Collections.singletonList(new AssetItemStub());
    }

    public List<Object> widgetContents(
        Object item, Object widget, Object finderName, Object params, boolean isNav) {
      return Collections.singletonList(new AssetItemStub());
    }

    public String html(Object assetItem, String fieldName) {
      if ("email_label".equals(fieldName)) {
        return "Email";
      }
      if ("password_label".equals(fieldName)) {
        return "Password";
      }
      if ("confirm_password_label".equals(fieldName)) {
        return "Confirm Password";
      }
      if ("submit_label".equals(fieldName)) {
        return "Submit";
      }
      return "";
    }

    public String navLink(Object linkContext, Object nav) {
      return "/index.html";
    }

    public String getDeliveryServer(Object pubServerId) {
      return "http://localhost:9980";
    }
  }

  public static class AssetItemStub {
    private final Object node = new Object();

    public Object getNode() {
      return node;
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
