# Issue #5 - Acceptance criteria test report

Tested 2026-11-16 on `main` (08fd658e5 plus #281) with OpenJDK 8, using the built installers (`delivery-tier-distribution.jar`,
`perc-distribution-tree.jar`) and MySQL 8.0.46 and 8.4.13 (Docker `mysql:8.4`). No product code or repository settings were changed
to test. Test installs and datasource edits were made in scratch directories only.

## Results

| # |                        Criterion                        | Result |                                                                                                                                     Evidence                                                                                                                                     |
|---|---------------------------------------------------------|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Decision recorded                                       | Pass   | `README.md` in this folder: mysql-connector-j 8.4.0 plus MariaDB 3.5.x. Both jars are Java 8 bytecode (class major 52) and ran on JDK 8.                                                                                                                                         |
| 2 | Connector(s) shipped in DTS and CMS                     | Pass   | Built DTS tree has `mysql-connector-j-8.4.0.jar` and `mariadb-java-client-3.5.11.jar`; built CMS tree has `jetty/base/lib/jdbc/mysql-connector.jar` (identical to 8.4.0).                                                                                                        |
| 3 | Upgrade removes legacy connectors                       | Pass   | Real DTS and CMS installers re-run as upgrades over trees seeded with a real Connector/J 5.1.12 jar (and 5.1.32, 8.0.x, `mm.mysql`, generic-name copies) in every managed dir. Only the shipped jar remained; removals logged (`Deleting <path>`: 13 DTS, 4 CMS); checker clean. |
| 4 | Clean DTS start on MySQL 8 with `caching_sha2_password` | Pass   | MySQL 8.0.46 and 8.4.13. Tomcat on JDK 8 with the legacy `com.mysql.jdbc.Driver` name: 0 ERROR/SEVERE, 60 pooled connections, 18 tables created, no authentication-protocol error.                                                                                               |
| 5 | No dual registration                                    | Pass   | In the live DTS `Deployment/` tree the only MySQL `java.sql.Driver` entry is `com.mysql.cj.jdbc.Driver` (plus MariaDB); no legacy driver found.                                                                                                                                  |
| 6 | Operator doc / release note                             | Pass   | `../v8.1.8-release-notes/RELEASE_NOTES_v8.1.8_MYSQL_DRIVER.md` (every claim in it was checked, see below).                                                                                                                                                                       |
| 7 | Scripted check that fails if 5.1.x remains              | Pass   | `scripts/test-mysql-connector-upgrade.py` (14 checks) runs in CI. It passes here and in a stock Ubuntu 24.04 container using apt `ant` + `ant-contrib`; run against the pre-#273 `install.xml` it fails 3 checks.                                                                |

Optional item not implemented: WARN on an unknown extra `mysql-*.jar`. The installers delete every `mysql-connector*.jar` and
`mm.mysql*.jar` from the managed directories, so a leftover can only have a different name pattern.

## Additional checks

- **Driver class rewrite.** With `com.mysql.jdbc.Driver` in the datasource files, a real DTS upgrade rewrote `jdbcDriver` in
  `perc-datasources.properties`, and a real CMS upgrade rewrote `perc.ds.1.driver.class` in `perc-ds.properties`, both to
  `com.mysql.cj.jdbc.Driver`.
- **CMS.** Jetty on JDK 8 against MySQL 8.4.13 (legacy driver class name, `caching_sha2_password` user) authenticated and ran SQL, and
  `jetty-ssl-context.xml` parsed. The CMS repository schema was not built in this test, so the CMS logged `Table ... doesn't exist`
  errors; a full CMS boot is not covered.
- **TLS.** With `useSSL=false` and a cold server password cache, a `caching_sha2_password` login fails with
  `Public Key Retrieval is not allowed` unless `allowPublicKeyRetrieval=true` is set. With the default TLS-preferred connection it is
  not needed. (MySQL caches the hash after the first login, so a flush of privileges is needed to reproduce.)
- **MariaDB driver.** Rejects `jdbc:mysql://` ("No suitable driver") and connects to MySQL 8.4 with `jdbc:mariadb://`.
- **Connector/J 9.x.** 9.0.0 and 9.3.0 are also Java 8 bytecode, so 8.4.0 is a conservative pin, not a Java 8 requirement.

## Known behaviour

- The DTS upgrade backup (`PreInstall/Backups/<timestamp>/`) keeps the removed 5.1.x jars. They are off the classpath, but a scan of the
  whole install directory reports them; scan `Deployment/` instead. Documented in the release note.
- A fresh DTS install ships only `mysql-connector-j-8.4.0.jar`; the generic `mysql-connector.jar` copy is created by the upgrade path.
  A fresh CMS install ships `mysql-connector.jar`.

## Running the checks

```bash
python3 -I scripts/check-legacy-mysql-connector.py --self-test   # checker self-test
python3 -I scripts/check-legacy-mysql-connector.py               # tracked files
python3 -I scripts/check-legacy-mysql-connector.py <Deployment dir or jetty/base/lib/jdbc>   # an upgraded install
python3 -I scripts/test-mysql-connector-upgrade.py               # upgrade simulation (needs Ant 1.10 + ant-contrib)
```

