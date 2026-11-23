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
 * Nashorn-based regression tests for {@code PercRssView.js} covering issue #389 (RSS "Remove HTML
 * from fields before displaying" having no effect on feed item descriptions).
 */
public class PercRssWidgetJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    installDomAndJQueryStub();
    evalScript(resolveDeliveryJs("views/PercRssView.js"));
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
        "var __readyCallbacks = [];\n"
            + "var __appendedHtml = [];\n"
            + "var __widgetAttrs = {};\n"
            + "var __queryConfig = {};\n"
            + "var __mockFeed = { title: 'Test Feed', link: 'https://example.com', items: [] };\n"
            + "var document = {};\n"
            + "function escapeTextForStub(val) {\n"
            + "  var s = (val === undefined || val === null) ? '' : String(val);\n"
            + "  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');\n"
            + "}\n"
            + "function makeDivElement() {\n"
            + "  return {\n"
            + "    _text: '',\n"
            + "    text: function(val) {\n"
            + "      if (arguments.length === 0) { return this._text; }\n"
            + "      this._text = String(val);\n"
            + "      return this;\n"
            + "    },\n"
            + "    html: function() {\n"
            + "      return escapeTextForStub(this._text);\n"
            + "    }\n"
            + "  };\n"
            + "}\n"
            + "var __widget = {\n"
            + "  attr: function(k, v) {\n"
            + "    if (arguments.length === 1) {\n"
            + "      if (k === 'data-query') { return JSON.stringify(__queryConfig); }\n"
            + "      return __widgetAttrs[k];\n"
            + "    }\n"
            + "    __widgetAttrs[k] = v;\n"
            + "    return this;\n"
            + "  },\n"
            + "  append: function(html) {\n"
            + "    __appendedHtml.push(String(html));\n"
            + "    return this;\n"
            + "  }\n"
            + "};\n"
            + "function jQuery(sel) {\n"
            + "  if (sel === __widget) {\n"
            + "    return __widget;\n"
            + "  }\n"
            + "  if (sel === document) {\n"
            + "    return {\n"
            + "      ready: function(fn) { __readyCallbacks.push(fn); }\n"
            + "    };\n"
            + "  }\n"
            + "  if (sel === '.perc-feed-widget') {\n"
            + "    return {\n"
            + "      each: function(fn) { fn.call(__widget); }\n"
            + "    };\n"
            + "  }\n"
            + "  if (sel === '<div>') {\n"
            + "    return makeDivElement();\n"
            + "  }\n"
            + "  return makeDivElement();\n"
            + "}\n"
            + "jQuery.datepicker = {\n"
            + "  formatDate: function(fmt, date) { return 'Mon Jan 01, 2026 at hh:nn'; }\n"
            + "};\n"
            + "jQuery.PercRssService = {\n"
            + "  getExternalFeed: function(urlFeed, queryString, callback) {\n"
            + "    callback(true, __mockFeed);\n"
            + "  }\n"
            + "};\n"
            + "var $ = jQuery;\n"
            + "function JFeed(result) {\n"
            + "  this.title = result.title;\n"
            + "  this.link = result.link;\n"
            + "  this.items = result.items;\n"
            + "}\n"
            + "function __renderDescription(description, overrides) {\n"
            + "  __appendedHtml = [];\n"
            + "  __widgetAttrs = {};\n"
            + "  __queryConfig = {\n"
            + "    feedId: 'test_feed',\n"
            + "    urlFeed: 'encryptedUrl',\n"
            + "    itemLimit: 'limit_1',\n"
            + "    showTitle: false,\n"
            + "    showItemTitle: false,\n"
            + "    showItemDate: false,\n"
            + "    showItemDescription: true,\n"
            + "    itemRemoveHtml: true,\n"
            + "    itemDescriptionLength: 100,\n"
            + "    itemDescriptionEmpty: false,\n"
            + "    itemDateFormat: \"D M d, yy 'at' hh:nn\",\n"
            + "    wrappingElement: 'none',\n"
            + "    titleElement: 'div',\n"
            + "    itemElement: 'div',\n"
            + "    itemTitleElement: 'div',\n"
            + "    itemDateElement: 'div',\n"
            + "    itemDescriptionElement: 'div',\n"
            + "    wrappingElemAriaLabel: ''\n"
            + "  };\n"
            + "  if (overrides) {\n"
            + "    for (var k in overrides) {\n"
            + "      if (Object.prototype.hasOwnProperty.call(overrides, k)) {\n"
            + "        __queryConfig[k] = overrides[k];\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "  __mockFeed = {\n"
            + "    title: 'Test Feed',\n"
            + "    link: 'https://example.com',\n"
            + "    items: [{ title: 'Item 1', link: 'https://example.com/1', description: description }]\n"
            + "  };\n"
            + "  $.PercRssView.updateRssFeed();\n"
            + "  return __appendedHtml.join('');\n"
            + "}\n"
            + "function __extractDescriptionInnerHtml(renderedHtml) {\n"
            + "  var start = renderedHtml.indexOf('class=\"perc-feed-item-description\">');\n"
            + "  if (start === -1) { return ''; }\n"
            + "  start += 'class=\"perc-feed-item-description\">'.length;\n"
            + "  var end = renderedHtml.indexOf('</div>', start);\n"
            + "  return renderedHtml.substring(start, end);\n"
            + "}\n";
    engine.eval(stub);
  }

  private String renderDescription(String description, String overridesJs) throws ScriptException {
    engine.put("__descInput", description);
    return (String)
        engine.eval(
            "__extractDescriptionInnerHtml(__renderDescription(__descInput, " + overridesJs + "))");
  }

  @Test
  public void testDefaultOptionsStripHtmlAndTruncateTo100Chars() throws ScriptException {
    String raw =
        "<b>Bold</b> Long description that goes on and on and on and on and on and on and on and"
            + " on and on and on and beyond one hundred characters";
    String actual = renderDescription(raw, "null");
    assertFalse("HTML tags must be stripped", actual.contains("<b>"));
    assertFalse("HTML tags must be escaped/removed", actual.contains("&lt;b&gt;"));
    assertTrue("Expected stripped prefix", actual.startsWith("Bold Long description"));
    assertTrue("Expected ellipsis after truncation", actual.endsWith("..."));
    assertEquals(103, actual.length());
  }

  @Test
  public void testShortHtmlDescriptionDoesNotAppendSpuriousEllipsis() throws ScriptException {
    String raw =
        "<p class=\"summary-paragraph-with-long-attribute-name\">"
            + "<b>Short</b> summary text</p>";
    String actual = renderDescription(raw, "{ itemDescriptionLength: 40 }");
    assertEquals("Short summary text", actual);
  }

  @Test
  public void testExactBoundaryLengthTruncation() throws ScriptException {
    String exact = "<p>1234567890</p>";
    assertEquals("1234567890", renderDescription(exact, "{ itemDescriptionLength: 10 }"));

    String overByOne = "<p>12345678901</p>";
    assertEquals("1234567890...", renderDescription(overByOne, "{ itemDescriptionLength: 10 }"));
  }

  @Test
  public void testItemDescriptionEmptyTrueSkipsTruncation() throws ScriptException {
    String raw =
        "<div><b>Full</b> "
            + "12345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890"
            + " end</div>";
    String actual = renderDescription(raw, "{ itemDescriptionEmpty: true }");
    assertTrue(actual.startsWith("Full 1234567890"));
    assertTrue(actual.endsWith(" end"));
    assertFalse(actual.endsWith("..."));
  }

  @Test
  public void testItemRemoveHtmlFalsePreservesEscapedTags() throws ScriptException {
    String raw = "<b>Bold</b> text";
    assertEquals(
        "&lt;b&gt;Bold&lt;/b&gt; text", renderDescription(raw, "{ itemRemoveHtml: false }"));
    assertEquals(
        "&lt;b&gt;Bold&lt;/b&gt; text", renderDescription(raw, "{ itemRemoveHtml: 'false' }"));
  }

  @Test
  public void testItemRemoveHtmlStringTrueStripsHtml() throws ScriptException {
    String raw = "<b>Bold</b> text";
    assertEquals("Bold text", renderDescription(raw, "{ itemRemoveHtml: 'true' }"));
  }

  @Test
  public void testNestedSelfClosingQuotedAttributesApostrophesAndComments() throws ScriptException {
    String raw =
        "<!-- hidden --><div><span>Hello</span><br/><a title=\"a > b\" href='c > d'>"
            + "World</a> <a title=Bob's>Link</a></div>";
    String actual = renderDescription(raw, "{ itemDescriptionEmpty: true }");
    assertEquals("HelloWorld Link", actual);
  }

  @Test
  public void testXssPayloadsAndRawTextElementsAreStrippedSafely() throws ScriptException {
    assertEquals(
        "Safe",
        renderDescription("<img src=x onerror=alert(1)>Safe", "{ itemDescriptionEmpty: true }"));
    assertEquals(
        "Safe",
        renderDescription("<script>alert(1)</script>Safe", "{ itemDescriptionEmpty: true }"));
    assertEquals(
        "Safe",
        renderDescription("<style>.a{color:red}</style>Safe", "{ itemDescriptionEmpty: true }"));
    assertEquals(
        "Safe",
        renderDescription("<noscript>hidden</noscript>Safe", "{ itemDescriptionEmpty: true }"));
  }

  @Test
  public void testHtmlEntitiesDecodedOnceAndReescapedSafely() throws ScriptException {
    String raw =
        "<p>Tom &amp; Jerry &mdash; &#8217;quote&#x2019; &quot;hi&quot; &lt;b&gt;tag&lt;/b&gt;</p>";
    String actual = renderDescription(raw, "{ itemDescriptionEmpty: true }");
    assertEquals(
        "Tom &amp; Jerry \u2014 \u2019quote\u2019 &quot;hi&quot; &lt;b&gt;tag&lt;/b&gt;", actual);
    assertFalse("Decoded &lt;b&gt; must not become raw <b>", actual.contains("<b>"));
  }

  @Test
  public void testPlainTextComparisonsArePreserved() throws ScriptException {
    String raw = "<b>Note:</b> 2 < 3 and 5 > 4";
    String actual = renderDescription(raw, "{ itemDescriptionEmpty: true }");
    assertEquals("Note: 2 &lt; 3 and 5 &gt; 4", actual);
  }

  @Test
  public void testTagsOnlyAndShowItemDescriptionFalse() throws ScriptException {
    assertEquals("", renderDescription("<b></b>", "{ itemDescriptionEmpty: true }"));
    String fullHtml =
        (String)
            engine.eval("__renderDescription('<b>Visible</b>', { showItemDescription: false })");
    assertFalse(fullHtml.contains("perc-feed-item-description"));
  }
}
