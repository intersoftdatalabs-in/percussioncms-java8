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
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

/**
 * Nashorn-based regression tests for {@code PercRssView.js} covering issue #389 (stripping HTML
 * markup from feed item descriptions when {@code itemRemoveHtml} is enabled while preserving
 * HTML-escaped output and accurate truncation).
 */
public class PercRssViewJsTest {

  private ScriptEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    engine.put("__jsoupHelper", new JsoupDomHelper());
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
        "var document = {};\n"
            + "var __readyCallbacks = [];\n"
            + "var __feedResult = { title: 'Test Feed', link: 'https://example.com', items: [] };\n"
            + "\n"
            + "var DOMParser = function() {};\n"
            + "DOMParser.prototype.parseFromString = function(html, type) {\n"
            + "  var helperBody = __jsoupHelper.parseHtml(html);\n"
            + "  var body = {\n"
            + "    querySelectorAll: function(sel) {\n"
            + "      var arr = helperBody.querySelectorAll(sel);\n"
            + "      var res = [];\n"
            + "      for (var i = 0; i < arr.length; i++) {\n"
            + "        (function(el) {\n"
            + "          res.push({\n"
            + "            parentNode: {\n"
            + "              removeChild: function(child) { el.remove(); }\n"
            + "            }\n"
            + "          });\n"
            + "        })(arr[i]);\n"
            + "      }\n"
            + "      return res;\n"
            + "    }\n"
            + "  };\n"
            + "  Object.defineProperty(body, 'textContent', {\n"
            + "    get: function() { return helperBody.getTextContent(); }\n"
            + "  });\n"
            + "  return { body: body };\n"
            + "};\n"
            + "\n"
            + "function JFeed(result) {\n"
            + "  return result;\n"
            + "}\n"
            + "\n"
            + "function escapeTextForHtml(s) {\n"
            + "  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');\n"
            + "}\n"
            + "\n"
            + "function makeWidget() {\n"
            + "  return {\n"
            + "    _attrs: {},\n"
            + "    _appended: [],\n"
            + "    attr: function(k, v) {\n"
            + "      if (arguments.length === 1) { return this._attrs[k]; }\n"
            + "      this._attrs[k] = v;\n"
            + "      return this;\n"
            + "    },\n"
            + "    append: function(html) {\n"
            + "      this._appended.push(String(html));\n"
            + "      return this;\n"
            + "    },\n"
            + "    each: function(fn) {\n"
            + "      fn.call(this, 0, this);\n"
            + "      return this;\n"
            + "    }\n"
            + "  };\n"
            + "}\n"
            + "\n"
            + "var __widget = makeWidget();\n"
            + "\n"
            + "var jQuery = function(arg) {\n"
            + "  if (arg === document) {\n"
            + "    return { ready: function(fn) { __readyCallbacks.push(fn); } };\n"
            + "  }\n"
            + "  if (arg === '.perc-feed-widget' || arg === __widget) {\n"
            + "    return __widget;\n"
            + "  }\n"
            + "  if (arg === '<div>') {\n"
            + "    var holder = {\n"
            + "      _text: '',\n"
            + "      text: function(val) { this._text = String(val); return this; },\n"
            + "      html: function() { return escapeTextForHtml(this._text); }\n"
            + "    };\n"
            + "    return holder;\n"
            + "  }\n"
            + "  return makeWidget();\n"
            + "};\n"
            + "jQuery.PercRssService = {\n"
            + "  getExternalFeed: function(urlFeed, queryString, callback) {\n"
            + "    callback(true, __feedResult);\n"
            + "  }\n"
            + "};\n"
            + "var $ = jQuery;\n"
            + "\n"
            + "function __renderRss(queryObj, description) {\n"
            + "  __widget = makeWidget();\n"
            + "  __widget.attr('data-query', JSON.stringify(queryObj));\n"
            + "  __feedResult = {\n"
            + "    title: 'Feed Title',\n"
            + "    link: 'https://example.com/feed',\n"
            + "    items: [{\n"
            + "      title: 'Item 1',\n"
            + "      link: 'https://example.com/1',\n"
            + "      description: description\n"
            + "    }]\n"
            + "  };\n"
            + "  $.PercRssView.updateRssFeed();\n"
            + "  return __widget._appended.join('');\n"
            + "}\n";
    engine.eval(stub);
  }

  private String renderDescription(
      Object itemRemoveHtml, boolean itemDescriptionEmpty, int maxLen, String description)
      throws ScriptException {
    engine.put("__qRemoveHtml", itemRemoveHtml);
    engine.put("__qDescEmpty", itemDescriptionEmpty);
    engine.put("__qDescLen", maxLen);
    engine.put("__desc", description);
    return String.valueOf(
        engine.eval(
            "__renderRss({"
                + "feedId: 'test_feed',"
                + "urlFeed: 'enc',"
                + "itemLimit: 'limit_5',"
                + "showTitle: false,"
                + "showItemTitle: false,"
                + "showItemDate: false,"
                + "showItemDescription: true,"
                + "itemRemoveHtml: __qRemoveHtml,"
                + "itemDescriptionEmpty: __qDescEmpty,"
                + "itemDescriptionLength: __qDescLen,"
                + "wrappingElement: 'none',"
                + "itemElement: 'div',"
                + "itemDescriptionElement: 'div'"
                + "}, __desc);"));
  }

  @Test
  public void testRemoveHtmlTrueStripsMarkupAndTruncatesPlainText() throws ScriptException {
    String longDesc =
        "<b>Bold</b> Long description that goes on and on past one hundred characters to verify"
            + " truncation works cleanly.";
    String html = renderDescription(Boolean.TRUE, false, 100, longDesc);

    String expectedText =
        "Bold Long description that goes on and on past one hundred characters to verify"
            + " truncation works cle...";
    assertTrue(
        "Expected stripped and truncated description in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">" + expectedText + "</div>"));
    assertFalse("Did not expect literal <b> tag in: " + html, html.contains("<b>"));
    assertFalse("Did not expect escaped &lt;b&gt; in: " + html, html.contains("&lt;b&gt;"));
  }

  @Test
  public void testRemoveHtmlTrueDoesNotAddEllipsisWhenStrippedTextFitsWithinLimit()
      throws ScriptException {
    String markup = "<p><strong>Short</strong> <em>text</em></p>";
    String html = renderDescription(Boolean.TRUE, false, 15, markup);

    assertTrue(
        "Expected exact stripped text without ellipsis in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">Short text</div>"));
  }

  @Test
  public void testRemoveHtmlTrueStripsScriptAndStyleElements() throws ScriptException {
    String markup =
        "Hello <script>alert(1)</script><style>.a{color:red}</style>"
            + "<img src=\"x\" onerror=\"alert(2)\">world";
    String html = renderDescription(Boolean.TRUE, true, 100, markup);

    assertTrue(
        "Expected script/style/img stripped in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">Hello world</div>"));
  }

  @Test
  public void testRemoveHtmlTrueEscapesDecodedHtmlEntitiesSafely() throws ScriptException {
    String markup = "<p>Tom &amp; Jerry &lt;3&gt;</p>";
    String html = renderDescription(Boolean.TRUE, true, 100, markup);

    assertTrue(
        "Expected entities re-escaped safely in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">Tom &amp; Jerry &lt;3&gt;</div>"));
  }

  @Test
  public void testRemoveHtmlFalsePreservesEscapedMarkupText() throws ScriptException {
    String markup = "<b>Bold</b> text";
    String html = renderDescription(Boolean.FALSE, true, 100, markup);

    assertTrue(
        "Expected escaped markup text when itemRemoveHtml is false in: " + html,
        html.contains(
            "<div class=\"perc-feed-item-description\">&lt;b&gt;Bold&lt;/b&gt; text</div>"));
  }

  @Test
  public void testRemoveHtmlStringFalseDoesNotStripHtml() throws ScriptException {
    String markup = "<i>Italic</i>";
    String html = renderDescription("false", true, 100, markup);

    assertTrue(
        "Expected escaped markup text when itemRemoveHtml is 'false' in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">&lt;i&gt;Italic&lt;/i&gt;</div>"));
  }

  @Test
  public void testFallbackWithoutDomParserStillEscapesSafely() throws ScriptException {
    engine.eval("DOMParser = undefined;");
    String html = renderDescription(Boolean.TRUE, true, 100, "<b>Safe</b>");

    assertTrue(
        "Expected safe escaped output without DOMParser in: " + html,
        html.contains("<div class=\"perc-feed-item-description\">&lt;b&gt;Safe&lt;/b&gt;</div>"));
    assertEquals("false", String.valueOf(engine.eval("__widget.attr('aria-busy')")));
  }

  public static class JsoupDomHelper {
    public JsoupBody parseHtml(String html) {
      return new JsoupBody(Jsoup.parse(html));
    }
  }

  public static class JsoupBody {
    private final Document doc;

    public JsoupBody(Document doc) {
      this.doc = doc;
    }

    public JsoupElement[] querySelectorAll(String selector) {
      Elements elements = doc.body().select(selector);
      JsoupElement[] result = new JsoupElement[elements.size()];
      for (int i = 0; i < elements.size(); i++) {
        result[i] = new JsoupElement(elements.get(i));
      }
      return result;
    }

    public String getTextContent() {
      StringBuilder sb = new StringBuilder();
      appendTextContent(doc.body(), sb);
      return sb.toString();
    }

    private static void appendTextContent(Node node, StringBuilder sb) {
      if (node instanceof TextNode) {
        sb.append(((TextNode) node).getWholeText());
        return;
      }
      if (node instanceof DataNode) {
        sb.append(((DataNode) node).getWholeData());
        return;
      }
      List<Node> children = node.childNodes();
      for (Node child : children) {
        appendTextContent(child, sb);
      }
    }
  }

  public static class JsoupElement {
    private final Element element;

    public JsoupElement(Element element) {
      this.element = element;
    }

    public void remove() {
      element.remove();
    }
  }
}
