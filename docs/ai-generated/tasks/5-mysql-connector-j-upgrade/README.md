# Issue #5 - Modern MySQL JDBC driver; purge legacy Connector/J on upgrade

## Decision

Ship **mysql-connector-j 8.4.0** as the MySQL driver for CMS and DTS, and keep **MariaDB Connector/J 3.5.x**
for MariaDB servers (DTS only).

Rationale:
- `jdbc:mysql://` URLs, which existing installs and docs use, keep working with no rewrite.
- 8.4.0 is Java 8 compatible (its class files are Java 8 bytecode; verified running on JDK 8 against MySQL 8.0 and 8.4).
Connector/J 9.x is also Java 8 bytecode, so 8.4.0 is a conservative pin, not a Java 8 requirement.
- MariaDB Connector/J 3.x does not take `jdbc:mysql://` URLs by default, so MariaDB-only would force a URL
migration on every MySQL customer.
- Verified with both jars on the DTS classpath: `jdbc:mysql://` is handled by `com.mysql.cj.jdbc.Driver`,
`jdbc:mariadb://` by `org.mariadb.jdbc.Driver`; no `com.mysql.jdbc` driver is registered. Hikari connects
with a `caching_sha2_password` user.

## Operator notes (for the next release, 8.1.8; the release-note text is in `../v8.1.8-release-notes/RELEASE_NOTES_v8.1.8_MYSQL_DRIVER.md`)

- MySQL 8 users with `caching_sha2_password` work with the shipped driver. If you ran
  `ALTER USER ... IDENTIFIED WITH mysql_native_password` as a workaround you may optionally switch back.
- Existing configs naming `com.mysql.jdbc.Driver` still work (8.x maps it with a deprecation warning);
  prefer `com.mysql.cj.jdbc.Driver`.
- On install/upgrade the installers remove every `mysql-connector*.jar` and `mm.mysql*.jar` from the managed
  JDBC dirs, logging each removed file, then install the shipped pin only:
  - DTS: `Deployment/Server/{common/lib,lib,lib/jdbc,perc-lib,perc-lib/jdbc}`
  - CMS: `jetty/base/lib/jdbc`
- Custom or newer MySQL drivers placed in those dirs are replaced by the shipped version.
  Put a different driver elsewhere if you need one.

## Automated check

`scripts/check-legacy-mysql-connector.py` fails if a legacy driver (by file name or by `com.mysql.jdbc`
classes inside a jar) is tracked in the repo, or present in directories passed as arguments (use it on an
unpacked distribution or an upgraded install). It runs on every PR via
`.github/workflows/legacy-mysql-connector-check.yml`.
