/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.designmanagement.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link PSFileSystemService}, verifying Finder-style leading-slash resolution and
 * path-traversal enforcement under the service root directory.
 */
public class PSFileSystemServiceTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private File rootDir;
  private PSFileSystemService service;

  @Before
  public void setUp() throws IOException {
    rootDir = tempFolder.newFolder("web_resources");
    service = new PSFileSystemService(rootDir.getAbsolutePath());
  }

  /**
   * Regression test for issue #284/#285: Finder-style leading-slash paths such as {@code "/"} and
   * {@code "/category/category.xml"} must resolve relative to {@code rootDir} on Linux instead of
   * being treated as OS-root paths.
   */
  @Test
  public void shouldResolveLeadingSlashPathsUnderRoot() throws IOException {
    assertEquals(rootDir.getCanonicalPath(), service.getFile("/").getCanonicalPath());

    File expectedCategory = new File(rootDir, "category/category.xml");
    assertEquals(
        expectedCategory.getCanonicalPath(),
        service.getFile("/category/category.xml").getCanonicalPath());

    File slashTheme = service.getFile("/themes/percussion/perc_theme.css");
    File relativeTheme = service.getFile("themes/percussion/perc_theme.css");
    assertEquals(relativeTheme.getCanonicalPath(), slashTheme.getCanonicalPath());
    assertTrue(
        slashTheme.getCanonicalPath().startsWith(rootDir.getCanonicalPath() + File.separator));
  }

  /**
   * Ensures {@link PSFileSystemService#getChildren(String)} filters only the root path {@code "/"}
   * by {@code includes}, while returning all children of subdirectories such as {@code "/themes"}.
   */
  @Test
  public void shouldFilterRootChildrenByIncludesForRootSlash() throws IOException {
    File themesDir = new File(rootDir, "themes");
    assertTrue(themesDir.mkdirs());
    File cssFile = new File(themesDir, "site.css");
    assertTrue(cssFile.createNewFile());
    File otherDir = new File(rootDir, "other");
    assertTrue(otherDir.mkdirs());

    service.setIncludes(Collections.singletonList("themes"));

    List<File> rootChildren = service.getChildren("/");
    assertEquals(1, rootChildren.size());
    assertEquals("themes", rootChildren.get(0).getName());

    List<File> themeChildren = service.getChildren("/themes");
    assertEquals(1, themeChildren.size());
    assertEquals("site.css", themeChildren.get(0).getName());
  }

  /**
   * Ensures {@link PSFileSystemService#getChildren(String)} throws {@link FileNotFoundException}
   * when the configured root directory does not exist (e.g., missing {@code rxconfig/ssh-keys}).
   */
  @Test(expected = FileNotFoundException.class)
  public void shouldThrowFileNotFoundWhenRootDoesNotExistInGetChildren()
      throws FileNotFoundException {
    File missingRoot = new File(tempFolder.getRoot(), "missing-root");
    PSFileSystemService missingService = new PSFileSystemService(missingRoot.getAbsolutePath());
    missingService.getChildren("/");
  }

  /**
   * Verifies path-traversal and NUL-byte payloads are still rejected with {@link
   * IllegalArgumentException} after leading-slash normalization.
   */
  @Test
  public void shouldBlockPathTraversalAttempts() {
    assertThrowsIllegalArgument(() -> service.getFile("/../../etc/passwd"));
    assertThrowsIllegalArgument(() -> service.getFile("../../etc/passwd"));
    assertThrowsIllegalArgument(() -> service.getFile("/themes/../../../etc/passwd"));
    assertThrowsIllegalArgument(
        () -> service.getFile("/../" + rootDir.getName() + "_other/secret.txt"));
    assertThrowsIllegalArgument(() -> service.getFile("/themes/bad\0.css"));
    assertThrowsIllegalArgument(() -> service.getChildren("/../../etc"));
  }

  private static void assertThrowsIllegalArgument(ThrowingAction action) {
    try {
      action.run();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    } catch (Exception other) {
      fail("Expected IllegalArgumentException, got " + other.getClass().getName());
    }
  }

  @FunctionalInterface
  private interface ThrowingAction {
    void run() throws Exception;
  }
}
