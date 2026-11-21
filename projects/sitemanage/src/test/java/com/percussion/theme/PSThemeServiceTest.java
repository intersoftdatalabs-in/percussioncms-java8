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
package com.percussion.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.percussion.share.service.IPSDataService.PSThemeNotFoundException;
import com.percussion.theme.data.PSRegionCSS;
import com.percussion.theme.data.PSRegionCSS.Property;
import com.percussion.theme.data.PSRegionCssList;
import com.percussion.theme.data.PSTheme;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.impl.PSThemeService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSThemeServiceTest {
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private PSThemeService themeService;

  @Before
  public void setup() {
    themeService = new PSThemeService();
    themeService.setThemesRootDirectory("src/test/resources/themes");
    themeService.setThemesRootRelativeUrl("/Rhythmyx/web_resources/themes");
    themeService.setThemesTempRootDirectory("src/test/resources/themes.tmp");
    themeService.setThemesTempRootRelativeUrl("/Rhythmyx/sys_resources/temp/themes");
  }

  @Test
  public void testFind() throws Exception {
    PSThemeSummary summary = themeService.find("test");
    assertNotNull(summary);
    assertNotNull(summary.getName());
    assertEquals("test", summary.getName());
    assertEquals("CSS filename", "test/perc_theme.css", summary.getCssFilePath());
    assertNotNull("Region CSS file", summary.getRegionCssFilePath());

    summary = themeService.find("more-than-one-css");
    assertNotNull(summary);
    assertNotNull(summary.getName());
    assertEquals("more-than-one-css", summary.getName());
    assertEquals(
        "CSS filename", "more-than-one-css/more-than-one-css.css", summary.getCssFilePath());
    assertNotNull("Region CSS file", summary.getRegionCssFilePath());
  }

  @Test
  public void testFindAll() throws Exception {
    List<PSThemeSummary> sums = themeService.findAll();
    assertNotNull(sums);
    assertFalse(sums.isEmpty());
    assertTrue(sums.size() == 4);
    for (PSThemeSummary sum : sums) {
      if (sum.getName().equals("test")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull("Region CSS file", sum.getRegionCssFilePath());
      } else if (sum.getName().equals("more-than-one-css")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull("Region CSS file", sum.getRegionCssFilePath());
      } else if (sum.getName().equals("more-than-one-thumb-images")) {
        assertNotNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNotNull("Region CSS file", sum.getRegionCssFilePath());
      } else if (sum.getName().equals("no-thumb-image")) {
        assertNull(sum.getThumbUrl());
        assertNotNull(sum.getCssFilePath());
        assertNull("There is no Region CSS file", sum.getRegionCssFilePath());
      }
    }
  }

  @Test
  public void testLoad() throws Exception {
    PSTheme theme = themeService.load("test");
    assertNotNull("theme", theme);
    assertNotNull("css", theme.getCSS());

    theme = themeService.load("more-than-one-css");
    assertNotNull("theme", theme);
    assertNotNull("css", theme.getCSS());
  }

  @Test
  public void testCreate() throws Exception {
    PSThemeSummary newSum = null;
    try {
      PSThemeSummary sum = themeService.find("test");
      newSum = themeService.create("mynewtheme", sum.getName());
      assertNotNull(newSum);
      assertTrue(newSum.getCssFilePath().startsWith(newSum.getName()));
      assertNotNull("Region CSS file", newSum.getRegionCssFilePath());

      PSTheme theme = themeService.load(sum.getName());
      PSTheme newTheme = themeService.load(newSum.getName());
      assertEquals(newTheme.getCSS(), theme.getCSS());

      try {
        PSThemeSummary newSum2 = themeService.create("shouldnotbecreated", "doesnotexist");
        fail("New theme '" + newSum2.getName() + "' should not have been created");
      } catch (Exception e) {
        // expected
      }
    } finally {
      if (newSum != null) {
        themeService.delete(newSum.getName());
      }
    }
  }

  @Test
  public void testDelete() throws Exception {
    PSThemeSummary sum = themeService.find("test");
    PSThemeSummary newSum = themeService.create("mynewtheme", sum.getName());
    themeService.delete(newSum.getName());
    try {
      newSum = themeService.find(newSum.getName());
      fail("Theme '" + newSum.getName() + "' should have been deleted");
    } catch (Exception e) {
      // expected
    }

    try {
      themeService.delete(newSum.getName());
    } catch (Exception e) {
      // issue CM-276
      fail(
          "Theme '"
              + newSum.getName()
              + "' does not exist anymore, but the attemp to delete it again, should not throw any exception");
    }
  }

  // --- Path-injection defense (CodeQL java/path-injection, T043) ---

  @Test
  public void safeSessionSegmentRejectsPathTraversal() {
    assertEquals("pssession", PSThemeService.safeSessionSegment(null));
    assertEquals("pssession", PSThemeService.safeSessionSegment(""));
    assertEquals("pssession", PSThemeService.safeSessionSegment("   "));
    // forward slash / backslash / parent markers are replaced with "_"
    assertEquals("a_.._b", PSThemeService.safeSessionSegment("a/../b"));
    assertEquals("a_.._b", PSThemeService.safeSessionSegment("a\\..\\b"));
    assertEquals("normal_session-1.0", PSThemeService.safeSessionSegment("normal_session-1.0"));
    // null bytes and assorted special chars are also replaced
    assertEquals("a_b", PSThemeService.safeSessionSegment("a b"));
    assertEquals("a_b", PSThemeService.safeSessionSegment("a$b"));
  }

  @Test
  public void createRejectsTraversalThemeName() {
    try {
      themeService.create("../escape", "roundedui");
      fail("expected IllegalArgumentException for traversal themeName");
    } catch (IllegalArgumentException expected) {
      // pass
    } catch (Exception other) {
      // Other exceptions (e.g. from create internals) are also acceptable as long as
      // the input was rejected before any file I/O. Re-throw to make sure the test
      // surfaces unexpected failures.
      if (!(other.getCause() instanceof IllegalArgumentException)) {
        throw new AssertionError(
            "Expected IllegalArgumentException or IllegalArgumentException cause, got "
                + other.getClass().getName(),
            other);
      }
    }
  }

  @Test
  public void createFromDefaultRejectsTraversalThemeName() {
    try {
      themeService.createFromDefault("../escape");
      fail("expected IllegalArgumentException for traversal themeName");
    } catch (IllegalArgumentException expected) {
      // pass
    } catch (Exception other) {
      if (!(other.getCause() instanceof IllegalArgumentException)) {
        throw new AssertionError(
            "Expected IllegalArgumentException or IllegalArgumentException cause, got "
                + other.getClass().getName(),
            other);
      }
    }
  }

  @Test
  public void regionCssOperationsAllowRxDeployDirWithDotDotSegmentAndRejectTraversal()
      throws Exception {
    File installDir = tempFolder.newFolder("cms-install");
    File jettyDir = new File(installDir, "jetty");
    assertTrue(jettyDir.mkdirs());
    String rxDeployDir = jettyDir.getAbsolutePath() + File.separator + "..";

    File regionCssFile =
        new File(installDir, "web_resources/themes/percussion/perc/perc_region.css");
    FileUtils.writeStringToFile(
        regionCssFile,
        "#container.perc-region { font-size: 11px; }\n"
            + "#container.perc-region #header.perc-region { font-size: 22px; }\n",
        StandardCharsets.UTF_8);

    File noRegionCssFile =
        new File(installDir, "web_resources/themes/no-region-css/perc_theme.css");
    FileUtils.writeStringToFile(noRegionCssFile, "body { margin: 0; }\n", StandardCharsets.UTF_8);

    PSThemeService svc = new PSThemeService();
    svc.setThemesRootDirectory(
        rxDeployDir + File.separator + "web_resources" + File.separator + "themes");
    svc.setThemesRootRelativeUrl("/web_resources/themes");
    svc.setThemesTempRootDirectory(
        rxDeployDir
            + File.separator
            + "sys_resources"
            + File.separator
            + "temp"
            + File.separator
            + "themes");
    svc.setThemesTempRootRelativeUrl("/sys_resources/temp/themes");
    svc.init();

    String existingUrl = svc.getCachedRegionCSSRelativeURL("percussion");
    assertEquals("pssession/percussion/perc/perc_region.css", existingUrl);

    String emptyUrl = svc.getCachedRegionCSSRelativeURL("no-region-css");
    assertEquals("pssession/no-region-css/perc/perc_region.css", emptyUrl);
    File cachedEmpty =
        new File(
            installDir, "sys_resources/temp/themes/pssession/no-region-css/perc/perc_region.css");
    assertTrue(
        FileUtils.readFileToString(cachedEmpty, StandardCharsets.UTF_8).contains(".percDummyRule"));

    svc.prepareForEditRegionCSS("percussion", "qa-blog-index");

    PSRegionCSS found = svc.getRegionCSS("percussion", "qa-blog-index", "container", "header");
    assertEquals("header", found.getRegionName());

    PSRegionCSS updated = new PSRegionCSS("container", "middle");
    updated.setProperties(Collections.singletonList(new Property("height", "25px")));
    svc.saveRegionCSS("percussion", "qa-blog-index", updated);
    assertEquals(
        "middle",
        svc.getRegionCSS("percussion", "qa-blog-index", "container", "middle").getRegionName());

    svc.deleteRegionCSS("percussion", "qa-blog-index", "container", "header");
    assertNull(
        svc.getRegionCSS("percussion", "qa-blog-index", "container", "header").getRegionName());

    svc.mergeRegionCSS("percussion", "qa-blog-index", new PSRegionCssList());
    assertTrue(
        FileUtils.readFileToString(regionCssFile, StandardCharsets.UTF_8).contains("#middle"));

    svc.clearCacheRegionCSS("percussion", "qa-blog-index");
    assertFalse(new File(installDir, "sys_resources/temp/themes/pssession").exists());

    for (String badTheme : new String[] {"../escape", "..\\escape"}) {
      try {
        svc.getCachedRegionCSSRelativeURL(badTheme);
        fail("expected IllegalArgumentException for " + badTheme);
      } catch (IllegalArgumentException expected) {
        // expected
      }
      try {
        svc.prepareForEditRegionCSS(badTheme, "qa-blog-index");
        fail("expected IllegalArgumentException for " + badTheme);
      } catch (IllegalArgumentException expected) {
        // expected
      }
      try {
        svc.getRegionCSS(badTheme, "qa-blog-index", "container", "header");
        fail("expected IllegalArgumentException for " + badTheme);
      } catch (IllegalArgumentException expected) {
        // expected
      }
    }
  }

  @Test
  public void clearCacheRegionCSSRejectsSymlinkedSessionDirectory() throws Exception {
    File installDir = tempFolder.newFolder("cms-install-symlink-cache");
    File jettyDir = new File(installDir, "jetty");
    assertTrue(jettyDir.mkdirs());
    String rxDeployDir = jettyDir.getAbsolutePath() + File.separator + "..";

    File tempRoot = new File(installDir, "sys_resources/temp/themes");
    File otherSessionDir = new File(tempRoot, "other-session");
    File keepFile = new File(otherSessionDir, "keep.css");
    FileUtils.writeStringToFile(keepFile, "/* keep */", StandardCharsets.UTF_8);

    PSThemeService svc = new PSThemeService();
    svc.setThemesTempRootDirectory(
        rxDeployDir
            + File.separator
            + "sys_resources"
            + File.separator
            + "temp"
            + File.separator
            + "themes");

    File pssessionLink = new File(tempRoot, "pssession");
    Files.createSymbolicLink(pssessionLink.toPath(), otherSessionDir.toPath());
    try {
      svc.clearCacheRegionCSS("percussion", "qa-blog-index");
      fail("expected IllegalArgumentException for symlinked session directory");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    assertTrue("Other session file must not be deleted", keepFile.exists());

    Files.delete(pssessionLink.toPath());
    Files.createSymbolicLink(pssessionLink.toPath(), tempRoot.toPath());
    try {
      svc.clearCacheRegionCSS("percussion", "qa-blog-index");
      fail("expected IllegalArgumentException for session symlink pointing to tempRoot");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    assertTrue("Temp root contents must not be deleted", keepFile.exists());
  }

  @Test
  public void findAllSkipsThemeWithEscapingRegionCssSymlink() throws Exception {
    File installDir = tempFolder.newFolder("cms-install-symlink-theme");
    File themesRoot = new File(installDir, "web_resources/themes");
    File outsideDir = tempFolder.newFolder("outside-theme-target");
    FileUtils.writeStringToFile(
        new File(outsideDir, "perc_region.css"),
        "#container.perc-region { font-size: 10px; }\n",
        StandardCharsets.UTF_8);

    FileUtils.writeStringToFile(
        new File(themesRoot, "valid-theme/perc_theme.css"), "body {}\n", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(
        new File(themesRoot, "valid-theme/perc/perc_region.css"),
        "#container.perc-region { font-size: 11px; }\n",
        StandardCharsets.UTF_8);

    File escapingThemeDir = new File(themesRoot, "escaping-theme");
    FileUtils.writeStringToFile(
        new File(escapingThemeDir, "perc_theme.css"), "body {}\n", StandardCharsets.UTF_8);
    Files.createSymbolicLink(new File(escapingThemeDir, "perc").toPath(), outsideDir.toPath());

    PSThemeService svc = new PSThemeService();
    svc.setThemesRootDirectory(themesRoot.getAbsolutePath());
    svc.setThemesRootRelativeUrl("/web_resources/themes");

    List<PSThemeSummary> summaries = svc.findAll();
    assertEquals(1, summaries.size());
    assertEquals("valid-theme", summaries.get(0).getName());

    try {
      svc.find("escaping-theme");
      fail("expected PSThemeNotFoundException for theme with escaping region CSS symlink");
    } catch (PSThemeNotFoundException expected) {
      // expected
    }
  }
}
