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

package com.percussion.security.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * JUnit 4 port of the 004 PSRedirectValidationTest (T051 / PR #1344) covering CWE-601 (Open
 * Redirect) prevention: relative URL validation, absolute URL whitelist validation, open-redirect
 * attack prevention, protocol-relative / javascript / data URIs, directory traversal, and internal
 * redirect validation.
 */
public class PSRedirectValidationTest {

  private static final Set<String> DEFAULT_WHITELIST = createDefaultWhitelist();

  private static Set<String> createDefaultWhitelist() {
    Set<String> whitelist = new HashSet<>();
    whitelist.add("example.com");
    whitelist.add("www.example.com");
    whitelist.add("api.example.com");
    return whitelist;
  }

  // ---- Relative URL validation (internal redirects) ----

  @Test
  public void testSimpleRelativePath() {
    assertEquals(
        "/dashboard", PSRedirectValidation.validateRedirectUrl("/dashboard", DEFAULT_WHITELIST));
  }

  @Test
  public void testRelativePathWithQuery() {
    assertEquals(
        "/pages/view?id=123&tab=summary",
        PSRedirectValidation.validateRedirectUrl(
            "/pages/view?id=123&tab=summary", DEFAULT_WHITELIST));
  }

  @Test
  public void testRelativePathWithFragment() {
    assertEquals(
        "/docs/api#authentication",
        PSRedirectValidation.validateRedirectUrl("/docs/api#authentication", DEFAULT_WHITELIST));
  }

  @Test
  public void testDirectoryTraversal() {
    assertNull(PSRedirectValidation.validateRedirectUrl("/../../etc/passwd", DEFAULT_WHITELIST));
  }

  @Test
  public void testDirectoryTraversalVariant() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl(
            "/safe/path/../../../sensitive", DEFAULT_WHITELIST));
  }

  @Test
  public void testDeeplyNestedPath() {
    assertEquals(
        "/a/b/c/d/e/f/g/h",
        PSRedirectValidation.validateRedirectUrl("/a/b/c/d/e/f/g/h", DEFAULT_WHITELIST));
  }

  // ---- Open redirect attack prevention ----

  @Test
  public void testProtocolRelativeUrl() {
    assertNull(PSRedirectValidation.validateRedirectUrl("//evil.com/phishing", DEFAULT_WHITELIST));
  }

  @Test
  public void testProtocolRelativeUrlVariant() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl(
            "//www.attacker.com/steal-data", DEFAULT_WHITELIST));
  }

  @Test
  public void testUnwhitelistedExternalUrl() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl(
            "http://attacker.com/phishing", DEFAULT_WHITELIST));
  }

  @Test
  public void testUnwhitelistedHttpsUrl() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("https://malicious.org/steal", DEFAULT_WHITELIST));
  }

  @Test
  public void testWhitelistedHttpUrl() {
    assertEquals(
        "http://example.com/page",
        PSRedirectValidation.validateRedirectUrl("http://example.com/page", DEFAULT_WHITELIST));
  }

  @Test
  public void testWhitelistedHttpsUrl() {
    assertEquals(
        "https://www.example.com/secure",
        PSRedirectValidation.validateRedirectUrl(
            "https://www.example.com/secure", DEFAULT_WHITELIST));
  }

  @Test
  public void testWhitelistedSubdomain() {
    assertEquals(
        "https://api.example.com/v1/data",
        PSRedirectValidation.validateRedirectUrl(
            "https://api.example.com/v1/data", DEFAULT_WHITELIST));
  }

  @Test
  public void testRejectFtpUrl() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("ftp://example.com/file", DEFAULT_WHITELIST));
  }

  @Test
  public void testRejectFileUrl() {
    assertNull(PSRedirectValidation.validateRedirectUrl("file:///etc/passwd", DEFAULT_WHITELIST));
  }

  // ---- JavaScript and data URI attack prevention ----

  @Test
  public void testRejectJavaScriptUrl() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("javascript:alert('XSS')", DEFAULT_WHITELIST));
  }

  @Test
  public void testRejectJavaScriptUrlCapitalization() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("JavaScript:alert('XSS')", DEFAULT_WHITELIST));
  }

  @Test
  public void testRejectDataUri() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl(
            "data:text/html,<script>alert('XSS')</script>", DEFAULT_WHITELIST));
  }

  @Test
  public void testRejectVbScriptUrl() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("vbscript:msgbox('XSS')", DEFAULT_WHITELIST));
  }

  // ---- Edge cases and special characters ----

  @Test
  public void testNullUrlThrows() {
    try {
      PSRedirectValidation.validateRedirectUrl(null, DEFAULT_WHITELIST);
      fail("null URL should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertTrue(true);
    }
  }

  @Test
  public void testEmptyUrl() {
    assertNull(PSRedirectValidation.validateRedirectUrl("", DEFAULT_WHITELIST));
  }

  @Test
  public void testWhitespaceOnlyUrl() {
    assertNull(PSRedirectValidation.validateRedirectUrl("   ", DEFAULT_WHITELIST));
  }

  @Test
  public void testWhitespaceTrimming() {
    assertEquals(
        "/dashboard",
        PSRedirectValidation.validateRedirectUrl("  /dashboard  ", DEFAULT_WHITELIST));
  }

  @Test
  public void testEncodedCharacters() {
    assertEquals(
        "/search?q=hello%20world&sort=date",
        PSRedirectValidation.validateRedirectUrl(
            "/search?q=hello%20world&sort=date", DEFAULT_WHITELIST));
  }

  @Test
  public void testEmptyWhitelist() {
    assertNull(
        PSRedirectValidation.validateRedirectUrl("http://example.com/page", new HashSet<String>()));
  }

  @Test
  public void testUrlWithPort() {
    assertEquals(
        "http://example.com:8080/api",
        PSRedirectValidation.validateRedirectUrl("http://example.com:8080/api", DEFAULT_WHITELIST));
  }

  // ---- Internal redirect validation ----

  @Test
  public void testSimpleInternalPath() {
    assertEquals("/admin", PSRedirectValidation.validateInternalRedirectUrl("/admin"));
  }

  @Test
  public void testInternalPathWithQuery() {
    assertEquals(
        "/pages?id=123", PSRedirectValidation.validateInternalRedirectUrl("/pages?id=123"));
  }

  @Test
  public void testRejectExternalUrlInInternal() {
    assertNull(PSRedirectValidation.validateInternalRedirectUrl("http://example.com/page"));
  }

  @Test
  public void testRejectProtocolRelativeInInternal() {
    assertNull(PSRedirectValidation.validateInternalRedirectUrl("//evil.com"));
  }

  @Test
  public void testRejectTraversalInInternal() {
    assertNull(PSRedirectValidation.validateInternalRedirectUrl("/../../etc/passwd"));
  }

  @Test
  public void testRejectNonSlashRelativeInInternal() {
    assertNull(PSRedirectValidation.validateInternalRedirectUrl("dashboard"));
  }

  // ---- Default whitelist ----

  @Test
  public void testCreateDefaultWhitelist() {
    Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("example.com");
    assertTrue(whitelist.contains("example.com"));
    assertTrue(whitelist.contains("www.example.com"));
  }
}
