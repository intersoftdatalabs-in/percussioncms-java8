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
 * Nashorn-based regression tests for issue #460: verifying cross-origin DTS and CSRF requests are
 * skipped in CMS edit/preview mode across {@code PercServiceUtils.js}, {@code PercCommentsView.js},
 * {@code PercArchiveListService.js}, {@code PercTagListService.js}, {@code PercTagListView.js},
 * {@code PercResultService.js}, {@code PercBlogPostService.js}, and {@code PercBlogPostView.js},
 * while continuing to run normally on published pages.
 */
public class PercEditModeIsolationJsTest {

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

  private void evalFile(ScriptEngine engine, String webUiRelative, String rootRelative)
      throws Exception {
    String content =
        new String(
            Files.readAllBytes(resolvePath(webUiRelative, rootRelative)), StandardCharsets.UTF_8);
    engine.eval(content);
  }

  private ScriptEngine createBaseEngine(String windowFlagsJs) throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    String stub =
        "var window = { location: { protocol: 'https:', host: 'cms.example.com:9991', href: 'https://cms.example.com:9991/Rhythmyx/services/pagemanagement/render/page/editmode/1', pathname: '/Rhythmyx/services/pagemanagement/render/page/editmode/1' } };\n"
            + windowFlagsJs
            + "\n"
            + "var document = {};\n"
            + "var localStorage = { _data: {}, setItem: function(k, v) { this._data[k] = String(v); }, getItem: function(k) { return this._data.hasOwnProperty(k) ? this._data[k] : null; } };\n"
            + "var console = { debug: function() {}, error: function() {} };\n"
            + "var __readyCallbacks = [];\n"
            + "var __ajaxCalls = [];\n"
            + "var __commentForm = { length: 1, _attrs: {}, attr: function(k, v) { if (arguments.length === 1) return this._attrs[k]; this._attrs[k] = v; return this; } };\n"
            + "var __submitBtn = { attr: function() { return undefined; } };\n"
            + "var __emptyCollection = { length: 0, each: function() { return this; }, find: function() { return this; }, hide: function() { return this; }, attr: function() { return ''; } };\n"
            + "function jQuery(sel) {\n"
            + "  if (typeof sel === 'function') { __readyCallbacks.push(sel); return; }\n"
            + "  if (sel === document) { return { ready: function(fn) { __readyCallbacks.push(fn); } }; }\n"
            + "  if (sel === \"form[name = 'commentForm']\") { return __commentForm; }\n"
            + "  if (sel === \"input[type = 'submit']\") { return __submitBtn; }\n"
            + "  return __emptyCollection;\n"
            + "}\n"
            + "var $ = jQuery;\n"
            + "$.fn = {};\n"
            + "$.noop = function() {};\n"
            + "$.getDeliveryServiceBase = function() { return 'https://dts.example.com'; };\n"
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
            + "$.ajax = function(urlOrOpts, maybeOpts) {\n"
            + "  var opts = (typeof urlOrOpts === 'string') ? maybeOpts : urlOrOpts;\n"
            + "  var url = (typeof urlOrOpts === 'string') ? urlOrOpts : opts.url;\n"
            + "  __ajaxCalls.push({ url: url, method: opts.method || opts.type || 'GET' });\n"
            + "};\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) { __readyCallbacks[i](); }\n"
            + "}\n";
    engine.eval(stub);
    evalFile(engine, "war/services/PercServiceUtils.js", "WebUI/war/services/PercServiceUtils.js");
    return engine;
  }

  @Test
  public void percServiceUtilsSkipsCsrfAndXdmRequestsInEditMode() throws Exception {
    ScriptEngine engine = createBaseEngine("window.percIsEditMode = 'true';");
    engine.eval(
        "var __errCalled = false;\n"
            + "$.PercServiceUtils.csrfGetToken('/perc-comments-services/comment/csrf', function() {}, function() { __errCalled = true; });\n"
            + "$.PercServiceUtils.makeXdmJsonRequest(null, '/perc-metadata-services/metadata/get', $.PercServiceUtils.TYPE_POST, function() {}, { criteria: [] });\n");

    assertEquals(0, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertTrue((Boolean) engine.eval("__errCalled"));
    assertEquals("true", engine.eval("$.isEditMode"));
  }

  @Test
  public void percServiceUtilsAllowsRequestsOnPublishedPages() throws Exception {
    ScriptEngine engine =
        createBaseEngine("window.percIsEditMode = 'false'; window.percIsPreviewMode = 'false';");
    engine.eval(
        "$.PercServiceUtils.csrfGetToken('/perc-comments-services/comment/csrf', function() {});\n");

    assertEquals(1, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals(
        "https://dts.example.com/perc-comments-services/comment/csrf",
        engine.eval("__ajaxCalls[0].url"));
  }

  @Test
  public void commentsViewSkipsCsrfTokenInEditAndPreviewModes() throws Exception {
    ScriptEngine editEngine = createBaseEngine("window.percIsEditMode = 'true';");
    evalFile(
        editEngine,
        "../delivery/common/js/views/PercCommentsView.js",
        "delivery/common/js/views/PercCommentsView.js");
    editEngine.eval("__runReady();");
    assertEquals(0, ((Number) editEngine.eval("__ajaxCalls.length")).intValue());

    ScriptEngine previewEngine =
        createBaseEngine("window.percIsEditMode = 'false'; window.percIsPreviewMode = 'true';");
    evalFile(
        previewEngine,
        "../delivery/common/js/views/PercCommentsView.js",
        "delivery/common/js/views/PercCommentsView.js");
    previewEngine.eval("__runReady();");
    assertEquals(0, ((Number) previewEngine.eval("__ajaxCalls.length")).intValue());

    ScriptEngine publishedEngine =
        createBaseEngine("window.percIsEditMode = 'false'; window.percIsPreviewMode = 'false';");
    evalFile(
        publishedEngine,
        "../delivery/common/js/views/PercCommentsView.js",
        "delivery/common/js/views/PercCommentsView.js");
    publishedEngine.eval("__runReady();");
    assertEquals(1, ((Number) publishedEngine.eval("__ajaxCalls.length")).intValue());
    assertEquals(
        "https://dts.example.com/perc-comments-services/comment/csrf",
        publishedEngine.eval("__ajaxCalls[0].url"));
  }

  @Test
  public void deliveryMetadataServicesSkipDtsWhenQueryStringIsEditModeStringOrBoolean()
      throws Exception {
    ScriptEngine engine = createBaseEngine("window.percIsEditMode = 'false';");
    evalFile(
        engine,
        "../delivery/common/js/services/PercArchiveListService.js",
        "delivery/common/js/services/PercArchiveListService.js");
    evalFile(
        engine,
        "../delivery/common/js/services/PercTagListService.js",
        "delivery/common/js/services/PercTagListService.js");
    evalFile(
        engine,
        "../delivery/common/js/services/PercResultService.js",
        "delivery/common/js/services/PercResultService.js");
    evalFile(
        engine,
        "../delivery/common/js/services/PercBlogPostService.js",
        "delivery/common/js/services/PercBlogPostService.js");

    engine.eval(
        "var __cbStatuses = [];\n"
            + "$.PercArchiveListService.getArchiveEntries({ isEditMode: 'true', deliveryurl: 'https://dts.example.com' }, function(s) { __cbStatuses.push('arch:' + s); });\n"
            + "$.PercArchiveListService.getArchiveEntries({ isEditMode: true, deliveryurl: 'https://dts.example.com' }, function(s) { __cbStatuses.push('archb:' + s); });\n"
            + "$.PercTagListService.getTagEntries({ isEditMode: 'true', deliveryurl: 'https://dts.example.com' }, 'alpha', function(s) { __cbStatuses.push('tag:' + s); });\n"
            + "$.PercResultService.getPageEntries({ isEditMode: 'true', deliveryurl: 'https://dts.example.com' }, function(s) { __cbStatuses.push('res:' + s); });\n"
            + "$.PercBlogPostService.getPostNavEntries({ isEditMode: 'true', deliveryurl: 'https://dts.example.com' }, '/site/post', function(s) { __cbStatuses.push('blog:' + s); });\n");

    assertEquals(0, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    assertEquals("tag:false,res:false,blog:false", engine.eval("__cbStatuses.join(',')"));
  }

  @Test
  public void formWidgetSkipsCsrfTokenInEditAndPreviewModes() throws Exception {
    for (String flags :
        new String[] {
          "window.percIsEditMode = 'true'; window.percIsPreviewMode = 'false';",
          "window.percIsEditMode = 'false'; window.percIsPreviewMode = 'true';"
        }) {
      ScriptEngine engine = createBaseEngine(flags);
      engine.eval(
          "var __formEl = {\n"
              + "  _attrs: { action: '/perc-form-processor/forms/form/' },\n"
              + "  attr: function(k, v) { if (arguments.length === 1) return this._attrs[k]; this._attrs[k] = v; return this; },\n"
              + "  append: function() { return this; },\n"
              + "  find: function() { return { each: function() { return this; } }; },\n"
              + "  validate: function() { return this; }\n"
              + "};\n"
              + "var __origJQuery = jQuery;\n"
              + "jQuery = function(sel) {\n"
              + "  if (sel === '.form-datepicker') return { datepicker: function() { return this; } };\n"
              + "  if (sel === '.perc-form') return { find: function() { return { each: function(fn) { fn.call(__formEl); return this; } }; } };\n"
              + "  if (sel === '<input/>') return { attr: function() { return this; } };\n"
              + "  if (sel === __formEl) return __formEl;\n"
              + "  return __origJQuery(sel);\n"
              + "};\n"
              + "for (var k in __origJQuery) { jQuery[k] = __origJQuery[k]; }\n"
              + "$ = jQuery;\n"
              + "var location = { href: 'https://cms.example.com:9991/page', search: '' };\n"
              + "document.getElementById = function() { return null; };\n");
      evalFile(
          engine,
          "../system/Packages/perc.widget.form/sys__UserDependency--web_resources/widgets/form/js/form.js",
          "system/Packages/perc.widget.form/sys__UserDependency--web_resources/widgets/form/js/form.js");
      engine.eval("__runReady();");
      assertEquals(0, ((Number) engine.eval("__ajaxCalls.length")).intValue());
    }
  }
}
