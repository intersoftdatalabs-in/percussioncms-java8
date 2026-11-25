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
 * Nashorn-based regression tests for {@code percPoll.min.js} and {@code percPoll.js} covering:
 *
 * <ul>
 *   <li>Issue #377: unvoted answers show {@code 0%} and {@code (0/N)} instead of blank spans.
 *   <li>Issue #378: {@code Session} restriction parses string {@code "false"} / {@code "true"} from
 *       {@code /perc-polls-services/polls/canuservote/{question}}.
 * </ul>
 */
public class PercPollWidgetJsTest {

  private static final String POLL_MIN_JS_REL =
      "system/Packages/perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js/percPoll.min.js";
  private static final String POLL_JS_REL =
      "system/Packages/perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js/percPoll.js";

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveRepoFile(POLL_MIN_JS_REL));
  }

  @Test
  public void testSourceAndMinifiedPollJsAreIdentical() throws Exception {
    String source =
        new String(Files.readAllBytes(resolveRepoFile(POLL_JS_REL)), StandardCharsets.UTF_8);
    String minified =
        new String(Files.readAllBytes(resolveRepoFile(POLL_MIN_JS_REL)), StandardCharsets.UTF_8);
    assertEquals("percPoll.js and percPoll.min.js must stay in sync", source, minified);
  }

  @Test
  public void testUnvotedAnswersShowZeroPercentAndZeroCountOnSingleVote() throws Exception {
    initPollWidget("Unrestricted");
    engine.eval(
        "__answers[0]._checked = true;\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { pollName: 'TestPoll', pollQuestion: 'Favorite color?', totalVotes: 1, pollResults: { Red: 1 } }"
            + "}), status: 'success' });\n"
            + "__submitBtn.trigger('click');\n");

    assertFalse(isFormVisible());
    assertTrue(isResultsVisible());
    assertEquals("100%", getResultPercent(0));
    assertEquals("(1/1)", getResultCount(0));
    assertEquals(200, getResultFillWidth(0));
    assertEquals(0, getResultEmptyWidth(0));

    assertEquals("0%", getResultPercent(1));
    assertEquals("(0/1)", getResultCount(1));
    assertEquals(0, getResultFillWidth(1));
    assertEquals(200, getResultEmptyWidth(1));

    assertEquals("0%", getResultPercent(2));
    assertEquals("(0/1)", getResultCount(2));
    assertEquals(0, getResultFillWidth(2));
    assertEquals(200, getResultEmptyWidth(2));

    assertEquals(" 1", getTotalVotesText());
    assertTrue(isBackToPollVisible());
  }

  @Test
  public void testMultiVoteZeroVotesAndCaseInsensitiveResultKeys() throws Exception {
    initPollWidget("Unrestricted");
    engine.eval(
        "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 4, pollResults: { Red: 1, Blue: 3 } }"
            + "}), status: 'success' });\n"
            + "__viewResultsLink.trigger('click');\n");

    assertEquals("25%", getResultPercent(0));
    assertEquals("(1/4)", getResultCount(0));
    assertEquals("0%", getResultPercent(1));
    assertEquals("(0/4)", getResultCount(1));
    assertEquals("75%", getResultPercent(2));
    assertEquals("(3/4)", getResultCount(2));

    engine.eval(
        "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 0, pollResults: {} }"
            + "}), status: 'success' });\n"
            + "__viewResultsLink.trigger('click');\n");

    assertEquals("0%", getResultPercent(0));
    assertEquals("(0/0)", getResultCount(0));
    assertEquals(0, getResultFillWidth(0));
    assertEquals(200, getResultEmptyWidth(0));
    assertEquals("0%", getResultPercent(1));
    assertEquals("(0/0)", getResultCount(1));
    assertEquals("0%", getResultPercent(2));
    assertEquals("(0/0)", getResultCount(2));
    assertEquals(" 0", getTotalVotesText());

    engine.eval(
        "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 2, pollResults: { red: 2 } }"
            + "}), status: 'success' });\n"
            + "__viewResultsLink.trigger('click');\n");

    assertEquals("100%", getResultPercent(0));
    assertEquals("(2/2)", getResultCount(0));
    assertEquals("0%", getResultPercent(1));
    assertEquals("(0/2)", getResultCount(1));
  }

  @Test
  public void testSessionRestrictionFreshVisitorShowsFormAndViewResultsShowsBackLink()
      throws Exception {
    engine.eval("__queueResponse('success', { data: 'true', status: 'success' });\n");
    initPollWidget("Session");

    assertTrue(isFormVisible());
    assertFalse(isResultsVisible());
    assertEquals(
        "/perc-polls-services/polls/canuservote/Favorite%20color%3F", getLastRequestedUrl());

    engine.eval(
        "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Green: 1 } }"
            + "}), status: 'success' });\n"
            + "__queueResponse('success', { data: 'true', status: 'success' });\n"
            + "__viewResultsLink.trigger('click');\n");

    assertTrue(isResultsVisible());
    assertTrue("Back to poll should stay visible before voting", isBackToPollVisible());
  }

  @Test
  public void testSessionRestrictionAfterVotingHidesBackToPollAndReloadShowsResults()
      throws Exception {
    engine.eval("__queueResponse('success', { data: 'true', status: 'success' });\n");
    initPollWidget("Session");

    engine.eval(
        "__answers[0]._checked = true;\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Red: 1 } }"
            + "}), status: 'success' });\n"
            + "__queueResponse('success', { data: 'false', status: 'success' });\n"
            + "__submitBtn.trigger('click');\n");

    assertTrue(Boolean.TRUE.equals(engine.eval("__lastSavePayload.restrictBySession === true")));
    assertFalse(isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse("Back to poll must be hidden after voting under Session", isBackToPollVisible());

    engine.eval(
        "__formContainer.hide();\n"
            + "__resultsContainer.hide();\n"
            + "__queueResponse('success', { data: 'false', status: 'success' });\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Red: 1 } }"
            + "}), status: 'success' });\n"
            + "__queueResponse('success', { data: 'false', status: 'success' });\n");
    initPollWidget("Session");

    assertFalse("Reload after voting must keep form hidden", isFormVisible());
    assertTrue("Reload after voting must show results", isResultsVisible());
    assertFalse("Reload after voting must hide Back to poll", isBackToPollVisible());
    assertEquals("0%", getResultPercent(1));
    assertEquals("(0/1)", getResultCount(1));
  }

  @Test
  public void testSessionRestrictionHandlesQuotedBooleansWhitespaceAndErrors() throws Exception {
    String[] denyPayloads = new String[] {"'\"false\"'", "' false '", "false", "'invalid'"};
    for (String payload : denyPayloads) {
      engine.eval(
          "__formContainer.hide();\n"
              + "__resultsContainer.hide();\n"
              + "__queueResponse('success', { data: "
              + payload
              + ", status: 'success' });\n"
              + "__queueResponse('success', { data: JSON.stringify({"
              + "status: 'SUCCESS',"
              + "result: { totalVotes: 1, pollResults: { Red: 1 } }"
              + "}), status: 'success' });\n"
              + "__queueResponse('success', { data: "
              + payload
              + ", status: 'success' });\n");
      initPollWidget("Session");
      assertFalse("Payload " + payload + " must deny voting", isFormVisible());
      assertTrue("Payload " + payload + " must show results", isResultsVisible());
      assertFalse("Payload " + payload + " must hide Back to poll", isBackToPollVisible());
    }

    String[] allowPayloads = new String[] {"'\"true\"'", "' true '", "true"};
    for (String payload : allowPayloads) {
      engine.eval(
          "__formContainer.hide();\n"
              + "__resultsContainer.hide();\n"
              + "__queueResponse('success', { data: "
              + payload
              + ", status: 'success' });\n");
      initPollWidget("Session");
      assertTrue("Payload " + payload + " must allow voting", isFormVisible());
      assertFalse("Payload " + payload + " must hide results", isResultsVisible());
    }

    engine.eval(
        "__formContainer.hide();\n"
            + "__resultsContainer.hide();\n"
            + "__queueResponse('error', { message: 'Server error', status: 'error' });\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Red: 1 } }"
            + "}), status: 'success' });\n"
            + "__queueResponse('error', { message: 'Server error', status: 'error' });\n");
    initPollWidget("Session");
    assertFalse("Error from canuservote must fail closed", isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse(isBackToPollVisible());
  }

  @Test
  public void testUnrestrictedBackToPollAndCookieRestriction() throws Exception {
    initPollWidget("Unrestricted");
    engine.eval(
        "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Red: 1 } }"
            + "}), status: 'success' });\n"
            + "__viewResultsLink.trigger('click');\n");

    assertTrue(isResultsVisible());
    assertTrue(isBackToPollVisible());
    engine.eval("__backToPoll.trigger('click');");
    assertTrue(isFormVisible());
    assertFalse(isResultsVisible());

    initPollWidget("Cookie");
    assertTrue(isFormVisible());
    engine.eval(
        "__answers[1]._checked = true;\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Green: 1 } }"
            + "}), status: 'success' });\n"
            + "__submitBtn.trigger('click');\n");

    assertEquals("voted", engine.eval("__cookies['PercPoll-Favorite color?']"));
    assertFalse(isBackToPollVisible());

    engine.eval(
        "__formContainer.hide();\n"
            + "__resultsContainer.hide();\n"
            + "__queueResponse('success', { data: JSON.stringify({"
            + "status: 'SUCCESS',"
            + "result: { totalVotes: 1, pollResults: { Green: 1 } }"
            + "}), status: 'success' });\n");
    initPollWidget("Cookie");
    assertFalse(isFormVisible());
    assertTrue(isResultsVisible());
    assertFalse(isBackToPollVisible());
  }

  @Test
  public void testMalformedJsonSubmitDoesNotThrowTypeError() throws Exception {
    initPollWidget("Unrestricted");
    engine.eval(
        "__answers[0]._checked = true;\n"
            + "__queueResponse('success', { data: 'not-json', status: 'success' });\n"
            + "__submitBtn.trigger('click');\n");

    String loggedError = String.valueOf(engine.eval("__consoleErrors[__consoleErrors.length - 1]"));
    assertTrue(loggedError.contains("Poll Submit Failed:"));
  }

  private void initPollWidget(String restrictionType) throws ScriptException {
    engine.eval(
        "__pollRoot.attr('data-poll', JSON.stringify({ pollName: 'TestPoll', restrictionType: '"
            + restrictionType
            + "' }));\n"
            + "jQuery.PercPollRenderer().init(__pollRoot);\n");
  }

  private boolean isFormVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__formContainer._visible"));
  }

  private boolean isResultsVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__resultsContainer._visible"));
  }

  private boolean isBackToPollVisible() throws ScriptException {
    return Boolean.TRUE.equals(engine.eval("__backToPoll._visible"));
  }

  private String getResultPercent(int idx) throws ScriptException {
    return String.valueOf(engine.eval("__resultRows[" + idx + "]._percent.text()"));
  }

  private String getResultCount(int idx) throws ScriptException {
    return String.valueOf(engine.eval("__resultRows[" + idx + "]._count.text()"));
  }

  private int getResultFillWidth(int idx) throws ScriptException {
    return ((Number) engine.eval("__resultRows[" + idx + "]._fill.width()")).intValue();
  }

  private int getResultEmptyWidth(int idx) throws ScriptException {
    return ((Number) engine.eval("__resultRows[" + idx + "]._empty.width()")).intValue();
  }

  private String getTotalVotesText() throws ScriptException {
    return String.valueOf(engine.eval("__totalVotesSpan.text()"));
  }

  private String getLastRequestedUrl() throws ScriptException {
    return String.valueOf(engine.eval("__lastRequestedUrl"));
  }

  private Path resolveRepoFile(String relativePath) {
    String basedir = System.getProperty("basedir", ".");
    Path fromWebUi = Paths.get(basedir).resolve("..").resolve(relativePath);
    if (Files.exists(fromWebUi)) {
      return fromWebUi.normalize();
    }
    Path fromRoot = Paths.get(basedir).resolve(relativePath);
    if (Files.exists(fromRoot)) {
      return fromRoot.normalize();
    }
    throw new IllegalStateException(
        "Unable to locate " + relativePath + " from " + new File(basedir).getAbsolutePath());
  }

  private void evalScript(Path path) throws Exception {
    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    engine.eval(content);
  }

  private void installDomAndJQueryStub() throws ScriptException {
    String stub =
        "var __consoleErrors = [];\n"
            + "var console = { error: function(msg) { __consoleErrors.push(String(msg)); } };\n"
            + "var __cookies = {};\n"
            + "var __nextResponses = [];\n"
            + "var __lastRequestedUrl = null;\n"
            + "var __lastSavePayload = null;\n"
            + "\n"
            + "function makeEl(sel) {\n"
            + "  return {\n"
            + "    _sel: sel,\n"
            + "    _attrs: {},\n"
            + "    _handlers: {},\n"
            + "    _text: '',\n"
            + "    _visible: false,\n"
            + "    _width: 0,\n"
            + "    _checked: false,\n"
            + "    _children: {},\n"
            + "    _next: null,\n"
            + "    length: 1,\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = v; return this;\n"
            + "    },\n"
            + "    text: function(v) {\n"
            + "      if (arguments.length === 0) { return this._text; }\n"
            + "      this._text = String(v); return this;\n"
            + "    },\n"
            + "    width: function(v) {\n"
            + "      if (arguments.length === 0) { return this._width; }\n"
            + "      this._width = v; return this;\n"
            + "    },\n"
            + "    show: function() { this._visible = true; return this; },\n"
            + "    hide: function() { this._visible = false; return this; },\n"
            + "    on: function(evt, fn) { this._handlers[evt] = fn; return this; },\n"
            + "    off: function(evt) { delete this._handlers[evt]; return this; },\n"
            + "    trigger: function(evt) {\n"
            + "      if (this._handlers[evt]) { this._handlers[evt]({}); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    next: function() { return this._next || makeEl('next'); },\n"
            + "    find: function(s) { return this._children[s] || makeList([]); },\n"
            + "    siblings: function(s) { return this._children[s] || makeEl(s); }\n"
            + "  };\n"
            + "}\n"
            + "function makeList(items) {\n"
            + "  return {\n"
            + "    _items: items,\n"
            + "    length: items.length,\n"
            + "    each: function(fn) {\n"
            + "      for (var i = 0; i < this._items.length; i++) {\n"
            + "        fn.call(this._items[i], i, this._items[i]);\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    on: function(evt, fn) {\n"
            + "      for (var i = 0; i < this._items.length; i++) { this._items[i].on(evt, fn); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    width: function() { return this._items.length ? this._items[0].width() : 0; }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __labels = ['Red', 'Green', 'Blue'];\n"
            + "var __answers = [];\n"
            + "var __resultRows = [];\n"
            + "var __barContainers = [];\n"
            + "for (var idx = 0; idx < __labels.length; idx++) {\n"
            + "  var inputEl = makeEl('input');\n"
            + "  inputEl.checked = false;\n"
            + "  Object.defineProperty(inputEl, 'checked', {\n"
            + "    get: function() { return this._checked; },\n"
            + "    set: function(v) { this._checked = v; }\n"
            + "  });\n"
            + "  var spanEl = makeEl('span');\n"
            + "  spanEl.text(__labels[idx]);\n"
            + "  inputEl._children['span'] = spanEl;\n"
            + "  __answers.push(inputEl);\n"
            + "\n"
            + "  var row = makeEl('.perc-poll-answer-container');\n"
            + "  var ansSpan = makeEl('.perc-poll-answer');\n"
            + "  ansSpan.text(__labels[idx]);\n"
            + "  var pctSpan = makeEl('.perc-poll-result-percent');\n"
            + "  var cntSpan = makeEl('.perc-poll-result-count');\n"
            + "  row._children['.perc-poll-answer'] = ansSpan;\n"
            + "  row._children['.perc-poll-result-percent'] = pctSpan;\n"
            + "  row._children['.perc-poll-result-count'] = cntSpan;\n"
            + "  row._percent = pctSpan;\n"
            + "  row._count = cntSpan;\n"
            + "\n"
            + "  var bar = makeEl('.perc-poll-result-container');\n"
            + "  bar.width(200);\n"
            + "  var fill = makeEl('.perc-poll-result-fill');\n"
            + "  var empty = makeEl('.perc-poll-result-empty');\n"
            + "  bar._children['.perc-poll-result-fill'] = fill;\n"
            + "  bar._children['.perc-poll-result-empty'] = empty;\n"
            + "  row._next = bar;\n"
            + "  row._fill = fill;\n"
            + "  row._empty = empty;\n"
            + "  __resultRows.push(row);\n"
            + "  __barContainers.push(bar);\n"
            + "}\n"
            + "\n"
            + "var __pollRoot = makeEl('.perc-polls');\n"
            + "var __formContainer = makeEl('.perc-poll-render-container');\n"
            + "var __resultsContainer = makeEl('.perc-poll-results-container');\n"
            + "var __questionEl = makeEl('.perc-poll-question:first');\n"
            + "__questionEl.text('Favorite color?');\n"
            + "var __viewResultsLink = makeEl('.view-results-link-action a');\n"
            + "var __viewResultsWrap = makeEl('.view-results-link-action');\n"
            + "var __submitBtn = makeEl('.perc-poll-submit-button-span-action');\n"
            + "var __validationText = makeEl('.poll-validation-text');\n"
            + "var __totalVotesSpan = makeEl('.perc-poll-total-votes span');\n"
            + "var __backToPoll = makeEl('.perc-poll-backto-poll');\n"
            + "\n"
            + "__resultsContainer._children['.perc-poll-result-container'] = makeList(__barContainers);\n"
            + "__resultsContainer._children['.perc-poll-answer-container'] = makeList(__resultRows);\n"
            + "__resultsContainer._children['.perc-poll-total-votes span'] = __totalVotesSpan;\n"
            + "\n"
            + "__pollRoot._children['.perc-poll-results-container'] = __resultsContainer;\n"
            + "__pollRoot._children['.perc-poll-render-container'] = __formContainer;\n"
            + "__pollRoot._children['.view-results-link-action a'] = __viewResultsLink;\n"
            + "__pollRoot._children['.view-results-link-action'] = __viewResultsWrap;\n"
            + "__pollRoot._children['.perc-poll-submit-button, .perc-poll-submit-button-span-action'] = __submitBtn;\n"
            + "__pollRoot._children['input[name=perc-poll-answer]'] = makeList(__answers);\n"
            + "__pollRoot._children['.poll-validation-text'] = __validationText;\n"
            + "__pollRoot._children['.perc-poll-question:first'] = __questionEl;\n"
            + "__pollRoot._children['.perc-poll-backto-poll'] = __backToPoll;\n"
            + "__pollRoot._children['input[name=perc-poll-answer]:checked'] = {\n"
            + "  get length() {\n"
            + "    var count = 0;\n"
            + "    for (var i = 0; i < __answers.length; i++) { if (__answers[i]._checked) count++; }\n"
            + "    return count;\n"
            + "  }\n"
            + "};\n"
            + "\n"
            + "var jQuery = function(arg) {\n"
            + "  if (typeof arg === 'function') { return arg; }\n"
            + "  if (arg && typeof arg === 'object') { return arg; }\n"
            + "  return makeEl(String(arg));\n"
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
            + "  extractDefaultErrorMessage: function(req) { return req ? 'err' : ''; },\n"
            + "  makeXdmJsonRequest: function(servicebase, url, type, callback, payload) {\n"
            + "    __lastRequestedUrl = url;\n"
            + "    if (payload) { __lastSavePayload = payload; }\n"
            + "    var next = __nextResponses.shift();\n"
            + "    if (!next) { throw new Error('No stubbed response for ' + url); }\n"
            + "    callback(next.status, next.result);\n"
            + "  }\n"
            + "};\n"
            + "var $ = jQuery;\n"
            + "function __queueResponse(status, result) {\n"
            + "  __nextResponses.push({ status: status, result: result });\n"
            + "}\n";
    engine.eval(stub);
  }
}
