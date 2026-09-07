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

import java.io.IOException;
import java.io.InputStream;
import org.apache.sanselan.common.BinaryFileFunctions;

/**
 * Shim abstract class matching the public API of sanselan 0.97-incubator's {@code
 * org.apache.sanselan.common.byteSources.ByteSource}.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. The abstract
 * methods are present so any subclass (e.g. {@link ByteSourceInputStream}) can satisfy the API. The
 * implementation in this shim is documented in {@link org.apache.sanselan.Sanselan}.
 */
public abstract class ByteSource extends BinaryFileFunctions {
  protected final String filename;

  public ByteSource(String filename) {
    this.filename = filename;
  }

  public final InputStream getInputStream(int unused) throws IOException {
    return getInputStream();
  }

  public abstract InputStream getInputStream() throws IOException;

  public abstract byte[] getBlock(int start, int length) throws IOException;

  public abstract byte[] getAll() throws IOException;

  public abstract long getLength() throws IOException;

  public abstract String getDescription();

  public final String getFilename() {
    return filename;
  }
}
