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

import java.io.File;
import java.io.IOException;

/**
 * Zip-slip (CWE-22) defenses for archive extraction (CodeQL java/zipslip). Each entry name from an
 * attacker-controlled archive must be validated against the trusted extract directory before any
 * file or directory creation, otherwise an entry like {@code ../../../etc/passwd} escapes the
 * extract root.
 *
 * <p>This is a small, focused subset of the development-branch {@code PathValidation} helper,
 * scoped to the eight zip-slip sinks on the 8.1.x branch. The full {@code PathValidation}
 * (constructSafePath, combineSafePaths, validatePathWithinDirectory, etc.) lives on the development
 * branch and will be brought forward when Task 6 path-injection residual work begins.
 *
 * <p>All methods throw {@link SecurityException} on a containment violation; callers that need a
 * recoverable signal should catch and translate.
 */
public final class ZipSlipGuard {

  private ZipSlipGuard() {}

  /**
   * Assert that {@code entryName} resolves to a path strictly within {@code extractDir} (or to
   * {@code extractDir} itself for the directory marker entry).
   *
   * @param extractDir the trusted extraction root, must be an existing directory
   * @param entryName the archive entry name (forward slashes per zip spec; backslashes tolerated
   *     because some installers emit Windows-style names)
   * @return the canonical {@link File} for the safe destination
   * @throws IOException if canonicalization fails
   * @throws SecurityException if {@code entryName} resolves outside {@code extractDir}
   */
  public static File safeDestFile(File extractDir, String entryName) throws IOException {
    if (extractDir == null) {
      throw new IllegalArgumentException("extractDir cannot be null");
    }
    if (entryName == null || entryName.isEmpty()) {
      throw new IllegalArgumentException("entryName cannot be null or empty");
    }
    // Zip entry names use forward slashes; some installers (notably Windows-built jars)
    // also emit backslashes — normalize both to File.separator before resolving.
    String normalized =
        entryName.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    // Strip any leading separators so the new File(parent, name) join is purely relative.
    while (normalized.startsWith(File.separator) || normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    File dest = new File(extractDir, normalized);
    String canonicalDir = extractDir.getCanonicalPath();
    String canonicalDest = dest.getCanonicalPath();
    // Allow the entry to resolve to the extract root itself (directory markers).
    if (!canonicalDest.equals(canonicalDir)
        && !canonicalDest.startsWith(canonicalDir + File.separator)) {
      throw new SecurityException(
          "Zip slip: entry '"
              + entryName
              + "' resolves to '"
              + canonicalDest
              + "' which is outside the extract root '"
              + canonicalDir
              + "'");
    }
    return dest;
  }
}
