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

package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link PSUpgradeSiteConfig} safe no-op behavior when webapps/secure is absent. */
public class PSUpgradeSiteConfigTest {

  @TempDir Path tempDir;

  @Test
  @SuppressWarnings("deprecation")
  void skipsSafelyAndPreservesSiteConfigWhenSourceTemplateAbsent() throws Exception {
    Path siteDir = tempDir.resolve("rxconfig").resolve("SiteConfigs").resolve("TestSite");
    Files.createDirectories(siteDir);
    Path marker = siteDir.resolve("marker.txt");
    Files.write(marker, "preserve-me".getBytes(StandardCharsets.UTF_8));

    PSUpgradeSiteConfig task = new PSUpgradeSiteConfig();
    task.setRootDir(tempDir.toString());
    task.execute();

    assertTrue(Files.exists(marker));
    assertEquals("preserve-me", new String(Files.readAllBytes(marker), StandardCharsets.UTF_8));
  }
}
