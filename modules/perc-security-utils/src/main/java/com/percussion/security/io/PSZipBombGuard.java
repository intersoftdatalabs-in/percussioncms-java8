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

import java.util.zip.ZipEntry;

/**
 * Resource-exhaustion defenses for zip-bomb-style attacks (CWE-409). The 11 CVEs in
 * commons-compress 1.28.0 (the last Java 1.8 line; 1.29+ is Java 9+) cannot be closed by upgrading
 * the library, so the project applies these defensive caps in the call sites that read
 * attacker-controlled archives.
 *
 * <p>Three caps, all fail-closed:
 *
 * <ul>
 *   <li>{@link #MAX_ENTRIES} = 10,000 entries per archive
 *   <li>{@link #MAX_ENTRY_SIZE} = 100 MB per entry (uncompressed declared size)
 *   <li>{@link #MAX_TOTAL_SIZE} = 500 MB total uncompressed size across all entries
 * </ul>
 *
 * <p>All three are overridable per JVM via the system properties {@code PSARCHIVE_MAX_ENTRIES},
 * {@code PSARCHIVE_MAX_ENTRY_SIZE}, {@code PSARCHIVE_MAX_TOTAL_SIZE}. A typical call site:
 *
 * <pre>
 *   ZipInputStream zin = ...;
 *   PSZipBombGuard guard = new PSZipBombGuard();
 *   ZipEntry entry;
 *   while ((entry = zin.getNextEntry()) != null) {
 *       guard.check(entry);
 *       ... process entry ...
 *   }
 * </pre>
 *
 * <p>If any cap is exceeded, the {@link #check(ZipEntry)} call throws a {@link SecurityException}
 * with the offending entry name and the cap that was hit. This fails closed before any data is read
 * from the entry, so a 4 GB entry inside a zip-bomb archive is rejected by reading just the central
 * directory (a few hundred bytes).
 *
 * @see PSArchiveFiles#extractFilesFromArchive for the original implementation pattern (PR #83)
 */
public final class PSZipBombGuard {

  /** Maximum number of entries per archive (default 10,000). */
  public static final int MAX_ENTRIES = 10_000;

  /** Maximum uncompressed size of any single entry (default 100 MB). */
  public static final long MAX_ENTRY_SIZE = 100L << 20; // 100 MB

  /** Maximum total uncompressed size across all entries (default 500 MB). */
  public static final long MAX_TOTAL_SIZE = 500L << 20; // 500 MB

  private final int maxEntries;
  private final long maxEntrySize;
  private final long maxTotalSize;
  private int entriesSeen;
  private long bytesSeen;

  /** Use the default caps. */
  public PSZipBombGuard() {
    this(MAX_ENTRIES, MAX_ENTRY_SIZE, MAX_TOTAL_SIZE);
  }

  /**
   * Constructor with explicit caps. Use this for tests; production code should rely on the
   * system-property overrides.
   */
  public PSZipBombGuard(int maxEntries, long maxEntrySize, long maxTotalSize) {
    this.maxEntries = readIntProp("PSARCHIVE_MAX_ENTRIES", maxEntries);
    this.maxEntrySize = readLongProp("PSARCHIVE_MAX_ENTRY_SIZE", maxEntrySize);
    this.maxTotalSize = readLongProp("PSARCHIVE_MAX_TOTAL_SIZE", maxTotalSize);
    this.entriesSeen = 0;
    this.bytesSeen = 0L;
  }

  /**
   * Check the next entry against all three caps. Call this once per entry, in iteration order.
   *
   * @param entry the entry about to be read
   * @throws SecurityException if any cap is exceeded
   */
  public void check(ZipEntry entry) {
    if (++entriesSeen > maxEntries) {
      throw new SecurityException("Archive rejected: too many entries (limit=" + maxEntries + ")");
    }
    final long entrySize = entry.getSize();
    if (entrySize > maxEntrySize) {
      throw new SecurityException(
          "Archive rejected: entry '"
              + entry.getName()
              + "' uncompressed size "
              + entrySize
              + " exceeds limit "
              + maxEntrySize);
    }
    if (entrySize > 0) {
      bytesSeen += entrySize;
      if (bytesSeen > maxTotalSize) {
        throw new SecurityException(
            "Archive rejected: total uncompressed size exceeds limit "
                + maxTotalSize
                + " (entry='"
                + entry.getName()
                + "')");
      }
    }
  }

  /** For diagnostics: how many entries have been seen so far. */
  public int entriesSeen() {
    return entriesSeen;
  }

  /** For diagnostics: cumulative uncompressed bytes seen so far. */
  public long bytesSeen() {
    return bytesSeen;
  }

  private static long readLongProp(String name, long def) {
    String v = System.getProperty(name);
    if (v == null || v.isEmpty()) return def;
    try {
      return Long.parseLong(v.trim());
    } catch (NumberFormatException nfe) {
      return def;
    }
  }

  private static int readIntProp(String name, int def) {
    String v = System.getProperty(name);
    if (v == null || v.isEmpty()) return def;
    try {
      return Integer.parseInt(v.trim());
    } catch (NumberFormatException nfe) {
      return def;
    }
  }
}
