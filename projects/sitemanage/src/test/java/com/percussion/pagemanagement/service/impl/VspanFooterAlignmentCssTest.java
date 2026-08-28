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
 *   <li>Decoration must not declare {@code hspan_*} column widths at all, because it loads
 *       <em>before</em> the site theme in the editor/preview header (see {@code
 *       PSHTMLHeaderImporterTest}: {@code perc_decoration.css} is link index 0, {@code
 *       perc_theme.css} follows). Forcing a width would either override the default theme's fixed
 *       160/640/800/960 grid at the same cascade tier, or be defeated by an {@code !important}
 *       counter in responsive themes (the original symptom).
 * </ul>
 */
public class VspanFooterAlignmentCssTest {

  private static final Pattern VSPAN_BLOCK =
      Pattern.compile("\\.vspan_([2468])\\s*\\{([^}]*)\\}", Pattern.DOTALL);

  /** Matches any {@code .hspan_X { ... }} rule in decoration, used to assert absence. */
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

  /**
   * Canonical source of truth for the published #757 footer/sidebar min-height floors. There is
   * exactly one on disk in this repo; the test fails fast if it is missing (no soft skip).
   */
  private static final String THEME_CSS_PATH =
      "system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css";

  /** Decoration CSS paths shipped with the CMS UI. */
  private static final List<String> DECORATION_PATHS =
      Arrays.asList("WebUI/war/css/perc_decoration.css");

  @Test
  public void themeCssUsesMinHeightForVspanRegions() throws Exception {
    Path root = resolveRepoRoot();
    Path cssPath = root.resolve(THEME_CSS_PATH);
    if (!Files.exists(cssPath)) {
      fail("expected default theme CSS at " + cssPath.toAbsolutePath());
    }
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
        THEME_CSS_PATH + ": expected at least vspan_2/4/6/8 rules, found " + blocks, blocks >= 4);
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
      assertHspanNotDeclaredInDecoration(rel, css);
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
              + " must not reset min-height: 0 (kills theme floors): "
              + body.replace('\n', ' '),
          Pattern.compile("min-height\\s*:\\s*0(?![0-9])").matcher(body).find());
      assertTrue(
          rel + " .vspan_" + span + " must use height: auto: " + body.replace('\n', ' '),
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
      assertTrue(rel + " .vspan_" + e.getKey() + " count=" + e.getValue(), e.getValue() >= 1);
    }
  }

  private static void assertHspanNotDeclaredInDecoration(String rel, String css) {
    Matcher m = HSPAN_BLOCK.matcher(css);
    if (m.find()) {
      fail(
          rel
              + " must not declare .hspan_"
              + m.group(1)
              + " column widths (decoration loads before the site theme; column widths are owned"
              + " by the theme). Found: "
              + m.group(2).replace('\n', ' '));
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
