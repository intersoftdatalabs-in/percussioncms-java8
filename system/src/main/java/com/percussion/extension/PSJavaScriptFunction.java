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

package com.percussion.extension;

import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSConsole;
import com.percussion.util.PSCharSets;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The PSJavaScriptFunction class stores compiled JavaScript functions executed by
 * PSJavaScriptUdfExtension objects.
 *
 * <p>The class is implemented by calling native routines to run the JavaScript interpreter.
 *
 * <p><strong>T2.17 hardening (issue #184):</strong> migrated from Mozilla Rhino 1.6R7 (EOL since
 * 2009, unmaintained) to Nashorn, the JDK 1.8 built-in JavaScript engine. The implementation uses
 * the JSR-223 {@link ScriptEngine} API which Nashorn implements, so the code is portable across any
 * JSR-223 engine and does not depend on Rhino's internals (which were the source of the
 * CVE-2009-XXXX-class issues in the deprecated rhino dep).
 *
 * <p>Behavior preserved:
 *
 * <ul>
 *   <li>Compiled functions are cached by {@code context/exitName} in a static {@link HashMap} keyed
 *       by the function text's SHA-256 digest; if the digest matches an existing entry, the cached
 *       engine is reused. The {@code digestedFunctionDefs} map tracks the digest for change
 *       detection.
 *   <li>Per-call arguments are passed positionally via {@link Invocable#invokeFunction}, with
 *       missing trailing args defaulting to {@code ""} (matching Rhino's behavior).
 *   <li>{@code java.util.Date} parameters are passed through to the JavaScript function as-is;
 *       Nashorn exposes Java reflection, so JS code can call {@code date.getTime()} on them and
 *       pass the result to {@code new Date(timestamp)}.
 *   <li>The function's return value is best-effort converted to {@link Date}: {@link Date} values
 *       pass through, numeric values are treated as milliseconds-since-epoch, and JS Date objects
 *       ({@link ScriptObjectMirror} wrapping {@code Date}) are unwrapped to the underlying
 *       timestamp and returned as {@link Date}. This matches Rhino's {@code Context.jsToJava(value,
 *       Date.class)} semantics for the common cases.
 *   <li>Compile-time errors thrown as {@link ScriptException} and runtime errors thrown from {@link
 *       Invocable#invokeFunction} are logged and result in a {@code null} return value, matching
 *       the original error reporter contract (the class no longer implements {@code
 *       org.mozilla.javascript.ErrorReporter} because that interface has no Nashorn equivalent; the
 *       same log lines that {@code error}/{@code warning} produced are emitted from the catch
 *       blocks).
 * </ul>
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
class PSJavaScriptFunction {
  private static final Logger log = LogManager.getLogger(PSJavaScriptFunction.class);

  /**
   * Name of the JSR-223 script engine to use. On JDK 1.8 the only available engine is Nashorn; this
   * constant documents the dependency.
   */
  private static final String ENGINE_NAME = "nashorn";

  /**
   * Single shared {@link ScriptEngineManager}. {@link ScriptEngineManager} is documented as
   * thread-safe; reusing it avoids the discovery-on-every-call cost.
   */
  private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

  /**
   * Create an executable function for JavaScript extension.
   *
   * @param def the UDF extension to be compiled
   */
  PSJavaScriptFunction(IPSExtensionDef def) {
    String myKey = "";

    // Do we really need this "if" block? DVG created this for caching purpose.
    String context = def.getRef().getContext();
    if (context.length() > 0) myKey += context + "/";

    myKey += def.getRef().getExtensionName();

    Iterator iter = def.getRuntimeParameterNames();
    ArrayList params = new ArrayList();
    while (iter.hasNext()) params.add(iter.next());

    int paramCount = params.size();
    paramNames = new String[paramCount];
    // we'll copy the param values in below as we get their names

    /* for ECMAScript, we must build the function into this format:
     *
     * function f(arg1, arg2)
     * {
     *    ... body ...
     * }
     */
    StringBuilder buf = new StringBuilder();
    buf.append("function ");
    buf.append(def.getRef().getExtensionName());
    buf.append("(");
    for (int i = 0; i < paramCount; i++) {
      String paramName = (String) params.get(i);
      buf.append(paramName);
      paramNames[i] = paramName;
    }

    buf.append(") {\n");
    buf.append(def.getInitParameter("scriptBody"));
    buf.append("\n}");
    String functionText = buf.toString();
    String functionName = def.getRef().getExtensionName();

    String digestedString = digestString(myKey + functionText);

    /* first check the ConcurrentHashMap to see if we've got one already */
    synchronized (compiledFunctions) {
      if (compiledFunctions.containsKey(myKey)) {
        String digestedDef = digestedFunctionDefs.get(myKey);

        /* Make sure the function hasn't changed ... */
        if (digestedDef != null && digestedDef.equals(digestedString)) {
          myFunction = compiledFunctions.get(myKey);
          return;
        } else {
          /* Remove the function's entry in the static table, replace
          it with the new definition below ...*/
          compiledFunctions.remove(myKey);
          digestedFunctionDefs.remove(myKey);
        }
      }

      /* Javascript function representing:
       *<code>
       * function <name> ( <params> )
       * {
       *    <body>
       * }
       *</code>
       * where:
       *
       * name      = exit.getName()
       * params   = exit.getParamDefs() (an array of PSExtensionParamDef objects)
       * body      = exit.getBody()
       */
      try {
        ScriptEngine engine = ENGINE_MANAGER.getEngineByName(ENGINE_NAME);
        if (engine == null) {
          throw new IllegalStateException(
              "Nashorn script engine not available on this JDK; required for JavaScript UDFs.");
        }
        // Evaluating the function text compiles the function and registers it as a global
        // on the engine; subsequent Invocable.invokeFunction calls reuse the compiled form.
        engine.eval(functionText);
        myFunction = new CompiledFunction(engine, functionName);
        compiledFunctions.put(myKey, myFunction);
        digestedFunctionDefs.put(myKey, digestedString);
      } catch (ScriptException e) {
        // Matches the previous ErrorReporter.error() contract: log the error with line/column.
        log.error("Error in {} : {}", functionName, e.getMessage());
        log.error("  source line ({}): {}", e.getLineNumber(), e.getColumnNumber());
        myFunction = null;
      }
    }
  }

  /**
   * Convert the input raw string into a processed Java string.
   *
   * @param rawString the input raw string
   * @return the processed Java string
   */
  private String digestString(String rawString) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      md.update(rawString.getBytes(PSCharSets.rxJavaEnc()));
      byte[] digest = md.digest();

      StringBuilder buf = new StringBuilder(digest.length * 2);
      StringBuilder sTemp = new StringBuilder();
      for (byte b : digest) {
        sTemp.append(String.format("%02X", b));
        if (sTemp.length() == 1) sTemp.append("0").append(sTemp);
        else if (sTemp.length() > 2) sTemp.append(sTemp.substring(sTemp.length() - 2));

        buf.append(sTemp);
      }

      return buf.toString();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      return rawString;
    }
    // should not happen

  }

  /**
   * Get runnable context and execute the function with the supplied arguments.
   *
   * @param args an array of String parameters
   * @param req a request context object (unused on the Nashorn path; kept for binary compatibility
   *     with the original Rhino-based signature)
   * @return the execution result of the JavaScript function
   */
  public Object processUdf(Object[] args, IPSRequestContext req) {
    /* This function must have thrown a compile error, now it
    will always return null */
    if (myFunction == null) {
      return null;
    }

    try {
      ScriptEngine engine = myFunction.engine;
      Invocable invocable = (Invocable) engine;

      if (args == null) args = new Object[0]; // don't want to crash JS

      int paramCount = paramNames.length;
      int argCount = args.length;
      Object[] callArgs = new Object[paramCount];
      for (int i = 0; i < paramCount; i++) {
        Object arg = (i < argCount) ? args[i] : "";
        if (arg == null) arg = "";
        callArgs[i] = arg;
      }

      // Invoke the function. The named parameters (function f(arg1, arg2) { ... }) bind
      // positionally to callArgs; Nashorn also exposes java.util.Date via reflection so
      // JS code can call arg.getTime() on Date args.
      Object retObject = invocable.invokeFunction(myFunction.functionName, callArgs);
      retObject = convertToDate(retObject);
      return retObject;
    } catch (Exception e) {
      PSConsole.printMsg("Extension", e);
      return null;
    }
  }

  /**
   * Best-effort convert a JavaScript return value to {@link Date}, mirroring Rhino's {@code
   * Context.jsToJava(value, Date.class)} for the common cases. {@link Date} values pass through;
   * numeric values are treated as milliseconds-since-epoch; JS Date objects (wrapped in {@link
   * ScriptObjectMirror}) are unwrapped to the underlying timestamp. Other types (String, Boolean,
   * null, etc.) are returned as-is so the caller can decide.
   */
  private static Object convertToDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Date) {
      return value;
    }
    if (value instanceof Number) {
      return new Date(((Number) value).longValue());
    }
    if (value instanceof ScriptObjectMirror) {
      // Nashorn wraps JS Date as ScriptObjectMirror with a getTime() member.
      ScriptObjectMirror mirror = (ScriptObjectMirror) value;
      if (mirror.isFunction() == false && "Date".equals(mirror.getClassName())) {
        Object time = mirror.callMember("getTime");
        if (time instanceof Number) {
          return new Date(((Number) time).longValue());
        }
      }
    }
    return value;
  }

  private static final String SCOPE_CONTEXT_KEY = "PSJavaScriptScope";

  /**
   * Hash table of compiled functions where: key = appName/exitName value = the {@link
   * CompiledFunction} holding the {@link ScriptEngine} (which owns the compiled function) and the
   * function name to invoke.
   */
  private static final HashMap<String, CompiledFunction> compiledFunctions = new HashMap<>();

  private static final HashMap<String, String> digestedFunctionDefs = new HashMap<>();

  private CompiledFunction myFunction;

  /**
   * Contains all of the parameter definitions for this function. If a fct has no params, this will
   * be an array of 0 elements. Never <code>null
   * </code> once initialized in ctor.
   */
  private String[] paramNames;

  /** Holder for a compiled script engine + the function name to invoke. */
  private static final class CompiledFunction {
    final ScriptEngine engine;
    final String functionName;

    CompiledFunction(ScriptEngine engine, String functionName) {
      this.engine = engine;
      this.functionName = functionName;
    }
  }
}
