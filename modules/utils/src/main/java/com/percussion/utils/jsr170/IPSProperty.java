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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * Extend the jcr property interface with useful information for our implementation.
 *
 * @author dougrand
 */
public interface IPSProperty extends Property {
  /**
   * If this property object wraps a <code>null</code> value then this method returns <code>true
   * </code>.
   *
   * @return <code>true</code> for <code>null</code> values.
   */
  boolean isNull();

  /**
   * JCR 2.0 added {@link Property#isMultiple()}. The JCR 1.0 implementations in this codebase did
   * not provide it, so we add a default here that returns {@code false} (the common case for
   * single-valued properties). Implementations that wrap a multi-valued property (e.g. {@link
   * PSMultiProperty}) must override and return {@code true}.
   */
  @Override
  default boolean isMultiple() {
    return false;
  }

  /**
   * JCR 2.0 added {@link Property#getBinary()}. Default delegates through {@code getValue()};
   * single-value implementations get the right answer for free. Multi-value implementations should
   * override (since {@code getValue()} throws on a multi-valued property).
   */
  @Override
  default javax.jcr.Binary getBinary() throws javax.jcr.ValueFormatException, RepositoryException {
    return getValue().getBinary();
  }

  /** JCR 2.0 added {@link Property#getDecimal()}. Default delegates through {@code getValue()}. */
  @Override
  default java.math.BigDecimal getDecimal()
      throws javax.jcr.ValueFormatException, RepositoryException {
    return getValue().getDecimal();
  }

  /**
   * JCR 2.0 added {@link Property#getProperty()}. For a Property itself the natural implementation
   * is to return {@code this}.
   */
  @Override
  default Property getProperty() throws RepositoryException {
    return this;
  }

  /**
   * JCR 2.0 added {@link Property#setValue(BigDecimal)}. The JCR 1.0-derived value wrappers in this
   * codebase do not have a Decimal value type, so throw {@code
   * UnsupportedRepositoryOperationException} by default. Implementations that need to support
   * decimal values should override.
   */
  @Override
  default void setValue(BigDecimal value)
      throws javax.jcr.ValueFormatException, RepositoryException {
    throw new javax.jcr.UnsupportedRepositoryOperationException(
        "setValue(BigDecimal) is not supported by the legacy PSValue wrappers");
  }

  /**
   * JCR 2.0 added {@link Property#setValue(Binary)}. The JCR 1.0-derived value wrappers in this
   * codebase have an {@code InputStream}-based binary path; this default throws so callers know to
   * use the {@code setValue(InputStream)} path instead. Implementations that hold a real binary can
   * override.
   */
  @Override
  default void setValue(javax.jcr.Binary value)
      throws javax.jcr.ValueFormatException, RepositoryException {
    throw new javax.jcr.UnsupportedRepositoryOperationException(
        "setValue(Binary) is not supported; use setValue(InputStream) instead");
  }
}
