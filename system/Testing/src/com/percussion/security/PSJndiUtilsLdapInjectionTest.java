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
package com.percussion.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link PSJndiUtils#getFilterString(String[], String, String)} and the LDAP filter
 * escape helper, ensuring user-supplied filter values cannot break out of the LDAP search filter
 * (CWE-90 / CodeQL java/ldap-injection).
 */
public class PSJndiUtilsLdapInjectionTest {

  @Test
  public void escapeHelperPreservesWildcard() throws Exception {
    // '*' is the supported wildcard and must be preserved verbatim.
    assertEquals("*", PSJndiUtils.escapeLdapFilterValue("*"));
    assertEquals("a*b", PSJndiUtils.escapeLdapFilterValue("a*b"));
  }

  @Test
  public void escapeHelperEscapesReservedChars() throws Exception {
    assertEquals("\\28", PSJndiUtils.escapeLdapFilterValue("("));
    assertEquals("\\29", PSJndiUtils.escapeLdapFilterValue(")"));
    assertEquals("\\5c", PSJndiUtils.escapeLdapFilterValue("\\"));
    assertEquals("\\00", PSJndiUtils.escapeLdapFilterValue("\u0000"));
  }

  @Test
  public void escapeHelperNullSafe() throws Exception {
    assertNull(PSJndiUtils.escapeLdapFilterValue(null));
    assertEquals("", PSJndiUtils.escapeLdapFilterValue(""));
  }

  @Test
  public void escapeHelperPreservesAscii() throws Exception {
    assertEquals("admin", PSJndiUtils.escapeLdapFilterValue("admin"));
  }

  @Test
  public void escapeHelperEncodesUtf8Multibyte() throws Exception {
    // 'é' is 0xc3 0xa9 in UTF-8 — must be hex-escaped byte-by-byte.
    assertEquals("\\c3\\a9", PSJndiUtils.escapeLdapFilterValue("é"));
  }

  @Test
  public void getFilterStringEscapesUserInputFromInjection() throws Exception {
    // Without escaping, this input would terminate the parenthesised filter and start a new
    // OR'd clause that matches every entry in the directory. After the fix the special chars are
    // hex-escaped, so the filter still wraps a single attribute comparison.
    String malicious = "*)(uid=*";
    String result = PSJndiUtils.getFilterString(new String[] {malicious}, "cn", "(objectClass=*)");
    assertNotNull(result);
    assertTrue(
        "expected RFC 4515 escape for '(' in user value, got: " + result,
        result.contains("\\28"));
    assertTrue(
        "expected RFC 4515 escape for ')' in user value, got: " + result,
        result.contains("\\29"));
    assertTrue("wildcard '*' must be preserved, got: " + result, result.contains("=*"));
  }

  @Test
  public void getFilterStringEscapesBackslashAndNul() throws Exception {
    String[] specials = {"\\", "(", ")", "\u0000"};
    for (String s : specials) {
      String result = PSJndiUtils.getFilterString(new String[] {s}, "cn", "");
      assertNotNull("escape must not return null for: " + s, result);
      if (s.equals("\\")) {
        assertTrue("backslash must be hex-escaped", result.contains("\\5c"));
      } else if (s.equals("(")) {
        assertTrue("'(' must be hex-escaped", result.contains("\\28"));
      } else if (s.equals(")")) {
        assertTrue("')' must be hex-escaped", result.contains("\\29"));
      } else if (s.equals("\u0000")) {
        assertTrue("NUL must be hex-escaped", result.contains("\\00"));
      }
    }
  }

  @Test
  public void getFilterStringPreservesWildcardContract() throws Exception {
    // Public contract: '%' is the documented wildcard input; it is converted to '*' by
    // processFilter. After the LDAP-injection fix the '*' must remain unescaped so wildcard
    // searches still work end-to-end.
    String result = PSJndiUtils.getFilterString(new String[] {"adm%n"}, "cn", "");
    assertEquals("(| (cn=adm*n))", result);
  }

  @Test
  public void getFilterStringRejectsEmptyPatternInput() throws Exception {
    try {
      PSJndiUtils.getFilterString(new String[] {""}, "cn", "");
      fail("expected IllegalArgumentException for empty filter input");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }
}