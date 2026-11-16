/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for issue #279: the DTS installer runs {@link PSUpgradeDerby} with a class path
 * that does not contain Derby, so the task must load the driver from the bundled {@code
 * Deployment/Server/common/lib/derby-*.jar}. The upgrade used to fail with "Unable to load embedded
 * Derby driver: ClassNotFoundException" because the driver was looked up again on the task class
 * loader after being loaded from the bundled jar.
 */
public class PSUpgradeDerbyTest {

  @TempDir Path tempDir;

  /**
   * Defines every non-JDK class itself, from the same class path as the test, but hides {@code
   * org.apache.derby.*}. This reproduces the installer, where the task class path does not contain
   * Derby (and no Derby package is visible to a class loader created by the task). The parent is
   * only the JDK (extension/platform) loader.
   */
  private static final class NoDerbyClassLoader extends ClassLoader {

    private final ClassLoader source;

    NoDerbyClassLoader(ClassLoader source) {
      super(ClassLoader.getSystemClassLoader().getParent());
      this.source = source;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("org.apache.derby.")) {
        throw new ClassNotFoundException(name);
      }
      synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          try {
            c = getParent().loadClass(name);
          } catch (ClassNotFoundException notJdk) {
            c = define(name);
          }
        }
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
    }

    private Class<?> define(String name) throws ClassNotFoundException {
      String resource = name.replace('.', '/') + ".class";
      try (InputStream in = source.getResourceAsStream(resource)) {
        if (in == null) {
          throw new ClassNotFoundException(name);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
        return defineClass(name, out.toByteArray(), 0, out.size());
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      }
    }
  }

  private static void createDatabase(Path db) throws SQLException {
    new EmbeddedDriver();
    try (Connection c =
        DriverManager.getConnection("jdbc:derby:" + db + ";create=true", "APP", "test")) {
      c.createStatement().execute("CREATE TABLE T1 (ID INT)");
    }
    try {
      DriverManager.getConnection("jdbc:derby:" + db + ";shutdown=true", "APP", "test");
    } catch (SQLException expected) {
      // Derby reports a successful database shutdown as an exception (SQLState 08006)
    }
  }

  private static void invoke(Object task, String method, String value) throws Exception {
    task.getClass().getMethod(method, String.class).invoke(task, value);
  }

  @Test
  public void shouldUpgradeDatabaseWhenDerbyIsOnlyInBundledJar() throws Exception {
    Path server = tempDir.resolve("Deployment").resolve("Server");
    Path db = server.resolve("derbydata").resolve("percmetadata");
    Files.createDirectories(db.getParent());
    createDatabase(db);

    // The installer ships the driver as Deployment/Server/common/lib/derby-<version>.jar
    Path lib = Files.createDirectories(server.resolve("common").resolve("lib"));
    Path derbyJar =
        Paths.get(EmbeddedDriver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Files.copy(derbyJar, lib.resolve("derby-test.jar"), StandardCopyOption.REPLACE_EXISTING);
    Path backups = Files.createDirectories(tempDir.resolve("backups"));

    // com.percussion.* classes are defined by NoDerbyClassLoader and org.apache.derby.* is hidden
    // from
    // it; everything else (Ant, log4j, java.*) comes from the application class loader.
    ClassLoader appLoader = PSUpgradeDerbyTest.class.getClassLoader();
    ClassLoader isolated = new NoDerbyClassLoader(appLoader);
    Class<?> taskClass = isolated.loadClass(PSUpgradeDerby.class.getName());
    assertTrue(taskClass != PSUpgradeDerby.class, "task must be defined by the isolated loader");
    boolean derbyVisible;
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver", false, isolated);
      derbyVisible = true;
    } catch (ClassNotFoundException expected) {
      derbyVisible = false;
    }
    assertTrue(!derbyVisible, "precondition: Derby must not be visible to the task");

    Object task = taskClass.getDeclaredConstructor().newInstance();
    invoke(task, "setRootDir", tempDir.toString());
    invoke(task, "setDatabasePath", db.toString());
    invoke(task, "setBackupDirectory", backups.toString());
    invoke(task, "setTargetVersion", "10.14.2.0");
    invoke(task, "setUserName", "APP");
    invoke(task, "setPassword", "test");

    try {
      taskClass.getMethod("execute").invoke(task);
    } catch (InvocationTargetException e) {
      throw new AssertionError("Derby upgrade failed: " + e.getCause(), e.getCause());
    }
  }
}
