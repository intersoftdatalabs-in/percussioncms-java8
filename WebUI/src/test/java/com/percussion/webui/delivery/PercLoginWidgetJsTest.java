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
 * Nashorn-based regression tests for {@code PercMembershipService.js}, {@code PercLoginView.js},
 * {@code PercProtectedRegion.js}, and {@code PercRegistrationView.js} covering issue #365 (JSON
 * text responses from {@code makeXdmJsonRequest} not being parsed before reading {@code
 * userSummary} and session fields).
 */
public class PercLoginWidgetJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveDeliveryJs("services/PercMembershipService.js"));
    evalScript(resolveDeliveryJs("views/PercLoginView.js"));
    evalScript(resolveDeliveryJs("views/PercProtectedRegion.js"));
    evalScript(resolveDeliveryJs("views/PercRegistrationView.js"));
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
        "var __initialUrl = 'http://localhost:9980/login.html';\n"
            + "var __replacedUrl = null;\n"
            + "var __alerts = [];\n"
            + "function alert(msg) { __alerts.push(String(msg)); }\n"
            + "var window = {\n"
            + "  location: {\n"
            + "    protocol: 'http:',\n"
            + "    host: 'localhost:9980',\n"
            + "    pathname: '/login.html',\n"
            + "    href: __initialUrl,\n"
            + "    toString: function() { return __initialUrl; },\n"
            + "    replace: function(u) { __replacedUrl = String(u); window.location = String(u); }\n"
            + "  }\n"
            + "};\n"
            + "var document = {};\n"
            + "var __cookies = {};\n"
            + "var __queryParams = {};\n"
            + "var __readyCallbacks = [];\n"
            + "var __nextResponses = [];\n"
            + "\n"
            + "function makeElement(selector) {\n"
            + "  return {\n"
            + "    _isElement: true,\n"
            + "    _selector: selector,\n"
            + "    _attrs: {},\n"
            + "    _data: {},\n"
            + "    _handlers: {},\n"
            + "    _html: '',\n"
            + "    _val: '',\n"
            + "    _visible: false,\n"
            + "    _removed: false,\n"
            + "    _children: {},\n"
            + "    _parent: null,\n"
            + "    _parentsMap: {},\n"
            + "    _present: true,\n"
            + "    length: 1,\n"
            + "    each: function(fn) {\n"
            + "      if (this._present && !this._removed) { fn.call(this, 0, this); }\n"
            + "      return this;\n"
            + "    },\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = v;\n"
            + "      return this;\n"
            + "    },\n"
            + "    data: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._data[k]; }\n"
            + "      this._data[k] = v;\n"
            + "      return this;\n"
            + "    },\n"
            + "    html: function(val) {\n"
            + "      if (arguments.length === 0) { return this._html; }\n"
            + "      this._html = String(val);\n"
            + "      return this;\n"
            + "    },\n"
            + "    text: function(val) {\n"
            + "      if (arguments.length === 0) { return this._html; }\n"
            + "      this._html = String(val);\n"
            + "      return this;\n"
            + "    },\n"
            + "    val: function(v) {\n"
            + "      if (arguments.length === 0) { return this._val; }\n"
            + "      this._val = String(v);\n"
            + "      return this;\n"
            + "    },\n"
            + "    show: function() { this._visible = true; return this; },\n"
            + "    hide: function() { this._visible = false; return this; },\n"
            + "    remove: function() { this._removed = true; this.length = 0; return this; },\n"
            + "    on: function(evt, fn) { this._handlers[evt] = fn; return this; },\n"
            + "    submit: function(fn) {\n"
            + "      if (arguments.length === 1) { this._handlers['submit'] = fn; return this; }\n"
            + "      return this.trigger('submit');\n"
            + "    },\n"
            + "    trigger: function(evt) {\n"
            + "      if (this._handlers[evt]) {\n"
            + "        this._handlers[evt].call(this, { preventDefault: function() {} });\n"
            + "      }\n"
            + "      return this;\n"
            + "    },\n"
            + "    validate: function() { return { form: function() { return true; } }; },\n"
            + "    find: function(sel) { return this._children[sel] || jQuery(sel); },\n"
            + "    parent: function() { return this._parent || makeElement('parent'); },\n"
            + "    parents: function(sel) { return this._parentsMap[sel] || makeElement(sel); }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __loginWidget = makeElement('.percLogin');\n"
            + "__loginWidget.attr('data', JSON.stringify({\n"
            + "  notLoggedInMessage: 'Please log in',\n"
            + "  loggedInMessage: 'Welcome back, ',\n"
            + "  deliveryurl: '',\n"
            + "  showUsername: 'showUsername'\n"
            + "}));\n"
            + "var __loginContentsBox = makeElement('.login-contents-box');\n"
            + "var __loginForm = makeElement('.perc-login-form');\n"
            + "var __emailInput = makeElement('input[name=perc-login-email-field]');\n"
            + "var __passwordInput = makeElement('input[name=perc-login-password-field]');\n"
            + "var __loginError = makeElement('.perc-login-error-message');\n"
            + "__loginForm._children['input[name=perc-login-email-field]'] = __emailInput;\n"
            + "__loginForm._children['input[name=perc-login-password-field]'] = __passwordInput;\n"
            + "__loginForm._parentsMap['.percLogin'] = __loginWidget;\n"
            + "__loginForm._parent = __loginContentsBox;\n"
            + "__loginContentsBox._children['.perc-login-error-message'] = __loginError;\n"
            + "__loginWidget._children['.login-contents-box'] = __loginContentsBox;\n"
            + "\n"
            + "var __loginPageLink = makeElement('.perc-login-page-link');\n"
            + "__loginPageLink._present = false;\n"
            + "__loginPageLink.length = 0;\n"
            + "\n"
            + "var __protectedRegionInfo = makeElement('#protectedRegionInformation');\n"
            + "__protectedRegionInfo._present = false;\n"
            + "__protectedRegionInfo.length = 0;\n"
            + "__protectedRegionInfo.attr('data', JSON.stringify({\n"
            + "  protectedRegion: 'protectedRegion',\n"
            + "  protectedRegionText: 'Login to view',\n"
            + "  siteLoginPage: '/login.html'\n"
            + "}));\n"
            + "var __protectedRegion = makeElement('#protectedRegion');\n"
            + "__protectedRegion.html('Secret member content');\n"
            + "\n"
            + "var __pwResetForm = makeElement('#perc-password-reset');\n"
            + "__pwResetForm._present = false;\n"
            + "__pwResetForm.length = 0;\n"
            + "var __pwRequestForm = makeElement('#perc-password-request');\n"
            + "__pwRequestForm._present = false;\n"
            + "__pwRequestForm.length = 0;\n"
            + "var __pwFormsCombined = {\n"
            + "  _isElement: true,\n"
            + "  remove: function() { __pwRequestForm.remove(); __pwResetForm.remove(); return this; }\n"
            + "};\n"
            + "var __regEmailInput = makeElement('#perc-registration-email-field');\n"
            + "var __regPasswordInput = makeElement('#perc-registration-password-field');\n"
            + "var __regError = makeElement('.perc-reg-error-message');\n"
            + "var __regConfirmationMsg = makeElement('.perc-confirmation-message');\n"
            + "var __pwResetPageInput = makeElement(\"input[name='perc_password_reset_page']\");\n"
            + "__pwResetPageInput.val('/reset-password.html');\n"
            + "var __regConfirmMode = makeElement('.perc-reg-confirmation-mode');\n"
            + "__regConfirmMode.length = 0;\n"
            + "var __regMode = makeElement('.perc-registration-mode');\n"
            + "__regMode.length = 0;\n"
            + "var __regForms = makeElement('#perc-registration-form, #perc-registration-cform');\n"
            + "__regForms._present = false;\n"
            + "__regForms.length = 0;\n"
            + "var __regCform = makeElement('#perc-registration-cform');\n"
            + "__regCform.length = 0;\n"
            + "\n"
            + "var jQuery = function(arg) {\n"
            + "  if (arg && arg._isElement) { return arg; }\n"
            + "  if (arg === document) {\n"
            + "    return { ready: function(fn) { __readyCallbacks.push(fn); } };\n"
            + "  }\n"
            + "  if (arg === '.percLogin') { return __loginWidget; }\n"
            + "  if (arg === '.perc-login-form') { return __loginForm; }\n"
            + "  if (arg === '.perc-login-page-link') { return __loginPageLink; }\n"
            + "  if (arg === '#protectedRegionInformation') { return __protectedRegionInfo; }\n"
            + "  if (arg === '#protectedRegion') { return __protectedRegion; }\n"
            + "  if (arg === '#perc-password-reset') { return __pwResetForm; }\n"
            + "  if (arg === '#perc-password-request') { return __pwRequestForm; }\n"
            + "  if (arg === '#perc-password-request, #perc-password-reset') { return __pwFormsCombined; }\n"
            + "  if (arg === '#perc-registration-email-field') { return __regEmailInput; }\n"
            + "  if (arg === '#perc-registration-password-field') { return __regPasswordInput; }\n"
            + "  if (arg === '.perc-reg-error-message') { return __regError; }\n"
            + "  if (arg === '.perc-confirmation-message') { return __regConfirmationMsg; }\n"
            + "  if (arg === \"input[name='perc_password_reset_page']\") { return __pwResetPageInput; }\n"
            + "  if (arg === '.perc-reg-confirmation-mode') { return __regConfirmMode; }\n"
            + "  if (arg === '.perc-registration-mode') { return __regMode; }\n"
            + "  if (arg === '#perc-registration-form, #perc-registration-cform') { return __regForms; }\n"
            + "  if (arg === '#perc-registration-cform') { return __regCform; }\n"
            + "  return makeElement(String(arg));\n"
            + "};\n"
            + "jQuery.ajaxSetup = function() {};\n"
            + "jQuery.cookie = function(name, value) {\n"
            + "  if (arguments.length === 1) {\n"
            + "    return __cookies.hasOwnProperty(name) ? __cookies[name] : null;\n"
            + "  }\n"
            + "  __cookies[name] = value;\n"
            + "};\n"
            + "jQuery.deparam = {\n"
            + "  querystring: function() { return __queryParams; }\n"
            + "};\n"
            + "jQuery.param = {\n"
            + "  querystring: function() { return ''; }\n"
            + "};\n"
            + "jQuery.PercServiceUtils = {\n"
            + "  STATUS_SUCCESS: 'success',\n"
            + "  STATUS_ERROR: 'error',\n"
            + "  TYPE_POST: 'POST',\n"
            + "  toJSON: function(payload) {\n"
            + "    if ('' === payload) { payload = {}; }\n"
            + "    if ('undefined' !== typeof(payload)) {\n"
            + "      if (Array.isArray(payload) || 'string' === typeof(payload)) {\n"
            + "        return JSON.parse(payload);\n"
            + "      } else {\n"
            + "        return payload;\n"
            + "      }\n"
            + "    } else {\n"
            + "      return payload;\n"
            + "    }\n"
            + "  },\n"
            + "  extractDefaultErrorMessage: function(request) {\n"
            + "    return (request && request.responseText) ? request.responseText : '';\n"
            + "  },\n"
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
            + "function __queueError(responseText) {\n"
            + "  __nextResponses.push({\n"
            + "    status: 'error',\n"
            + "    result: { message: 'err', status: 'error', request: { responseText: responseText || '' } }\n"
            + "  });\n"
            + "}\n"
            + "function __runReady() {\n"
            + "  for (var i = 0; i < __readyCallbacks.length; i++) {\n"
            + "    __readyCallbacks[i]();\n"
            + "  }\n"
            + "}\n";
    engine.eval(stub);
  }

  private static String sessionSuccessJson(String email) {
    return "{\"status\":\"SUCCESS\",\"message\":\"\",\"userSummary\":{\"email\":\""
        + email
        + "\",\"createdDate\":\"2026-11-23T00:00:00Z\",\"status\":\"Active\",\"groups\":\"\"}}";
  }

  @Test
  public void testLoginInitWithJsonStringSessionShowsLoggedInMessageAndEmail()
      throws ScriptException {
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-123';");
    engine.eval("__queueSuccess('" + sessionSuccessJson("qalogin0638255@example.com") + "');");
    engine.eval("__queueSuccess('" + sessionSuccessJson("qalogin0638255@example.com") + "');");
    engine.eval("__runReady();");

    assertEquals(
        "Welcome back, qalogin0638255@example.com", engine.eval("__loginContentsBox.html()"));
    assertTrue(
        "Login form must be removed when session is active",
        Boolean.TRUE.equals(engine.eval("__loginForm._removed")));
  }

  @Test
  public void testLoginInitWithShowUsernameDisabledOmitsEmail() throws ScriptException {
    engine.eval(
        "__loginWidget.attr('data', JSON.stringify({"
            + "notLoggedInMessage: 'Please log in',"
            + "loggedInMessage: 'You are signed in.',"
            + "deliveryurl: '',"
            + "showUsername: ''"
            + "}));");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-123';");
    engine.eval("__queueSuccess('" + sessionSuccessJson("user@example.com") + "');");
    engine.eval("__queueSuccess('" + sessionSuccessJson("user@example.com") + "');");
    engine.eval("__runReady();");

    assertEquals("You are signed in.", engine.eval("__loginContentsBox.html()"));
    assertTrue(Boolean.TRUE.equals(engine.eval("__loginForm._removed")));
  }

  @Test
  public void testLoginInitWithAlreadyParsedObjectSessionWorks() throws ScriptException {
    engine.eval(
        "$.PercMembershipService.getUser = function(sessionId, cb) {"
            + "  cb('success', { status: 'SUCCESS', userSummary: { email: 'obj@example.com' } });"
            + "};");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-obj';");
    engine.eval("__runReady();");

    assertEquals("Welcome back, obj@example.com", engine.eval("__loginContentsBox.html()"));
    assertTrue(Boolean.TRUE.equals(engine.eval("__loginForm._removed")));
  }

  @Test
  public void testLoginInitWithNullOrMissingUserSummaryKeepsForm() throws ScriptException {
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-expired';");
    engine.eval(
        "__queueSuccess('{\"status\":\"INVALID_SESSION\",\"message\":\"Expired\",\"userSummary\":null}');");
    engine.eval(
        "__queueSuccess('{\"status\":\"INVALID_SESSION\",\"message\":\"Expired\",\"userSummary\":null}');");
    engine.eval("__runReady();");

    assertEquals("", engine.eval("__loginContentsBox.html()"));
    assertFalse(Boolean.TRUE.equals(engine.eval("__loginForm._removed")));
  }

  @Test
  public void testLoginInitWithMalformedJsonOrAjaxErrorKeepsFormWithoutThrowing()
      throws ScriptException {
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-bad';");
    engine.eval("__queueSuccess('<html>500</html>');");
    engine.eval("__queueError('Server down');");
    engine.eval("__runReady();");

    assertEquals("", engine.eval("__loginContentsBox.html()"));
    assertFalse(Boolean.TRUE.equals(engine.eval("__loginForm._removed")));
  }

  @Test
  public void testLoginSubmitSuccessSetsCookieAndRedirects() throws ScriptException {
    engine.eval("__runReady();");
    engine.eval("__emailInput.val('visitor@example.com');");
    engine.eval("__passwordInput.val('secret123');");
    engine.eval("__queueSuccess('{\"status\":\"SUCCESS\",\"sessionId\":\"new-sess-456\"}');");
    engine.eval("__loginForm.trigger('submit');");

    assertEquals("new-sess-456", engine.eval("__cookies['perc_membership_session_id']"));
    assertEquals("http://localhost:9980/login.html", engine.eval("String(window.location)"));
    assertFalse(Boolean.TRUE.equals(engine.eval("__loginError._visible")));

    // Reset window.location and verify loginRedirect query parameter takes precedence
    engine.eval(
        "window.location = { toString: function() { return __initialUrl; } };"
            + "__queryParams = { loginRedirect: '/members/home.html' };"
            + "__queueSuccess('{\"status\":\"SUCCESS\",\"sessionId\":\"new-sess-789\"}');"
            + "__loginForm.trigger('submit');");

    assertEquals("new-sess-789", engine.eval("__cookies['perc_membership_session_id']"));
    assertEquals("/members/home.html", engine.eval("String(window.location)"));
  }

  @Test
  public void testLoginSubmitAuthFailedShowsInlineError() throws ScriptException {
    engine.eval("__runReady();");
    engine.eval("__emailInput.val('visitor@example.com');");
    engine.eval("__passwordInput.val('wrong');");
    engine.eval(
        "__queueSuccess('{\"status\":\"AUTH_FAILED\",\"message\":\"Invalid email or password.\"}');");
    engine.eval("__loginForm.trigger('submit');");

    assertNull(engine.eval("__cookies['perc_membership_session_id']"));
    assertTrue(Boolean.TRUE.equals(engine.eval("__loginError._visible")));
    assertEquals("Invalid email or password.", engine.eval("__loginError.html()"));
  }

  @Test
  public void testLoginSubmitAjaxErrorShowsUnexpectedErrorWithoutThrowing() throws ScriptException {
    engine.eval("__runReady();");
    engine.eval("__emailInput.val('visitor@example.com');");
    engine.eval("__passwordInput.val('secret123');");
    engine.eval("__queueError('');");
    engine.eval("__loginForm.trigger('submit');");

    assertTrue(Boolean.TRUE.equals(engine.eval("__loginError._visible")));
    assertEquals("There was an unexpected error.", engine.eval("__loginError.html()"));
  }

  @Test
  public void testProtectedRegionWithJsonStringSessionRevealsProtectedContent()
      throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__protectedRegionInfo._present = true; __protectedRegionInfo.length = 1;");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-ok';");
    engine.eval("__queueSuccess('" + sessionSuccessJson("member@example.com") + "');");
    engine.eval("__queueSuccess('" + sessionSuccessJson("member@example.com") + "');");
    engine.eval("__runReady();");

    assertTrue(Boolean.TRUE.equals(engine.eval("__protectedRegion._visible")));
    assertEquals("Secret member content", engine.eval("__protectedRegion.html()"));
  }

  @Test
  public void testProtectedRegionWithMissingUserSummaryShowsLoginLinkWithoutThrowing()
      throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__protectedRegionInfo._present = true; __protectedRegionInfo.length = 1;");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-expired';");
    engine.eval("__queueSuccess('{\"status\":\"INVALID_SESSION\",\"message\":\"Expired\"}');");
    engine.eval("__queueSuccess('{\"status\":\"INVALID_SESSION\",\"message\":\"Expired\"}');");
    engine.eval("__runReady();");

    assertTrue(Boolean.TRUE.equals(engine.eval("__protectedRegion._visible")));
    String html = String.valueOf(engine.eval("__protectedRegion.html()"));
    assertTrue(html.contains("/login.html?loginRedirect="));
    assertTrue(html.contains("Login to view"));
  }

  @Test
  public void testProtectedRegionWithMalformedJsonOrErrorShowsLoginLink() throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__protectedRegionInfo._present = true; __protectedRegionInfo.length = 1;");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-err';");
    engine.eval("__queueError('Service unavailable');");
    engine.eval("__queueError('Service unavailable');");
    engine.eval("__runReady();");

    assertTrue(Boolean.TRUE.equals(engine.eval("__protectedRegion._visible")));
    String html = String.valueOf(engine.eval("__protectedRegion.html()"));
    assertTrue(html.contains("/login.html?loginRedirect="));
  }

  @Test
  public void testRegistrationInitWithJsonStringSessionRemovesPasswordResetForms()
      throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__pwRequestForm._present = true; __pwRequestForm.length = 1;");
    engine.eval("__pwResetForm._present = true; __pwResetForm.length = 1;");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-reg';");
    engine.eval("__queueSuccess('" + sessionSuccessJson("reg@example.com") + "');");
    engine.eval("__runReady();");

    assertTrue(Boolean.TRUE.equals(engine.eval("__pwRequestForm._removed")));
    assertTrue(Boolean.TRUE.equals(engine.eval("__pwResetForm._removed")));
  }

  @Test
  public void testRegistrationInitWithInvalidSessionKeepsPasswordResetForms()
      throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__pwRequestForm._present = true; __pwRequestForm.length = 1;");
    engine.eval("__pwResetForm._present = true; __pwResetForm.length = 1;");
    engine.eval("__cookies['perc_membership_session_id'] = 'sess-invalid';");
    engine.eval(
        "__queueSuccess('{\"status\":\"INVALID_SESSION\",\"message\":\"Expired\",\"userSummary\":null}');");
    engine.eval("__runReady();");

    assertFalse(Boolean.TRUE.equals(engine.eval("__pwRequestForm._removed")));
    assertFalse(Boolean.TRUE.equals(engine.eval("__pwResetForm._removed")));
  }

  @Test
  public void testResetPwRequestAuthFailedWithJsonStringShowsError() throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__pwRequestForm._present = true; __pwRequestForm.length = 1;");
    engine.eval("__runReady();");
    engine.eval("__regEmailInput.val('unknown@example.com');");
    engine.eval("__queueSuccess('{\"status\":\"AUTH_FAILED\",\"message\":\"Email not found.\"}');");
    engine.eval("__pwRequestForm.trigger('submit');");

    assertTrue(Boolean.TRUE.equals(engine.eval("__regError._visible")));
    assertEquals("Email not found.", engine.eval("__regError.html()"));
    assertFalse(Boolean.TRUE.equals(engine.eval("__pwRequestForm._removed")));
  }

  @Test
  public void testResetPasswordSuccessWithJsonStringSetsSessionCookie() throws ScriptException {
    engine.eval("__loginWidget._present = false; __loginWidget.length = 0;");
    engine.eval("__pwResetForm._present = true; __pwResetForm.length = 1;");
    engine.eval("__pwResetForm.data('key', 'reset-key-1');");
    engine.eval("__runReady();");
    engine.eval("__regEmailInput.val('user@example.com');");
    engine.eval("__regPasswordInput.val('newPass123');");
    engine.eval("__queueSuccess('{\"status\":\"SUCCESS\",\"sessionId\":\"reset-sess-999\"}');");
    engine.eval("__pwResetForm.trigger('submit');");

    assertEquals("reset-sess-999", engine.eval("__cookies['perc_membership_session_id']"));
    assertEquals("/", engine.eval("__replacedUrl"));
  }
}
