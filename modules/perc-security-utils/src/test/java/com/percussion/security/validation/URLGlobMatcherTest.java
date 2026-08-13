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
package com.percussion.security.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.junit.Test;

/** JUnit 4 port of the development-branch URLGlobMatcher tests. */
public class URLGlobMatcherTest {

  @Test
  public void testNormalize() throws Exception {
    URL u = new URL("HTTPS://Example.COM/Path?q=1");
    assertEquals("https://example.com/Path?q=1", URLGlobMatcher.normalize(u));
  }

  @Test
  public void testNormalizePort() throws Exception {
    URL u = new URL("http://hr.internal:8080/api");
    assertEquals("http://hr.internal:8080/api", URLGlobMatcher.normalize(u));
  }

  @Test
  public void testGlobPath() throws Exception {
    String n = URLGlobMatcher.normalize(new URL("https://api.example.com/v1/x"));
    assertTrue(URLGlobMatcher.matches("https://api.example.com/v1/*", n));
  }

  @Test
  public void testGlobPathMismatch() throws Exception {
    String n = URLGlobMatcher.normalize(new URL("https://api.example.com/v2/x"));
    assertFalse(URLGlobMatcher.matches("https://api.example.com/v1/*", n));
  }

  @Test
  public void testLoneStar() {
    assertFalse(URLGlobMatcher.matches("*", "https://evil.com/"));
  }
}
