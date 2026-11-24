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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.webui.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Before;
import org.junit.Test;

/**
 * Nashorn-based regression tests for {@code PercArchiveListView.js} covering issue #404 (string
 * {@code numberEntries} ignored in the {@code perc-list-flat} "List by months" layout).
 */
public class PercArchiveListViewJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveDeliveryJs("views/PercArchiveListView.js"));
  }

  private Path resolveDeliveryJs(String relativePath) {
    String basedir = System.getProperty("basedir", ".");
    Path fromWebUi =
        Paths.get(basedir).resolve("..").resolve("delivery/common/js").resolve(relativePath);
    if (Files.exists(fromWebUi)) {
      return fromWebUi.normalize();
    }
    Path fromRoot = Paths.get(basedir).resolve("delivery/common/js").resolve(relativePath);
    if (Files.exists(fromRoot)) {
      return fromRoot.normalize();
    }
    throw new IllegalStateException(
        "Unable to locate delivery JS file: "
            + relativePath
            + " from basedir "
            + new File(basedir).getAbsolutePath());
  }

  private void evalScript(Path path) throws Exception {
    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    engine.eval(content);
  }

  private void installDomAndJQueryStub() throws ScriptException {
    String stub =
        "var window = {\n"
            + "  location: { protocol: 'https:', host: 'example.com', pathname: '/index.html' }\n"
            + "};\n"
            + "var console = { debug: function() {}, error: function() {} };\n"
            + "function URL(href, base) {\n"
            + "  var s = String(href);\n"
            + "  var colon = s.indexOf(':');\n"
            + "  if (colon > 0 && s.indexOf('/') !== 0) {\n"
            + "    this.protocol = s.substring(0, colon + 1).toLowerCase();\n"
            + "  } else {\n"
            + "    this.protocol = 'https:';\n"
            + "  }\n"
            + "  this.href = s;\n"
            + "}\n"
            + "function makeEl(tag) {\n"
            + "  return {\n"
            + "    _tag: tag || 'div',\n"
            + "    _classes: [],\n"
            + "    _attrs: {},\n"
            + "    _text: '',\n"
            + "    _children: [],\n"
            + "    _namedChildren: {},\n"
            + "    addClass: function(cls) {\n"
            + "      var parts = String(cls).split(/\\s+/);\n"
            + "      for (var i = 0; i < parts.length; i++) {\n"
            + "        if (parts[i] && this._classes.indexOf(parts[i]) === -1) {\n"
            + "          this._classes.push(parts[i]);\n"
            + "        }\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    hasClass: function(cls) {\n"
            + "      return this._classes.indexOf(cls) !== -1;\n"
            + "    },\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = String(v);\n"
            + "      return this;\n"
            + "    },\n"
            + "    text: function(val) {\n"
            + "      if (arguments.length === 0) { return this._text; }\n"
            + "      this._text = String(val);\n"
            + "      return this;\n"
            + "    },\n"
            + "    html: function(val) {\n"
            + "      if (arguments.length === 0) { return ''; }\n"
            + "      this._children = [];\n"
            + "      return this;\n"
            + "    },\n"
            + "    append: function(child) {\n"
            + "      if (child) { this._children.push(child); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) {\n"
            + "      return this._namedChildren[sel] || makeEl('div');\n"
            + "    }\n"
            + "  };\n"
            + "}\n"
            + "var __container = makeEl('div');\n"
            + "var __widget = makeEl('div');\n"
            + "__widget._namedChildren['.perc-archive-list-container'] = __container;\n"
            + "var __capturedQuery = null;\n"
            + "var __archivePayload = null;\n"
            + "var jQuery = function(arg) {\n"
            + "  if (typeof arg === 'function') { return; }\n"
            + "  if (arg === '.perc-archive-list') {\n"
            + "    return {\n"
            + "      each: function(fn) { fn.call(__widget); }\n"
            + "    };\n"
            + "  }\n"
            + "  if (typeof arg === 'string') {\n"
            + "    var el = makeEl('el');\n"
            + "    if (arg.indexOf('perc-archive-month-wrapper') !== -1) {\n"
            + "      el.addClass('perc-archive-month-wrapper');\n"
            + "    }\n"
            + "    if (arg.indexOf('#') !== -1) {\n"
            + "      el.attr('href', '#');\n"
            + "    }\n"
            + "    return el;\n"
            + "  }\n"
            + "  return arg;\n"
            + "};\n"
            + "jQuery.PercServiceUtils = {\n"
            + "  toJSON: function(obj) {\n"
            + "    return typeof obj === 'string' ? JSON.parse(obj) : obj;\n"
            + "  }\n"
            + "};\n"
            + "jQuery.PercArchiveListService = {\n"
            + "  getArchiveEntries: function(queryString, callback) {\n"
            + "    __capturedQuery = JSON.parse(JSON.stringify(queryString));\n"
            + "    callback(true, __archivePayload);\n"
            + "  }\n"
            + "};\n";
    engine.eval(stub);
  }

  private void runWidget(String dataQueryJson, String archivePayloadJson) throws ScriptException {
    engine.put("__queryJson", dataQueryJson);
    engine.put("__payloadJson", archivePayloadJson);
    engine.eval(
        "__widget.attr('data-query', __queryJson);\n"
            + "__archivePayload = JSON.parse(__payloadJson);\n"
            + "__capturedQuery = null;\n"
            + "jQuery.PercArchiveListView.updateArchiveList();\n");
  }

  private String defaultTwoYearThreeMonthPayload() {
    return "{"
        + "\"years\":["
        + "  {\"year\":2026,\"yearCount\":3,\"months\":["
        + "    {\"month\":\"March\",\"count\":2},"
        + "    {\"month\":\"February\",\"count\":0},"
        + "    {\"month\":\"January\",\"count\":1}"
        + "  ]},"
        + "  {\"year\":2025,\"yearCount\":4,\"months\":["
        + "    {\"month\":\"December\",\"count\":4},"
        + "    {\"month\":\"November\",\"count\":0}"
        + "  ]}"
        + "]}";
  }

  @Test
  public void testFlatLayoutLimitsMonthsWhenNumberEntriesIsString() throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"2\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    runWidget(query, defaultTwoYearThreeMonthPayload());

    assertEquals(2, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
    assertEquals(
        "March 2026 (2)", engine.eval("__container._children[0]._children[0]._children[0].text()"));
    assertEquals(
        "January 2026 (1)",
        engine.eval("__container._children[0]._children[1]._children[0].text()"));
  }

  @Test
  public void testFlatLayoutStopsAcrossYearBoundaryAndSkipsZeroCountMonths()
      throws ScriptException {
    String payload =
        "{"
            + "\"years\":["
            + "  {\"year\":2026,\"yearCount\":2,\"months\":["
            + "    {\"month\":\"February\",\"count\":0},"
            + "    {\"month\":\"January\",\"count\":2}"
            + "  ]},"
            + "  {\"year\":2025,\"yearCount\":5,\"months\":["
            + "    {\"month\":\"December\",\"count\":3},"
            + "    {\"month\":\"November\",\"count\":2}"
            + "  ]}"
            + "]}";
    String query =
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"2\",\"archive_page_result\":\"/archives.html\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    runWidget(query, payload);

    assertEquals(2, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
    assertEquals(
        "January 2026 (2)",
        engine.eval("__container._children[0]._children[0]._children[0].text()"));
    assertEquals(
        "December 2025 (3)",
        engine.eval("__container._children[0]._children[1]._children[0].text()"));
    String firstHref =
        String.valueOf(
            engine.eval("__container._children[0]._children[0]._children[0].attr('href')"));
    assertTrue(firstHref.startsWith("https://example.com/archives.html?filter="));
  }

  @Test
  public void testFlatLayoutRespectsStringOneNumericAndWhitespaceLimits() throws ScriptException {
    String payload = defaultTwoYearThreeMonthPayload();

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"1\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(1, ((Number) engine.eval("__container._children[0]._children.length")).intValue());

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":2,\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(2, ((Number) engine.eval("__container._children[0]._children.length")).intValue());

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\" 2 \",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(2, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
  }

  @Test
  public void testFlatLayoutRendersZeroMonthsForZeroOrNegativeLimit() throws ScriptException {
    String payload = defaultTwoYearThreeMonthPayload();

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"0\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(0, ((Number) engine.eval("__container._children[0]._children.length")).intValue());

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"-1\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(0, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
  }

  @Test
  public void testFlatLayoutShowsAllMonthsWhenLimitExceedsOrIsBlankOrInvalid()
      throws ScriptException {
    String payload = defaultTwoYearThreeMonthPayload();
    String[] limits = {"\"10\"", "\"\"", "\"   \"", "\"abc\""};
    for (String limit : limits) {
      runWidget(
          "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
              + "\"numberEntries\":"
              + limit
              + ",\"archive_page_result\":\"\","
              + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
          payload);
      assertEquals(
          "Expected all 3 non-empty months for limit " + limit,
          3,
          ((Number) engine.eval("__container._children[0]._children.length")).intValue());
    }

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(3, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
  }

  @Test
  public void testHierarchicalLayoutRespectsStringZeroAndOmittedLimits() throws ScriptException {
    String payload = defaultTwoYearThreeMonthPayload();

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-hierarchical\","
            + "\"numberEntries\":\"1\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(1, ((Number) engine.eval("__container._children[0]._children.length")).intValue());

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-hierarchical\","
            + "\"numberEntries\":\"0\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(0, ((Number) engine.eval("__container._children[0]._children.length")).intValue());

    runWidget(
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-hierarchical\","
            + "\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}",
        payload);
    assertEquals(2, ((Number) engine.eval("__container._children[0]._children.length")).intValue());
  }

  @Test
  public void testNumberEntriesIsStrippedFromOutgoingServiceQuery() throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\"],\"layout\":\"perc-list-flat\","
            + "\"numberEntries\":\"2\",\"archive_page_result\":\"\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    runWidget(query, defaultTwoYearThreeMonthPayload());

    assertFalse(Boolean.TRUE.equals(engine.eval("'numberEntries' in __capturedQuery")));
    assertFalse(Boolean.TRUE.equals(engine.eval("'layout' in __capturedQuery")));
    assertFalse(Boolean.TRUE.equals(engine.eval("'archive_page_result' in __capturedQuery")));
  }
}
