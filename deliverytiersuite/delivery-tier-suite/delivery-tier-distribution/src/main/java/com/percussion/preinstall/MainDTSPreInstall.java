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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.preinstall;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainDTSPreInstall {
  private static String DISTRIBUTION_DIR = "distribution";
  private static final String PERC_JAVA_HOME = "perc.java.home";
  private static final String JAVA_HOME = "java.home";
  private static final String PERCUSSION_VERSION = "perc.version";
  private static final String INSTALL_TEMPDIR = "percDTSInstallTmp_";
  private static final String PERC_ANT_JAR = "perc-ant";
  private static final String ANT_INSTALL = "installDts.xml";

  /**
   * Find a jar by path pattern to avoid hard coding / forcing version.
   *
   * @param execPath Folder containing the jar
   * @param fileNameWithPattern A File name with a glob pattern like perc-ant-*.jar
   * @return Path to the ant jar
   * @throws IOException
   */
  private static Path getVersionLessJarFilePath(Path execPath, String fileNameWithPattern)
      throws IOException {
    try (DirectoryStream<Path> ds =
        Files.newDirectoryStream(execPath.toAbsolutePath(), fileNameWithPattern)) {
      List<Path> paths = new ArrayList<>();
      for (Path path : ds) {
        paths.add(path);
      }
      if (paths.isEmpty()) {
        throw new IOException(fileNameWithPattern + " not found.");
      } else if (paths.size() == 1) {
        return paths.get(0);
      } else {
        System.out.println(
            "Warning: Multiple "
                + fileNameWithPattern
                + " jars found, selecting the first one: "
                + paths.get(0).toAbsolutePath().toString());
        return paths.get(0);
      }
    }
  }

  private static File tmpFolder;

  public static void main(String[] args) {
    int exitCode = 0;
    try {

      String javaHome = System.getProperty(PERC_JAVA_HOME);
      if (javaHome == null || javaHome.trim().isEmpty()) javaHome = System.getProperty(JAVA_HOME);

      Path javaBinPath =
          Paths.get(
              javaHome,
              "bin",
              System.getProperty("file.separator").equals("/") ? "java" : "java.exe");
      String javabin = javaBinPath.toString();

      String percVersion = System.getProperty(PERCUSSION_VERSION);
      if (percVersion == null) percVersion = "";

      System.out.println("perc.java.home=" + javaHome);
      System.out.println("java.executable=" + javabin);
      System.out.println("perc.version=" + percVersion);

      if (args.length < 1) {
        System.out.println("Must specify installation or upgrade folder");
        System.exit(0);
      }

      System.out.println("Installation folder =" + args[0]);
      Path installPath = Paths.get(args[0]).normalize();

      String isProduction = "true";
      isProduction = System.getProperty("install.prod.dts");
      System.out.println(
          "====Will remove below code if value of is Production comes fine PSDeliveryTierServerTYpePanel"
              + isProduction);

      Path staging = installPath.resolve("Staging");
      Path prod = installPath.resolve("Deployment");

      if (Files.exists(staging) && !Files.exists(prod)) {
        isProduction = "false";
      }
      // if isProduction value is not passed in and we are not able to figure out either, then set
      // the value to be true
      // e.g. in case of upgrade installer is passing value $DTS_SERVER_TYPE$, which doesn't match
      // any of the cases and thus fails
      if (isProduction == null
          || isProduction.isEmpty()
          || (!isProduction.toLowerCase(Locale.ROOT).equals("true")
              && !isProduction.toLowerCase(Locale.ROOT).equals("false"))) {
        isProduction = "true"; // change done for dev environment
      }

      Path installSrc;
      Path currentJar =
          Paths.get(
              MainDTSPreInstall.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!Files.isDirectory(currentJar)) {
        installSrc = Files.createTempDirectory(INSTALL_TEMPDIR);
        System.out.println("install.tempdir=" + installSrc);
        // add option to not delete for debugging
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread() {
                  @Override
                  public void run() {
                    try {
                      Files.walk(installSrc)
                          .sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                    } catch (IOException ex) {
                      System.out.println(
                          "An error occurred processing installation files. " + ex.getMessage());
                    }
                  }
                });

        extractArchive(currentJar, installSrc, DISTRIBUTION_DIR);
      } else {
        System.out.println("Running from extracted jar");
        installSrc = currentJar.resolve(DISTRIBUTION_DIR);
      }

      Path execPath = installSrc.resolve(Paths.get("rxconfig", "Installer"));
      Path installAntJarPath =
          execPath.resolve(getVersionLessJarFilePath(execPath, PERC_ANT_JAR + "-*.jar"));

      exitCode = execJar(installAntJarPath, execPath, installPath, isProduction);

    } catch (IOException | URISyntaxException | InterruptedException e) {
      System.out.println(
          "An unexpected error occurred processing installation files. " + e.getMessage());
      throw new AntJobFailedException(
          String.format(Locale.ROOT, "Installation failed. %s", e.getMessage()));
    }
    System.out.println(String.format(Locale.ROOT, "Done extracting exit code %d", exitCode));
    if (exitCode != 0) {
      throw new AntJobFailedException(
          String.format(Locale.ROOT, "Installation failed. Exit code: %d ", exitCode));
    }
  }

  public static void extractArchive(Path archiveFile, Path destPath, String folderPrefix)
      throws IOException {

    Files.createDirectories(destPath); // create dest path folder(s)

    try (ZipFile archive = new ZipFile(archiveFile.toFile())) {

      // sort entries by name to always create folders first
      List<? extends ZipEntry> entries =
          archive.stream()
              .sorted(Comparator.comparing(ZipEntry::getName))
              .collect(Collectors.toList());

      // copy each entry in the dest path
      // T2.6 hardening (issue #89): the default 10k-entry / 500 MB caps are for
      // attacker-controlled archives. The archive here is the DTS distribution jar
      // itself, a build-controlled trusted artifact, so disable the caps. The
      // ZipSlipGuard + canonical-path checks below still apply.
      com.percussion.security.io.PSZipBombGuard guard =
          new com.percussion.security.io.PSZipBombGuard(
              Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
      for (ZipEntry entry : entries) {
        guard.check(entry);
        String entryName = entry.getName();
        // Analyzer-visible zipslip sanitizer (java/zipslip): dominating check on the raw
        // ZipEntry name. ZipSlipGuard is a runtime guard only — CodeQL does not model it.
        if (entryName.indexOf("..") >= 0
            || entryName.startsWith("/")
            || entryName.startsWith("\\")) {
          throw new SecurityException("zip slip: " + entryName);
        }
        if (!entryName.startsWith(folderPrefix)) continue;

        String name = entryName.substring(folderPrefix.length() + 1);
        if (name.length() == 0) continue;
        if (name.indexOf("..") >= 0 || name.startsWith("/") || name.startsWith("\\")) {
          throw new SecurityException("zip slip: " + name);
        }

        // Zip-slip guard (CodeQL java/zipslip alert #494): validate that the resolved path
        // stays under destPath before any mkdirs/Files.copy. The entry name and the
        // folderPrefix stripping are both attacker-controlled (jar archive).
        File extractDir = destPath.toFile();
        File newFile = com.percussion.security.io.ZipSlipGuard.safeDestFile(extractDir, name);
        String destCanon = newFile.getCanonicalPath();
        String rootCanon = extractDir.getCanonicalPath();
        if (!destCanon.equals(rootCanon) && !destCanon.startsWith(rootCanon + File.separator)) {
          throw new SecurityException("zip slip: " + name);
        }

        if (entry.isDirectory()) {
          // codeql[java/zipslip] justification: ZipSlipGuard + canonical startsWith; re-review by
          // 2027-07-31
          Files.createDirectory(newFile.toPath());
          continue;
        }
        System.out.println("Creating file " + newFile);
        // codeql[java/zipslip] justification: ZipSlipGuard + canonical startsWith; re-review by
        // 2027-07-31
        Files.copy(archive.getInputStream(entry), newFile.toPath());

        // Preserve executable permissions for shell scripts
        if (entryName.endsWith(".sh")) {
          newFile.setExecutable(true, false); // Set executable for owner, group, and others
        }
      }
    }
  }

  public static int execJar(Path jar, Path execPath, Path installDir, String isProduction)
      throws IOException, InterruptedException {

    String dir = installDir.toAbsolutePath().toString();
    String javaHome = System.getProperty(PERC_JAVA_HOME);
    if (javaHome == null || javaHome.trim().isEmpty()) javaHome = System.getProperty(JAVA_HOME);

    Path javaBinPath =
        Paths.get(
            javaHome,
            "bin",
            System.getProperty("file.separator").equals("/") ? "java" : "java.exe");
    String javabin = javaBinPath.toString();

    System.out.println("isProduction:" + isProduction);
    System.out.println("Install Dir:" + dir);
    System.out.println("Java Executable:" + javabin);

    ProcessBuilder builder =
        new ProcessBuilder(
                javabin,
                "-Dinstall.prod.dts=" + isProduction,
                "-Dfile.encoding=UTF8",
                "-Dsun.jnu.encoding=UTF8",
                "-Dinstall.dir=" + dir,
                "-Drxdeploydir=" + dir,
                "-jar",
                jar.toAbsolutePath().toString(),
                "-f",
                ANT_INSTALL)
            .directory(execPath.toFile());
    Process process = builder.inheritIO().start();
    process.waitFor();
    return process.exitValue();
  }
}
