# Percussion CMS Security Mitigations

This document tracks the application-layer mitigations applied against CVEs that cannot be
closed by a dependency upgrade on the Java 1.8 line. It is the closing evidence for the
sub-tasks of [epic #73](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/73).

Each entry links the parent epic, the sub-task issue, the GAV and CVE list, and the precise
mitigation applied. Per-CVE OWASP `dependency-check` suppressions live in
`owasp-suppressions.xml`.

## How to read this file

|   Column   |                             Meaning                             |
|------------|-----------------------------------------------------------------|
| GAV        | Maven groupId : artifactId : version                            |
| CVE        | National Vulnerability Database identifier                      |
| Mitigation | Where in the repo the close-the-gap code or config change lives |
| Tests      | The unit test(s) that pin the mitigation in place               |

## T2.14 — Hibernate ORM 5.6.15.Final + Hibernate Validator 6.2.5.Final

|                            GAV                            |                                                                                     CVE                                                                                     |                                                                                                                                                                                                  Mitigation                                                                                                                                                                                                   |                                  Tests                                  |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `org.hibernate:hibernate-core@5.6.15.Final`               | [CVE-2019-14900](https://nvd.nist.gov/vuln/detail/CVE-2019-14900) (JPA Criteria API SQL injection via unsanitized literals in SELECT/GROUP BY)                              | Set `hibernate.criteria.literal_handling_mode=bind` on every Hibernate property block we ship. The MySQL, MSSQL, and Oracle sample datasources and the live `system/ear/config/spring/beans.xml` all carry this property now.                                                                                                                                                                                 | `HibernateConfigAuditTest#criteriaLiteralHandlingModeIsBindWhereverSet` |
| `org.hibernate:hibernate-core@5.6.15.Final`               | [CVE-2020-25638](https://nvd.nist.gov/vuln/detail/CVE-2020-25638) (SQL injection via `hibernate.use_sql_comments=true`)                                                     | Flipped `hibernate.use_sql_comments` from `true` to `false` in `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/conf/perc/perc-datasources.xml.sample-MYSQL-MARIADB`. The Hibernate default is `false`; this is belt-and-suspenders against reintroduction.                                                                                                                         | `HibernateConfigAuditTest#useSqlCommentsIsNeverTrue`                    |
| `org.hibernate:hibernate-core@5.6.15.Final`               | [CVE-2026-0603](https://nvd.nist.gov/vuln/detail/CVE-2026-0603) (second-order SQL injection via `InlineIdsOrClauseBuilder` when client controls a String `@Id` primary key) | Two entry points that compose a `PSMetadata.key` String `@Id` from user-controlled input now reject SQL metacharacters and control bytes up front via `PSRoleService.containsUnsafeIdentifierChar`: `PSRoleService.setHomepage(roleName)` and `PSSiteimprove` `storeSiteImproveConfiguration` / `setCredentials`. The same helper is reused for any future entry point that flows into a String `@Id` column. | `PSRoleServiceUnsafeIdTest`                                             |
| `org.hibernate:hibernate-core@5.6.15.Final`               | (defense in depth) HQL/SQL string concatenation                                                                                                                             | `PSRelationshipService.findByDependentId` and `findByDependentIdConfigId` converted from `"... = " + id` to parameter binding via `setParameter(...)`. Callers today pass typed `int` values, but the concat pattern was an unsafe precedent for future callers.                                                                                                                                              | n/a (manual code review)                                                |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2019-10219](https://nvd.nist.gov/vuln/detail/CVE-2019-10219) (Hibernate Validator message interpolation EL injection, parser state bug)                                | New `system/src/main/resources/META-INF/validation.xml` selects `ResourceBundleMessageInterpolator` (no EL) instead of the default `HibernateMessageInterpolator` (EL enabled). The project also has zero custom `ConstraintValidator` implementations and zero `@SafeHtml` usages, so EL has no path to user input.                                                                                          | `HibernateConfigAuditTest#validationXmlDisablesElInterpolation`         |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2020-10693](https://nvd.nist.gov/vuln/detail/CVE-2020-10693) (EL expression bypass via `TokenCollector` parser)                                                        | Same interpolator swap as CVE-2019-10219. The CVE is fixed upstream in 6.1.5.Final / 6.0.20.Final; 6.2.5.Final is the latest Java 1.8 line.                                                                                                                                                                                                                                                                   | `HibernateConfigAuditTest#validationXmlDisablesElInterpolation`         |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2023-1932](https://nvd.nist.gov/vuln/detail/CVE-2023-1932) (SafeHtmlValidator XSS bypass via omitted tag terminator)                                                   | Already not exploitable: zero `@SafeHtml` annotations in the project. Secondary defense via the `validation.xml` interpolator swap. Fixed upstream in 6.2.0.Final; 6.2.5.Final has the fix.                                                                                                                                                                                                                   | grep audit (zero `@SafeHtml`)                                           |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2025-35036](https://nvd.nist.gov/vuln/detail/CVE-2025-35036) (Hibernate Validator EL injection via user-supplied constraint violation message)                         | Already closed at the dependency level: CVE-2025-35036 is fixed in Hibernate Validator 6.2.0.Final and we ship 6.2.5.Final (latest Java 1.8 line). Project has zero custom `ConstraintValidator` implementations.                                                                                                                                                                                             | grep audit (zero custom ConstraintValidators)                           |

### How the audit test runs

`HibernateConfigAuditTest` walks the project root from the test process, parses every Spring
bean XML and `.properties` file that ships Hibernate configuration, and asserts three
invariants:

1. `hibernate.use_sql_comments` is never `true`.
2. `hibernate.criteria.literal_handling_mode` is `bind` wherever it is set.
3. Any `META-INF/validation.xml` in the project selects
   `ResourceBundleMessageInterpolator` and does not select `HibernateMessageInterpolator`.

The test lives at
`modules/perc-security-utils/src/test/java/com/percussion/security/audit/HibernateConfigAuditTest.java`
and runs on `mvn test -pl modules/perc-security-utils`.

### Why no version bump

- `hibernate-core 5.6.15.Final` is the last line on the Java 1.8 support matrix per Hibernate
  ORM's published compatibility tables. 6.x and beyond require Java 11+.
- `hibernate-validator 6.2.5.Final` is the last 6.2.x patch line on Java 1.8. The 7.x line
  requires Jakarta EE / Java 11+.

Both GAVs will move forward when the project moves to Java 11+ on a future major branch; until
then the application-layer mitigations above close the CVE class.

### References

- Epic: [#73](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/73)
- Sub-task: [#223](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/223)
- Per-GAV table: `docs/ai-generated/tasks/PR#-DependencyVulnerabilityAnalysis/categorized-final.json`
  (keys `NO_JAVA8_UPGRADE`, entries `hibernate-core` and `hibernate-validator`)
- OWASP dependency-check suppressions: `owasp-suppressions.xml`

