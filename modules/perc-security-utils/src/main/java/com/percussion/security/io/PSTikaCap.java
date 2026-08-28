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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Caps the number of bytes readable from an {@link InputStream} and returns EOF once the cap is
 * reached. Used to defend the project's Tika 2.9.x call sites against resource-exhaustion attacks
 * (15 CVEs in commons-tika 2.9.x; the library is the latest Java 1.8-compatible line and the CVEs
 * are mostly DoS / zip-bomb style in the parser tree).
 *
 * <p>Unlike {@link java.io.InputStream#mark} / reset, this is a hard cap with no possibility of the
 * wrapped stream returning more bytes. Tika's parsers handle the EOF gracefully (they get a short
 * result rather than OOM or hang).
 *
 * <p>The default cap is 100 MB. The cap is overridable per JVM via the system property {@code
 * PSARCHIVE_MAX_TIKA_INPUT_BYTES} (same naming convention as the other caps).
 *
 * @see <a href="https://tika.apache.org/security.html">Tika 2.9.x security advisories</a>
 */
public final class PSTikaCap {

  /** Default cap: 100 MB. */
  public static final long DEFAULT_MAX_BYTES = 100L << 20;

  private static final long MAX_BYTES =
      readLongProp("PSARCHIVE_MAX_TIKA_INPUT_BYTES", DEFAULT_MAX_BYTES);

  /**
   * Wrap the given input stream with a hard byte cap. Use in a try-with-resources.
   *
   * @param in the source stream
   * @return a stream that returns EOF after {@link #MAX_BYTES} bytes have been read
   */
  public static InputStream truncate(InputStream in) {
    if (in == null) throw new IllegalArgumentException("input stream is null");
    return new BoundedInputStream(in, MAX_BYTES);
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

  private static final class BoundedInputStream extends FilterInputStream {
    private final long maxBytes;
    private long bytesRead;

    BoundedInputStream(InputStream in, long maxBytes) {
      super(in);
      this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= maxBytes) return -1;
      int b = super.read();
      if (b != -1) bytesRead++;
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesRead >= maxBytes) return -1;
      int allowed = (int) Math.min(len, maxBytes - bytesRead);
      int n = super.read(b, off, allowed);
      if (n > 0) bytesRead += n;
      return n;
    }

    @Override
    public long skip(long n) throws IOException {
      long allowed = Math.min(n, maxBytes - bytesRead);
      long skipped = super.skip(allowed);
      if (skipped > 0) bytesRead += skipped;
      return skipped;
    }
  }
}
