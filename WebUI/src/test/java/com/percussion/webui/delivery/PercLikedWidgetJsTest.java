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
import static org.junit.Assert.assertNull;
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
 * Nashorn-based regression tests for {@code PercLikedService.js} and {@code PercLikedView.js}
 * covering issue #359 (string counts from {@code makeXdmJsonRequest} causing {@code "1 people"} and
 * visible {@code "0 people"}).
 */
public class PercLikedWidgetJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveDeliveryJs("services/PercLikedService.js"));
    evalScript(resolveDeliveryJs("views/PercLikedView.js"));
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
            + "  location: { protocol: 'http:', pathname: '/index.html' }\n"
            + "};\n"
            + "var document = {};\n"
            + "var __cookies = {};\n"
            + "var __readyCallbacks = [];\n"
            + "var __nextResponses = [];\n"
            + "\n"
            + "function makeElement(selector) {\n"
            + "  return {\n"
            + "    _selector: selector,\n"
            + "    _classes: {},\n"
            + "    _attrs: {},\n"
            + "    _handlers: {},\n"
            + "    _text: '',\n"
            + "    _visible: true,\n"
            + "    _children: {},\n"
            + "    length: 1,\n"
            + "    addClass: function(cls) { this._classes[cls] = true; return this; },\n"
            + "    removeClass: function(cls) { delete this._classes[cls]; return this; },\n"
            + "    hasClass: function(cls) { return !!this._classes[cls]; },\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = v;\n"
            + "      return this;\n"
            + "    },\n"
            + "    text: function(val) {\n"
            + "      if (arguments.length === 0) { return this._text; }\n"
            + "      this._text = String(val);\n"
            + "      return this;\n"
            + "    },\n"
            + "    show: function() { this._visible = true; return this; },\n"
            + "    hide: function() { this._visible = false; return this; },\n"
            + "    on: function(evt, fn) { this._handlers[evt] = fn; return this; },\n"
            + "    off: function(evt) { delete this._handlers[evt]; return this; },\n"
            + "    trigger: function(evt) {\n"
            + "      if (this._handlers[evt]) { this._handlers[evt]({}); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) { return this._children[sel] || jQuery(sel); }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __widget = makeElement('.perc-like-widget');\n"
            + "__widget.attr('siteName', 'TestSite');\n"
            + "__widget.addClass('perc-disabled');\n"
            + "var __button = makeElement('button');\n"
            + "__button.attr('title', 'Like');\n"
            + "var __counter = makeElement('.perc-like-counter');\n"
            + "var __totalLikes = makeElement('.perc-like-total-likes');\n"
            + "__totalLikes.text('0');\n"
            + "var __people = makeElement('.perc-like-people');\n"
            + "__people.text('people');\n"
            + "__widget._children['button'] = __button;\n"
            + "__widget._children['.perc-like-counter'] = __counter;\n"
            + "__widget._children['.perc-like-total-likes'] = __totalLikes;\n"
            + "__widget._children['.perc-like-people'] = __people;\n"
            + "\n"
            + "var jQuery = function(arg) {\n"
            + "  if (arg === document) {\n"
            + "    return { ready: function(fn) { __readyCallbacks.push(fn); } };\n"
            + "  }\n"
            + "  if (arg === '.perc-like-widget') { return __widget; }\n"
            + "  if (arg === '.perc-like-counter') { return __counter; }\n"
            + "  if (arg === '.perc-like-total-likes') { return __totalLikes; }\n"
            + "  if (arg === '.perc-like-people') { return __people; }\n"
            + "  return makeElement(String(arg));\n"
            + "};\n"
            + "jQuery.cookie = function(name, value) {\n"
            + "  if (arguments.length === 1) {\n"
            + "    return __cookies.hasOwnProperty(name) ? __cookies[name] : null;\n"
            + "  }\n"
            + "  __cookies[name] = value;\n"
            + "};\n"
            + "jQuery.PercServiceUtils = {\n"
            + "  STATUS_SUCCESS: 'success',\n"
            + "  STATUS_ERROR: 'error',\n"
            + "  TYPE_POST: 'POST',\n"
            + "  makeXdmJsonRequest: function(servicebase, url, type, callback) {\n"
            + "    var next = __nextResponses.shift();\n"
            + "    if (!next) { throw new Error('No stubbed response for ' + url); }\n"
            + "    callback(next.status, next.result);\n"
            + "  }\n"
            + "};\n"
            + "var $ = jQuery;\n"
            + "\n"
            + "function __queueSuccess(textData) {\n"
            + "  __nextResponses.push({\n"
            + "    status: 'success',\n"
            + "    result: { data: textData, status: 'success' }\n"
            + "  });\n"
            + "}\n"
            + "function __queueError() {\n"
            + "  __nextResponses.push({\n"
            + "    status: 'error',\n"
            + "    result: { message: 'err', status: 'error' }\n"
            + "  });\n"
            + "}\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) {\n"
            + "    __readyCallbacks[i]();\n"
            + "  }\n"
            + "}\n";
    engine.eval(stub);
  }

  private boolean isCounterVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__counter._visible"));
  }

  private String getTotalText() throws ScriptException {
    return String.valueOf(engine.eval("__totalLikes.text()"));
  }

  private String getPeopleText() throws ScriptException {
    return String.valueOf(engine.eval("__people.text()"));
  }

  private boolean widgetHasClass(String cls) throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__widget.hasClass('" + cls + "')"));
  }

  private String getButtonTitle() throws ScriptException {
    return String.valueOf(engine.eval("__button.attr('title')"));
  }

  @Test
  public void testInitialZeroLikesHidesCounter() throws ScriptException {
    engine.eval("__queueSuccess('0'); __runReady();");

    assertFalse("Counter must be hidden when total likes is 0", isCounterVisible());
    assertEquals("0", getTotalText());
    assertEquals("people", getPeopleText());
    assertTrue(widgetHasClass("perc-unliked"));
    assertEquals("Like", getButtonTitle());
  }

  @Test
  public void testLikeFromZeroShowsOnePerson() throws ScriptException {
    engine.eval("__queueSuccess('0'); __runReady();");
    engine.eval("__queueSuccess('1'); __button.trigger('click');");

    assertTrue("Counter must be visible when total likes is 1", isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
    assertTrue(widgetHasClass("perc-liked"));
    assertEquals("Remove", getButtonTitle());
    assertEquals("true", engine.eval("__cookies['http://TestSite/']"));
  }

  @Test
  public void testReloadWithCookieAndOneLikeShowsOnePerson() throws ScriptException {
    engine.eval("__cookies['http://TestSite/'] = 'true';");
    engine.eval("__queueSuccess('1'); __runReady();");

    assertTrue(isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
    assertTrue(widgetHasClass("perc-liked"));
    assertEquals("Remove", getButtonTitle());
  }

  @Test
  public void testSecondVisitorLikesShowsTwoPeople() throws ScriptException {
    engine.eval("__queueSuccess('1'); __runReady();");
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());

    engine.eval("__queueSuccess('2'); __button.trigger('click');");
    assertTrue(isCounterVisible());
    assertEquals("2", getTotalText());
    assertEquals("people", getPeopleText());
    assertTrue(widgetHasClass("perc-liked"));
  }

  @Test
  public void testUnlikeToOneShowsOnePersonAndUnlikeToZeroHidesCounter() throws ScriptException {
    engine.eval("__cookies['http://TestSite/'] = 'true';");
    engine.eval("__queueSuccess('2'); __runReady();");
    assertEquals("2", getTotalText());
    assertEquals("people", getPeopleText());

    engine.eval("__queueSuccess('1'); __button.trigger('click');");
    assertTrue(isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
    assertTrue(widgetHasClass("perc-unliked"));
    assertEquals("Like", getButtonTitle());
    assertNull(engine.eval("__cookies['http://TestSite/']"));

    // Simulate second visitor unliking from 1 to 0
    engine.eval("__cookies['http://TestSite/'] = 'true';");
    engine.eval("__queueSuccess('1'); __runReady();");
    engine.eval("__queueSuccess('0'); __button.trigger('click');");
    assertFalse("Counter must hide again when unlike returns '0'", isCounterVisible());
    assertEquals("0", getTotalText());
    assertEquals("people", getPeopleText());
    assertTrue(widgetHasClass("perc-unliked"));
  }

  @Test
  public void testWhitespacePaddedNumericStringIsParsed() throws ScriptException {
    engine.eval("__queueSuccess(' 1\\n'); __runReady();");

    assertTrue(isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
  }

  @Test
  public void testNonNumericResponseFallsBackWithoutSettingCookie() throws ScriptException {
    engine.eval("__queueSuccess('<html>error</html>'); __runReady();");
    assertFalse(isCounterVisible());
    assertEquals("0", getTotalText());

    engine.eval("__queueSuccess(''); __button.trigger('click');");
    assertNull(
        "Cookie must not be set when like response is invalid",
        engine.eval("__cookies['http://TestSite/']"));
    assertTrue(isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
  }

  @Test
  public void testViewNormalizesStringCountsFromMockedService() throws ScriptException {
    engine.eval(
        "$.PercLikedService.getTotalLikesForThisPage = function(cb) {"
            + "  cb(true, { url: 'TestSite', totalLikes: '0' });"
            + "};"
            + "__runReady();");
    assertFalse("View must hide counter for string '0'", isCounterVisible());

    engine.eval(
        "$.PercLikedService.getTotalLikesForThisPage = function(cb) {"
            + "  cb(true, { url: 'TestSite', totalLikes: '1' });"
            + "};"
            + "__runReady();");
    assertTrue(isCounterVisible());
    assertEquals("1", getTotalText());
    assertEquals("person", getPeopleText());
  }

  @Test
  public void testNegativeCountHidesCounter() throws ScriptException {
    engine.eval("__queueSuccess('-1'); __runReady();");
    assertFalse(isCounterVisible());
    assertEquals("0", getTotalText());
    assertEquals("people", getPeopleText());
  }
}
