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
package com.percussion.search.lucene.textconverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared helpers for the {@link IPSLuceneTextConverter} implementations. Currently limited to the
 * size-bounded stream read introduced for the T2.10 POI hardening (issue tracked in the parent epic
 * #73). Kept package-private and final; no public API.
 */
final class PSTextConverterUtils {

  private PSTextConverterUtils() {
    // no instances
  }

  /**
   * Reads the supplied input stream into a byte array, rejecting any input whose total size exceeds
   * {@code maxBytes}. The partial bytes that were read before the cap was hit are discarded; an
   * {@link IOException} is thrown instead.
   *
   * <p>This is the Java 1.8-safe equivalent of {@code is.readNBytes(maxBytes + 1)} followed by a
   * length check, with the difference that the read loop counts as it goes and short-circuits on
   * overflow rather than buffering the oversized input. The resulting byte array is fully seekable
   * (mark/reset supported) so it can be fed to parsers — POI's {@code HSSFWorkbook}, {@code
   * XSSFWorkbook}, {@code XWPFDocument}, {@code XMLSlideShow}, and similar — that need to backtrack
   * into the input.
   *
   * @param is the source stream; not closed here (caller's responsibility)
   * @param maxBytes the hard cap, must be positive
   * @return a byte array sized between 0 and {@code maxBytes} inclusive
   * @throws IOException if reading fails or the cap is exceeded
   */
  static byte[] readAndCap(InputStream is, long maxBytes) throws IOException {
    if (maxBytes <= 0) {
      throw new IllegalArgumentException("maxBytes must be positive");
    }
    // Pre-size to the cap so we don't reallocate; bounded at 8 KiB initial to keep
    // tiny inputs cheap.
    ByteArrayOutputStream baos = new ByteArrayOutputStream((int) Math.min(maxBytes, 8L * 1024L));
    byte[] buf = new byte[8192];
    long total = 0;
    int n;
    while ((n = is.read(buf)) != -1) {
      total += n;
      if (total > maxBytes) {
        throw new IOException(
            "Input exceeds " + maxBytes + " bytes (read " + total + " before cutoff)");
      }
      baos.write(buf, 0, n);
    }
    return baos.toByteArray();
  }
}
