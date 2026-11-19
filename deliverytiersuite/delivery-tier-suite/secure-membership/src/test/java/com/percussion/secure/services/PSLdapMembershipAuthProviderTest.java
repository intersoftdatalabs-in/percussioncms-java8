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
package com.percussion.secure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Unit tests verifying Wave 0 input validation in {@link PSLdapMembershipAuthProvider} before any
 * JNDI bind attempt (including CVE-2026-41720 empty-password protection).
 */
public class PSLdapMembershipAuthProviderTest {

  private PSLdapMembershipAuthProvider provider;
  private AtomicBoolean bindAttempted;
  private AtomicReference<Hashtable<?, ?>> capturedEnv;

  @BeforeEach
  public void setUp() {
    provider = new PSLdapMembershipAuthProvider("example.com", "ldap://localhost:389");
    bindAttempted = new AtomicBoolean(false);
    capturedEnv = new AtomicReference<>();
    provider.contextFactory =
        new PSLdapMembershipAuthProvider.ContextFactory() {
          @Override
          DirContext createContext(Hashtable<?, ?> env) throws NamingException {
            bindAttempted.set(true);
            capturedEnv.set(env);
            throw new AuthenticationException("simulated bind failure");
          }
        };
  }

  @Test
  public void doAuthenticationRejectsEmptyPasswordWithoutLdapBind() {
    assertRejectedBeforeBind(new UsernamePasswordAuthenticationToken("jdoe", ""));
  }

  @Test
  public void doAuthenticationRejectsNullPasswordWithoutLdapBind() {
    assertRejectedBeforeBind(new UsernamePasswordAuthenticationToken("jdoe", null));
  }

  @Test
  public void doAuthenticationRejectsNonStringCredentialsWithoutLdapBind() {
    assertRejectedBeforeBind(new UsernamePasswordAuthenticationToken("jdoe", new Object()));
  }

  @Test
  public void doAuthenticationRejectsNulInUsernameWithoutLdapBind() {
    assertRejectedBeforeBind(new UsernamePasswordAuthenticationToken("jdoe\0admin", "secret"));
  }

  @Test
  public void doAuthenticationProceedsToBindForNonEmptyPassword() {
    try {
      provider.doAuthentication(new UsernamePasswordAuthenticationToken("jdoe", "s3cr3t"));
    } catch (RuntimeException expected) {
      // Simulated AuthenticationException leads to NPE inside searchForUser when ctx is null;
      // this test specifically verifies bindAsUser reached contextFactory with the credential.
    }
    assertTrue(bindAttempted.get(), "Expected non-empty password to attempt JNDI bind");
    assertEquals("jdoe@example.com", capturedEnv.get().get(Context.SECURITY_PRINCIPAL));
    assertEquals("s3cr3t", capturedEnv.get().get(Context.SECURITY_CREDENTIALS));
  }

  private void assertRejectedBeforeBind(UsernamePasswordAuthenticationToken token) {
    assertThrows(BadCredentialsException.class, () -> provider.doAuthentication(token));
    assertFalse(bindAttempted.get(), "JNDI bind must not be attempted for rejected credentials");
  }
}
