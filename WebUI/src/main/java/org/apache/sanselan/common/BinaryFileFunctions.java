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

package org.apache.sanselan.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import org.apache.sanselan.ImageReadException;

/**
 * Shim superclass matching the public API of sanselan 0.97-incubator's {@code
 * org.apache.sanselan.common.BinaryFileFunctions}. Referenced as the superclass of {@code
 * ByteSource}.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. The methods that
 * actually parse image bytes throw {@code UnsupportedOperationException}; the helpers that just
 * print debug info or do trivial byte-array checks are preserved as no-ops or simple
 * implementations. See {@link org.apache.sanselan.Sanselan} for context.
 */
public class BinaryFileFunctions implements BinaryConstants {
  protected boolean debug;

  public BinaryFileFunctions() {}

  public final void setDebug(boolean debug) {
    this.debug = debug;
  }

  public final boolean getDebug() {
    return debug;
  }

  protected final void readRandomBytes(InputStream is) throws ImageReadException, IOException {
    throw new UnsupportedOperationException(
        "Sanselan binary parsing is disabled in the T2.17 shim (issue #186).");
  }

  public final void debugNumber(String message, int value) {
    if (debug) System.err.println(message + ": " + value);
  }

  public final void debugNumber(String message, int value, int byteOrder) {
    if (debug) System.err.println(message + ": " + value + " (order=" + byteOrder + ")");
  }

  public final void debugNumber(PrintWriter pw, String message, int value) {
    if (debug) pw.println(message + ": " + value);
  }

  public final void debugNumber(PrintWriter pw, String message, int value, int byteOrder) {
    if (debug) pw.println(message + ": " + value + " (order=" + byteOrder + ")");
  }

  public final boolean startsWith(byte[] haystack, byte[] prefix) {
    if (haystack == null || prefix == null) return false;
    if (haystack.length < prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) {
      if (haystack[i] != prefix[i]) return false;
    }
    return true;
  }

  public final byte[] readBytes(InputStream is, int length) throws ImageReadException, IOException {
    byte[] out = new byte[length];
    int total = 0;
    while (total < length) {
      int read = is.read(out, total, length - total);
      if (read < 0) {
        throw new ImageReadException(
            "Unexpected end of stream after " + total + " of " + length + " bytes");
      }
      total += read;
    }
    return out;
  }

  public final void readAndVerifyBytes(InputStream is, byte[] expected, String label)
      throws ImageReadException, IOException {
    readAndVerifyBytes(null, is, expected, label);
  }

  protected final void readAndVerifyBytes(
      String context, InputStream is, byte[] expected, String label)
      throws ImageReadException, IOException {
    byte[] actual = readBytes(is, expected.length);
    if (!java.util.Arrays.equals(actual, expected)) {
      throw new ImageReadException(
          (context == null ? "" : context + ": ") + label + ": byte mismatch");
    }
  }

  public final void skipBytes(InputStream is, int count, String label) throws IOException {
    long total = 0;
    while (total < count) {
      long skipped = is.skip(count - total);
      if (skipped <= 0) {
        if (is.read() < 0) {
          throw new IOException("Unexpected end of stream while skipping " + label);
        }
        total += 1;
      } else {
        total += skipped;
      }
    }
  }

  protected final void scanForByte(InputStream is, byte target) throws IOException {
    int b;
    while ((b = is.read()) != -1) {
      if ((byte) b == target) {
        return;
      }
    }
  }

  public final byte readByte(String context, InputStream is, String label)
      throws ImageReadException, IOException {
    int b = is.read();
    if (b < 0) {
      throw new ImageReadException(
          (context == null ? "" : context + ": ") + label + ": unexpected end of stream");
    }
    return (byte) b;
  }

  protected final RationalNumber[] convertByteArrayToRationalArray(
      String context, byte[] bytes, int offset, int count, int stride) {
    RationalNumber[] out = new RationalNumber[count];
    for (int i = 0; i < count; i++) {
      out[i] =
          new RationalNumber(
              convertByteArrayToInt(context, bytes, offset + i * stride),
              convertByteArrayToInt(context, bytes, offset + i * stride + 4));
    }
    return out;
  }

