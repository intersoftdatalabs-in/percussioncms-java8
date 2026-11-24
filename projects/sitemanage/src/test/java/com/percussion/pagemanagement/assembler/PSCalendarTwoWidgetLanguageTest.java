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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.pagemanagement.assembler.impl.PSResourceInstanceHelper;
import com.percussion.pagemanagement.dao.impl.PSResourceDefinitionData;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService;
import com.percussion.pagemanagement.service.impl.PSRenderLinkService;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #400: verifies Calendar 2.0 loads {@code perc_common_ui.js} (which
 * bundles {@code moment-with-locales.js}) before {@code fullcalendar.min.js} and {@code
 * lang-all.js}, does not emit the English-only {@code moment.min.js}, and localizes both button
 * text and month/day names.
 */
public class PSCalendarTwoWidgetLanguageTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.calendar/sys__UserDependency--rxconfig/Widgets/percCalendarTwo.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";
  private static final String SYSTEM_RESOURCES_REL_PATH =
      "projects/sitemanage/src/main/resources/Rhythmyx/rxconfig/Resources/percSystem.xml";
  private static final String PACKAGES_REL_PATH = "system/Packages";
  private static final String MOMENT_WITH_LOCALES_REL_PATH =
      "WebUI/war/jslib/profiles/3x/libraries/momentjs/moment-with-locales.js";
  private static final String FULLCALENDAR_MIN_REL_PATH =
      "system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.min.js";
  private static final String LANG_ALL_REL_PATH =
      "system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/lang-all.js";

  private static final Pattern JEXL_CODE_PATTERN =
      Pattern.compile(
          "<Code\\s+type=\"jexl\">(?:\\s|<!--.*?-->)*<!\\[CDATA\\[(.*?)\\]\\]>(?:\\s|<!--.*?-->)*</Code>",
          Pattern.DOTALL);

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">(?:\\s|<!--.*?-->)*<!\\[CDATA\\[(.*?)\\]\\]>(?:\\s|<!--.*?-->)*</Content>",
          Pattern.DOTALL);

  @Test
  public void shouldOrderPercCommonUiBeforeFullCalendarAndLangAllForCalendarTwo() throws Exception {
    PSRenderLinkService service = createRenderLinkServiceWithRealResources();
    PSRenderLinkContext context = createPublishContext();

    List<PSRenderLink> singleWidgetLinks =
        service.renderJavascriptLinks(context, Collections.singleton("percCalendarTwo"));
    assertCalendarScriptOrder(singleWidgetLinks);

    List<PSRenderLink> multiWidgetLinks =
        service.renderJavascriptLinks(
            context, new HashSet<>(Arrays.asList("percCalendarTwo", "percBlogPost")));
    assertCalendarScriptOrder(multiWidgetLinks);
  }

  @Test
  public void shouldRenderFrenchLangWithoutMomentMinJsInPublishAndPreviewModes() throws Exception {
    for (boolean preview : new boolean[] {false, true}) {
      String html = renderCalendarTwoTemplate("fr", preview);
      assertTrue("Expected lang: 'fr' in rendered HTML: " + html, html.contains("lang: 'fr'"));
      assertTrue("Expected firstDay: 1 in rendered HTML: " + html, html.contains("firstDay: 1"));
      assertFalse(
          "Must not emit English-only moment.min.js: " + html, html.contains("moment.min.js"));
    }
  }

  @Test
  public void shouldLocalizeFullCalendarButtonsAndMomentDatesWhenLoadedInCorrectOrder()
      throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    String jqueryStub =
        "var window = this, self = this;\n"
            + "var jQuery = function() { return { length: 0, each: function() { return this; } }; };\n"
            + "jQuery.fn = {};\n"
            + "jQuery.extend = function() {\n"
            + "  var deep = false, target = arguments[0], i = 1;\n"
            + "  if (typeof target === 'boolean') { deep = target; target = arguments[1]; i = 2; }\n"
            + "  target = target || {};\n"
            + "  for (; i < arguments.length; i++) {\n"
            + "    var src = arguments[i];\n"
            + "    if (src) {\n"
            + "      for (var k in src) {\n"
            + "        if (Object.prototype.hasOwnProperty.call(src, k)) {\n"
            + "          if (deep && src[k] && typeof src[k] === 'object' && !Array.isArray(src[k])) {\n"
            + "            target[k] = jQuery.extend(true, target[k] || {}, src[k]);\n"
            + "          } else {\n"
            + "            target[k] = src[k];\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "  return target;\n"
            + "};\n"
            + "jQuery.each = function(obj, fn) {\n"
            + "  if (Array.isArray(obj)) {\n"
            + "    for (var i = 0; i < obj.length; i++) { fn.call(obj[i], i, obj[i]); }\n"
            + "  } else if (obj) {\n"
            + "    for (var k in obj) {\n"
            + "      if (Object.prototype.hasOwnProperty.call(obj, k)) { fn.call(obj[k], k, obj[k]); }\n"
            + "    }\n"
            + "  }\n"
            + "};\n"
            + "jQuery.isArray = Array.isArray;\n"
            + "jQuery.isFunction = function(f) { return typeof f === 'function'; };\n"
            + "jQuery.isPlainObject = function(o) { return o && typeof o === 'object' && !Array.isArray(o); };\n"
            + "jQuery.proxy = function(fn, ctx) { return function() { return fn.apply(ctx, arguments); }; };\n"
            + "jQuery.datepicker = { regional: {}, setDefaults: function() {} };\n"
            + "var $ = jQuery;\n";

    engine.eval(jqueryStub);
    engine.eval(readRepoFile(MOMENT_WITH_LOCALES_REL_PATH));
    engine.eval(readRepoFile(FULLCALENDAR_MIN_REL_PATH));
    engine.eval(readRepoFile(LANG_ALL_REL_PATH));

    assertEquals("Mois", engine.eval("jQuery.fullCalendar.langs.fr.buttonText.month"));
    assertEquals(
        "novembre 2026",
        engine.eval("jQuery.fullCalendar.moment('2026-11-18').locale('fr').format('MMMM YYYY')"));
    assertEquals(
        "mer.", engine.eval("jQuery.fullCalendar.moment('2026-11-18').locale('fr').format('ddd')"));
  }

  private static void assertCalendarScriptOrder(List<PSRenderLink> links) {
    List<String> urls = new ArrayList<>();
    for (PSRenderLink link : links) {
      urls.add(link.getUrl());
    }
    int commonUiIdx = urls.indexOf("/web_resources/cm/common/js/perc_common_ui.js");
    int fullCalendarIdx = urls.indexOf("/web_resources/widgets/calendarTwo/js/fullcalendar.min.js");
    int langAllIdx = urls.indexOf("/web_resources/widgets/calendarTwo/js/lang-all.js");
    int gcalIdx = urls.indexOf("/web_resources/widgets/calendarTwo/js/gcal.js");

    assertTrue("Expected perc_common_ui.js in " + urls, commonUiIdx >= 0);
    assertEquals(
        "Expected perc_common_ui.js exactly once in " + urls,
        commonUiIdx,
        urls.lastIndexOf("/web_resources/cm/common/js/perc_common_ui.js"));
    assertTrue("Expected fullcalendar.min.js in " + urls, fullCalendarIdx >= 0);
    assertTrue("Expected lang-all.js in " + urls, langAllIdx >= 0);
    assertTrue("Expected gcal.js in " + urls, gcalIdx >= 0);
    assertTrue(
        "perc_common_ui.js must load before fullcalendar.min.js: " + urls,
        commonUiIdx < fullCalendarIdx);
    assertTrue(
        "fullcalendar.min.js must load before lang-all.js: " + urls, fullCalendarIdx < langAllIdx);
    assertTrue("fullcalendar.min.js must load before gcal.js: " + urls, fullCalendarIdx < gcalIdx);
  }

  private static PSRenderLinkService createRenderLinkServiceWithRealResources() throws Exception {
    final PSResourceDefinitionData data = new PSResourceDefinitionData();
    JAXBContext jaxb = JAXBContext.newInstance(PSResourceDefinitionGroup.class);
    Unmarshaller unmarshaller = jaxb.createUnmarshaller();

    loadResourceGroup(data, unmarshaller, resolveRepoFile(SYSTEM_RESOURCES_REL_PATH));
    Path packagesDir = resolveRepoFile(PACKAGES_REL_PATH).toPath();
    try (Stream<Path> walk = Files.walk(packagesDir)) {
      walk.filter(
              p ->
                  Files.isRegularFile(p)
                      && p.toString().contains("/sys__UserDependency--rxconfig/Resources/")
                      && p.toString().endsWith(".xml"))
          .sorted()
          .forEach(
              p -> {
                try {
                  loadResourceGroup(data, unmarshaller, p.toFile());
                } catch (Exception e) {
                  throw new RuntimeException("Failed to load " + p, e);
                }
              });
    }

    IPSResourceDefinitionService resourceDefService =
        new IPSResourceDefinitionService() {
          @Override
          public List<PSResourceDefinition> findAllResources() {
            return new ArrayList<>(data.getResourceDefinitions().values());
          }

          @Override
          public PSResourceDefinition findResource(String uniqueId) {
            for (PSResourceDefinition rd : data.getResourceDefinitions().values()) {
              if (uniqueId.equals(rd.getUniqueId())) {
                return rd;
              }
            }
            return null;
          }

          @Override
          public List<PSResourceDefinitionGroup> findAll() {
            return new ArrayList<>(data.getResourceDefinitionGroups().values());
          }

          @Override
          public PSResourceDefinitionGroup find(String id) {
            return data.getResourceDefinitionGroups().get(id);
          }

          @Override
          public PSAssetResource findDefaultAssetResourceForType(String contentType) {
            return null;
          }

          @Override
          public List<PSAssetResource> findAssetResourcesForType(String contentType) {
            return Collections.emptyList();
          }

          @Override
          public List<PSAssetResource> findAssetResourcesForLegacyTemplate(String template) {
            return Collections.emptyList();
          }

          @Override
          public String createUniqueId(String groupId, String id) {
            return groupId + NAMESPACE_SEPARATOR + id;
          }
        };

    Constructor<PSResourceInstanceHelper> ctor =
        PSResourceInstanceHelper.class.getDeclaredConstructor(
            IPSAssetService.class,
            IPSSiteDataService.class,
            IPSPageService.class,
            IPSSiteTemplateService.class,
            IPSAssemblyService.class);
    ctor.setAccessible(true);
    PSResourceInstanceHelper instanceHelper = ctor.newInstance(null, null, null, null, null);

    return new PSRenderLinkService(
        null, null, null, null, resourceDefService, instanceHelper, null, null);
  }

  private static void loadResourceGroup(
      PSResourceDefinitionData data, Unmarshaller unmarshaller, File file)
      throws Exception, PSDataServiceException {
    PSResourceDefinitionGroup group = (PSResourceDefinitionGroup) unmarshaller.unmarshal(file);
    String name = file.getName();
    group.setId(name.substring(0, name.length() - ".xml".length()));
    data.add(group);
  }

  private static PSRenderLinkContext createPublishContext() {
    final PSSiteSummary site = new PSSiteSummary();
    return new PSRenderLinkContext() {
      @Override
      public Mode getMode() {
        return Mode.PUBLISH;
      }

      @Override
      public PSSiteSummary getSite() {
        return site;
      }

      @Override
      public boolean isDeliveryContext() {
        return true;
      }
    };
  }

  private static String renderCalendarTwoTemplate(String language, boolean previewMode)
      throws Exception {
    String widgetXml = readRepoFile(WIDGET_REL_PATH);
    Matcher jexlMatcher = JEXL_CODE_PATTERN.matcher(widgetXml);
    assertTrue("Could not find JEXL <Code> block in percCalendarTwo.xml", jexlMatcher.find());
    String jexlCode = jexlMatcher.group(1);

    Matcher velocityMatcher = VELOCITY_CONTENT_PATTERN.matcher(widgetXml);
    assertTrue(
        "Could not find Velocity <Content> block in percCalendarTwo.xml", velocityMatcher.find());
    String velocityTemplate = velocityMatcher.group(1);

    Map<String, Object> props = new HashMap<>();
    props.put("perc_layout_type", "calendar");
    props.put("perc_max_results", "5");
    props.put("enableCalenderIcon", "false");
    props.put("showEventDate", "true");
    props.put("showEventTime", "true");
    props.put("perc_calendar_view", "full");
    props.put("perc_calendar_type", "month");
    props.put("perc_calendar_detail", "basic");
    props.put("perc_show_page_summary", "false");
    props.put("perc_calendar_display_language", language);
    props.put("perc_start_day", "Day_1");
    props.put("perc_show_perc_events_button", "N");
    props.put("perc_event_button_label", "Other");
    props.put("eventDateFormat", "dddd, MMMM Do YYYY");
    props.put("eventTimeFormat", "h:mmA");
    props.put("perc_event_title_tag_type", "p");
    props.put("perc_enable_view_toggle_buttons", "false");
    props.put("perc_time_format", "ampm");

    StubCalendarAssetItem assetItem = new StubCalendarAssetItem("QA Calendar", "qa-cal");
    StubPerc perc = new StubPerc(props, previewMode);
    StubPageUtils pageUtils = new StubPageUtils(Collections.singletonList(assetItem));

    Map<String, Object> rx = new HashMap<>();
    rx.put("pageutils", pageUtils);
    Map<String, Object> sys = new HashMap<>();
    sys.put("assemblyItem", new Object());
    Map<String, Object> tools = new HashMap<>();
    tools.put("esc", new StubEsc());

    PSJexlEvaluator eval = new PSJexlEvaluator();
    eval.bind("$perc", perc);
    eval.bind("$rx", rx);
    eval.bind("$sys", sys);
    eval.bind("$tools", tools);

    IPSScript script = PSJexlEvaluator.createScript(jexlCode);
    eval.evaluate(script);

    RuntimeInstance ri = new RuntimeInstance();
    Properties velProps = new Properties();
    try (FileInputStream in = new FileInputStream(resolveRepoFile(VELOCITY_PROPS_REL_PATH))) {
      velProps.load(in);
    }
    velProps.remove("velocimacro.library");
    ri.init(velProps);

    VelocityContext ctx = new VelocityContext();
    for (Map.Entry<String, Object> entry : eval.getVars().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("$")) {
        key = key.substring(1);
      }
      ctx.put(key, entry.getValue());
    }
    ctx.put("perc", perc);
    ctx.put("rx", rx);
    ctx.put("sys", sys);
    ctx.put("tools", tools);

    StringWriter out = new StringWriter();
    ri.evaluate(ctx, out, "percCalendarTwo.xml", velocityTemplate);
    return out.toString();
  }

  private static File resolveRepoFile(String relativePath) {
    Path candidate = Paths.get(relativePath);
    if (Files.exists(candidate)) {
      return candidate.toFile();
    }
    Path fromSubmodule = Paths.get("..", "..", relativePath);
    if (Files.exists(fromSubmodule)) {
      return fromSubmodule.normalize().toFile();
    }
    throw new IllegalStateException("Cannot locate repo file: " + relativePath);
  }

  private static String readRepoFile(String relativePath) throws IOException {
    return new String(
        Files.readAllBytes(resolveRepoFile(relativePath).toPath()), StandardCharsets.UTF_8);
  }

  public static class StubWidgetItem {
    private final Map<String, Object> properties;
    private final Map<String, String> cssProperties = new HashMap<>();

    public StubWidgetItem(Map<String, Object> properties) {
      this.properties = properties;
    }

    public String getId() {
      return "101";
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public Map<String, String> getCssProperties() {
      return cssProperties;
    }
  }

  public static class StubWidget {
    private final StubWidgetItem item;

    public StubWidget(Map<String, Object> properties) {
      this.item = new StubWidgetItem(properties);
    }

    public StubWidgetItem getItem() {
      return item;
    }
  }

  public static class StubPerc {
    private final StubWidget widget;
    private final boolean previewMode;
    private List<?> widgetContents = Collections.emptyList();

    public StubPerc(Map<String, Object> properties, boolean previewMode) {
      this.widget = new StubWidget(properties);
      this.previewMode = previewMode;
    }

    public StubWidget getWidget() {
      return widget;
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return previewMode;
    }

    public void setWidgetContents(List<?> widgetContents) {
      this.widgetContents = widgetContents;
    }

    public List<?> getWidgetContents() {
      return widgetContents;
    }
  }

  public static class StubProperty {
    private final String value;

    public StubProperty(String value) {
      this.value = value;
    }

    public String getString() {
      return value;
    }
  }

  public static class StubNode {
    private final Map<String, String> props = new HashMap<>();

    public StubNode(String title, String sysTitle) {
      props.put("calendar_title", title);
      props.put("sys_title", sysTitle);
      props.put("full_calendar_page", "");
      props.put("calendar_start_date", "");
      props.put("rx:googleSetupData", "{\"config\":[]}");
      props.put("rx:sys_contentid", "42");
    }

    public StubProperty getProperty(String name) {
      return new StubProperty(props.getOrDefault(name, ""));
    }
  }

  public static class StubCalendarAssetItem {
    private final StubNode node;

    public StubCalendarAssetItem(String title, String sysTitle) {
      this.node = new StubNode(title, sysTitle);
    }

    public String getId() {
      return "42";
    }

    public StubNode getNode() {
      return node;
    }
  }

  public static class StubPageUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final List<StubCalendarAssetItem> items;

    public StubPageUtils(List<StubCalendarAssetItem> items) {
      this.items = items;
    }

    public List<StubCalendarAssetItem> widgetContents(
        Object assemblyItem, Object widget, Object a, Object b) {
      return items;
    }

    public String getPagesForCalendar(String calName) {
      return "[]";
    }

    public JsonNode createJsonObject(String json) throws IOException {
      return MAPPER.readTree(json);
    }

    public String getDeliveryServer() {
      return "http://localhost:9980";
    }
  }

  public static class StubEsc {
    public String html(String value) {
      return value;
    }
  }
}
