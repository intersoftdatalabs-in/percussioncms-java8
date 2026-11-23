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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Nashorn-based regression tests for {@code percPoll.js} and {@code percPoll.min.js} covering issue
 * #378 (Session restriction failing to parse {@code "false"} from {@code makeXdmJsonRequest} and
 * leaving {@code "Back to poll"} visible).
 */
@RunWith(Parameterized.class)
public class PercPollSessionRestrictionJsTest {

  private final String scriptName;
  private ScriptEngine engine;

  public PercPollSessionRestrictionJsTest(String scriptName) {
    this.scriptName = scriptName;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> scripts() {
    return Arrays.asList(new Object[][] {{"percPoll.js"}, {"percPoll.min.js"}});
  }

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub("Session", "Session question 123?");
    evalScript(resolvePollJs(scriptName));
  }

  private Path resolvePollJs(String fileName) {
    String basedir = System.getProperty("basedir", ".");
    Path fromWebUi =
        Paths.get(basedir)
            .resolve("..")
            .resolve(
                "system/Packages/perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js")
            .resolve(fileName);
    if (Files.exists(fromWebUi)) {
      return fromWebUi.normalize();
    }
    Path fromRoot =
        Paths.get(basedir)
            .resolve(
                "system/Packages/perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js")
            .resolve(fileName);
    if (Files.exists(fromRoot)) {
      return fromRoot.normalize();
    }
    throw new IllegalStateException(
        "Unable to locate poll JS file: "
            + fileName
            + " from basedir "
            + new File(basedir).getAbsolutePath());
  }

  private void evalScript(Path path) throws Exception {
    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    engine.eval(content);
  }

