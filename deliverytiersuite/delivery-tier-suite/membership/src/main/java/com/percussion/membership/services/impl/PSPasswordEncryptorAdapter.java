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

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Adapter that implements the project-local {@link PSPasswordEncryptor} interface but delegates the
 * actual encoding to Spring Security's {@link PasswordEncoder}.
 *
 * <p>T2.x.8a hardening (issue #119) introduced this adapter to use BCrypt under the hood. T2.x.8b
 * hardening (issue #121) renamed the interface from jasypt's {@code PasswordEncryptor} to the
 * project-local {@code PSPasswordEncryptor} so the jasypt 1.9.3 dep can be removed.
 *
 * <p>Call sites see {@link PSPasswordEncryptor}; the actual implementation is Spring Security's
 * {@code DelegatingPasswordEncoder} (BCrypt for new passwords, SHA-256 for legacy hashes via the
 * default-for-matches).
 */
class PSPasswordEncryptorAdapter implements PSPasswordEncryptor {

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
