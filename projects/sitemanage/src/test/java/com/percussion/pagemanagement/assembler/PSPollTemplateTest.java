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
package com.percussion.pagemanagement.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.junit.Test;

/**
 * Regression test for issue #376: verifies {@code percPoll.xml} Velocity template renders poll
 * answers using {@code asText()} rather than {@code TextNode.toString()} literal quotation marks.
 */
public class PSPollTemplateTest {

  private static final String WIDGET_REL_PATH =
      "system/Packages/perc.widget.poll/sys__UserDependency--rxconfig/Widgets/percPoll.xml";
  private static final String VELOCITY_PROPS_REL_PATH =
      "system/Tools/velocity/config/velocity.properties";

  private static final Pattern VELOCITY_CONTENT_PATTERN =
      Pattern.compile(
          "<Content\\s+type=\"velocity\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Content>",
          Pattern.DOTALL);

  @Test
  public void shouldRenderAnswersWithoutQuotationMarksInVoteFormAndResults() throws Exception {
    String jsonPayload =
        "{\"question\":\"Favorite color?\","
            + "\"answerType\":\"single\","
            + "\"answerChoices\":[\"Red 4922087\",\"Green 4922087\",\"Blue 4922087\"]}";

    String html = renderTemplate(jsonPayload);

    assertTrue(html.contains("type=\"radio\""));
    assertTrue(html.contains("<div class=\"perc-poll-question\">Favorite color?</div>"));
    assertEquals(2, countOccurrences(html, "<span class=\"perc-poll-answer\">Red 4922087</span>"));
    assertEquals(
        2, countOccurrences(html, "<span class=\"perc-poll-answer\">Green 4922087</span>"));
    assertEquals(2, countOccurrences(html, "<span class=\"perc-poll-answer\">Blue 4922087</span>"));

    assertFalse(html.contains("\"Red 4922087\""));
    assertFalse(html.contains("\"Green 4922087\""));
    assertFalse(html.contains("\"Blue 4922087\""));
    assertFalse(html.contains("$ans"));
    assertFalse(html.contains("$!{"));
  }

  @Test
  public void shouldRenderMultiChoiceAnswersWithInternalQuotesWithoutJsonEscaping()
      throws Exception {
    String jsonPayload =
        "{\"question\":\"Pick all\","
            + "\"answerType\":\"multi\","
            + "\"answerChoices\":[\"Say \\\"Yes\\\"\",\"No \\\\ Maybe\"]}";

    String html = renderTemplate(jsonPayload);

    assertTrue(html.contains("type=\"checkbox\""));
    assertEquals(2, countOccurrences(html, "<span class=\"perc-poll-answer\">Say \"Yes\"</span>"));
    assertEquals(2, countOccurrences(html, "<span class=\"perc-poll-answer\">No \\ Maybe</span>"));
    assertFalse(html.contains("\\\"Yes\\\""));
    assertFalse(html.contains("\\\\ Maybe"));
  }

  @Test
  public void shouldRenderCleanlyWhenAnswerChoicesAreMissing() throws Exception {
    String jsonPayload = "{\"question\":\"Empty\",\"answerType\":\"single\"}";

    String html = renderTemplate(jsonPayload);

    assertTrue(html.contains("<div class=\"perc-poll-question\">Empty</div>"));
    assertEquals(0, countOccurrences(html, "<span class=\"perc-poll-answer\">"));
    assertFalse(html.contains("$ans"));
  }

  private static String renderTemplate(String pollQuestionJson) throws Exception {
    File widgetFile = resolveRepoFile(WIDGET_REL_PATH);
    File velocityPropsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

    String xml = new String(Files.readAllBytes(widgetFile.toPath()), StandardCharsets.UTF_8);
    Matcher matcher = VELOCITY_CONTENT_PATTERN.matcher(xml);
    assertTrue("Velocity <Content> section not found in " + widgetFile, matcher.find());
    String velocityTemplate = matcher.group(1);

    RuntimeInstance runtime = new RuntimeInstance();
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(velocityPropsFile)) {
      props.load(in);
    }
    runtime.init(props);

    PSPageUtils pageUtils = new PSPageUtils();
    ObjectNode qJson = pageUtils.createJsonObject(pollQuestionJson);
    String question = qJson.path("question").asText();
    String answerType = qJson.path("answerType").asText();
    String inputType = "multi".equals(answerType) ? "checkbox" : "radio";
    JsonNode ansArray = qJson.path("answerChoices");

    VelocityContext ctx = new VelocityContext();
    ctx.put("perc", new PercHelper());
    ctx.put("rootclass", "");
    ctx.put("pollName", "test-poll");
    ctx.put("pollTitle", "Test Poll");
    ctx.put("restrictionType", "Unrestricted");
    ctx.put("dsUrl", "http://localhost:9980");
    ctx.put("question", question);
    ctx.put("inputType", inputType);
    ctx.put("ansArray", ansArray);
    ctx.put("pollSubmitLabel", "Vote");
    ctx.put("viewResultsLink", Boolean.TRUE);
    ctx.put("viewResultsLinkText", "View results");
    ctx.put("validationText", "Please choose an answer first!");
    ctx.put("totalVotesText", "Total votes");
    ctx.put("backToPollText", "Back to poll");

    StringWriter writer = new StringWriter();
    runtime.evaluate(ctx, writer, "percPoll", velocityTemplate);
    return writer.toString();
  }

  private static int countOccurrences(String text, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = text.indexOf(needle, idx)) != -1) {
      count++;
      idx += needle.length();
    }
    return count;
  }

  private static File resolveRepoFile(String relativePath) throws IOException {
    File fromModule = new File(new File("../.."), relativePath);
    if (fromModule.exists()) {
      return fromModule;
    }
    File fromRoot = new File(relativePath);
    if (fromRoot.exists()) {
      return fromRoot;
    }
    throw new IOException(
        "Could not locate " + relativePath + " from " + new File(".").getAbsolutePath());
  }

  public static class PercHelper {
    public List<Object> getWidgetContents() {
      return Collections.singletonList(new Object());
    }

    public boolean isEditMode() {
      return false;
    }

    public boolean isPreviewMode() {
      return false;
    }
  }
}
