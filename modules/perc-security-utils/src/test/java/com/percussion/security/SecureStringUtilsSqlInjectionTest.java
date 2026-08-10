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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Regression tests for SQL-injection barriers on {@link SecureStringUtils} (CodeQL {@code
 * java/sql-injection}, T042 / PR #1343 backport).
 *
 * <p>JUnit 4 (same as {@code TestSecureStringUtils} in this module). The barrier methods throw
 * {@link IllegalArgumentException}; we assert that explicitly rather than via JUnit 5's {@code
 * assertThrows}.
 */
public class SecureStringUtilsSqlInjectionTest {

  @Test
  public void requireSqlObjectNameAcceptsPlainIdentifier() {
    assertEquals("CONTENTSTATUS", SecureStringUtils.requireSqlObjectName("CONTENTSTATUS"));
    assertEquals("psx_templates", SecureStringUtils.requireSqlObjectName("psx_templates"));
  }

  @Test
  public void requireSqlObjectNameRejectsInjectionFragments() {
    expectIae(() -> SecureStringUtils.requireSqlObjectName("t; DROP TABLE users--"));
    expectIae(() -> SecureStringUtils.requireSqlObjectName("t' OR '1'='1"));
    expectIae(() -> SecureStringUtils.requireSqlObjectName(""));
    expectIae(() -> SecureStringUtils.requireSqlObjectName(null));
  }

  @Test
  public void requireSqlObjectNameOrNullAllowsBlank() {
    assertNull(SecureStringUtils.requireSqlObjectNameOrNull(null));
    assertNull(SecureStringUtils.requireSqlObjectNameOrNull("  "));
    assertEquals("dbo", SecureStringUtils.requireSqlObjectNameOrNull("dbo"));
  }

  @Test
  public void requireSafeMetadataTokenAcceptsNamespacedNames() {
    assertEquals("dcterms:created", SecureStringUtils.requireSafeMetadataToken("dcterms:created"));
    assertEquals("linktext_lower", SecureStringUtils.requireSafeMetadataToken("linktext_lower"));
  }

  @Test
  public void requireSafeMetadataTokenRejectsSqlMetacharacters() {
    expectIae(() -> SecureStringUtils.requireSafeMetadataToken("name; drop table x"));
    expectIae(() -> SecureStringUtils.requireSafeMetadataToken("name' OR '1'='1"));
    expectIae(() -> SecureStringUtils.requireSafeMetadataToken("a b"));
  }

  @Test
  public void requireSingleSqlStatementRejectsStackedQueriesOnly() {
    assertEquals("SELECT 1", SecureStringUtils.requireSingleSqlStatement("SELECT 1;"));
    expectIae(() -> SecureStringUtils.requireSingleSqlStatement("SELECT 1; DROP TABLE t"));
    // Comments allowed on general path (string literals / hints).
    assertEquals(
        "SELECT * FROM t WHERE c = 'a--b'",
        SecureStringUtils.requireSingleSqlStatement("SELECT * FROM t WHERE c = 'a--b'"));
  }

  @Test
  public void requireFactorySqlStatementAlsoRejectsComments() {
    expectIae(() -> SecureStringUtils.requireFactorySqlStatement("SELECT 1 -- x"));
  }

  private static void expectIae(Runnable r) {
    try {
      r.run();
    } catch (IllegalArgumentException expected) {
      return;
    } catch (Throwable other) {
      fail("Expected IllegalArgumentException, got " + other.getClass().getName());
      return;
    }
    fail("Expected IllegalArgumentException but no exception was thrown");
  }
}
