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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Unit tests for {@link ZipSlipGuard}. */
public class ZipSlipGuardTest {

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void allowsFlatFile() throws Exception {
    File root = tmp.newFolder("extract");
    File dest = ZipSlipGuard.safeDestFile(root, "foo/bar.txt");
    assertNotNull(dest);
    assertTrue(dest.getCanonicalPath().startsWith(root.getCanonicalPath()));
  }

  @Test
  public void allowsDeepNestedRelativePath() throws Exception {
    File root = tmp.newFolder("extract");
    File dest = ZipSlipGuard.safeDestFile(root, "a/b/c/d/e/f.txt");
    String expected = new File(root, "a/b/c/d/e/f.txt").getCanonicalPath();
    assertEquals(expected, dest.getCanonicalPath());
  }

  @Test
  public void rejectsParentTraversal() throws Exception {
    File root = tmp.getRoot();
    expectZipSlip(() -> ZipSlipGuard.safeDestFile(root, "../../../etc/passwd"));
  }

  @Test
  public void rejectsParentTraversalWithBackslash() throws Exception {
    File root = tmp.getRoot();
    expectZipSlip(() -> ZipSlipGuard.safeDestFile(root, "..\\..\\windows\\win.ini"));
  }

  @Test
  public void normalizesAbsolutePathToRelative() throws Exception {
    // Absolute path entries (a malformed but non-malicious zip) get leading-separator
    // stripped, then resolved as relative to extractDir. The result is contained and safe.
    File root = tmp.getRoot();
    File dest = ZipSlipGuard.safeDestFile(root, "/etc/passwd");
    assertTrue(
        "absolute path must resolve under extractDir after leading-separator strip, got: "
            + dest.getCanonicalPath(),
        dest.getCanonicalPath().startsWith(root.getCanonicalPath()));
  }

  @Test
  public void rejectsTraversalAtEnd() throws Exception {
    File root = tmp.getRoot();
    expectZipSlip(() -> ZipSlipGuard.safeDestFile(root, "ok/../../escape"));
  }

  @Test
  public void rejectsNullExtractDir() {
    try {
      ZipSlipGuard.safeDestFile(null, "x");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    } catch (Exception other) {
      fail("expected IllegalArgumentException, got: " + other.getClass().getName());
    }
  }

  @Test
  public void rejectsNullEntryName() throws Exception {
    File root = tmp.getRoot();
    try {
      ZipSlipGuard.safeDestFile(root, null);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  @Test
  public void rejectsEmptyEntryName() throws Exception {
    File root = tmp.getRoot();
    try {
      ZipSlipGuard.safeDestFile(root, "");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  private static void expectZipSlip(ThrowingRunnable r) throws Exception {
    try {
      r.run();
      fail("expected SecurityException");
    } catch (SecurityException expected) {
      assertTrue(
          "expected message to contain 'Zip slip', got: " + expected.getMessage(),
          expected.getMessage().contains("Zip slip"));
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
