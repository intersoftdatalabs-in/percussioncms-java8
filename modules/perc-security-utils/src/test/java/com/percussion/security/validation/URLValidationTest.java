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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link URLValidation} - SSRF prevention + allow/block lists (issue #1205). JUnit 4
 * port of the development-branch Jupiter suite.
 */
public class URLValidationTest {

  @Before
  public void resetConfig() {
    URLValidationConfig.resetDefault();
    URLValidationConfig.setDefault(new URLValidationConfig());
  }

  @After
  public void clear() {
    URLValidationConfig.resetDefault();
  }

  // Baseline public and loopback

  @Test
  public void allowHttpsPublic() throws Exception {
    URLValidation.validateURL(new URL("https://example.com/api/resource"));
  }

  @Test
  public void allowHttpPort80() throws Exception {
    URLValidation.validateURL(new URL("http://example.com/path"));
  }

  @Test
  public void rejectNonStandardPortWithoutAllow() throws Exception {
    expectSecurity(() -> URLValidation.validateURL(new URL("http://example.com:8080/api")));
  }

  @Test
  public void allowLocalhostHighPort() throws Exception {
    URLValidation.validateURL(new URL("http://localhost:8080/api"));
  }

  @Test
  public void allowLoopback() throws Exception {
    URLValidation.validateURL(new URL("http://127.0.0.1:9992/"));
  }

  // Hard / default blocks

  @Test
  public void rejectAwsMetadata() throws Exception {
    expectSecurity(
        () -> URLValidation.validateURL(new URL("http://169.254.169.254/latest/meta-data/")));
  }

  @Test
  public void rejectPrivateWithoutAllow() throws Exception {
    expectSecurity(() -> URLValidation.validateURL(new URL("http://10.0.0.5/api")));
  }

  @Test
  public void rejectFileScheme() throws Exception {
    expectSecurity(() -> URLValidation.validateURL(new URL("file:///etc/passwd")));
  }

  @Test
  public void blockListWinsOverAllow() throws Exception {
    URLValidationConfig cfg =
        URLValidationConfig.builder()
            .addAllowPattern("http://169.254.169.254/*")
            .addBlockPattern("http://169.254.169.254/*")
            .build();
    expectSecurity(
        () -> URLValidation.validateURL(new URL("http://169.254.169.254/latest/meta-data/"), cfg));
  }

  @Test
  public void blockListWinsOverAllowForPrivate() throws Exception {
    URLValidationConfig cfg =
        URLValidationConfig.builder()
            .addAllowPattern("http://10.0.0.5/*")
            .addBlockPattern("http://10.0.0.5/*")
            .build();
    expectSecurity(() -> URLValidation.validateURL(new URL("http://10.0.0.5/api"), cfg));
  }

  // Additive allow list (US1)

  @Test
  public void allowPrivateWithPattern() throws Exception {
    URLValidationConfig cfg =
        URLValidationConfig.builder().addAllowPattern("http://10.0.0.5/*").build();
    URLValidation.validateURL(new URL("http://10.0.0.5/api/status"), cfg);
  }

  @Test
  public void allowCustomPortWithPattern() throws Exception {
    URLValidationConfig cfg =
        URLValidationConfig.builder().addAllowPattern("http://api.example.com:8080/*").build();
    URLValidation.validateURL(new URL("http://api.example.com:8080/v1"), cfg);
  }

  @Test
  public void allowPathScopedPrivateHost() throws Exception {
    URLValidationConfig cfg =
        URLValidationConfig.builder().addAllowPattern("http://10.0.0.9/v1/*").build();
    URLValidation.validateURL(new URL("http://10.0.0.9/v1/translate"), cfg);
    expectSecurity(() -> URLValidation.validateURL(new URL("http://10.0.0.9/admin"), cfg));
  }

  @Test
  public void emptyAllowStillPermitsBaselinePublic() throws Exception {
    URLValidationConfig cfg = new URLValidationConfig();
    URLValidation.validateURL(new URL("https://example.com/ok"), cfg);
  }

  @Test
  public void loneStarDoesNotAllowAll() throws Exception {
    URLValidationConfig cfg = URLValidationConfig.builder().addAllowPattern("*").build();
    expectSecurity(() -> URLValidation.validateURL(new URL("http://10.1.1.1/x"), cfg));
  }

  // Input validation

  @Test
  public void nullUrl() {
    try {
      URLValidation.validateURL(null);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  @Test
  public void emptyString() {
    try {
      URLValidation.validateURLString("");
      fail("expected IllegalArgumentException");
    } catch (Exception expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void malformedString() {
    try {
      URLValidation.validateURLString("not a url");
      fail("expected MalformedURLException");
    } catch (MalformedURLException expected) {
      // pass
    } catch (Exception other) {
      fail("expected MalformedURLException, got: " + other.getClass().getName());
    }
  }

  private static void expectSecurity(ThrowingRunnable r) throws Exception {
    try {
      r.run();
      fail("expected SecurityException");
    } catch (SecurityException expected) {
      // pass
    } catch (Exception other) {
      fail("expected SecurityException, got: " + other.getClass().getName());
    }
  }

  /** Like {@link Runnable} but lets checked exceptions propagate. */
  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
