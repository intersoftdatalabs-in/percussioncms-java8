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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.Test;

/**
 * Nashorn-based regression tests for {@code PercServiceUtils.js} and {@code PercCommentsView.js}
 * covering issue #373 (first-time visitor comment list POST racing with HEAD /comment/csrf and
 * overwriting the XSRF-TOKEN cookie).
 */
public class PercCommentsCsrfJsTest {

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

  private ScriptEngine createEngine(boolean hasCommentForm, boolean hasCommentsView)
      throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    String stub =
        "var window = { location: { protocol: 'http:', href: 'http://localhost:9982/page.html', pathname: '/page.html' } };\n"
            + "var document = {};\n"
            + "var localStorage = { _data: {}, setItem: function(k, v) { this._data[k] = String(v); }, getItem: function(k) { return this._data.hasOwnProperty(k) ? this._data[k] : null; } };\n"
            + "var console = { debug: function() {}, error: function() {} };\n"
            + "var __readyCallbacks = [];\n"
            + "var __ajaxCalls = [];\n"
            + "var __commentForm = {\n"
            + "  length: "
            + (hasCommentForm ? "1" : "0")
            + ",\n"
            + "  _attrs: {},\n"
            + "  attr: function(k, v) { if (arguments.length === 1) return this._attrs[k]; this._attrs[k] = v; return this; }\n"
            + "};\n"
            + "var __commentsViewEl = {\n"
            + "  length: "
            + (hasCommentsView ? "1" : "0")
            + ",\n"
            + "  _classes: {},\n"
            + "  _attrs: { 'data-query': '{\"site\":\"TestSite\",\"pagepath\":\"/page.html\"}' },\n"
            + "  attr: function(k, v) { if (arguments.length === 1) return this._attrs[k]; this._attrs[k] = v; return this; },\n"
            + "  addClass: function(c) { this._classes[c] = true; return this; },\n"
            + "  each: function(fn) { if (this.length > 0) fn.call(this, 0, this); return this; },\n"
            + "  PercCommentsView: function() { return this; }\n"
            + "};\n"
            + "function SimpleDateFormat() { this.format = function() { return ''; }; }\n"
            + "function jQuery(arg) {\n"
            + "  if (typeof arg === 'function') { __readyCallbacks.push(arg); return; }\n"
            + "  if (arg === document) { return { ready: function(fn) { __readyCallbacks.push(fn); } }; }\n"
            + "  if (arg === \"form[name = 'commentForm']\") { return __commentForm; }\n"
            + "  if (arg === '.perc-comments-view' || arg === __commentsViewEl) { return __commentsViewEl; }\n"
            + "  if (arg === \"input[type = 'submit']\") { return { attr: function() { return undefined; } }; }\n"
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
            + "$.perc_utils = { percParseInt: function(v) { return parseInt(v, 10) || 0; } };\n"
            + "$.ajax = function(urlOrOpts, maybeOpts) {\n"
            + "  var opts = (typeof urlOrOpts === 'string') ? maybeOpts : urlOrOpts;\n"
            + "  var url = (typeof urlOrOpts === 'string') ? urlOrOpts : opts.url;\n"
            + "  __ajaxCalls.push({ url: url, method: opts.method || opts.type || 'GET', opts: opts });\n"
            + "};\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) { __readyCallbacks[i](); }\n"
            + "}\n"
            + "function __makeResponse(header, token) {\n"
            + "  return { getResponseHeader: function(name) { if (name === 'X-CSRF-HEADER') return header; if (name === 'X-CSRF-TOKEN') return token; return null; } };\n"
            + "}\n";
    engine.eval(stub);

    Path serviceUtils =
        resolvePath("war/services/PercServiceUtils.js", "WebUI/war/services/PercServiceUtils.js");
    Path commentsView =
        resolvePath(
            "../delivery/common/js/views/PercCommentsView.js",
            "delivery/common/js/views/PercCommentsView.js");

