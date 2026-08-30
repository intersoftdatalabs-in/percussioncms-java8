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

import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Adapter that implements jasypt's {@link PasswordEncryptor} interface but delegates the actual
 * encoding and verification to Spring Security's {@link PasswordEncoder}.
 *
 * <p>T2.x.8a hardening (issue #119): this adapter lets the membership service keep its existing
 * {@code PasswordEncryptor}-typed dependency while the actual implementation is BCrypt (via {@code
 * DelegatingPasswordEncoder}). Call sites do not need code changes.
 *
 * <p>The shim has two methods:
 *
 * <ul>
 *   <li>{@link #encryptPassword(String)} — produces a new hash using the default encoder (BCrypt).
 *   <li>{@link #checkPassword(String, String)} — verifies a raw password against a stored hash. The
 *       stored hash may be in any format supported by the {@code DelegatingPasswordEncoder}
 *       (BCrypt, SHA-256, etc.) — the encoder picks the right algorithm based on the prefix.
 * </ul>
 */
class PSPasswordEncryptorAdapter implements PasswordEncryptor {

  private final PasswordEncoder delegate;

  PSPasswordEncryptorAdapter(PasswordEncoder delegate) {
    this.delegate = delegate;
  }

  @Override
  public String encryptPassword(String password) {
    return delegate.encode(password);
  }

  @Override
  public boolean checkPassword(String rawPassword, String encryptedPassword) {
    return delegate.matches(rawPassword, encryptedPassword);
  }
}
