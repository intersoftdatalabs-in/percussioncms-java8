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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.webui.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.Test;

/**
 * Nashorn-based regression tests for {@code PercCommentsView.js} covering issue #408 (Comments
 * widget {@code titleFormat} option ignored because default {@code settings.fields} hardcoded
 * {@code element: 'h3'}).
 */
public class PercCommentsViewTitleFormatJsTest {

  private static final String SAMPLE_COMMENT_JSON =
      "{id: 1, title: 'My Comment', text: 'Body', username: 'alice', createdDate: '2026-11-24T00:00:00Z'}";

  private Path resolvePath(String webUiRelative, String rootRelative) {
    String basedir = System.getProperty("basedir", ".");
    Path fromWebUi = Paths.get(basedir).resolve(webUiRelative);
    if (Files.exists(fromWebUi)) {
      return fromWebUi.normalize();
    }
    Path fromRoot = Paths.get(basedir).resolve(rootRelative);
    if (Files.exists(fromRoot)) {
      return fromRoot.normalize();
    }
    throw new IllegalStateException(
        "Unable to locate JS file: "
            + rootRelative
            + " from basedir "
            + new File(basedir).getAbsolutePath());
  }

  private ScriptEngine createEngine(String... dataQueries) throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    StringBuilder viewElsJs = new StringBuilder("var __commentsViewEls = [\n");
    for (int i = 0; i < dataQueries.length; i++) {
      String escapedQuery = dataQueries[i].replace("\\", "\\\\").replace("'", "\\'");
      viewElsJs
          .append("  __makeElement('div', { 'data-query': '")
          .append(escapedQuery)
          .append("' })");
      if (i < dataQueries.length - 1) {
        viewElsJs.append(",");
      }
      viewElsJs.append("\n");
    }
    viewElsJs.append("];\n");