  private void installDomAndJQueryStub(String restrictionType, String question)
      throws ScriptException {
    String stub =
        "var console = { error: function() {}, log: function() {}, debug: function() {} };\n"
            + "var __cookies = {};\n"
            + "var __readyCallbacks = [];\n"
            + "var __nextResponses = [];\n"
            + "var __requests = [];\n"
            + "var __pendingCallbacks = [];\n"
            + "var __deferResponses = false;\n"
            + "\n"
            + "function makeElement(selector) {\n"
            + "  return {\n"
            + "    _isElement: true,\n"
            + "    _selector: selector,\n"
            + "    _attrs: {},\n"
            + "    _handlers: {},\n"
            + "    _text: '',\n"
            + "    _visible: true,\n"
            + "    _width: 200,\n"
            + "    _children: {},\n"
            + "    length: 1,\n"
            + "    each: function(fn) {\n"
            + "      if (this.length > 0) { fn.call(this, 0, this); }\n"
            + "      return this;\n"
            + "    },\n"
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
            + "    width: function(val) {\n"
            + "      if (arguments.length === 0) { return this._width; }\n"
            + "      this._width = val;\n"
            + "      return this;\n"
            + "    },\n"
            + "    show: function() { this._visible = true; return this; },\n"
            + "    hide: function() { this._visible = false; return this; },\n"
            + "    on: function(evt, fn) {\n"
            + "      this._handlers[evt] = this._handlers[evt] || [];\n"
            + "      this._handlers[evt].push(fn);\n"
            + "      return this;\n"
            + "    },\n"
            + "    off: function(evt) { delete this._handlers[evt]; return this; },\n"
            + "    trigger: function(evt) {\n"
            + "      var list = this._handlers[evt] || [];\n"
            + "      for (var i = 0; i < list.length; i++) { list[i].call(this, {}); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) { return this._children[sel] || makeElement(sel); },\n"
            + "    siblings: function() { return makeElement('siblings'); },\n"
            + "    closest: function() { return makeElement('closest'); },\n"
            + "    next: function() { return makeElement('next'); },\n"
            + "    filter: function() { return makeElement('filter'); }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __pollRoot = makeElement('.perc-polls');\n"
            + "__pollRoot.attr('data-poll', JSON.stringify({\n"
            + "  pollName: 'QA Poll',\n"
            + "  restrictionType: '"
            + restrictionType
            + "',\n"
            + "  deliveryurl: ''\n"
            + "}));\n"
            + "\n"
            + "var __renderContainer = makeElement('.perc-poll-render-container');\n"
            + "__renderContainer.hide();\n"
            + "var __resultsContainer = makeElement('.perc-poll-results-container');\n"
            + "__resultsContainer.hide();\n"
            + "var __viewResultsAction = makeElement('.view-results-link-action');\n"
            + "var __viewResultsLink = makeElement('.view-results-link-action a');\n"
            + "var __submitButtons = makeElement('.perc-poll-submit-button, .perc-poll-submit-button-span-action');\n"
            + "var __validationText = makeElement('.poll-validation-text');\n"
            + "__validationText.hide();\n"
            + "var __backToPoll = makeElement('.perc-poll-backto-poll');\n"
            + "var __totalVotesSpan = makeElement('.perc-poll-total-votes span');\n"
            + "var __resultContainer = makeElement('.perc-poll-result-container');\n"
            + "__resultContainer.width(200);\n"
            + "\n"
            + "var __questionEl = makeElement('.perc-poll-question:first');\n"
            + "__questionEl.text('"
            + question
            + "');\n"
            + "\n"
            + "var __answerLabels = [makeElement('span'), makeElement('span')];\n"
            + "__answerLabels[0].text('A');\n"
            + "__answerLabels[1].text('B');\n"
            + "\n"
            + "var __inputs = [\n"
            + "  { checked: false, _label: __answerLabels[0] },\n"
            + "  { checked: false, _label: __answerLabels[1] }\n"
            + "];\n"
            + "var __allInputs = {\n"
            + "  length: 2,\n"
            + "  _handlers: {},\n"
            + "  on: function(evt, fn) { this._handlers[evt] = fn; return this; },\n"
            + "  each: function(fn) {\n"
            + "    for (var i = 0; i < __inputs.length; i++) { fn.call(__inputs[i], i, __inputs[i]); }\n"
            + "    return this;\n"
            + "  }\n"
            + "};\n"
            + "var __checkedInputs = { length: 0 };\n"
            + "function __selectAnswer(idx) {\n"
            + "  for (var i = 0; i < __inputs.length; i++) { __inputs[i].checked = (i === idx); }\n"
            + "  __checkedInputs.length = 1;\n"
            + "}\n"
            + "\n"
            + "var __answerContainers = [];\n"
            + "for (var idx = 0; idx < 2; idx++) {\n"
            + "  (function(i) {\n"
            + "    var ansText = makeElement('.perc-poll-answer');\n"
            + "    ansText.text(__answerLabels[i].text());\n"
            + "    var pct = makeElement('.perc-poll-result-percent');\n"
            + "    var cnt = makeElement('.perc-poll-result-count');\n"
            + "    var fill = makeElement('.perc-poll-result-fill');\n"
            + "    var empty = makeElement('.perc-poll-result-empty');\n"
            + "    var barRow = makeElement('.perc-poll-result-container');\n"
            + "    barRow._children['.perc-poll-result-fill'] = fill;\n"
            + "    barRow._children['.perc-poll-result-empty'] = empty;\n"
            + "    var row = makeElement('.perc-poll-answer-container');\n"
            + "    row._children['.perc-poll-answer'] = ansText;\n"
            + "    row._children['.perc-poll-result-percent'] = pct;\n"
            + "    row._children['.perc-poll-result-count'] = cnt;\n"
            + "    row.next = function() { return barRow; };\n"
            + "    __answerContainers.push(row);\n"
            + "  })(idx);\n"
            + "}\n"
            + "var __resultAnswerRows = {\n"
            + "  length: __answerContainers.length,\n"
            + "  each: function(fn) {\n"
            + "    for (var i = 0; i < __answerContainers.length; i++) {\n"
            + "      fn.call(__answerContainers[i], i, __answerContainers[i]);\n"
            + "    }\n"
            + "    return this;\n"
            + "  }\n"
            + "};\n"
            + "var __resultAnswers = {\n"
            + "  filter: function(fn) {\n"
            + "    for (var i = 0; i < __answerContainers.length; i++) {\n"
            + "      var el = __answerContainers[i]._children['.perc-poll-answer'];\n"
            + "      if (fn.call(el)) {\n"
            + "        return { closest: function() { return __answerContainers[i]; } };\n"
            + "      }\n"
            + "    }\n"
            + "    return { closest: function() { return makeElement('missing'); } };\n"
            + "  }\n"
            + "};\n"
            + "\n"
            + "__resultsContainer._children['.perc-poll-result-container'] = __resultContainer;\n"
            + "__resultsContainer._children['.perc-poll-answer-container'] = __resultAnswerRows;\n"
            + "__resultsContainer._children['.perc-poll-total-votes span'] = __totalVotesSpan;\n"
            + "\n"
            + "__pollRoot._children['.perc-poll-results-container'] = __resultsContainer;\n"
            + "__pollRoot._children['.perc-poll-render-container'] = __renderContainer;\n"
            + "__pollRoot._children['.view-results-link-action a'] = __viewResultsLink;\n"
            + "__pollRoot._children['.view-results-link-action'] = __viewResultsAction;\n"
            + "__pollRoot._children['.perc-poll-submit-button, .perc-poll-submit-button-span-action'] = __submitButtons;\n"
            + "__pollRoot._children['input[name=perc-poll-answer]'] = __allInputs;\n"
            + "__pollRoot._children['input[name=perc-poll-answer]:checked'] = __checkedInputs;\n"
            + "__pollRoot._children['.poll-validation-text'] = __validationText;\n"
            + "__pollRoot._children['.perc-poll-question:first'] = __questionEl;\n"
            + "__pollRoot._children['.perc-poll-result-container'] = __resultContainer;\n"
            + "__pollRoot._children['.perc-poll-answer'] = __resultAnswers;\n"
            + "__pollRoot._children['.perc-poll-backto-poll'] = __backToPoll;\n"
            + "\n"
            + "var jQuery = function(arg) {\n"
            + "  if (typeof arg === 'function') {\n"
            + "    __readyCallbacks.push(arg);\n"
            + "    return;\n"
            + "  }\n"
            + "  if (arg && arg._isElement) { return arg; }\n"
            + "  if (arg && arg._label) {\n"
            + "    return { siblings: function() { return arg._label; } };\n"
            + "  }\n"
            + "  if (arg === '.perc-polls') { return __pollRoot; }\n"
            + "  return makeElement(String(arg));\n"
            + "};\n"
            + "jQuery.each = function(obj, fn) {\n"
            + "  if (!obj) { return; }\n"
            + "  for (var k in obj) {\n"
            + "    if (Object.prototype.hasOwnProperty.call(obj, k)) { fn(k, obj[k]); }\n"
            + "  }\n"
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
            + "  TYPE_GET: 'GET',\n"
            + "  TYPE_POST: 'POST',\n"
            + "  extractDefaultErrorMessage: function(req) { return req ? String(req) : ''; },\n"
            + "  makeXdmJsonRequest: function(servicebase, url, type, callback, dataObject) {\n"
            + "    __requests.push({ url: url, type: type, dataObject: dataObject || null });\n"
            + "    var next = __nextResponses.shift();\n"
            + "    if (!next) { throw new Error('No stubbed response for ' + url); }\n"
            + "    if (__deferResponses) {\n"
            + "      __pendingCallbacks.push(function() { callback(next.status, next.result); });\n"
            + "      return;\n"
            + "    }\n"
            + "    callback(next.status, next.result);\n"
            + "  }\n"
            + "};\n"
            + "var $ = jQuery;\n"
            + "\n"
            + "function __queueResponse(outerStatus, data, innerStatus) {\n"
            + "  __nextResponses.push({\n"
            + "    status: outerStatus,\n"
            + "    result: { data: data, status: innerStatus || outerStatus }\n"
            + "  });\n"
            + "}\n"
            + "function __queueSuccess(data) {\n"
            + "  __queueResponse('success', data, 'success');\n"
            + "}\n"
            + "function __queueError() {\n"
            + "  __nextResponses.push({\n"
            + "    status: 'error',\n"
            + "    result: { message: 'err', status: 'error', request: '500' }\n"
            + "  });\n"
            + "}\n"
            + "function __flushNextPending() {\n"
            + "  var fn = __pendingCallbacks.shift();\n"
            + "  if (fn) { fn(); }\n"
            + "}\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) {\n"
            + "    __readyCallbacks[i]();\n"
            + "  }\n"
            + "}\n"
            + "function __setRestrictionType(rt) {\n"
            + "  __pollRoot.attr('data-poll', JSON.stringify({\n"
            + "    pollName: 'QA Poll',\n"
            + "    restrictionType: rt,\n"
            + "    deliveryurl: ''\n"
            + "  }));\n"
            + "}\n";
    engine.eval(stub);
  }

