# PercussionCMS v8.1.8 Release Notes - MySQL JDBC driver (issue #5)

This section is the MySQL driver part of the 8.1.8 release notes. Merge it into the full 8.1.8 notes when they are assembled.

## MySQL 8 authentication fixed (CMS and DTS)

Installations upgraded from earlier 8.1.x releases could keep an old MySQL Connector/J 5.1.x jar on the classpath. Against MySQL 8
this made the connection pool fail to start with:

```text
Client does not support authentication protocol requested by server; consider upgrading MySQL client
```

8.1.8 ships **MySQL Connector/J 8.4.0** for CMS and DTS, and **removes every older MySQL connector during install and upgrade**.
MySQL 8 users with the default `caching_sha2_password` plugin work without any change on the database side. DTS also ships MariaDB
Connector/J 3.5.x for MariaDB servers. Both drivers run on Java 8.

## What the installer does

On every CMS and DTS install or upgrade, all `mysql-connector*.jar` and `mm.mysql*.jar` files are deleted from the managed JDBC
directories, and the shipped driver is installed. Each removed file is written to the installer log (`Deleting <path>`).

| Product | Managed directories |
|---------|---------------------|
| DTS | `Deployment/Server/common/lib`, `lib`, `lib/jdbc`, `perc-lib`, `perc-lib/jdbc` |
| CMS | `jetty/base/lib/jdbc` |

On upgrade the installers also change `com.mysql.jdbc.Driver` to `com.mysql.cj.jdbc.Driver` in the datasource configuration:
`perc-datasources.properties` / `perc-datasources.xml` (DTS), and `perc-ds.properties`, `perc-ds.xml`, `rxconfig/Server/config.xml`
and `rxconfig/Installer/rxrepository.properties` (CMS).

## Operator notes

- **No `mysql_native_password` workaround needed.** If you ran `ALTER USER ... IDENTIFIED WITH mysql_native_password` to get around
  this error, you may switch the user back to `caching_sha2_password` after upgrading. This is optional.
- **Connections without TLS.** A `caching_sha2_password` user connecting with `useSSL=false` needs `allowPublicKeyRetrieval=true` in
  the JDBC URL (for example `jdbc:mysql://host:3306/percdts?useSSL=false&allowPublicKeyRetrieval=true`); otherwise the login fails with
  `Public Key Retrieval is not allowed`. With the default TLS-preferred connection it is not needed. Prefer TLS where possible.
- **URL and driver class.** `jdbc:mysql://` URLs keep working with the MySQL driver. Configurations that still name
  `com.mysql.jdbc.Driver` work (the 8.x driver maps it and logs a deprecation warning) until the installer rewrites them on upgrade.
  The MariaDB driver does not accept `jdbc:mysql://` URLs; use `jdbc:mariadb://` with it.
- **Custom or newer MySQL drivers** placed in the managed directories above are replaced by the shipped version on upgrade. Keep a
  different driver outside those directories if you need one.
- **Upgrade backup.** The DTS installer copies the previous `Server` directory to `PreInstall/Backups/<timestamp>/`. That copy still
  contains the old connector jars. They are not on the classpath; do not restore them into a MySQL 8 installation.

## Verification

- `scripts/check-legacy-mysql-connector.py` fails if a legacy connector is tracked in the repository or present in a directory passed
  to it (for example `Deployment/` of an upgraded install).
- `scripts/test-mysql-connector-upgrade.py` simulates a DTS and CMS upgrade over seeded legacy jars using the real installer steps
  and fails if any legacy connector remains. Both run on every pull request.
- Real DTS and CMS installers were run on Java 8 as fresh installs and as upgrades over trees seeded with Connector/J 5.1.12. Only the
  shipped driver remained afterwards. DTS started cleanly against MySQL 8.0.46 and 8.4.13 with a `caching_sha2_password` user, and the
  CMS datasource connected to MySQL 8.4.13 the same way.