    engine.eval(new String(Files.readAllBytes(serviceUtils), StandardCharsets.UTF_8));
    // Rewire PercCommentsView's $.fn.PercCommentsView into our stub jQuery selector result
    engine.eval(new String(Files.readAllBytes(commentsView), StandardCharsets.UTF_8));
    engine.eval(
        "__commentsViewEl.PercCommentsView = $.fn.PercCommentsView;\n"
            + "if (typeof __commentsViewEl.PercCommentsView === 'function') {\n"
            + "  // already wired\n"
            + "}\n");
    return engine;
  }

  @Test
  public void testFirstVisitDefersCommentListUntilCsrfCompletes() throws Exception {
    ScriptEngine engine = createEngine(true, true);
    engine.eval("__runReady();");

    // Only HEAD /comment/csrf should have been dispatched so far; POST /comment/list must wait.
    assertEquals(1, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals("HEAD", engine.eval("__ajaxCalls[0].method"));
    assertTrue(
        String.valueOf(engine.eval("__ajaxCalls[0].url"))
            .endsWith("/perc-comments-services/comment/csrf"));

    // Now resolve the HEAD /comment/csrf request.
    engine.eval(
        "__ajaxCalls[0].opts.success(null, 'success', __makeResponse('X-XSRF-TOKEN', 'tok-1'));");

    // Form action should have _csrf=tok-1 and POST /comment/list should now be dispatched.
    assertEquals(
        "http://localhost:9982/perc-comments-services/comment/addcomment?_csrf=tok-1",
        engine.eval("__commentForm.attr('action')"));
    assertEquals(2, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals("POST", engine.eval("__ajaxCalls[1].method"));
    assertTrue(
        String.valueOf(engine.eval("__ajaxCalls[1].url"))
            .endsWith("/perc-comments-services/comment/list"));
  }

  @Test
  public void testCsrfErrorStillFlushesPendingCommentList() throws Exception {
    ScriptEngine engine = createEngine(true, true);
    engine.eval("__runReady();");

    assertEquals(1, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    engine.eval("__ajaxCalls[0].opts.error({}, 'error', 'Network Error');");

    assertEquals(2, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals("POST", engine.eval("__ajaxCalls[1].method"));
    assertTrue(
        String.valueOf(engine.eval("__ajaxCalls[1].url"))
            .endsWith("/perc-comments-services/comment/list"));
  }

  @Test
  public void testCommentsOnlyPageLoadsListImmediatelyWithoutCsrfFetch() throws Exception {
    ScriptEngine engine = createEngine(false, true);
    engine.eval("__runReady();");

    assertEquals(1, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals("POST", engine.eval("__ajaxCalls[0].method"));
    assertTrue(
        String.valueOf(engine.eval("__ajaxCalls[0].url"))
            .endsWith("/perc-comments-services/comment/list"));
  }

  @Test
  public void testConcurrentCsrfGetTokenCoalescesInFlightRequests() throws Exception {
    ScriptEngine engine = createEngine(false, false);
    engine.eval(
        "var __tokens = [];\n"
            + "$.PercServiceUtils.csrfGetToken('/perc-comments-services/comment/csrf', function(r) { __tokens.push('cb1:' + r.getResponseHeader('X-CSRF-TOKEN')); });\n"
            + "$.PercServiceUtils.csrfGetToken('/perc-comments-services/comment/csrf', function(r) { __tokens.push('cb2:' + r.getResponseHeader('X-CSRF-TOKEN')); });\n");

    assertEquals(1, ((Number) engine.eval("__ajaxCalls.length")).intValue());

    engine.eval(
        "__ajaxCalls[0].opts.success(null, 'success', __makeResponse('X-XSRF-TOKEN', 'shared-tok'));");

    assertEquals(2, ((Number) engine.eval("__tokens.length")).intValue());
    assertEquals("cb1:shared-tok", engine.eval("__tokens[0]"));
    assertEquals("cb2:shared-tok", engine.eval("__tokens[1]"));

    // Subsequent call after completion must issue a new request (in-flight map cleared).
    engine.eval(
        "$.PercServiceUtils.csrfGetToken('/perc-comments-services/comment/csrf', function(r) { __tokens.push('cb3:' + r.getResponseHeader('X-CSRF-TOKEN')); });\n");
    assertEquals(2, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    engine.eval(
        "__ajaxCalls[1].opts.success(null, 'success', __makeResponse('X-XSRF-TOKEN', 'next-tok'));");
    assertEquals("cb3:next-tok", engine.eval("__tokens[2]"));
  }
}
