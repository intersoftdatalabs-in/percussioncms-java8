/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Nashorn-based regression tests for {@code PercCategoryListService.js} and {@code
 * PercCategoryListView.js} covering issue #460: skipping cross-origin DTS requests in CMS edit
 * mode, avoiding JSON parse exceptions on non-JSON error responses, and guarding Fancytree destroy
 * when no tree instance is initialized.
 */
public class PercCategoryListViewJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveDeliveryJs("services/PercCategoryListService.js"));
    evalScript(resolveDeliveryJs("views/PercCategoryListView.js"));
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
            + "var document = {};\n"
            + "var __consoleErrors = [];\n"
            + "var console = {\n"
            + "  debug: function() {},\n"
            + "  error: function(msg) { __consoleErrors.push(String(msg)); }\n"
            + "};\n"
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
            + "var __fancytreeDestroyCalls = 0;\n"
            + "var __fancytreeInitCalls = 0;\n"
            + "var __fancytreeInitialized = false;\n"
            + "function makeEl(tag) {\n"
            + "  var el = {\n"
            + "    length: 1,\n"
            + "    _tag: tag || 'div',\n"
            + "    _classes: [],\n"
            + "    _attrs: {},\n"
            + "    _text: '',\n"
            + "    _children: [],\n"
            + "    _namedChildren: {},\n"
            + "    _emptied: false,\n"
            + "    _htmlSet: null,\n"
            + "    addClass: function(cls) {\n"
            + "      var parts = String(cls).split(/\\s+/);\n"
            + "      for (var i = 0; i < parts.length; i++) {\n"
            + "        if (parts[i] && this._classes.indexOf(parts[i]) === -1) {\n"
            + "          this._classes.push(parts[i]);\n"
            + "        }\n"
            + "      }\n"
            + "      return this;\n"
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
            + "    empty: function() {\n"
            + "      this._emptied = true;\n"
            + "      this._children = [];\n"
            + "      return this;\n"
            + "    },\n"
            + "    html: function(val) {\n"
            + "      if (arguments.length === 0) { return ''; }\n"
            + "      this._htmlSet = val;\n"
            + "      return this;\n"
            + "    },\n"
            + "    append: function(child) {\n"
            + "      if (child) { this._children.push(child); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    fancytree: function(arg) {\n"
            + "      if (arg === 'destroy') {\n"
            + "        if (!__fancytreeInitialized) {\n"
            + "          throw new Error('cannot call methods on fancytree prior to initialization');\n"
            + "        }\n"
            + "        __fancytreeDestroyCalls++;\n"
            + "        __fancytreeInitialized = false;\n"
            + "        return this;\n"
            + "      }\n"
            + "      __fancytreeInitCalls++;\n"
            + "      __fancytreeInitialized = true;\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) {\n"
            + "      if (sel === '.perc-category-list-expanded,.perc-category-list-collapsible') {\n"
            + "        return {\n"
            + "          empty: function() {\n"
            + "            __expandedEl.empty();\n"
            + "            __collapsibleEl.empty();\n"
            + "          }\n"
            + "        };\n"
            + "      }\n"
            + "      return this._namedChildren[sel] || { length: 0, empty: function() {}, html: function() {}, fancytree: function() {} };\n"
            + "    }\n"
            + "  };\n"
            + "  el[0] = el;\n"
            + "  return el;\n"
            + "}\n"
            + "var __expandedEl = makeEl('div');\n"
            + "var __collapsibleEl = makeEl('div');\n"
            + "var __widget = makeEl('div');\n"
            + "__widget._namedChildren['.perc-category-list-expanded'] = __expandedEl;\n"
            + "__widget._namedChildren['.perc-category-list-collapsible'] = __collapsibleEl;\n"
            + "var __xdmRequests = [];\n"
            + "var __xdmResponseStatus = 'success';\n"
            + "var __xdmResponseData = [];\n"
            + "var __xdmErrorMsg = 'Service Unavailable';\n"
            + "var jQuery = function(arg) {\n"
            + "  if (arg === document) { return { ready: function() {} }; }\n"
            + "  if (typeof arg === 'function') { return; }\n"
            + "  if (arg === '.perc-category-list') {\n"
            + "    return { each: function(fn) { fn.call(__widget); } };\n"
            + "  }\n"
            + "  if (typeof arg === 'string') {\n"
            + "    return makeEl(arg);\n"
            + "  }\n"
            + "  return arg;\n"
            + "};\n"
            + "jQuery.ui = {\n"
            + "  fancytree: {\n"
            + "    getTree: function(el) {\n"
            + "      return (__fancytreeInitialized && el === __collapsibleEl) ? { _el: el } : null;\n"
            + "    }\n"
            + "  }\n"
            + "};\n"
            + "jQuery.PercServiceUtils = {\n"
            + "  TYPE_POST: 'POST',\n"
            + "  STATUS_SUCCESS: 'success',\n"
            + "  STATUS_ERROR: 'error',\n"
            + "  joinURL: function(a, b) {\n"
            + "    var left = a ? a.replace(/\\/+$/, '') : '';\n"
            + "    var right = b ? b.replace(/^\\/+/, '') : '';\n"
            + "    return left + '/' + right;\n"
            + "  },\n"
            + "  toJSON: function(payload) {\n"
            + "    if ('' === payload) { return {}; }\n"
            + "    if (typeof payload === 'string') { return JSON.parse(payload); }\n"
            + "    return payload;\n"
            + "  },\n"
            + "  extractDefaultErrorMessage: function(req) {\n"
            + "    return __xdmErrorMsg;\n"
            + "  },\n"
            + "  makeXdmJsonRequest: function(servicebase, url, type, callback, dataObject) {\n"
            + "    __xdmRequests.push({\n"
            + "      url: url,\n"
            + "      type: type,\n"
            + "      data: JSON.parse(JSON.stringify(dataObject))\n"
            + "    });\n"
            + "    if (__xdmResponseStatus === 'success') {\n"
            + "      callback('success', { data: __xdmResponseData });\n"
            + "    } else {\n"
            + "      callback('error', { request: { responseText: __xdmErrorMsg } });\n"
            + "    }\n"
            + "  }\n"
            + "};\n";
    engine.eval(stub);
  }

  private String defaultCategoriesJson() {
    return "[{\"category\":\"Categories\",\"count\":{\"first\":0},\"children\":["
        + "{\"category\":\"Blog\",\"count\":{\"first\":3},\"children\":["
        + "  {\"category\":\"B2B\",\"count\":{\"first\":2},\"children\":[]},"
        + "  {\"category\":\"CMS\",\"count\":{\"first\":1},\"children\":[]}"
        + "]},"
        + "{\"category\":\"News\",\"count\":{\"first\":4},\"children\":[]}"
        + "]}]";
  }

  @Test
  public void testEditModeSkipsDtsCallAndPreservesServerRenderedDom() throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\",\"site = 'test'\"],"
            + "\"category_page_result\":\"\",\"display_option\":\"expanded\","
            + "\"max_categories\":\"20\",\"deliveryurl\":\"https://percussioncmshelp.intsof.com\","
            + "\"isEditMode\":\"true\",\"isPreviewMode\":\"false\"}";
    engine.put("__queryJson", query);
    engine.eval(
        "__widget.attr('data-query', __queryJson);\n"
            + "jQuery.PercCategoryListView.updateCategoryList();\n");

    assertEquals(0, ((Number) engine.eval("__xdmRequests.length")).intValue());
    assertEquals(0, ((Number) engine.eval("__consoleErrors.length")).intValue());
    assertFalse(Boolean.TRUE.equals(engine.eval("__expandedEl._emptied")));
  }

  @Test
  public void testServiceSkipsDtsRequestInEditModeStringAndBoolean() throws ScriptException {
    engine.eval(
        "var __cbResults = [];\n"
            + "jQuery.PercCategoryListService.getCategories(\n"
            + "  { criteria: [\"type = 'page'\"], deliveryurl: 'https://dts.example.com', isEditMode: 'true' },\n"
            + "  function(ok, msg) { __cbResults.push({ ok: ok, msg: msg }); }\n"
            + ");\n"
            + "jQuery.PercCategoryListService.getCategories(\n"
            + "  { criteria: [\"type = 'page'\"], deliveryurl: 'https://dts.example.com', isEditMode: true },\n"
            + "  function(ok, msg) { __cbResults.push({ ok: ok, msg: msg }); }\n"
            + ");\n");

    assertEquals(0, ((Number) engine.eval("__xdmRequests.length")).intValue());
    assertEquals(2, ((Number) engine.eval("__cbResults.length")).intValue());
    assertFalse(Boolean.TRUE.equals(engine.eval("__cbResults[0].ok")));
    assertEquals("", engine.eval("__cbResults[0].msg"));
    assertFalse(Boolean.TRUE.equals(engine.eval("__cbResults[1].ok")));
    assertEquals("", engine.eval("__cbResults[1].msg"));
  }

  @Test
  public void testPublishedExpandedListRendersCategoriesAndRespectsMaxLimit()
      throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\",\"site = 'test'\"],"
            + "\"category_page_result\":\"/results.html\",\"display_option\":\"expanded\","
            + "\"max_categories\":\"3\",\"deliveryurl\":\"https://dts.example.com\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    engine.put("__queryJson", query);
    engine.put("__payloadJson", defaultCategoriesJson());
    engine.eval(
        "__xdmResponseData = JSON.parse(__payloadJson);\n"
            + "__widget.attr('data-query', __queryJson);\n"
            + "jQuery.PercCategoryListView.updateCategoryList();\n");

    assertEquals(1, ((Number) engine.eval("__xdmRequests.length")).intValue());
    assertEquals(
        "https://dts.example.com/perc-metadata-services/metadata/categories/get",
        engine.eval("__xdmRequests[0].url"));
    assertFalse(Boolean.TRUE.equals(engine.eval("'deliveryurl' in __xdmRequests[0].data")));
    assertFalse(Boolean.TRUE.equals(engine.eval("'display_option' in __xdmRequests[0].data")));
    assertFalse(Boolean.TRUE.equals(engine.eval("'max_categories' in __xdmRequests[0].data")));
    assertTrue(Boolean.TRUE.equals(engine.eval("__expandedEl._emptied")));
    assertEquals(
        "Categories", engine.eval("__expandedEl._htmlSet._children[0]._children[0].text()"));
    assertEquals(
        "Blog (3)",
        engine.eval(
            "__expandedEl._htmlSet._children[0]._children[1]._children[0]._children[0].text()"));
    assertEquals(
        1,
        ((Number)
                engine.eval(
                    "__expandedEl._htmlSet._children[0]._children[1]._children[0]._children[1]._children.length"))
            .intValue());
  }

  @Test
  public void testPublishedCollapsibleDestroysFancytreeOnlyWhenInitialized()
      throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\",\"site = 'test'\"],"
            + "\"category_page_result\":\"\",\"display_option\":\"collapsible\","
            + "\"max_categories\":\"10\",\"deliveryurl\":\"https://dts.example.com\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    engine.put("__queryJson", query);
    engine.put("__payloadJson", defaultCategoriesJson());

    // First run: fancytree is NOT yet initialized -> must not throw on destroy, then initializes.
    engine.eval(
        "__xdmResponseData = JSON.parse(__payloadJson);\n"
            + "__fancytreeInitialized = false;\n"
            + "__widget.attr('data-query', __queryJson);\n"
            + "jQuery.PercCategoryListView.updateCategoryList();\n");

    assertEquals(0, ((Number) engine.eval("__fancytreeDestroyCalls")).intValue());
    assertEquals(1, ((Number) engine.eval("__fancytreeInitCalls")).intValue());

    // Second run: fancytree IS initialized -> destroys previous tree before re-initializing.
    engine.eval("jQuery.PercCategoryListView.updateCategoryList();\n");
    assertEquals(1, ((Number) engine.eval("__fancytreeDestroyCalls")).intValue());
    assertEquals(2, ((Number) engine.eval("__fancytreeInitCalls")).intValue());
  }

  @Test
  public void testErrorResponseWithNonJsonMessageDoesNotThrow() throws ScriptException {
    String query =
        "{\"criteria\":[\"type = 'page'\",\"site = 'test'\"],"
            + "\"category_page_result\":\"\",\"display_option\":\"expanded\","
            + "\"max_categories\":\"10\",\"deliveryurl\":\"https://dts.example.com\","
            + "\"isEditMode\":\"false\",\"isPreviewMode\":\"false\"}";
    engine.put("__queryJson", query);
    engine.eval(
        "__xdmResponseStatus = 'error';\n"
            + "__xdmErrorMsg = 'Service Unavailable';\n"
            + "__widget.attr('data-query', __queryJson);\n"
            + "jQuery.PercCategoryListView.updateCategoryList();\n");

    assertEquals(1, ((Number) engine.eval("__xdmRequests.length")).intValue());
    assertFalse(Boolean.TRUE.equals(engine.eval("__expandedEl._emptied")));
    assertEquals(1, ((Number) engine.eval("__consoleErrors.length")).intValue());
  }
}