    String stub =
        "var window = { location: { protocol: 'http:', href: 'http://localhost:9982/page.html', pathname: '/page.html' } };\n"
            + "var document = {};\n"
            + "var localStorage = { _data: {}, setItem: function(k, v) { this._data[k] = String(v); }, getItem: function(k) { return this._data.hasOwnProperty(k) ? this._data[k] : null; } };\n"
            + "var console = { debug: function() {}, error: function() {} };\n"
            + "var __readyCallbacks = [];\n"
            + "var __ajaxCalls = [];\n"
            + "function __makeElement(tag, initialAttrs) {\n"
            + "  var el = {\n"
            + "    length: 1,\n"
            + "    tagName: String(tag).toLowerCase(),\n"
            + "    _classes: [],\n"
            + "    _attrs: initialAttrs || {},\n"
            + "    _children: [],\n"
            + "    _html: '',\n"
            + "    _text: '',\n"
            + "    attr: function(k, v) { if (arguments.length === 1) return this._attrs[k]; this._attrs[k] = v; return this; },\n"
            + "    addClass: function(c) {\n"
            + "      if (this._classes.indexOf(c) === -1) { this._classes.push(c); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    hasClass: function(c) { return this._classes.indexOf(c) !== -1; },\n"
            + "    html: function(v) { if (arguments.length === 0) return this._html; this._html = String(v); return this; },\n"
            + "    text: function(v) { if (arguments.length === 0) return this._text; this._text = String(v); return this; },\n"
            + "    append: function() {\n"
            + "      for (var i = 0; i < arguments.length; i++) {\n"
            + "        var arg = arguments[i];\n"
            + "        if (arg && typeof arg === 'object' && arg.tagName) { this._children.push(arg); }\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    each: function(fn) { fn.call(this, 0, this); return this; },\n"
            + "    position: function() { return null; },\n"
            + "    PercCommentsView: function() { return $.fn.PercCommentsView.apply(this, arguments); }\n"
            + "  };\n"
            + "  return el;\n"
            + "}\n"
            + viewElsJs
            + "var __commentsViewCollection = {\n"
            + "  length: __commentsViewEls.length,\n"
            + "  each: function(fn) {\n"
            + "    for (var i = 0; i < __commentsViewEls.length; i++) { fn.call(__commentsViewEls[i], i, __commentsViewEls[i]); }\n"
            + "    return this;\n"
            + "  }\n"
            + "};\n"
            + "function SimpleDateFormat() { this.format = function() { return '2026-11-24'; }; }\n"
            + "function jQuery(arg) {\n"
            + "  if (typeof arg === 'function') { __readyCallbacks.push(arg); return; }\n"
            + "  if (arg === document) { return { ready: function(fn) { __readyCallbacks.push(fn); } }; }\n"
            + "  if (arg === \"form[name = 'commentForm']\") { return { length: 0, attr: function() { return undefined; } }; }\n"
            + "  if (arg === '.perc-comments-view') { return __commentsViewCollection; }\n"
            + "  for (var i = 0; i < __commentsViewEls.length; i++) { if (arg === __commentsViewEls[i]) { return arg; } }\n"
            + "  if (typeof arg === 'string') {\n"
            + "    var m = arg.match(/^<([a-zA-Z0-9]+)\\s*\\/?>$/);\n"
            + "    if (m) { return __makeElement(m[1]); }\n"
            + "  }\n"
            + "  return { length: 0, attr: function() { return undefined; }, position: function() { return null; } };\n"
            + "}\n"
            + "var $ = jQuery;\n"
            + "$.fn = {};\n"
            + "$.isEditMode = 'false';\n"
            + "$.getDeliveryServiceBase = function() { return 'http://localhost:9982'; };\n"
            + "$.getCMSVersion = function() { return '8.1.8'; };\n"
            + "$.ajaxSetup = function() {};\n"
            + "$.extend = function(target) {\n"
            + "  for (var i = 1; i < arguments.length; i++) {\n"
            + "    var src = arguments[i];\n"
            + "    if (!src) continue;\n"
            + "    for (var k in src) { if (src.hasOwnProperty(k)) target[k] = src[k]; }\n"
            + "  }\n"
            + "  return target;\n"
            + "};\n"
            + "$.deparam = { querystring: function() { return {}; } };\n"
            + "$.timeago = { parse: function(s) { return s; } };\n"
            + "$.perc_utils = { percParseInt: function(v) { return parseInt(v, 10) || 0; } };\n"
            + "$.ajax = function(urlOrOpts, maybeOpts) {\n"
            + "  var opts = (typeof urlOrOpts === 'string') ? maybeOpts : urlOrOpts;\n"
            + "  var url = (typeof urlOrOpts === 'string') ? urlOrOpts : opts.url;\n"
            + "  __ajaxCalls.push({ url: url, method: opts.method || opts.type || 'GET', opts: opts });\n"
            + "};\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) { __readyCallbacks[i](); }\n"
            + "}\n"
            + "function __findByClass(root, className) {\n"
            + "  var out = [];\n"
            + "  function walk(node) {\n"
            + "    if (!node) return;\n"
            + "    if (node.hasClass && node.hasClass(className)) { out.push(node); }\n"
            + "    var children = node._children || [];\n"
            + "    for (var i = 0; i < children.length; i++) { walk(children[i]); }\n"
            + "  }\n"
            + "  walk(root);\n"
            + "  return out;\n"
            + "}\n";
    engine.eval(stub);

    Path serviceUtils =
        resolvePath("war/services/PercServiceUtils.js", "WebUI/war/services/PercServiceUtils.js");
    Path commentsView =
        resolvePath(
            "../delivery/common/js/views/PercCommentsView.js",
            "delivery/common/js/views/PercCommentsView.js");

