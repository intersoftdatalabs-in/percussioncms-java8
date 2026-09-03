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

package org.apache.sanselan.common.byteSources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shim concrete class matching the public API of sanselan 0.97-incubator's {@code
 * org.apache.sanselan.common.byteSources.ByteSourceInputStream}.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. The
 * implementation here is a thin wrapper that reads the input stream into a byte array once and
 * serves subsequent reads from the cached array. Sanselan's original implementation used a block
 * cache; for the shim, the simpler whole-read approach is enough to satisfy the API contract. See
 * {@link org.apache.sanselan.Sanselan} for context.
 */
public class ByteSourceInputStream extends ByteSource {
  private final InputStream is;
  private byte[] cached;
  private static final int BLOCK_SIZE = 1024;

  public ByteSourceInputStream(InputStream is, String filename) {
    super(filename);
    this.is = is;
  }

  private byte[] getCachedBytes() throws IOException {
    if (cached != null) {
      return cached;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[BLOCK_SIZE];
    int read;
    while ((read = is.read(buf)) != -1) {
      baos.write(buf, 0, read);
    }
    cached = baos.toByteArray();
    return cached;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new java.io.ByteArrayInputStream(getCachedBytes());
  }

  @Override
  public byte[] getBlock(int start, int length) throws IOException {
    byte[] all = getCachedBytes();
    if (start < 0 || start >= all.length) {
      return new byte[0];
    }
    int end = Math.min(start + length, all.length);
    byte[] out = new byte[end - start];
    System.arraycopy(all, start, out, 0, out.length);
    return out;
  }

  @Override
  public long getLength() throws IOException {
    return getCachedBytes().length;
  }

  @Override
  public byte[] getAll() throws IOException {
    return getCachedBytes();
  }

  @Override
  public String getDescription() {
    return "ByteSourceInputStream: " + (filename == null ? "<no name>" : filename);
  }
}
