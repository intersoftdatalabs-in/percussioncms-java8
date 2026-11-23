package com.percussion.packages;

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
import org.junit.Test;

/**
 * Nashorn-based regression tests for {@code percPoll.min.js} and {@code percPoll.js} covering issue
 * #377 (unvoted answers showing blank percent and count in poll results instead of {@code 0%} and
 * {@code (0/N)}).
 */
public class PercPollResultsJsTest {

  private static final String[] POLL_SCRIPTS = {"percPoll.min.js", "percPoll.js"};

  private Path resolvePollJs(String fileName) {
    String basedir = System.getProperty("basedir", ".");
    Path fromModule =
        Paths.get(basedir)
            .resolve(
                "../../system/Packages/perc.widget.poll/sys__UserDependency--web_resources/widgets/percPoll/js")
            .resolve(fileName);
    if (Files.exists(fromModule)) {
      return fromModule.normalize();
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

  private ScriptEngine createEngine(String scriptName, String[] answers) throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub(engine, answers);
    String content =
        new String(Files.readAllBytes(resolvePollJs(scriptName)), StandardCharsets.UTF_8);
    engine.eval(content);
    engine.eval("__triggerReady();");
    return engine;
  }

  private void installDomAndJQueryStub(ScriptEngine engine, String[] answers)
      throws ScriptException {
    StringBuilder answersArray = new StringBuilder("[");
    for (int i = 0; i < answers.length; i++) {
      if (i > 0) {
        answersArray.append(", ");
      }
      answersArray.append("'").append(escapeJs(answers[i])).append("'");
    }
    answersArray.append("]");

    String stub =
        "var window = {};\n"
            + "var document = {};\n"
            + "var console = { error: function() {} };\n"
            + "var __readyCallbacks = [];\n"
            + "var __nextGetResult = null;\n"
            + "var __nextSaveResult = null;\n"
            + "var __answerTexts = "
            + answersArray
            + ";\n"
            + "\n"
            + "function makeList(items) {\n"
            + "  var arr = items || [];\n"
            + "  return {\n"
            + "    length: arr.length,\n"
            + "    _items: arr,\n"
            + "    each: function(fn) {\n"
            + "      for (var i = 0; i < arr.length; i++) {\n"
            + "        fn.call(arr[i], i, arr[i]);\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    filter: function(fn) {\n"
            + "      var matched = [];\n"
            + "      for (var i = 0; i < arr.length; i++) {\n"
            + "        if (fn.call(arr[i], i)) { matched.push(arr[i]); }\n"
            + "      }\n"
            + "      return makeList(matched);\n"
            + "    },\n"
            + "    on: function(evt, fn) {\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].on(evt, fn); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    off: function(evt) {\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].off(evt); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    trigger: function(evt) {\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].trigger(evt); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) {\n"
            + "      if (arr.length === 0) { return makeList([]); }\n"
            + "      return arr[0].find(sel);\n"
            + "    },\n"
            + "    closest: function(sel) {\n"
            + "      if (arr.length === 0) { return makeList([]); }\n"
            + "      return arr[0].closest(sel);\n"
            + "    },\n"
            + "    next: function() {\n"
            + "      if (arr.length === 0) { return makeList([]); }\n"
            + "      return arr[0].next();\n"
            + "    },\n"
            + "    siblings: function(sel) {\n"
            + "      if (arr.length === 0) { return makeList([]); }\n"
            + "      return arr[0].siblings(sel);\n"
            + "    },\n"
            + "    text: function(val) {\n"
            + "      if (arguments.length === 0) {\n"
            + "        return arr.length > 0 ? arr[0].text() : '';\n"
            + "      }\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].text(val); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    width: function(val) {\n"
            + "      if (arguments.length === 0) {\n"
            + "        return arr.length > 0 ? arr[0].width() : undefined;\n"
            + "      }\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].width(val); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) {\n"
            + "        return arr.length > 0 ? arr[0].attr(k) : undefined;\n"
            + "      }\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].attr(k, v); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    show: function() {\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].show(); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    hide: function() {\n"
            + "      for (var i = 0; i < arr.length; i++) { arr[i].hide(); }\n"
            + "      return this;\n"
            + "    }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "function makeNode(opts) {\n"
            + "  opts = opts || {};\n"
            + "  return {\n"
            + "    _text: opts.text || '',\n"
            + "    _width: opts.width,\n"
            + "    _visible: opts.visible !== false,\n"
            + "    _attrs: opts.attrs || {},\n"
            + "    _handlers: {},\n"
            + "    _children: opts.children || {},\n"
            + "    _next: opts.next || null,\n"
            + "    _closest: opts.closest || null,\n"
            + "    _siblings: opts.siblings || {},\n"
            + "    checked: !!opts.checked,\n"
            + "    length: 1,\n"
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
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = v;\n"
            + "      return this;\n"
            + "    },\n"
            + "    show: function() { this._visible = true; return this; },\n"
            + "    hide: function() { this._visible = false; return this; },\n"
            + "    on: function(evt, fn) {\n"
            + "      if (!this._handlers[evt]) { this._handlers[evt] = []; }\n"
            + "      this._handlers[evt].push(fn);\n"
            + "      return this;\n"
            + "    },\n"
            + "    off: function(evt) { delete this._handlers[evt]; return this; },\n"
            + "    trigger: function(evt) {\n"
            + "      var list = this._handlers[evt] || [];\n"
            + "      for (var i = 0; i < list.length; i++) { list[i].call(this, {}); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    find: function(sel) {\n"
            + "      return makeList(this._children[sel] || []);\n"
            + "    },\n"
            + "    closest: function(sel) {\n"
            + "      return this._closest && this._closest[sel] ? makeList([this._closest[sel]]) : makeList([]);\n"
            + "    },\n"
            + "    next: function() {\n"
            + "      return this._next ? makeList([this._next]) : makeList([]);\n"
            + "    },\n"
            + "    siblings: function(sel) {\n"
            + "      return makeList(this._siblings[sel] || []);\n"
            + "    }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __formInputs = [];\n"
            + "var __formAnswerSpans = [];\n"
            + "var __formRows = [];\n"
            + "var __resultRows = [];\n"
            + "var __resultBarRows = [];\n"
            + "var __resultAnswerSpans = [];\n"
            + "var __resultPercentSpans = [];\n"
            + "var __resultCountSpans = [];\n"
            + "var __resultFillSpans = [];\n"
            + "var __resultEmptySpans = [];\n"
            + "\n"
            + "for (var idx = 0; idx < __answerTexts.length; idx++) {\n"
            + "  var ansText = __answerTexts[idx];\n"
            + "  var formAnsSpan = makeNode({ text: ansText });\n"
            + "  var formInput = makeNode({ siblings: { 'span': [formAnsSpan] } });\n"
            + "  var formRow = makeNode({ children: {\n"
            + "    'input[name=perc-poll-answer]': [formInput],\n"
            + "    '.perc-poll-answer': [formAnsSpan]\n"
            + "  } });\n"
            + "  __formInputs.push(formInput);\n"
            + "  __formAnswerSpans.push(formAnsSpan);\n"
            + "  __formRows.push(formRow);\n"
            + "\n"
            + "  var fillSpan = makeNode({ width: 0 });\n"
            + "  var emptySpan = makeNode({ width: 0 });\n"
            + "  var barContainer = makeNode({ width: 200, children: {\n"
            + "    '.perc-poll-result-fill': [fillSpan],\n"
            + "    '.perc-poll-result-empty': [emptySpan]\n"
            + "  } });\n"
            + "  var resAnsSpan = makeNode({ text: ansText });\n"
            + "  var resPctSpan = makeNode({ text: '' });\n"
            + "  var resCntSpan = makeNode({ text: '' });\n"
            + "  var resRow = makeNode({\n"
            + "    next: barContainer,\n"
            + "    children: {\n"
            + "      '.perc-poll-answer': [resAnsSpan],\n"
            + "      '.perc-poll-result-percent': [resPctSpan],\n"
            + "      '.perc-poll-result-count': [resCntSpan]\n"
            + "    }\n"
            + "  });\n"
            + "  resAnsSpan._closest = { '.perc-poll-answer-container': resRow };\n"
            + "  __resultRows.push(resRow);\n"
            + "  __resultBarRows.push(barContainer);\n"
            + "  __resultAnswerSpans.push(resAnsSpan);\n"
            + "  __resultPercentSpans.push(resPctSpan);\n"
            + "  __resultCountSpans.push(resCntSpan);\n"
            + "  __resultFillSpans.push(fillSpan);\n"
            + "  __resultEmptySpans.push(emptySpan);\n"
            + "}\n"
            + "\n"
            + "var __questionNode = makeNode({ text: 'Best colour?' });\n"
            + "var __viewResultsLink = makeNode({});\n"
            + "var __viewResultsWrapper = makeNode({ children: { 'a': [__viewResultsLink] } });\n"
            + "var __submitBtn = makeNode({});\n"
            + "var __validationText = makeNode({ visible: false });\n"
            + "var __totalVotesSpan = makeNode({ text: '' });\n"
            + "var __backToPoll = makeNode({ visible: true });\n"
            + "var __renderContainer = makeNode({ visible: false, children: {\n"
            + "  '.perc-poll-answer': __formAnswerSpans,\n"
            + "  '.perc-poll-answer-container': __formRows\n"
            + "} });\n"
            + "var __resultsContainer = makeNode({ visible: false, children: {\n"
            + "  '.perc-poll-answer-container': __resultRows,\n"
            + "  '.perc-poll-result-container': __resultBarRows,\n"
            + "  '.perc-poll-answer': __resultAnswerSpans,\n"
            + "  '.perc-poll-total-votes span': [__totalVotesSpan]\n"
            + "} });\n"
            + "\n"
            + "var __allAnswerSpans = __formAnswerSpans.concat(__resultAnswerSpans);\n"
            + "var __pollRoot = makeNode({\n"
            + "  attrs: { 'data-poll': JSON.stringify({ pollName: 'TestPoll', restrictionType: 'Unrestricted', deliveryurl: '' }) },\n"
            + "  children: {\n"
            + "    '.perc-poll-results-container': [__resultsContainer],\n"
            + "    '.perc-poll-render-container': [__renderContainer],\n"
            + "    '.view-results-link-action': [__viewResultsWrapper],\n"
            + "    '.view-results-link-action a': [__viewResultsLink],\n"
            + "    '.perc-poll-submit-button, .perc-poll-submit-button-span-action': [__submitBtn],\n"
            + "    'input[name=perc-poll-answer]': __formInputs,\n"
            + "    '.poll-validation-text': [__validationText],\n"
            + "    '.perc-poll-question:first': [__questionNode],\n"
            + "    '.perc-poll-result-container': __resultBarRows,\n"
            + "    '.perc-poll-answer': __allAnswerSpans,\n"
            + "    '.perc-poll-backto-poll': [__backToPoll]\n"
            + "  }\n"
            + "});\n"
            + "\n"
            + "__pollRoot.find = function(sel) {\n"
            + "  if (sel === 'input[name=perc-poll-answer]:checked') {\n"
            + "    var checked = [];\n"
            + "    for (var i = 0; i < __formInputs.length; i++) {\n"
            + "      if (__formInputs[i].checked) { checked.push(__formInputs[i]); }\n"
            + "    }\n"
            + "    return makeList(checked);\n"
            + "  }\n"
            + "  return makeList(this._children[sel] || []);\n"
            + "};\n"
            + "\n"
            + "function jQuery(arg) {\n"
            + "  if (typeof arg === 'function') {\n"
            + "    __readyCallbacks.push(arg);\n"
            + "    return;\n"
            + "  }\n"
            + "  if (arg === '.perc-polls') {\n"
            + "    return makeList([__pollRoot]);\n"
            + "  }\n"
            + "  if (arg && typeof arg === 'object') {\n"
            + "    return arg.length === 1 && typeof arg.find === 'function' ? arg : makeList([arg]);\n"
            + "  }\n"
            + "  return makeList([]);\n"
            + "}\n"
            + "\n"
            + "jQuery.each = function(obj, fn) {\n"
            + "  if (!obj) { return; }\n"
            + "  for (var k in obj) {\n"
            + "    if (Object.prototype.hasOwnProperty.call(obj, k)) { fn(k, obj[k]); }\n"
            + "  }\n"
            + "};\n"
            + "\n"
            + "jQuery.PercServiceUtils = {\n"
            + "  TYPE_GET: 'GET',\n"
            + "  TYPE_POST: 'POST',\n"
            + "  STATUS_SUCCESS: 'SUCCESS',\n"
            + "  makeXdmJsonRequest: function(url, path, type, callback, payload) {\n"
            + "    if (type === 'GET' && path.indexOf('/perc-polls-services/polls/question/') === 0) {\n"
            + "      callback('SUCCESS', { status: 'SUCCESS', data: JSON.stringify({ status: 'SUCCESS', result: __nextGetResult }) });\n"
            + "    } else if (type === 'POST' && path === '/perc-polls-services/polls/save') {\n"
            + "      callback('SUCCESS', { status: 'SUCCESS', data: JSON.stringify({ status: 'SUCCESS', result: __nextSaveResult }) });\n"
            + "    }\n"
            + "  },\n"
            + "  extractDefaultErrorMessage: function() { return 'error'; }\n"
            + "};\n"
            + "\n"
            + "function __triggerReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) { __readyCallbacks[i](jQuery); }\n"
            + "}\n";

    engine.eval(stub);
  }

  private String escapeJs(String value) {
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  @Test
  public void testUnvotedAnswersShowZeroPercentAndCountViaViewResults() throws Exception {
    String[] answers = {"Red", "Green", "Blue"};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 2, "
              + "pollResults: { 'Red': 1, 'Green': 1 } };\n"
              + "__viewResultsLink.trigger('click');");

      assertTrue(
          script + ": results container should be visible",
          (Boolean) engine.eval("__resultsContainer._visible"));
      assertFalse(
          script + ": form container should be hidden",
          (Boolean) engine.eval("__renderContainer._visible"));

      assertEquals("50%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(1/2)", engine.eval("__resultCountSpans[0]._text"));
      assertEquals(100, ((Number) engine.eval("__resultFillSpans[0]._width")).intValue());
      assertEquals(100, ((Number) engine.eval("__resultEmptySpans[0]._width")).intValue());

      assertEquals("50%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(1/2)", engine.eval("__resultCountSpans[1]._text"));
      assertEquals(100, ((Number) engine.eval("__resultFillSpans[1]._width")).intValue());
      assertEquals(100, ((Number) engine.eval("__resultEmptySpans[1]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/2)", engine.eval("__resultCountSpans[2]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[2]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[2]._width")).intValue());

      assertEquals(" 2", engine.eval("__totalVotesSpan._text"));
    }
  }

  @Test
  public void testUnvotedAnswersShowZeroPercentAndCountAfterSubmit() throws Exception {
    String[] answers = {"Red", "Green", "Blue"};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__formInputs[0].checked = true;\n"
              + "__nextSaveResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 1, "
              + "pollResults: { 'Red': 1 } };\n"
              + "__submitBtn.trigger('click');");

      assertEquals("100%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(1/1)", engine.eval("__resultCountSpans[0]._text"));
      assertEquals(200, ((Number) engine.eval("__resultFillSpans[0]._width")).intValue());
      assertEquals(0, ((Number) engine.eval("__resultEmptySpans[0]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[1]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[1]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[1]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[2]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[2]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[2]._width")).intValue());

      assertEquals(" 1", engine.eval("__totalVotesSpan._text"));
    }
  }

