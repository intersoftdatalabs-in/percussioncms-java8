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
package com.percussion.utils.jsr170;

import java.math.BigDecimal;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Base implementation class for JCR values
 *
 * @author dougrand
 * @param <Type> the value type encapsulated by the subclass of this abstract class.
 */
public abstract class PSBaseValue<Type> implements Value, IPSJcrCacheItem {
  /** Holds value, never <code>null</code> after ctor */
  protected Type m_value;

  /** Hold string value once loaded */
  protected String m_strValue = null;

  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSBaseValue)) return false;

    PSBaseValue b = (PSBaseValue) obj;
    EqualsBuilder eb = new EqualsBuilder();

    return eb.append(m_value, b.m_value).isEquals();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    HashCodeBuilder b = new HashCodeBuilder();
    return b.append(m_value).toHashCode();
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSBaseValue{");
    sb.append("m_value=").append(m_value);
    sb.append(", m_strValue='").append(m_strValue).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.utils.jsr170.IPSJcrCacheItem#getSizeInBytes()
   */
  public long getSizeInBytes() throws RepositoryException {
    return 8;
  }

  /**
   * JCR 2.0 added {@link Value#getDecimal()} (returns {@code BigDecimal}). The JCR 1.0
   * implementation in this codebase did not provide it, so we add a default here that delegates
   * through {@link #getString()}. Subclasses that can produce a more precise {@code BigDecimal}
   * (e.g. {@link PSLongValue}, {@link PSDoubleValue}) may override.
   */
  @Override
  public BigDecimal getDecimal() throws javax.jcr.ValueFormatException, RepositoryException {
    return new BigDecimal(getString());
  }

  /**
   * JCR 2.0 added {@link Value#getBinary()} (returns {@link javax.jcr.Binary}). The JCR 1.0
   * implementation in this codebase did not provide it. The legacy PSValue wrappers carry string /
   * numeric / stream state rather than direct binary state, so the default here delegates through
   * {@link #getString()} to produce a binary view of the value's textual representation. Subclasses
   * that hold a real binary (e.g. {@link PSInputStreamValue}, {@link PSReferenceValue}) should
   * override to expose the underlying bytes directly.
   */
  @Override
  public javax.jcr.Binary getBinary() throws RepositoryException {
    return new PSBinary(getString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
