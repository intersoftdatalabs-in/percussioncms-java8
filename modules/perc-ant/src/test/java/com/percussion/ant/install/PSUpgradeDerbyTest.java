/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the Derby upgrade task. The task has to be able to load the embedded Derby driver from the
 * jar shipped with the installation, because the driver is not part of the Ant build class path.
 */
@Tag("UnitTest")
public class PSUpgradeDerbyTest {

  private static final String DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";

  @TempDir Path tempDir;

  private File derbyJar;

  @BeforeEach
  public void setUp() throws Exception {
    derbyJar = locateDerbyJar();
  }

  /** Locates the Derby driver jar the tests are running against. */
  private static File locateDerbyJar() throws Exception {
    Class<?> driverClass = Class.forName(DRIVER_CLASS);
    return new File(driverClass.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  /**
   * A class loader that cannot see the Derby driver, mimicking an Ant build class path that does
   * not contain the Derby jars.
   */
  private static ClassLoader isolatedClassLoader() throws Exception {
    return new URLClassLoader(new URL[0], null);
  }

  @Test
  public void testLoadDriverFromInstallationJarWhenNotOnClassPath() throws Exception {
    ClassLoader isolated = isolatedClassLoader();
    PSUpgradeDerby task = new PSUpgradeDerby();
    task.setRootDir(tempDir.toString());

    Driver driver = task.loadDerbyDriver(derbyJar, isolated);

    assertNotNull(driver);
    assertEquals(DRIVER_CLASS, driver.getClass().getName());
    assertNotSame(isolated, driver.getClass().getClassLoader());
  }

  @Test
  public void testLoadDriverFailsWhenJarIsMissing() throws Exception {
    PSUpgradeDerby task = new PSUpgradeDerby();
    task.setRootDir(tempDir.toString());

    FileNotFoundException e =
        assertThrows(
            FileNotFoundException.class, () -> task.loadDerbyDriver(null, isolatedClassLoader()));

    assertTrue(e.getMessage().contains("Deployment"), e.getMessage());
  }

  @Test
  public void testUpgradeDatabaseWithDriverLoadedFromJar() throws Exception {
    ClassLoader isolated = isolatedClassLoader();
    PSUpgradeDerby task = new PSUpgradeDerby();
    task.setRootDir(tempDir.toString());
    Driver driver = task.loadDerbyDriver(derbyJar, isolated);

    Path dbDir = tempDir.resolve("derbydata").resolve("db");
    Files.createDirectories(dbDir.getParent());

    Properties props = new Properties();
    props.setProperty("user", "APP");
    props.setProperty("password", "test");

    try (Connection conn = driver.connect("jdbc:derby:" + dbDir + ";create=true", props)) {
      assertNotNull(conn);
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE TABLE TEST_TABLE (ID INT)");
      }
    }

    // Boot the database the same way the task does, using the upgrade property.
    props.setProperty("upgrade", "true");
    try (Connection conn = driver.connect("jdbc:derby:" + dbDir, props)) {
      assertNotNull(conn);
      try (Statement stmt = conn.createStatement()) {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM TEST_TABLE");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
      }
    }

    // Shutting the database down must report the shutdown error code.
    props.remove("upgrade");
    props.setProperty("shutdown", "true");
    SQLException e =
        assertThrows(SQLException.class, () -> driver.connect("jdbc:derby:" + dbDir, props));
    assertEquals("08006", e.getSQLState());
  }

  @Test
  public void testTaskUpgradesMetadataDatabase() throws Exception {
    Path root = tempDir.resolve("install");
    Path libDir = root.resolve("Deployment").resolve("Server").resolve("common").resolve("lib");
    Files.createDirectories(libDir);
    Files.copy(derbyJar.toPath(), libDir.resolve(derbyJar.getName()));

    Path dbDir =
        root.resolve("Deployment").resolve("Server").resolve("derbydata").resolve("percmetadata");
    Files.createDirectories(dbDir.getParent());
    Path backupDir = root.resolve("backups");
    Files.createDirectories(backupDir);

    Class<?> driverClass = Class.forName(DRIVER_CLASS);
    Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
    Properties props = new Properties();
    props.setProperty("user", "APP");
    props.setProperty("password", "test");
    try (Connection conn = driver.connect("jdbc:derby:" + dbDir + ";create=true", props)) {
      assertNotNull(conn);
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE TABLE PERC_COOKIE_CONSENT (ID INT)");
        stmt.execute("CREATE TABLE BLOG_POST_VISIT (ID INT)");
      }
    }

    PSUpgradeDerby task = new PSUpgradeDerby();
    task.setRootDir(root.toString());
    task.setDatabasePath(dbDir.toString());
    task.setBackupDirectory(backupDir.toString());
    task.setUserName("APP");
    task.setPassword("test");

    task.execute();

    // The empty metadata tables must have been dropped by the task.
    Properties readProps = new Properties();
    readProps.setProperty("user", "APP");
    readProps.setProperty("password", "test");
    try (Connection conn = driver.connect("jdbc:derby:" + dbDir, readProps)) {
      assertNotNull(conn);
      try (Statement stmt = conn.createStatement()) {
        ResultSet rs =
            stmt.executeQuery(
                "SELECT COUNT(*) FROM SYS.SYSTABLES WHERE TABLENAME = 'PERC_COOKIE_CONSENT'");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
      }
    }

    // Shut the database down again so that the temporary files are released.
    Properties shutdownProps = new Properties();
    shutdownProps.setProperty("user", "APP");
    shutdownProps.setProperty("password", "test");
    shutdownProps.setProperty("shutdown", "true");
    assertThrows(SQLException.class, () -> driver.connect("jdbc:derby:" + dbDir, shutdownProps));
  }
}
