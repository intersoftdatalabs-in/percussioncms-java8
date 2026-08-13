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
package com.percussion.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import org.junit.Test;

/**
 * SSRF regression tests for {@link PSDtdTree}. Verifies that the constructor validates the DTD URL
 * through {@code URLValidation.validateURLString} before opening a connection, and rejects unsafe
 * targets (CodeQL java/ssrf alert #432 on 8.1.x).
 */
public class PSDtdTreeSsrfTest {

  @Test
  public void rejectsAwsMetadataHost() throws Exception {
    URL malicious = new URL("http://169.254.169.254/latest/meta-data/");
    try {
      new PSDtdTree(malicious);
      fail("expected SSRF rejection for AWS metadata host");
    } catch (Exception expected) {
      // pass — URLValidation throws SecurityException, PSCatalogException wraps it
      assertNotNull(expected.getMessage());
      assertTrue(
          "expected message to mention SSRF or validation, got: " + expected.getMessage(),
          expected.getMessage().toLowerCase().contains("ssrf")
              || expected.getMessage().toLowerCase().contains("reserved")
              || expected.getMessage().toLowerCase().contains("metadata")
              || expected.getMessage().toLowerCase().contains("not allowed"));
    }
  }

  @Test
  public void rejectsPrivateRfc1918Host() throws Exception {
    URL malicious = new URL("http://10.0.0.5/internal");
    try {
      new PSDtdTree(malicious);
      fail("expected SSRF rejection for private host");
    } catch (Exception expected) {
      // pass — baseline deny for private hosts with no allow-list match
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void rejectsFileScheme() throws Exception {
    URL malicious = new URL("file:///etc/passwd");
    try {
      new PSDtdTree(malicious);
      fail("expected SSRF rejection for file:// scheme");
    } catch (Exception expected) {
      // pass — only http/https are accepted by URLValidation
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void acceptsLoopbackLocalhost() throws Exception {
    // Loopback on any port is in the baseline allow (server-internal DTD refs).
    URL safe = new URL("http://127.0.0.1:9992/Rhythmyx/dtd/foo.dtd");
    try {
      // Will fail to *connect* but must NOT fail validation.
      new PSDtdTree(safe);
      // If somehow the loopback port actually had a server, no exception is fine too.
    } catch (Exception other) {
      // PSDtdTree may wrap connection errors in PSCatalogException - that's acceptable.
      String msg = other.getMessage() == null ? "" : other.getMessage().toLowerCase();
      assertTrue(
          "expected PSCatalogException to NOT be an SSRF rejection, got: " + msg,
          !msg.contains("reserved") && !msg.contains("metadata") && !msg.contains("not allowed"));
    }
  }
}