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

import com.percussion.error.PSExceptionUtils;
import com.percussion.install.PSLogger;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.BuildException;

/**
 * * Provides an ant task to upgrade a derby database. Expects to be able to start the derby
 * database in embedded mode - single user
 */
public class PSUpgradeDerby extends PSAction {

  private String targetVersion;

  public String getTargetVersion() {
    return targetVersion;
  }

  public void setTargetVersion(String targetVersion) {
    this.targetVersion = targetVersion;
  }

  private String databasePath;

  public String getDatabasePath() {
    return databasePath;
  }

  public void setDatabasePath(String databasePath) {
    this.databasePath = databasePath;
  }

  private String backupDirectory;

  public String getBackupDirectory() {
    return backupDirectory;
  }

  public void setBackupDirectory(String backupDirectory) {
    this.backupDirectory = backupDirectory;
  }

  private String userName;
  private String password;
  private String schema;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  private static final String DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";

  private static final String DERBY_JAR_PATTERN = "derby-*.jar";

  private void dropPercMetadataPropertiesTable(Connection conn) {
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Could not create statement to drop the metadata tables"
              + PSExceptionUtils.getMessageForLog(e));
      return;
    }
    try {
      String sql2 = "DROP TABLE PERC_PAGE_METADATA_PROPERTIES";
      stmt.executeUpdate(sql2);

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_PAGE_METADATA_PROPERTIES failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
    try {
      String sql = "DROP TABLE PERC_PAGE_METADATA";
      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_PAGE_METADATA failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
    try {
      String sql = "SELECT COUNT(*) from PERC_COOKIE_CONSENT";
      ResultSet rs = stmt.executeQuery(sql);
      rs.next();
      int count = rs.getInt(1);
      rs.close();
      PSLogger.logInfo("Got Result in PERC_COOKIE_CONSENT.");
      if (count == 0) {
        PSLogger.logInfo("Rows in PERC_COOKIE_CONSENT are 0 thus deleting table.");
        String sql2 = "DROP TABLE PERC_COOKIE_CONSENT";
        stmt.execute(sql2);
      }

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_COOKIE_CONSENT failed, may be doesn't exist: "
              + PSExceptionUtils.getMessageForLog(e));
    }

    try {
      String sql = "SELECT COUNT(*) from BLOG_POST_VISIT";
      ResultSet rs = stmt.executeQuery(sql);
      rs.next();
      int count = rs.getInt(1);
      rs.close();
      PSLogger.logInfo("Got Result in BLOG_POST_VISIT.");
      if (count == 0) {
        PSLogger.logInfo("Rows in BLOG_POST_VISIT are 0 thus deleting table.");
        String sql2 = "DROP TABLE BLOG_POST_VISIT";
        stmt.execute(sql2);
      }

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for BLOG_POST_VISIT failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
  }

  /**
   * Finds the jar containing the embedded Derby driver that is shipped with the installation.
   *
   * @return the driver jar, or <code>null</code> if it cannot be found.
   */
  private File findDerbyDriverJar() {
    File dir = new File(getRootDir() + File.separator + "Deployment/Server/common/lib");
    FileFilter fileFilter = new WildcardFileFilter(DERBY_JAR_PATTERN);
    File[] files = dir.listFiles(fileFilter);
    if (files == null || files.length == 0) {
      return null;
    }
    if (files.length > 1) {
      PSLogger.logError("Multiple versions of DerbyDriver Exist in " + dir.toString());
    }
    return files[0];
  }

  /**
   * Loads the embedded Derby driver.
   *
   * <p>The driver is looked up on the given class loader first. When it is not available there,
   * which is the case when the Derby jars are not part of the Ant build class path, the driver is
   * loaded from the jar shipped with the installation using a dedicated class loader.
   *
   * <p>The driver is used directly instead of going through {@link java.sql.DriverManager}, because
   * {@code DriverManager} only hands out a driver to a caller that is able to see it through the
   * thread context class loader. The class loader of the Ant task is not guaranteed to be the
   * thread context class loader, so the lookup through {@code DriverManager} fails whenever the
   * driver is not on the build class path.
   *
   * @param driverJar jar to load the driver from, may be <code>null</code>.
   * @param parentLoader class loader used to look up the driver before falling back to the jar.
   * @return the Derby driver, never <code>null</code>.
   * @throws Exception if the driver cannot be loaded.
   */
  Driver loadDerbyDriver(File driverJar, ClassLoader parentLoader) throws Exception {
    try {
      Class<?> driverClass = Class.forName(DRIVER_CLASS, true, parentLoader);
      return (Driver) driverClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      PSLogger.logInfo(
          "Derby driver was not found on the build class path, loading it from the installation...");
    }

    if (driverJar == null) {
      throw new FileNotFoundException(
          "Derby driver jar not found in "
              + getRootDir()
              + File.separator
              + "Deployment"
              + File.separator
              + "Server"
              + File.separator
              + "common"
              + File.separator
              + "lib");
    }
    PSLogger.logInfo("Loading DerbyDriver File " + driverJar.toString());
    // The driver keeps a reference to the class loader that loaded it, so the loader stays open
    // for as long as the driver is used.
    URLClassLoader loader = new URLClassLoader(new URL[] {driverJar.toURI().toURL()}, parentLoader);
    Class<?> driverClass = Class.forName(DRIVER_CLASS, true, loader);
    return (Driver) driverClass.getDeclaredConstructor().newInstance();
  }

  @Override
  public void execute() throws BuildException {

    if (!Files.exists(Paths.get(databasePath))) {
      throw new BuildException("Database " + databasePath + " does not exist!");
    }

    if (!Files.exists(Paths.get(backupDirectory))) {
      throw new BuildException("Backup directory does not exist!");
    }

    Driver derbyDriver;
    try {
      derbyDriver = loadDerbyDriver(findDerbyDriverJar(), getClass().getClassLoader());
    } catch (Exception e) {
      throw new BuildException("Unable to load embedded Derby driver", e);
    }

    // Connection properties
    Properties props = new Properties();
    props.setProperty("user", userName);
    props.setProperty("password", password);
    props.setProperty("upgrade", "true");
    String connectionUrl = "jdbc:derby:" + databasePath;
    Connection conn;
    try {
      conn = derbyDriver.connect(connectionUrl, props);
      if (conn == null) {
        throw new SQLException("Derby driver does not accept the connection url " + connectionUrl);
      }
    } catch (SQLException e) {
      throw new BuildException(e);
    }

    try {
      if (getDatabasePath().contains("percmetadata")) {
        // Drop Metadata Tables on upgrade.
        dropPercMetadataPropertiesTable(conn);
      }
      DatabaseMetaData meta = conn.getMetaData();
      PSLogger.logInfo(
          "Derby database version: " + meta.getDatabaseProductVersion() + " detected...");
      conn.close();
      props.remove("upgrade");
      props.putIfAbsent("shutdown", "true");
      PSLogger.logInfo("Shutting down database :" + databasePath);
      conn = derbyDriver.connect(connectionUrl, props);
      conn.close();

    } catch (SQLNonTransientConnectionException e) {
      PSLogger.logWarn("SQL State:" + e.getSQLState());
      PSLogger.logWarn("SQL Error Code:" + e.getErrorCode());
      if (e.getErrorCode() == 45000 && e.getSQLState().equals("08006")) {
        PSLogger.logInfo("Database shutdown successfully.");
      } else {
        throw new BuildException(e);
      }
    } catch (SQLException e) {
      PSLogger.logWarn("SQL State:" + e.getSQLState());
      PSLogger.logWarn("SQL Error Code:" + e.getErrorCode());
      if (e.getErrorCode() == 45000 && e.getSQLState().equals("08006")) {
        PSLogger.logInfo("Database shutdown successfully.");
      } else {
        throw new BuildException(e);
      }
    } finally {
      try {
        conn.close();
      } catch (Exception e) {
        // do nada}
      }
    }
  }
}
