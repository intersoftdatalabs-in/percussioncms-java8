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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.membership.services.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;

/**
 * Unit tests verifying Wave 0 BCrypt 72-byte UTF-8 enforcement (CVE-2025-22228) and legacy SHA-256
 * verification compatibility in {@link PSMembershipPasswordEncryptorFactory}.
 */
public class PSMembershipPasswordEncryptorFactoryTest {

  private final PSPasswordEncryptor encryptor =
      PSMembershipPasswordEncryptorFactory.getPasswordEncryptor();

  @Test
  public void encryptAndCheckAcceptsExactly72AsciiBytes() {
    String p72 = repeatChar('a', 72);
    String encoded = encryptor.encryptPassword(p72);
    assertTrue(encoded.startsWith("{bcrypt}"));
    assertTrue(encryptor.checkPassword(p72, encoded));
    assertFalse(encryptor.checkPassword(repeatChar('a', 71) + "b", encoded));
  }

  @Test
  public void encryptRejects73AsciiBytes() {
    String p73 = repeatChar('a', 73);
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> encryptor.encryptPassword(p73));
    assertTrue(expected.getMessage().contains("72"));
  }

  @Test
  public void checkPasswordRejects73BytePasswordSharing72BytePrefix() {
    String p72 = repeatChar('a', 72);
    String encoded = encryptor.encryptPassword(p72);
    assertFalse(
        encryptor.checkPassword(p72 + "X", encoded),
        "Password exceeding 72 bytes must be rejected even when sharing the first 72 bytes");
  }

  @Test
  public void encryptAndCheckEnforceUtf8ByteLengthNotCharLength() {
    // U+20AC Euro sign encodes as 3 bytes in UTF-8: 24 chars = 72 bytes, 25 chars = 75 bytes.
    String twentyFourChars72Bytes = repeatChar('\u20AC', 24);
    String encoded = encryptor.encryptPassword(twentyFourChars72Bytes);
    assertTrue(encryptor.checkPassword(twentyFourChars72Bytes, encoded));

    String twentyFiveChars75Bytes = repeatChar('\u20AC', 25);
    assertFalse(encryptor.checkPassword(twentyFiveChars75Bytes, encoded));
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> encryptor.encryptPassword(twentyFiveChars75Bytes));
    assertTrue(expected.getMessage().contains("72"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void legacySha256FallbackRemainsFunctional() {
    MessageDigestPasswordEncoder legacyEncoder = new MessageDigestPasswordEncoder("SHA-256");
    String longLegacyPassword = repeatChar('z', 90);
    String legacyHash = legacyEncoder.encode(longLegacyPassword);
    assertFalse(legacyHash.startsWith("{bcrypt}"));
    assertTrue(encryptor.checkPassword(longLegacyPassword, legacyHash));
  }

  private static String repeatChar(char c, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, c);
    return new String(chars);
  }
}
