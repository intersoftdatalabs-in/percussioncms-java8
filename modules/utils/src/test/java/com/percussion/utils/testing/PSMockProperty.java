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
package com.percussion.utils.testing;

import com.percussion.utils.jsr170.PSValueFactory;
import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

public class PSMockProperty implements Property {
  private Value m_value = null;

  public void setValue(Value arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = arg0;
  }

  public void setValue(Value[] arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    throw new ValueFormatException("This is a single valued property");
  }

  public void setValue(String arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(String[] arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    throw new ValueFormatException("This is a single valued property");
  }

  public void setValue(InputStream arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(long arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(double arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(Calendar arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(boolean arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public void setValue(Node arg0)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    m_value = PSValueFactory.createValue((Object) arg0);
  }

  public Value getValue() throws ValueFormatException, RepositoryException {
    return m_value;
  }

  public Value[] getValues() throws ValueFormatException, RepositoryException {
    throw new ValueFormatException("This is a single valued property");
  }

  public String getString() throws ValueFormatException, RepositoryException {
    return m_value.getString();
  }

  public InputStream getStream() throws ValueFormatException, RepositoryException {
    return m_value.getStream();
  }

  public long getLong() throws ValueFormatException, RepositoryException {
    return m_value.getLong();
  }

  public double getDouble() throws ValueFormatException, RepositoryException {
    return m_value.getDouble();
  }

  public Calendar getDate() throws ValueFormatException, RepositoryException {
    return m_value.getDate();
  }

  public boolean getBoolean() throws ValueFormatException, RepositoryException {
    return m_value.getBoolean();
  }

  public Node getNode() throws ValueFormatException, RepositoryException {
    throw new RepositoryException("Not supported: getNode()");
  }

  public long getLength() throws ValueFormatException, RepositoryException {
    return 0;
  }

  public long[] getLengths() throws ValueFormatException, RepositoryException {
    throw new ValueFormatException("This is a single valued property");
  }

  public PropertyDefinition getDefinition() throws RepositoryException {
    return null;
  }

  public int getType() throws RepositoryException {
    return 0;
  }

  public String getPath() throws RepositoryException {
    return null;
  }

  public String getName() throws RepositoryException {
    return null;
  }

  public Item getAncestor(int arg0)
      throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    return null;
  }

  public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    return null;
  }

  public int getDepth() throws RepositoryException {
    return 0;
  }

  public Session getSession() throws RepositoryException {
    return null;
  }

  public boolean isNode() {
    return false;
  }

  public boolean isNew() {
    return false;
  }

  public boolean isModified() {
    return false;
  }

  public boolean isSame(Item arg0) throws RepositoryException {
    return equals(arg0);
  }

  /** JCR 2.0 addition: this test mock is single-valued. */
  @Override
  public boolean isMultiple() {
    return false;
  }

  /** JCR 2.0 addition: return {@code this} (Property-of-a-Property). */
  @Override
  public Property getProperty() throws RepositoryException {
    return this;
  }

  /** JCR 2.0 addition: delegate to the value's binary. */
  @Override
  public javax.jcr.Binary getBinary() throws ValueFormatException, RepositoryException {
    return m_value.getBinary();
  }

  /** JCR 2.0 addition: delegate to the value's decimal. */
  @Override
  public java.math.BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
    return m_value.getDecimal();
  }

  /** JCR 2.0 addition: throw, since the test mock has no decimal-write path. */
  @Override
  public void setValue(java.math.BigDecimal value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    throw new javax.jcr.UnsupportedRepositoryOperationException(
        "PSMockProperty.setValue(BigDecimal) is not implemented");
  }

  /** JCR 2.0 addition: throw, since the test mock has no binary-write path. */
  @Override
  public void setValue(javax.jcr.Binary value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
          RepositoryException {
    throw new javax.jcr.UnsupportedRepositoryOperationException(
        "PSMockProperty.setValue(Binary) is not implemented");
  }

  public void accept(ItemVisitor arg0) throws RepositoryException {
    arg0.visit((Property) this);
  }

  public void save()
      throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
          InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException,
          NoSuchNodeTypeException, RepositoryException {
    throw new RepositoryException("Not supported: save()");
  }

  public void refresh(boolean arg0) throws InvalidItemStateException, RepositoryException {
    throw new RepositoryException("Not supported: refresh()");
  }

  public void remove()
      throws VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Not supported: remove()");
  }
}
