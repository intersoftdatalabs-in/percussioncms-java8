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

/**
 * Password hashing / verification interface used by the membership service.
 *
 * <p>T2.x.8b hardening (issue #121): this is the project-local replacement for the previously-used
 * jasypt {@code org.jasypt.util.password.PasswordEncryptor} interface. It has the same shape (just
 * two methods) so call sites ({@code PSMembershipService}, {@code PSMembershipAuthProvider})
 * continue to work after the jasypt 1.9.3 dep is removed.
 *
 * <p>Implementations should be deterministic for {@link #checkPassword} (same raw + stored hash
 * always yields the same result) and idempotent for {@link #encryptPassword} (the output is a
 * one-way hash, not reversible).
 *
 * @see PSPasswordEncryptorAdapter
 */
public interface PSPasswordEncryptor {

  /**
   * Encodes (hashes) the given raw password for storage.
   *
   * @param password the raw (cleartext) password, assumed not <code>null</code> or empty
   * @return the encoded form, never <code>null</code> or empty
   */
  String encryptPassword(String password);

  /**
   * Verifies a raw password against a stored encoded password.
   *
   * @param rawPassword the raw (cleartext) password, assumed not <code>null</code> or empty
   * @param encryptedPassword the stored encoded password, assumed not <code>null</code> or empty
   * @return <code>true</code> if the raw password matches the stored hash, <code>false</code>
   *     otherwise
   */
  boolean checkPassword(String rawPassword, String encryptedPassword);
}