  private static String pollSuccessJson(int votesA, int votesB) {
    int total = votesA + votesB;
    return "{\"status\":\"SUCCESS\",\"result\":{\"pollName\":\"QA Poll\","
        + "\"pollQuestion\":\"Session question 123?\","
        + "\"pollResults\":{\"A\":"
        + votesA
        + ",\"B\":"
        + votesB
        + "},"
        + "\"totalVotes\":"
        + total
        + "}}";
  }

  private boolean isFormVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__renderContainer._visible"));
  }

  private boolean isResultsVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__resultsContainer._visible"));
  }

  private boolean isBackToPollVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__backToPoll._visible"));
  }

  @Test
  public void testSessionInitialLoadWhenCanVoteTrueShowsFormAndEncodesQuestion()
      throws ScriptException {
    engine.eval("__queueSuccess('true'); __runReady();");

    assertTrue("Form must be visible when canuservote returns 'true'", isFormVisible());
    assertFalse("Results must remain hidden on initial load", isResultsVisible());
    assertEquals(
        "/perc-polls-services/polls/canuservote/Session%20question%20123%3F",
        engine.eval("__requests[0].url"));
  }

  @Test
  public void testSessionViewResultsBeforeVotingShowsBackToPollLink() throws ScriptException {
    engine.eval("__queueSuccess('true'); __runReady();");
    engine.eval("__queueSuccess('" + pollSuccessJson(1, 0) + "');");
    engine.eval("__queueSuccess('true');");
    engine.eval("__viewResultsLink.trigger('click');");

    assertFalse(isFormVisible());
    assertTrue(isResultsVisible());
    assertTrue("Back to poll must be visible before voting", isBackToPollVisible());

    engine.eval("__backToPoll.trigger('click');");
    assertTrue("Clicking Back to poll must restore form", isFormVisible());
    assertFalse("Clicking Back to poll must hide results", isResultsVisible());
  }

  @Test
  public void testSessionAfterVotingHidesBackToPollImmediatelyAndWhenCanVoteFalse()
      throws ScriptException {
    engine.eval("__queueSuccess('true'); __runReady();");
    engine.eval("__selectAnswer(1);");
    engine.eval("__queueSuccess('" + pollSuccessJson(0, 1) + "');");
    engine.eval("__queueSuccess('false');");
    engine.eval("__deferResponses = true;");
    engine.eval("__submitButtons.trigger('click');");

    // Flush POST /save callback; canuservote GET is queued in __pendingCallbacks (in flight)
    engine.eval("__flushNextPending();");
    assertTrue(isResultsVisible());
    assertFalse(isFormVisible());
    assertFalse(
        "Back to poll must stay hidden while async canuservote check is in flight",
        isBackToPollVisible());
    assertEquals("/perc-polls-services/polls/save", engine.eval("__requests[1].url"));
    assertEquals(Boolean.TRUE, engine.eval("__requests[1].dataObject.restrictBySession"));

    // Now deliver "false" from GET /canuservote
    engine.eval("__flushNextPending();");
    assertFalse(
        "Back to poll must remain hidden when canuservote returns 'false'", isBackToPollVisible());
  }

  @Test
  public void testSessionReloadAfterVotingShowsResultsAndHidesFormAndBackToPoll()
      throws ScriptException {
    engine.eval("__queueSuccess('false');");
    engine.eval("__queueSuccess('" + pollSuccessJson(0, 1) + "');");
    engine.eval("__queueSuccess('false');");
    engine.eval("__runReady();");

    assertFalse("Form must stay hidden on reload after voting", isFormVisible());
    assertTrue("Results must be shown on reload after voting", isResultsVisible());
    assertFalse("Back to poll must be hidden on reload after voting", isBackToPollVisible());
    assertEquals(
        "/perc-polls-services/polls/question/Session%20question%20123%3F",
        engine.eval("__requests[1].url"));
  }

  @Test
  public void testSessionCanVoteParsingEdgeCasesAndHttp304() throws ScriptException {
    String[] restrictingResponses = {
      "'false'", "' false\\n'", "'\"false\"'", "false", "''", "'<html>500</html>'"
    };
    for (String resp : restrictingResponses) {
      engine.eval(
          "__nextResponses = []; __requests = []; __renderContainer.hide(); __resultsContainer.hide();");
      engine.eval("__queueSuccess(" + resp + ");");
      engine.eval("__queueSuccess('" + pollSuccessJson(1, 1) + "');");
      engine.eval("__queueSuccess(" + resp + ");");
      engine.eval("__readyCallbacks[0]();");

      assertFalse("Form must be hidden for response: " + resp, isFormVisible());
      assertTrue("Results must be visible for response: " + resp, isResultsVisible());
      assertFalse("Back to poll must be hidden for response: " + resp, isBackToPollVisible());
    }

    // AJAX error must fail closed (restrict)
    engine.eval(
        "__nextResponses = []; __requests = []; __renderContainer.hide(); __resultsContainer.hide();");
    engine.eval("__queueError();");
    engine.eval("__queueSuccess('" + pollSuccessJson(1, 1) + "');");
    engine.eval("__queueError();");
    engine.eval("__readyCallbacks[0]();");
    assertFalse("Form must stay hidden on canuservote error", isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse(isBackToPollVisible());

    // HTTP 304 notmodified with "false" payload must still restrict
    engine.eval(
        "__nextResponses = []; __requests = []; __renderContainer.hide(); __resultsContainer.hide();");
    engine.eval("__queueResponse('success', 'false', 'notmodified');");
    engine.eval("__queueSuccess('" + pollSuccessJson(1, 1) + "');");
    engine.eval("__queueResponse('success', 'false', 'notmodified');");
    engine.eval("__readyCallbacks[0]();");
    assertFalse("Form must stay hidden for HTTP 304 with 'false'", isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse(isBackToPollVisible());

    // Truthy string/boolean variants allow voting
    String[] allowingResponses = {"'true'", "' true\\n'", "'TRUE'", "'\"true\"'", "true"};
    for (String resp : allowingResponses) {
      engine.eval(
          "__nextResponses = []; __requests = []; __renderContainer.hide(); __resultsContainer.hide();");
      engine.eval("__queueSuccess(" + resp + ");");
      engine.eval("__readyCallbacks[0]();");

      assertTrue("Form must be visible for response: " + resp, isFormVisible());
      assertFalse("Results must be hidden for response: " + resp, isResultsVisible());
    }
  }

  @Test
  public void testUnrestrictedRestrictionAllowsVotingAndBackToPoll() throws ScriptException {
    engine.eval("__setRestrictionType('Unrestricted');");
    engine.eval("__runReady();");
    assertTrue(isFormVisible());
    assertEquals(0, ((Number) engine.eval("__requests.length")).intValue());

    engine.eval("__selectAnswer(0); __queueSuccess('" + pollSuccessJson(1, 0) + "');");
    engine.eval("__submitButtons.trigger('click');");
    assertTrue(isResultsVisible());
    assertTrue("Back to poll must stay visible for Unrestricted", isBackToPollVisible());
  }

  @Test
  public void testCookieRestrictionRestrictsAfterVoteAndOnReload() throws ScriptException {
    engine.eval("__setRestrictionType('Cookie');");
    engine.eval("__runReady();");
    assertTrue(isFormVisible());

    engine.eval("__selectAnswer(0); __queueSuccess('" + pollSuccessJson(2, 0) + "');");
    engine.eval("__submitButtons.trigger('click');");
    assertEquals("voted", engine.eval("__cookies['PercPoll-Session question 123?']"));
    assertTrue(isResultsVisible());
    assertFalse("Back to poll must be hidden after Cookie vote", isBackToPollVisible());

    // Reload with Cookie set (re-initialize renderer via PercPollRenderer)
    engine.eval("__renderContainer.hide(); __resultsContainer.hide();");
    engine.eval("__queueSuccess('" + pollSuccessJson(2, 0) + "');");
    engine.eval("$.PercPollRenderer().init(__pollRoot);");
    assertFalse(isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse(isBackToPollVisible());
  }
}
