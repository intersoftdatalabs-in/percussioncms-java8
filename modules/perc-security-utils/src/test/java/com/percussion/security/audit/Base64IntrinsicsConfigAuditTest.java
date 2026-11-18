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
package com.percussion.security.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Offline audit verifying proactive CVE-2026-114938 ("Java4Shell") JVM hardening defaults across
 * CMS Jetty and DTS Tomcat 9 / Procrun launch configurations while keeping JDK 8 startup clean
 * (GH-288).
 */
public class Base64IntrinsicsConfigAuditTest {

  private static final String IGNORE_UNRECOGNIZED = "-XX:+IgnoreUnrecognizedVMOptions";
  private static final String DISABLE_BASE64 = "-XX:-UseBASE64Intrinsics";
  private static final String RESET_IGNORE_UNRECOGNIZED = "-XX:-IgnoreUnrecognizedVMOptions";

  /**
   * Ensures CMS Jetty {@code system/Tools/jetty/defaults/start.d/jvm.ini} places {@code
   * -XX:+IgnoreUnrecognizedVMOptions} and {@code -XX:-UseBASE64Intrinsics} under {@code --exec}
   * without platform-specific or locked diagnostic flags.
   */
  @Test
  public void jettyJvmIniConfiguresSafeBase64IntrinsicsDisablement() throws IOException {
    File root = requireProjectRoot();
    File jvmIni = new File(root, "system/Tools/jetty/defaults/start.d/jvm.ini");
    assertTrue("Missing jvm.ini: " + jvmIni.getAbsolutePath(), jvmIni.isFile());

    List<String> activeLines = readActiveIniLines(jvmIni);
    int execIdx = activeLines.indexOf("--exec");
    int ignoreIdx = activeLines.indexOf(IGNORE_UNRECOGNIZED);
    int disableIdx = activeLines.indexOf(DISABLE_BASE64);

    assertTrue("jvm.ini must declare --exec", execIdx >= 0);
    assertTrue(
        "jvm.ini must set " + IGNORE_UNRECOGNIZED + " as a standalone line under --exec",
        ignoreIdx > execIdx);
    assertTrue(
        "jvm.ini must set " + DISABLE_BASE64 + " as a standalone line under --exec",
        disableIdx > execIdx);

    String activeJoined = joinLines(activeLines);
    assertFalse(
        "jvm.ini must not turn off IgnoreUnrecognizedVMOptions",
        activeJoined.contains(RESET_IGNORE_UNRECOGNIZED));
    assertFalse(
        "jvm.ini must not set -XX:UseAVX= (causes JVM startup failure on ARM64)",
        activeJoined.contains("-XX:UseAVX="));
    assertFalse(
        "jvm.ini must not set -XX:DisableIntrinsic= without -XX:+UnlockDiagnosticVMOptions",
        activeJoined.contains("-XX:DisableIntrinsic=")
            && !activeJoined.contains("-XX:+UnlockDiagnosticVMOptions"));
  }

  /**
   * Ensures all 8 DTS Tomcat 9 / Procrun launch and service scripts configure {@code
   * -XX:+IgnoreUnrecognizedVMOptions} before {@code -XX:-UseBASE64Intrinsics}, avoid unsafe flags,
   * and pass {@code bash -n} syntax validation for shell scripts.
   */
  @Test
  public void dtsLaunchAndServiceScriptsConfigureSafeBase64IntrinsicsDisablement()
      throws IOException, InterruptedException {
    File root = requireProjectRoot();
    File dtsDistRoot =
        new File(root, "deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main");

    List<File> dtsFiles =
        Arrays.asList(
            new File(dtsDistRoot, "tomcat9/bin/setenv.sh"),
            new File(dtsDistRoot, "tomcat9/bin/setenv.bat"),
            new File(dtsDistRoot, "rootFiles/TomcatStartup.sh"),
            new File(dtsDistRoot, "rootFiles/TomcatStartup.bat"),
            new File(dtsDistRoot, "rootFiles/DTSProductionService.sh"),
            new File(dtsDistRoot, "rootFiles/DTSStagingService.sh"),
            new File(dtsDistRoot, "rootFiles/DTSProductionService.bat"),
            new File(dtsDistRoot, "rootFiles/DTSStagingService.bat"));

    File bash = new File("/bin/bash");
    boolean canRunBash = bash.isFile() && bash.canExecute();

    for (File script : dtsFiles) {
      assertTrue("Missing DTS script: " + script.getAbsolutePath(), script.isFile());
      String content = new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8);

      int ignoreIdx = content.indexOf(IGNORE_UNRECOGNIZED);
      int disableIdx = content.indexOf(DISABLE_BASE64);
      assertTrue(script.getName() + " must contain " + IGNORE_UNRECOGNIZED, ignoreIdx >= 0);
      assertTrue(script.getName() + " must contain " + DISABLE_BASE64, disableIdx >= 0);
      assertTrue(
          script.getName() + " must pass " + IGNORE_UNRECOGNIZED + " before " + DISABLE_BASE64,
          ignoreIdx < disableIdx);
      assertFalse(
          script.getName() + " must not contain " + RESET_IGNORE_UNRECOGNIZED,
          content.contains(RESET_IGNORE_UNRECOGNIZED));
      assertFalse(
          script.getName() + " must not set -XX:UseAVX= (breaks ARM64)",
          content.contains("-XX:UseAVX="));
      assertFalse(
          script.getName() + " must not set -XX:DisableIntrinsic=",
          content.contains("-XX:DisableIntrinsic="));

      if (canRunBash && script.getName().endsWith(".sh")) {
        assertProcessExitsZero(
            Arrays.asList(bash.getAbsolutePath(), "-n", script.getAbsolutePath()),
            "bash -n syntax check failed for " + script.getName());
      }
    }
  }

  /**
   * Verifies that launching the active JVM (JDK 8 or JDK 17/21+) with {@code
   * -XX:+IgnoreUnrecognizedVMOptions -XX:-UseBASE64Intrinsics} exits cleanly with status 0.
   */
  @Test
  public void shippedFlagsStartCleanlyOnCurrentJvm() throws IOException, InterruptedException {
    String javaHome = System.getProperty("java.home");
    boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    File javaBin = new File(new File(javaHome, "bin"), isWindows ? "java.exe" : "java");
    assertTrue("Missing java binary at " + javaBin.getAbsolutePath(), javaBin.isFile());

    assertProcessExitsZero(
        Arrays.asList(javaBin.getAbsolutePath(), IGNORE_UNRECOGNIZED, DISABLE_BASE64, "-version"),
        "JVM failed to start with shipped CVE-2026-114938 flags");
  }

  private static File requireProjectRoot() {
    File root = HibernateConfigAuditTest.findProjectRoot();
    assertTrue(
        "Could not locate project root (no directory above user.dir contains mvn-env.sh).",
        root != null);
    return root;
  }

  private static List<String> readActiveIniLines(File file) throws IOException {
    List<String> rawLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    List<String> active = new ArrayList<>();
    for (String line : rawLines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
        active.add(trimmed);
      }
    }
    return active;
  }

  private static String joinLines(List<String> lines) {
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  private static void assertProcessExitsZero(List<String> command, String failurePrefix)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = readFully(process.getInputStream());
    boolean finished = process.waitFor(15, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
    }
    assertTrue(failurePrefix + " (timed out): " + output, finished);
    assertEquals(failurePrefix + ": " + output, 0, process.exitValue());
  }

  private static String readFully(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }
}