    engine.eval(new String(Files.readAllBytes(serviceUtils), StandardCharsets.UTF_8));
    engine.eval(new String(Files.readAllBytes(commentsView), StandardCharsets.UTF_8));
    return engine;
  }

  private void resolveAjaxCall(ScriptEngine engine, int index) throws Exception {
    engine.eval(
        "__ajaxCalls["
            + index
            + "].opts.success({ comments: ["
            + SAMPLE_COMMENT_JSON
            + "] }, 'success');");
  }

  @Test
  public void testDefaultTitleFormatOmittedRendersH3() throws Exception {
    ScriptEngine engine = createEngine("{\"site\":\"TestSite\",\"pagepath\":\"/page.html\"}");
    engine.eval("__runReady();");
    resolveAjaxCall(engine, 0);

    assertEquals(
        "h3", engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0].tagName"));
    assertEquals(
        "My Comment",
        engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0]._html"));
    assertEquals(
        "div",
        engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-createdDate')[0].tagName"));
    assertEquals(
        "div",
        engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-username')[0].tagName"));
    assertEquals(
        "div", engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-text')[0].tagName"));
  }

  @Test
  public void testAllAllowedTitleFormatsRenderChosenElement() throws Exception {
    String[] allowedFormats = {"h1", "h2", "h3", "h4", "h5", "h6", "p", "div"};
    for (String format : allowedFormats) {
      ScriptEngine engine =
          createEngine(
              "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\""
                  + format
                  + "\"}");
      engine.eval("__runReady();");
      resolveAjaxCall(engine, 0);

      assertEquals(
          "Expected title tag for format " + format,
          format,
          engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0].tagName"));
      assertEquals(
          "My Comment",
          engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0]._html"));
      assertEquals(
          "div",
          engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-text')[0].tagName"));
    }
  }

  @Test
  public void testTitleFormatNormalizationAndInvalidFallbacks() throws Exception {
    ScriptEngine normalizedEngine =
        createEngine(
            "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\" H2 \"}");
    normalizedEngine.eval("__runReady();");
    resolveAjaxCall(normalizedEngine, 0);
    assertEquals(
        "h2",
        normalizedEngine.eval(
            "__findByClass(__commentsViewEls[0], 'perc-comment-title')[0].tagName"));

    String[] invalidQueries = {
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\"\"}",
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\"   \"}",
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":null}",
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\"null\"}",
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\"span\"}",
      "{\"site\":\"TestSite\",\"pagepath\":\"/page.html\",\"titleFormat\":\"<script>\"}"
    };
    for (String query : invalidQueries) {
      ScriptEngine engine = createEngine(query);
      engine.eval("__runReady();");
      resolveAjaxCall(engine, 0);
      assertEquals(
          "Expected fallback to h3 for query " + query,
          "h3",
          engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0].tagName"));
    }
  }

  @Test
  public void testMultipleWidgetsOutOfOrderAndNoLeakage() throws Exception {
    ScriptEngine engine =
        createEngine(
            "{\"site\":\"TestSite\",\"pagepath\":\"/a.html\",\"titleFormat\":\"h1\"}",
            "{\"site\":\"TestSite\",\"pagepath\":\"/b.html\",\"titleFormat\":\"p\"}",
            "{\"site\":\"TestSite\",\"pagepath\":\"/c.html\"}");
    engine.eval("__runReady();");

    assertEquals(3, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    // Resolve out of order: widget 2 (p), widget 0 (h1), widget 2nd/omitted (h3)
    resolveAjaxCall(engine, 1);
    resolveAjaxCall(engine, 0);
    resolveAjaxCall(engine, 2);

    assertEquals(
        "h1", engine.eval("__findByClass(__commentsViewEls[0], 'perc-comment-title')[0].tagName"));
    assertEquals(
        "p", engine.eval("__findByClass(__commentsViewEls[1], 'perc-comment-title')[0].tagName"));
    assertEquals(
        "h3", engine.eval("__findByClass(__commentsViewEls[2], 'perc-comment-title')[0].tagName"));
  }

  @Test
  public void testCustomFieldsExplicitTitleElementNonTitleObjectAndContainers() throws Exception {
    ScriptEngine engine = createEngine();

    // 1. Explicit element on title field overrides titleFormat
    engine.eval(
        "var el1 = __makeElement('div', {});\n"
            + "el1.PercCommentsView({ site: 'TestSite', pagepath: '/1.html', titleFormat: 'h1', fields: [{ name: 'title', element: 'h2' }, { name: 'text', pretext: 'Body: ' }] });\n"
            + "el1.PercCommentsView('show');\n");
    resolveAjaxCall(engine, 0);
    assertEquals("h2", engine.eval("__findByClass(el1, 'perc-comment-title')[0].tagName"));
    assertEquals("div", engine.eval("__findByClass(el1, 'perc-comment-text')[0].tagName"));

    // 2. Nested container fields with titleFormat: 'p'
    engine.eval(
        "var el2 = __makeElement('div', {});\n"
            + "el2.PercCommentsView({ site: 'TestSite', pagepath: '/2.html', titleFormat: 'p', fields: [['createdDate'], [{ name: 'title' }, 'text']] });\n"
            + "el2.PercCommentsView('show');\n");
    resolveAjaxCall(engine, 1);
    assertNotNull(engine.eval("__findByClass(el2, 'comment-container-1')[0]"));
    assertEquals(
        "p",
        engine.eval(
            "__findByClass(__findByClass(el2, 'comment-container-1')[0], 'perc-comment-title')[0].tagName"));
  }
}