  @Test
  public void testCaseInsensitiveLookupAndPrototypePropertySafety() throws Exception {
    String[] answers = {"red", "toString", "constructor"};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 2, "
              + "pollResults: { 'Red': 2 } };\n"
              + "__viewResultsLink.trigger('click');");

      assertEquals("100%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(2/2)", engine.eval("__resultCountSpans[0]._text"));

      assertEquals("0%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(0/2)", engine.eval("__resultCountSpans[1]._text"));

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/2)", engine.eval("__resultCountSpans[2]._text"));
    }
  }

  @Test
  public void testZeroTotalVotesAndFormContainerIsolation() throws Exception {
    String[] answers = {"Red", "Green"};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 0, "
              + "pollResults: {} };\n"
              + "__viewResultsLink.trigger('click');");

      assertEquals("0%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(0/0)", engine.eval("__resultCountSpans[0]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[0]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[0]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(0/0)", engine.eval("__resultCountSpans[1]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[1]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[1]._width")).intValue());

      assertEquals(" 0", engine.eval("__totalVotesSpan._text"));
      assertEquals("Red", engine.eval("__formAnswerSpans[0]._text"));
      assertEquals("Green", engine.eval("__formAnswerSpans[1]._text"));
      assertEquals(1, ((Number) engine.eval("__backToPoll._handlers.click.length")).intValue());

      // Triggering results a second time must not stack click handlers on .perc-poll-backto-poll
      engine.eval("__viewResultsLink.trigger('click');");
      assertEquals(1, ((Number) engine.eval("__backToPoll._handlers.click.length")).intValue());
    }
  }

  @Test
  public void testQuotedAnswerTextsUnvotedShowZeroPercentAndCount() throws Exception {
    String[] answers = {"\"Red\"", "\"Green\"", "\"Blue\""};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 1, "
              + "pollResults: { '\"Red\"': 1 } };\n"
              + "__viewResultsLink.trigger('click');");

      assertEquals("100%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(1/1)", engine.eval("__resultCountSpans[0]._text"));

      assertEquals("0%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[1]._text"));

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[2]._text"));
    }
  }

  @Test
  public void testCaseInsensitiveFallbackDoesNotReuseExactMatchedAnswerVotes() throws Exception {
    String[] answers = {"Red", "red", "Blue"};
    for (String script : POLL_SCRIPTS) {
      ScriptEngine engine = createEngine(script, answers);
      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 1, "
              + "pollResults: { 'Red': 1 } };\n"
              + "__viewResultsLink.trigger('click');");

      assertEquals("100%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(1/1)", engine.eval("__resultCountSpans[0]._text"));
      assertEquals(200, ((Number) engine.eval("__resultFillSpans[0]._width")).intValue());
      assertEquals(0, ((Number) engine.eval("__resultEmptySpans[0]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[1]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[1]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[1]._width")).intValue());

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/1)", engine.eval("__resultCountSpans[2]._text"));
      assertEquals(0, ((Number) engine.eval("__resultFillSpans[2]._width")).intValue());
      assertEquals(200, ((Number) engine.eval("__resultEmptySpans[2]._width")).intValue());

      engine.eval(
          "__nextGetResult = { pollName: 'TestPoll', pollQuestion: 'Best colour?', totalVotes: 3, "
              + "pollResults: { 'Red': 1, 'red': 2 } };\n"
              + "__viewResultsLink.trigger('click');");

      assertEquals("33%", engine.eval("__resultPercentSpans[0]._text"));
      assertEquals("(1/3)", engine.eval("__resultCountSpans[0]._text"));

      assertEquals("67%", engine.eval("__resultPercentSpans[1]._text"));
      assertEquals("(2/3)", engine.eval("__resultCountSpans[1]._text"));

      assertEquals("0%", engine.eval("__resultPercentSpans[2]._text"));
      assertEquals("(0/3)", engine.eval("__resultCountSpans[2]._text"));
    }
  }
}
