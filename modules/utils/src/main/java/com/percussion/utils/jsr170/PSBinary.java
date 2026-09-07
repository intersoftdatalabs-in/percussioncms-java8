package com.percussion.utils.jsr170;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;

final class PSBinary implements Binary {
  private byte[] m_data;

  PSBinary(byte[] data) {
    m_data = data.clone();
  }

  @Override
  public InputStream getStream() throws RepositoryException {
    return new ByteArrayInputStream(m_data);
  }

  @Override
  public int read(byte[] buffer, long position) throws IOException, RepositoryException {
    if (position < 0 || position > m_data.length) {
      throw new IOException("Position is outside the binary data");
    }
    if (position == m_data.length) {
      return -1;
    }
    int length = Math.min(buffer.length, m_data.length - (int) position);
    System.arraycopy(m_data, (int) position, buffer, 0, length);
    return length;
  }

  @Override
  public long getSize() {
    return m_data.length;
  }

  @Override
  public void dispose() {
    m_data = new byte[0];
  }
}