  protected final RationalNumber convertByteArrayToRational(
      String context, byte[] bytes, int offset) {
    return new RationalNumber(
        convertByteArrayToInt(context, bytes, offset),
        convertByteArrayToInt(context, bytes, offset + 4));
  }

  protected final RationalNumber convertByteArrayToRational(
      String context, byte[] bytes, int offset, int length) {
    int numerator = 0, denominator = 0;
    for (int i = 0; i < Math.min(4, length); i++) {
      numerator = (numerator << 8) | (bytes[offset + i] & 0xff);
    }
    for (int i = 4; i < Math.min(8, length); i++) {
      denominator = (denominator << 8) | (bytes[offset + i] & 0xff);
    }
    return new RationalNumber(numerator, denominator);
  }

  protected final int convertByteArrayToInt(String context, byte[] bytes, int offset) {
    int b0 = bytes[offset] & 0xff;
    int b1 = bytes[offset + 1] & 0xff;
    int b2 = bytes[offset + 2] & 0xff;
    int b3 = bytes[offset + 3] & 0xff;
    return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
  }

  protected final int convertByteArrayToInt(String context, byte[] bytes, int offset, int length) {
    int result = 0;
    for (int i = 0; i < length; i++) {
      result = (result << 8) | (bytes[offset + i] & 0xff);
    }
    return result;
  }

  protected final int[] convertByteArrayToIntArray(
      String context, byte[] bytes, int offset, int count, int stride) {
    int[] out = new int[count];
    for (int i = 0; i < count; i++) {
      out[i] = convertByteArrayToInt(context, bytes, offset + i * stride);
    }
    return out;
  }

  protected final void writeIntInToByteArray(int value, byte[] bytes, int offset, int length) {
    for (int i = 0; i < length; i++) {
      bytes[offset + i] = (byte) ((value >> ((length - 1 - i) * 8)) & 0xff);
    }
  }

  protected static final byte[] int2ToByteArray(int value, int length) {
    byte[] out = new byte[length];
    for (int i = 0; i < length; i++) {
      out[i] = (byte) ((value >> ((length - 1 - i) * 8)) & 0xff);
    }
    return out;
  }

  protected final byte[] convertIntArrayToByteArray(int[] values, int length) {
    byte[] out = new byte[values.length * length];
    for (int i = 0; i < values.length; i++) {
      writeIntInToByteArray(values[i], out, i * length, length);
    }
    return out;
  }

  protected final byte[] convertShortArrayToByteArray(int[] values, int length) {
    byte[] out = new byte[values.length * length];
    for (int i = 0; i < values.length; i++) {
      int v = values[i];
      for (int j = 0; j < length; j++) {
        out[i * length + j] = (byte) ((v >> ((length - 1 - j) * 8)) & 0xff);
      }
    }
    return out;
  }

  protected final byte[] convertShortToByteArray(int value, int length) {
    byte[] out = new byte[length];
    for (int i = 0; i < length; i++) {
      out[i] = (byte) ((value >> ((length - 1 - i) * 8)) & 0xff);
    }
    return out;
  }

  protected final byte[] convertIntArrayToRationalArray(
      int[] numerators, int[] denominators, int length)
      throws org.apache.sanselan.ImageWriteException {
    if (numerators.length != denominators.length) {
      throw new org.apache.sanselan.ImageWriteException("numerators.length != denominators.length");
    }
    byte[] out = new byte[numerators.length * length * 2];
    for (int i = 0; i < numerators.length; i++) {
      writeIntInToByteArray(numerators[i], out, i * length * 2, length);
      writeIntInToByteArray(denominators[i], out, i * length * 2 + length, length);
    }
    return out;
  }

  /**
   * Stub rational-number type referenced by the public API of {@link BinaryFileFunctions}. Real
   * sanselan 0.97-incubator ships a {@code org.apache.sanselan.common.RationalNumber} class; this
   * shim provides a minimal version sufficient for class resolution.
   */
  public static final class RationalNumber {
    public final int numerator;
    public final int denominator;

    public RationalNumber(int numerator, int denominator) {
      this.numerator = numerator;
      this.denominator = denominator;
    }

    @Override
    public String toString() {
      return numerator + "/" + denominator;
    }
  }
}
