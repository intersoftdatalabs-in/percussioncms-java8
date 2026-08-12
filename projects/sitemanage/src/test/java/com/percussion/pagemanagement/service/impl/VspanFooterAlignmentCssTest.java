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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pagemanagement.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Regression for GH-757 / issue #2 (8.1.x java8 backport of GH-2352):
 *
 * <ul>
 *   <li>Published default theme {@code vspan_*} regions use {@code min-height} so sidebars grow
 *       with content (footer stays below) &mdash; GH-757.
 *   <li>Editor/preview decoration must <strong>not</strong> force the legacy fixed pixel grid with
 *       {@code !important} (or hard-coded px heights/widths) so responsive customer themes that set
 *       {@code height/width: auto} win in the CMS chrome &mdash; issue #2 / GH-2352.
 * </ul>
 */
public class VspanFooterAlignmentCssTest {

  private static final Pattern VSPAN_BLOCK =
      Pattern.compile("\\.vspan_([2468])\\s*\\{([^}]*)\\}", Pattern.DOTALL);

  private static final Pattern HSPAN_BLOCK =
      Pattern.compile("\\.hspan_(2|8|10|12)\\s*\\{([^}]*)\\}", Pattern.DOTALL);

  /** Design floor heights for empty vspan regions (px), keyed by span index (default theme). */
  private static final Map<String, String> VSPAN_FLOOR_PX =
      new LinkedHashMap<String, String>() {
        {
          put("2", "120");
          put("4", "240");
          put("6", "360");
          put("8", "480");
        }
      };

  /** Theme CSS paths that own the published #757 footer/sidebar min-height floors. */
  private static final List<String> THEME_PATHS =
      Arrays.asList(
          "system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css",
          "system/bin/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css");

  /** Decoration CSS paths shipped with the CMS UI. */
  private static final List<String> DECORATION_PATHS =
      Arrays.asList("WebUI/war/css/perc_decoration.css");

  private static final List<String> HSPAN_KEYS = Arrays.asList("2", "8", "10", "12");

  @Test
  public void themeCssUsesMinHeightForVspanRegions() throws Exception {
    Path root = resolveRepoRoot();
    boolean sawAtLeastOne = false;
    for (String rel : THEME_PATHS) {
      Path cssPath = root.resolve(rel);
      if (!Files.exists(cssPath)) {
        continue;
      }
      sawAtLeastOne = true;
      String css = readFile(cssPath);
      Matcher m = VSPAN_BLOCK.matcher(css);
      int blocks = 0;
      while (m.find()) {
        blocks++;
        String span = m.group(1);
        String body = m.group(2);
        String floor = VSPAN_FLOOR_PX.get(span);
        assertTrue(
            "theme.css .vspan_"
                + span
                + " must use min-height: "
                + floor
                + "px: "
                + body.replace('\n', ' '),
            Pattern.compile("min-height\\s*:\\s*" + floor + "px").matcher(body).find());
      }
      assertTrue(
          rel + ": expected at least vspan_2/4/6/8 rules, found " + blocks,
          blocks >= 4);
    }
    assertTrue("expected to find at least one theme.css on disk", sawAtLeastOne);
  }

  @Test
  public void decorationCssDoesNotForceFixedPixelGridWithImportant() throws Exception {
    Path root = resolveRepoRoot();
    for (String rel : DECORATION_PATHS) {
      Path cssPath = root.resolve(rel);
      if (!Files.exists(cssPath)) {
        fail("expected decoration CSS at " + cssPath.toAbsolutePath());
      }
      String css = readFile(cssPath);
      assertVspanAllowsResponsiveThemes(rel, css);
      assertHspanAllowsResponsiveThemes(rel, css);
    }
  }

  private static void assertVspanAllowsResponsiveThemes(String rel, String css) {
    Matcher m = VSPAN_BLOCK.matcher(css);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : VSPAN_FLOOR_PX.keySet()) {
      counts.put(k, 0);
    }
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String span = m.group(1);
      String body = m.group(2);
      assertFalse(
          rel
              + " .vspan_"
              + span
              + " must not force height with !important: "
              + body.replace('\n', ' '),
          Pattern.compile("(?<!min-|max-)height\\s*:[^;!]*!important").matcher(body).find());
      assertFalse(
          rel
              + " .vspan_"
              + span
              + " must not reset min-height: 0 !important (kills theme floors): "
              + body.replace('\n', ' '),
          Pattern.compile("min-height\\s*:\\s*0\\s*!important").matcher(body).find());
      assertTrue(
          rel
              + " .vspan_"
              + span
              + " must use height: auto: "
              + body.replace('\n', ' '),
          Pattern.compile("(?<!min-|max-)height\\s*:\\s*auto").matcher(body).find());
      String floor = VSPAN_FLOOR_PX.get(span);
      assertFalse(
          rel
              + " .vspan_"
              + span
              + " must not set fixed height: "
              + floor
              + "px: "
              + body.replace('\n', ' '),
          Pattern.compile("(?<!min-|max-)height\\s*:\\s*" + floor + "px").matcher(body).find());
      counts.merge(span, 1, Integer::sum);
    }
    assertTrue(rel + ": expected at least vspan_2/4/6/8 rules, found " + blocks, blocks >= 4);
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(
          rel + " .vspan_" + e.getKey() + " count=" + e.getValue(),
          e.getValue() >= 1);
    }
  }

  private static void assertHspanAllowsResponsiveThemes(String rel, String css) {
    Matcher m = HSPAN_BLOCK.matcher(css);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : HSPAN_KEYS) {
      counts.put(k, 0);
    }
    int blocks = 0;
    while (m.find()) {
      blocks++;
      String span = m.group(1);
      String body = m.group(2);
      assertFalse(
          rel
              + " .hspan_"
              + span
              + " must not force width with !important: "
              + body.replace('\n', ' '),
          Pattern.compile("width\\s*:[^;!]*!important").matcher(body).find());
      assertTrue(
          rel
              + " .hspan_"
              + span
              + " must use width: auto: "
              + body.replace('\n', ' '),
          Pattern.compile("width\\s*:\\s*auto").matcher(body).find());
      assertFalse(
          rel
              + " .hspan_"
              + span
              + " must not set fixed legacy grid width: "
              + body.replace('\n', ' '),
          Pattern.compile("width\\s*:\\s*(160|640|800|960)px").matcher(body).find());
      counts.merge(span, 1, Integer::sum);
    }
    assertTrue(rel + ": expected at least hspan_2/8/10/12 rules, found " + blocks, blocks >= 4);
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(
          rel + " .hspan_" + e.getKey() + " count=" + e.getValue(),
          e.getValue() >= 1);
    }
  }

  private static String readFile(Path path) throws Exception {
    byte[] bytes = Files.readAllBytes(path);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static Path resolveRepoRoot() {
    File f = new File("").getAbsoluteFile();
    for (int i = 0; i < 6 && f != null; i++, f = f.getParentFile()) {
      File webui = new File(f, "WebUI");
      File projects = new File(f, "projects");
      if (webui.exists() && projects.exists()) {
        return Paths.get(f.toURI());
      }
    }
    return Paths.get("").toAbsolutePath();
  }
}