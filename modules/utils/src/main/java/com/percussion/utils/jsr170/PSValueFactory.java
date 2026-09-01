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

import com.percussion.utils.io.PSReaderInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * Factory to create value objects
 *
 * @author dougrand
 */
public class PSValueFactory implements ValueFactory {
  /** Used in the static method */
  private static final PSValueFactory ms_fact = new PSValueFactory();

  /**
   * Takes the input object and determines which concrete subclass should be used to represent the
   * data. If no subclass is appropriate then this throws an exception.
   *
   * @param data the data to encapsulate, never <code>null</code>
   * @return an appropriate value object that implements the value interface
   * @throws ValueFormatException if the object doesn't match a known type and cannot be coerced to
   *     a type
   */
  public static Value createValue(Object data) throws ValueFormatException {
    if (data == null) {
      return null;
    }
    if (data instanceof Value) {
      return (Value) data;
    }
    if (data instanceof Number) {
      if (data instanceof Double || data instanceof Float) {
        return ms_fact.createValue(((Number) data).doubleValue());
      } else {
        return ms_fact.createValue(((Number) data).longValue());
      }
    } else if (data instanceof String) {
      return new PSStringValue((String) data);
    } else if (data instanceof Blob) {
      try (InputStream io = ((Blob) data).getBinaryStream()) {

        return new PSInputStreamValue(io);
      } catch (SQLException | IOException e) {
        throw new ValueFormatException("Couldn't extract data", e);
      }
    } else if (data instanceof Clob) {
      try (InputStream io = new PSReaderInputStream(((Clob) data).getCharacterStream())) {
        return new PSInputStreamValue(io);
      } catch (SQLException | IOException e) {
        throw new ValueFormatException("Couldn't extract data", e);
      }

    } else if (data instanceof Date) {
      Calendar cal = new GregorianCalendar();
      cal.setTime((Date) data);
      return ms_fact.createValue(cal);
    } else if (data instanceof Calendar) {
      return ms_fact.createValue((Calendar) data);
    } else if (data instanceof byte[]) {
      try (InputStream stream = new ByteArrayInputStream((byte[]) data)) {
        return ms_fact.createValue(stream);
      } catch (IOException e) {
        throw new ValueFormatException("Couldn't extract data", e);
      }
    } else if (data instanceof Clob) {

      try (InputStream stream = new PSReaderInputStream(((Clob) data).getCharacterStream())) {
        return ms_fact.createValue(stream);
      } catch (SQLException | IOException e) {
        throw new ValueFormatException("Problem creating stream value", e);
      }
    } else if (data instanceof Blob) {

      try (InputStream stream = ((Blob) data).getBinaryStream()) {
        return ms_fact.createValue(stream);
      } catch (SQLException | IOException e) {
        throw new ValueFormatException("Problem creating stream value", e);
      }
    } else if (data instanceof Boolean) {
      return ms_fact.createValue(((Boolean) data).booleanValue());
    } else if (data instanceof Node) {
      try {
        return ms_fact.createValue((Node) data);
      } catch (RepositoryException e) {
        throw new ValueFormatException("Problem creating node value", e);
      }
    }
    throw new ValueFormatException("Cannot represent " + data.getClass() + " as a value");
  }

  public Value createValue(String arg0) {
    return new PSStringValue(arg0);
  }

  public Value createValue(String value, int type) throws ValueFormatException {
    switch (type) {
      case PropertyType.BINARY:
        return createValue(value.getBytes());
      case PropertyType.BOOLEAN:
        return new PSBooleanValue(value);
      case PropertyType.DATE:
        return new PSCalendarValue(PSValueConverter.convertToCalendar(value));
      case PropertyType.DOUBLE:
        return new PSDoubleValue(value);
      case PropertyType.LONG:
        return new PSLongValue(value);
      case PropertyType.NAME:
      case PropertyType.STRING:
      case PropertyType.PATH:
        return new PSStringValue(value);
      case PropertyType.REFERENCE:
      case PropertyType.UNDEFINED:
      default:
        throw new ValueFormatException("Unimplemented type");
    }
  }

  public Value createValue(long arg0) {
    return new PSLongValue(arg0);
  }

  public Value createValue(double arg0) {
    return new PSDoubleValue(arg0);
  }

  public Value createValue(boolean arg0) {
    return new PSBooleanValue(arg0);
  }

  public Value createValue(Calendar arg0) {
    return new PSCalendarValue(arg0);
  }

  public Value createValue(InputStream arg0) {
    return new PSInputStreamValue(arg0);
  }

  public Value createValue(Node arg0) throws RepositoryException {
    return new PSReferenceValue(arg0);
  }

  /**
   * JCR 2.0 added {@link ValueFactory#createBinary(InputStream)} which returns a {@link Binary}
   * rather than a {@code Value}. The JCR 1.0 implementation in this codebase did not provide it,
   * and the underlying legacy value wrappers do not expose a {@code Binary}. The JCR 1.0 way to get
   * a binary is {@link #createValue(InputStream)}; callers that need a JCR 2.0 {@code Binary}
   * should be updated to use {@link org.apache.jackrabbit.value.BinaryImpl} or read the input
   * stream themselves. For now, fail loudly so that the gap is visible rather than silently
   * returning null.
   */
  @Override
  public javax.jcr.Binary createBinary(InputStream stream) throws RepositoryException {
    throw new javax.jcr.UnsupportedRepositoryOperationException(
        "PSValueFactory.createBinary(InputStream) is not implemented; "
            + "use createValue(InputStream) for the JCR 1.0 binary path, "
            + "or org.apache.jackrabbit.value.BinaryImpl for JCR 2.0 Binary.");
  }

  /**
   * JCR 2.0 added this overload of {@link ValueFactory#createValue(Node, boolean)}; the boolean is
   * the {@code weakReference} flag. The JCR 1.0 implementation in this codebase did not provide it.
   * Delegate to the existing single-argument {@link #createValue(Node)}; the weak/strong
   * distinction is not represented in the legacy PSValue types.
   */
  @Override
  public Value createValue(Node node, boolean weakReference) throws RepositoryException {
    return createValue(node);
  }

  /**
   * JCR 2.0 added this overload. Delegate to the existing {@link #createValue(InputStream)} by
   * pulling bytes out of the {@code Binary}. The JCR 2.0 {@code createValue(Binary)} signature does
   * not throw {@code RepositoryException} per the spec, so any underlying {@link
   * javax.jcr.Binary#getStream()} failure is wrapped as a {@link RuntimeException}.
   */
  @Override
  public Value createValue(javax.jcr.Binary binary) {
    if (binary == null) {
      throw new IllegalArgumentException("binary may not be null");
    }
    try {
      return createValue(binary.getStream());
    } catch (RepositoryException e) {
      throw new RuntimeException("Failed to read Binary in PSValueFactory.createValue", e);
    }
  }

  /**
   * JCR 2.0 added this overload. The legacy PSValue types do not have a native decimal value type,
   * so convert to a string representation and store as a string. The original {@code BigDecimal} is
   * recoverable via {@code getString().toString()} + {@code new BigDecimal(...)}, mirroring the
   * {@link PSBaseValue#getDecimal()} default.
   */
  @Override
  public Value createValue(java.math.BigDecimal decimal) {
    if (decimal == null) {
      throw new IllegalArgumentException("decimal may not be null");
    }
    return new PSStringValue(decimal.toPlainString());
  }
}
