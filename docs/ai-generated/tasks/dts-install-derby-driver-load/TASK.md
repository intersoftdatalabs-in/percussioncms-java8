# DTS install fails with "Unable to load embedded Derby driver"

## Problem

The Delivery Tier Suite (DTS) install/upgrade aborts while upgrading the embedded Derby
databases:

```
[PSUpgradeDerby] 13:52:25.904 [main] INFO  com.percussion.install.PSLogger - Loading DerbyDriver at RunTime
[PSUpgradeDerby] 13:52:25.912 [main] INFO  com.percussion.install.PSLogger - Loading DerbyDriver File D:\installs\cms-8.1.7\Deployment\Server\common\lib\derby-10.14.2.0.jar

BUILD FAILED
...\percDTSInstallTmp_...\rxconfig\Installer\installDts.xml:94: The following error occurred while executing this line:
...\percDTSInstallTmp_...\rxconfig\Installer\installDts.xml:631: Unable to load embedded Derby driver

Exception in thread "main" com.percussion.preinstall.AntJobFailedException: Installation failed. Exit code: 1
```

The failure happens in `PSUpgradeDerby`, the Ant task that boots each Derby database
(`percmetadata`, `perccomments`, `percfeeds`, `percforms`, `percmembership`, `percpolls`) in
upgrade mode and shuts it down again.

## Root cause

Three things combine, all of them visible in the install log
(`<install.dir>/rxconfig/Installer/dts-install.log`):

1. **The Ant build class path is missing the `perc-ant` jar.**
   `installDts.xml` declares `<property name="perc.version" value="${project.parent.version}"/>`.
   `${project.parent.version}` is a Maven expression that Ant cannot resolve, so `perc.version`
   stays the literal string `${project.parent.version}` (see `dts-install.log`:
   `[echoproperties] perc.version=${project.parent.version}`). The class path entry
   `<include name="perc-ant-${perc.version}.jar"/>` therefore matches nothing, so the
   `perc-ant` jar is not part of the `ant.deps` path.

2. **The Derby driver is not on that class path either.**
   `org.apache.derby:derby` is `provided` scope in the root `pom.xml` dependency management, so
   the shaded `perc-ant` jar contains no Derby classes. Because the `perc-ant` jar was missing
   from `ant.deps`, the task class was loaded by the Ant JVM system class loader, and
   `Class.forName("org.apache.derby.jdbc.EmbeddedDriver")` failed with `ClassNotFoundException`.

3. **The fallback that loads the driver at run time cannot work on a modern JDK.**
   `PSUpgradeDerby.loadDerbyJDBCJar()` reflectively called
   `java.net.URLClassLoader#addURL` on the thread context class loader. On JDK 9+ the system
   class loader is no longer a `URLClassLoader`, and on JDK 16+ `setAccessible(true)` on
   `java.net.URLClassLoader#addURL` throws `InaccessibleObjectException` because `java.base`
   does not open `java.net` to the unnamed module. The exception was swallowed by
   `catch (Exception ex)` and reported as the generic `Unable to load embedded Derby driver`.

   The Ant JVM runs on the JDK of the DTC installer, not on the JRE shipped with the CMS:
   `MainDTSPreInstall.execJar()` starts the build with `java.home` because `perc.java.home` is
   only an *Ant* property of `installDts.xml`, never a system property of the Ant JVM. The
   failing install ran on `D:\tools\jdk-21.0.9` (`ant.java.version=21` in the install log).

## Fix

### 1. `modules/perc-ant/src/main/java/com/percussion/ant/install/PSUpgradeDerby.java`

- The driver is now loaded explicitly: first from the class loader of the task, and when it is
  not there, from `Deployment/Server/common/lib/derby-*.jar` of the installation through a
  dedicated `URLClassLoader`. No reflection into `java.base` is used any more, so the task works
  on JDK 8 through 21.
- The driver instance is used directly (`Driver#connect`) instead of
  `DriverManager.getConnection()`. `DriverManager` only hands a driver to a caller that can see
  it through the *thread context* class loader, which is not necessarily the class loader of the
  Ant task.
- The Derby implementation classes (`org.apache.derby.impl.jdbc.EmbedStatement`) are no longer
  referenced; the standard `java.sql.Statement` interface is used instead. The Derby classes
  loaded by the dedicated loader are different classes than the ones on the build class path, so
  the casts would fail.
- The `BuildException` now carries the original exception as its cause, so the install log shows
  why the driver could not be loaded instead of a generic message.

### 2. `deliverytiersuite/.../rxconfig/Installer/installDts.xml`

The `perc-ant` jar is located by pattern (`perc-ant-*.jar`) instead of by the unresolvable
`${perc.version}` property, so the Ant build class path no longer depends on a Maven expression.
`MainDTSPreInstall` already locates the same jar with the same pattern.

### 3. `modules/perc-ant/src/test/java/com/percussion/ant/install/PSUpgradeDerbyTest.java`

New test that covers the regression: loading the driver from the installation jar with a class
loader that cannot see Derby, booting and shutting down a database with that driver, and running
the Ant task against a metadata database.

## Verification

- The failing scenario was reproduced outside of the installer (JDK 21, `perc.version`
  unresolvable, Ant class path without `perc-ant`): it failed with
  `Unable to load embedded Derby driver` before the change and completes with
  `BUILD SUCCESSFUL` after the change, on both JDK 21 and the CMS JRE (Java 8).
- `mvn-env.bat -pl modules/perc-ant test -Dtest=PSUpgradeDerbyTest` - 4 tests, all green.
