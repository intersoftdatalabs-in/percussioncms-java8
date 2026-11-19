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
package com.percussion.membership.services.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Factory class to get a password encryptor.
 *
 * <p>T2.x.8a hardening (issue #119): migrated from jasypt's {@code ConfigurablePasswordEncryptor}
 * (SHA-256, fast/brute-forceable, EOL jasypt 1.9.3) to Spring Security's {@code
 * DelegatingPasswordEncoder} (BCrypt default for new passwords, with SHA-256 fallback for matching
 * legacy hashes in the database). The dual-format approach lets the migration happen transparently
 * — existing SHA-256 hashes are still verifiable, and new passwords get a stronger BCrypt hash.
 *
 * <p>Wave 0 hardening (CVE-2025-22228): enforces BCrypt's 72-byte UTF-8 input ceiling via {@link
 * LengthValidatingBCryptEncoder} (matching Spring Security 5.8.18 behavior) so passwords exceeding
 * 72 UTF-8 bytes are rejected rather than silently truncated during {@code encode} or {@code
 * matches}, while leaving legacy SHA-256 verification unchanged.
 *
 * <p>T2.x.8b will remove jasypt entirely after a release cycle.
 */
public class PSMembershipPasswordEncryptorFactory {

  /**
   * BCrypt cost factor. 10 is the Spring Security default and provides a good balance between
   * security and login latency (~100ms on commodity hardware).
   */
  private static final int BCRYPT_STRENGTH = 10;

  /** Maximum password length accepted by OpenBSD BCrypt (UTF-8 bytes, CVE-2025-22228). */
  private static final int BCRYPT_MAX_BYTES = 72;

  /**
   * The prefix that {@code DelegatingPasswordEncoder} prepends to new BCrypt hashes. New passwords
   * in the database will start with this prefix.
   */
  private static final String BCRYPT_ID = "bcrypt";

  /**
   * The single shared password encoder. New passwords are encoded with BCrypt (and prefixed with
   * {@value #BCRYPT_ID}); legacy SHA-256 hashes (un-prefixed base64) are still verified via the
   * SHA-256 encoder registered as the default-for-matches.
   */
  private static final PasswordEncoder ENCODER;

  static {
    PasswordEncoder bcrypt =
        new LengthValidatingBCryptEncoder(new BCryptPasswordEncoder(BCRYPT_STRENGTH));
    MessageDigestPasswordEncoder sha256 = new MessageDigestPasswordEncoder("SHA-256");
    // base64 encoding matches the jasypt default (setPlainDigest(false) +
    // setStringOutputType("base64") was the implicit default).
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(BCRYPT_ID, bcrypt);
    encoders.put("SHA-256", sha256);
    DelegatingPasswordEncoder dpe = new DelegatingPasswordEncoder(BCRYPT_ID, encoders);
    // When a stored hash doesn't start with "{bcrypt}" or "{SHA-256}", treat it
    // as a legacy un-prefixed SHA-256 hash. This is what makes the
    // migration transparent.
    dpe.setDefaultPasswordEncoderForMatches(sha256);
    ENCODER = dpe;
  }

  public static PSPasswordEncryptor getPasswordEncryptor() {
    return new PSPasswordEncryptorAdapter(ENCODER);
  }

  /**
   * Wraps {@link BCryptPasswordEncoder} to reject passwords exceeding {@value #BCRYPT_MAX_BYTES}
   * UTF-8 bytes rather than allowing BCrypt's 72-byte truncation (CVE-2025-22228).
   */
  private static final class LengthValidatingBCryptEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate;

    private LengthValidatingBCryptEncoder(BCryptPasswordEncoder delegate) {
      this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
      if (exceedsMaxBytes(rawPassword)) {
        throw new IllegalArgumentException(
            "Password cannot be more than " + BCRYPT_MAX_BYTES + " bytes");
      }
      return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
      if (exceedsMaxBytes(rawPassword)) {
        return false;
      }
      return delegate.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
      return delegate.upgradeEncoding(encodedPassword);
    }

    private static boolean exceedsMaxBytes(CharSequence rawPassword) {
      return rawPassword != null
          && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
  }
}
