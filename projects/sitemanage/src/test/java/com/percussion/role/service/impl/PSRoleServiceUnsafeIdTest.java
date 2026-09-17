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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.role.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the identifier safety helper added in T2.14 (epic #73, issue #223). The helper
 * sits at the entry point of any user-controlled String value that flows into a Hibernate String
 * {@code @Id} column, and is meant to block CVE-2026-0603 second-order SQL injection at the
 * boundary before the tainted value is persisted.
 *
 * <p>JUnit 4 to match the rest of the role service test package.
 */
public class PSRoleServiceUnsafeIdTest {

  @Test
  public void rejectsNull() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar(null));
  }

  @Test
  public void acceptsPlausibleRoleNames() {
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("Admin"));
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("Editor"));
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("Site_Author"));
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("Author.1"));
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("role with spaces"));
    assertFalse(PSRoleService.containsUnsafeIdentifierChar("café"));
  }

  @Test
  public void rejectsSingleQuote() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin'"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("' OR 1=1"));
  }

  @Test
  public void rejectsDoubleQuote() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\""));
  }

  @Test
  public void rejectsSemicolon() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin;DROP TABLE users"));
  }

  @Test
  public void rejectsBackslash() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\\"));
  }

  @Test
  public void rejectsDash() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin--comment"));
  }

  @Test
  public void rejectsSlashes() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin/x"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin/*x"));
  }

  @Test
  public void rejectsAsteriskAndParens() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin*"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin(1)"));
  }

  @Test
  public void rejectsNullAndControlCharacters() {
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\u0000"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\n"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\r"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\t"));
  }

  @Test
  public void rejectsC1ControlCharacters() {
    // U+0085 (NEL) and U+009F (APC) are C1 controls and must be rejected.
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\u0085"));
    assertTrue(PSRoleService.containsUnsafeIdentifierChar("Admin\u009F"));
  }
}
