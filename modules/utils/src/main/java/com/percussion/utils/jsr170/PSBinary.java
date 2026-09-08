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
package com.percussion.utils.jsr170;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;

/**
 * Minimal in-package {@link Binary} implementation used by the JCR 1.0-derived value wrappers in
 * this package. JCR 2.0's {@code Value.getBinary()} and {@code Property.getBinary()} need to return
 * a {@code Binary}, but we don't depend on {@code jackrabbit-jcr-commons} here, so we ship a tiny
 * standalone implementation backed by a byte array.
 *
 * <p>Added for the JCR 1.0 -> 2.0 migration. Callers that need full {@code Binary} semantics (read
 * offset/length, multi-stream sharing, etc.) should use {@code
 * org.apache.jackrabbit.value.BinaryImpl} in modules that already depend on {@code
 * jackrabbit-jcr-commons}.
 */
public class PSBinary implements Binary {

  private final byte[] m_bytes;
  private boolean m_disposed = false;

  public PSBinary(byte[] bytes) {
    if (bytes == null) throw new IllegalArgumentException("bytes may not be null");
    m_bytes = bytes;
  }

  @Override
  public InputStream getStream() throws RepositoryException {
    if (m_disposed) throw new RepositoryException("Binary has been disposed");
    return new ByteArrayInputStream(m_bytes);
  }

  @Override
  public int read(byte[] b, long position) throws IOException, RepositoryException {
    if (m_disposed) throw new RepositoryException("Binary has been disposed");
    if (position < 0 || position >= m_bytes.length) return -1;
    int len = Math.min(b.length, (int) (m_bytes.length - position));
    System.arraycopy(m_bytes, (int) position, b, 0, len);
    return len;
  }

  @Override
  public long getSize() throws RepositoryException {
    if (m_disposed) throw new RepositoryException("Binary has been disposed");
    return m_bytes.length;
  }

  @Override
  public void dispose() {
    m_disposed = true;
  }
}
