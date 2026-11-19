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

package com.percussion.ant.install;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Legacy Ant installer task that upgraded per-site secure configurations from {@code
 * sys_resources/webapps/secure/WEB-INF}.
 *
 * @deprecated {@code perc-secure-membership} and {@code sys_resources/webapps/secure} were retired
 *     in 8.1.9 (#297); retained as a safe no-op for Ant installer backwards compatibility ({@code
 *     install.xml} / {@code antlib.xml}).
 */
@Deprecated
public class PSUpgradeSiteConfig extends PSAction {

  Logger log = LogManager.getLogger(PSUpgradeSiteConfig.class);

  /**
   * The relative path (from the root install dir) of the source folder where the default secure
   * sites configurations files are stored.
   */
  public static final String SECURE_FILES_SOURCE_FOLDER = "sys_resources/webapps/secure/WEB-INF";

  /**
   * The relative path (from the root install dir) of the source folder where the default non secure
   * sites configurations files are stored.
   */
  public static final String NON_SECURE_FILES_SOURCE_FOLDER =
      "sys_resources/webapps/non-secure/WEB-INF";

  // see base class
  @Override
  public void execute() {
    File sourceFolder = getSourceConfigurationFolder();
    if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
      log.info(
          "Secure site configuration template folder not found (retired in 8.1.9); skipping"
              + " SiteConfig upgrade: "
              + sourceFolder);
      return;
    }
    String m_strRootDir = getRootDir();
    log.info("Adding Secure Site Config at : " + m_strRootDir.toString());
    File siteConfigDir =
        new File(m_strRootDir + File.separator + "rxconfig" + File.separator + "SiteConfigs");
    log.info("Adding Secure Site Config at : " + siteConfigDir);
    if (Files.exists(siteConfigDir.toPath())) {
      log.info("Adding Secure Site Config as SiteConfig Exists : " + siteConfigDir);
      File[] siteNames = siteConfigDir.listFiles(File::isDirectory);
      if (siteNames != null) {
        for (File siteName : siteNames) {
          try {
            log.info("Adding Secure Site Config at for Site: " + siteName);
            // Delete the folder and recreate
            FileUtils.deleteDirectory(siteName);
            FileUtils.copyDirectory(sourceFolder, siteName, false);
          } catch (IOException e) {
            log.error(
                "SiteConfigUpgrade Failed for Site"
                    + siteName.toString()
                    + " , Error: "
                    + e.getMessage());
            log.debug(
                "SiteConfigUpgrade Failed for Site"
                    + siteName.toString()
                    + " , Error: "
                    + e.getMessage(),
                e);
          }
        }
      } else {
        log.info("No Site Folder for Secure Site Config Found at : " + siteConfigDir.toString());
      }
    } else {
      log.info("No Secure Site Config found at : " + siteConfigDir.toString());
    }
  }

  private File getSourceConfigurationFolder() {
    String path = getRootDir();

    path +=
        (path.endsWith(File.separator))
            ? SECURE_FILES_SOURCE_FOLDER
            : File.separator + SECURE_FILES_SOURCE_FOLDER;

    return new File(path);
  }
}
