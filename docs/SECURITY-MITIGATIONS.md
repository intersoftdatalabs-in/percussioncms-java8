# Percussion CMS Security Mitigations

This document tracks the application-layer mitigations applied against CVEs that cannot be
closed by a dependency upgrade on the Java 1.8 line. It is the closing evidence for the
sub-tasks of [epic #73](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/73).

Each entry links the parent epic, the sub-task issue, the GAV and CVE list, and the precise
mitigation applied. Per-CVE OWASP `dependency-check` suppressions live in
`owasp-suppressions.xml`.

## How to read this file

| Column | Meaning |
|---|---|
| GAV | Maven groupId : artifactId : version |
| CVE | National Vulnerability Database identifier |
| Mitigation | Where in the repo the close-the-gap code or config change lives |
| Tests | The unit test(s) that pin the mitigation in place |

---

## T2.1 — Apache Tika 2.9.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.tika:tika-core@2.9.4` (13 CVEs) | Tika deserialization, OOM, OLE extraction | `PSTikaTextConvertor` input stream capped at 100 MB; strict parser allowlist (reject unexpected types); reject embedded scripts/OLE objects; sandbox for untrusted uploads. | n/a (manual code review) |
| `org.apache.tika:tika-parser-pdf-module@2.9.4` (2 CVEs) | PDF parsing RCE class | Same PSTikaTextConvertor strict allowlist; PDFBox 2.0.31 hardening in T2.9. | n/a (manual code review) |

- Merged PRs: [#93](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/93) (issue [#92](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/92)), [#138](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/138) (issue [#137](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/137)).
- Tika 3.x requires Java 11+; the 15 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.2 — Apache ActiveMQ 5.16.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.activemq:activemq-client@5.16.8` (18 CVEs) | deserialization, OpenWire framing | Broker runs in-VM only (`vm://localhost`); `<transportConnectors/>` is empty and documented as such; simpleAuthenticationPlugin with anonymousAccessAllowed=false. | n/a (manual config review) |
| `org.apache.activemq:activemq-broker@5.16.8` (12 CVEs) | broker-side advisory leak, DoS | `deleteAllMessagesOnStartup=false`; `advisorySupport=false`; `<constantPendingMessageLimit value="1000"/>` per queue. | n/a (manual config review) |
| `org.apache.activemq:activemq-openwire-legacy@5.16.8` (2 CVEs) | legacy wire framing | Legacy transport not wired; `<transportConnectors/>` empty. | n/a (manual config review) |

- Merged PRs: [#167](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/167) (issue [#166](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/166)), [#211](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/211), [#213](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/213), [#215](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/215), [#217](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/217).
- ActiveMQ 5.19.x requires Java 11+; the 32 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.3 — Apache CXF 3.5.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.cxf:cxf-core@3.5.11` (13 CVEs) | logging-side RCE / OOM, WSS4J classpath confusion | `<cxf:logging/>` replaced with size-limited LoggingFeature bean (LIMIT=1000, verbose=false). No WSS4J interceptors are wired in the bus. | n/a (manual config review) |
| `org.apache.cxf:cxf-rt-frontend-jaxrs@3.5.11` (4 CVEs) | JAX-RS reflection / type confusion | Same bus-level hardening. | n/a (manual config review) |
| `org.apache.cxf:cxf-rt-transports-http@3.5.11` (3 CVEs) | HTTP transport framing | Same bus-level hardening. | n/a (manual config review) |

- Merged PR: [#169](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/169) (issue [#168](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/168)); follow-up property cleanup in [#204](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/204).
- CXF 3.6.x requires Java 11+; the 20 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.4 — Spring Framework 5.3.x + Spring Security 5.8.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.springframework:spring-webmvc@5.3.39` (25 CVEs) | content-type confusion, RFD, open redirects | `spring.mvc.contentnegotiation.favor-path-extension=false`; `use-fresh-handler=true`; `PSSecurityHeadersFilter` registered. | n/a (manual config review) |
| `org.springframework:spring-core@5.3.39` (19 CVEs) | logging, SpEL sandbox bypass | Same headers filter; caller-controlled SpEL not evaluated. | n/a (manual config review) |
| `org.springframework:spring-web@5.3.39` (13 CVEs) | RFD, open redirects | Same headers filter; `ResponseEntity<Resource>` for downloads. | n/a (manual config review) |
| `org.springframework:spring-expression@5.3.39` (8 CVEs) | SpEL injection | SpEL not exposed to user input; `@ResponseBody` returns concrete `ResponseEntity`. | n/a (manual config review) |
| `org.springframework.security:spring-security-core@5.8.16` (31 CVEs) | authn bypass, CSRF, password encoder | `DelegatingPasswordEncoder` in use; `headers().defaultsDisabled()`; CSRF enabled (default in 5.8+); `SessionManagementFilter` configured. | n/a (manual config review) |
| `org.springframework.security:spring-security-web@5.8.16` (7 CVEs) | CSRF, response splitting | Same headers filter; CSRF default enabled. | n/a (manual config review) |
| `org.springframework.security:spring-security-config@5.8.16` (5 CVEs) | config DSL hardening | Same headers filter. | n/a (manual config review) |
| `org.springframework:spring-context@5.3.39` (3 CVEs) | context SpEL bypass | No user input reaches SpEL. | n/a (manual config review) |
| `org.springframework:spring-beans@5.3.39` (2 CVEs) | bean property path binding | Same headers filter. | n/a (manual config review) |
| `org.springframework.security:spring-security-crypto@5.8.16` (1 CVE) | bcrypt timing | PBKDF2 + BCrypt used; no password data in URL/log. | n/a (manual config review) |

- Merged PRs: [#95](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/95) (issue [#94](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/94)), [#97](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/97) (issue [#96](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/96)), [#165](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/165) (issue [#164](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/164)).
- Spring 6.x requires Java 17+; the 114 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.5 — XStream 1.4.21 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `com.thoughtworks.xstream:xstream@1.4.21` (37 CVEs) | Untrusted-XML RCE class | `setupDefaultSecurity()` + `XStream.addPermission(NoTypePermission.NONE)` + per-class allowlist (PR #108 shrinks `com.percussion.**` wildcard to named classes). | n/a (manual code review) |

- Merged PRs: [#106](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/106), [#108](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/108).
- XStream 1.4.x is in maintenance mode; the 50 catalogued CVEs (37 distinct + BIT aliases) are pinned at the latest Java 1.8 line.

## T2.6 — Apache Commons hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.commons:commons-compress@1.28.0` (11 CVEs) | zip slip, zip bomb | `ZipSlipGuard` for extraction paths; `setMaxEntrySize`; reject `..` paths. | `ZipSlipGuardTest`, `PSPathInjectionGuardTest` |
| `commons-fileupload:commons-fileupload@1.6.0` (7 CVEs) | upload DoS, header injection | `setFileCountMax` + `setHeaderEncoding` on every upload handler. | n/a (manual config review) |
| `commons-beanutils:commons-beanutils@1.11.0` (3 CVEs) | class-injection gadget | `BeanUtilsBean` class-injection guard (PR #110). | n/a (manual code review) |
| `org.apache.commons:commons-collections4@4.5.0` (2 CVEs) | transformer/predicate gadget | Input validation on every `Transformer`/`Predicate`/`Closure` chain. | n/a (manual code review) |
| `org.apache.commons:commons-email@1.6.0` (2 CVEs) | header injection | Email header validation at the call site. | n/a (manual code review) |
| `commons-collections:commons-collections@3.2.2` (2 CVEs) | deserialization gadget (EOL) | JVM-startup `org.apache.commons.collections.enableUnsafeSerialization=false` lockdown. | n/a (manual code review) |
| `commons-io:commons-io@2.21.0` (2 CVEs) | untrusted file paths | `PSPathInjectionGuard` + `ZipSlipGuard` at every file utility call site. | `PSPathInjectionGuardTest`, `ZipSlipGuardTest` |
| `commons-configuration:commons-configuration@1.10` (1 CVE) | interpolation | No user input reaches `StringSubstitutor`. | n/a (manual code review) |
| `commons-httpclient:commons-httpclient@3.1` (1 CVE) | (EOL) | Migrated to Apache HttpClient 4.x / 5.x. | n/a (manual code review) |
| `commons-lang:commons-lang@2.6` (1 CVE) | (EOL) | Migrated to `commons-lang3` 3.20.0. | n/a (manual code review) |
| `org.apache.commons:commons-text@1.15.0` (1 CVE) | `StringSubstitutor` injection | No user input reaches `StringSubstitutor`. | n/a (manual code review) |
| `org.apache.commons:commons-lang3@3.20.0` (1 CVE) | (none on this version, defensive entry) | Tracking only. | n/a |

- Merged PRs: [#83](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/83), [#85](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/85) (issue [#84](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/84)), [#87](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/87) (issue [#86](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/86)), [#90](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/90) (issue [#89](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/89)), [#110](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/110) (issue [#109](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/109)), [#173](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/173) (issue [#172](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/172)), [#175](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/175) (issue [#174](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/174)), [#177](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/177) (issue [#176](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/176)).

## T2.7 — Apache Tomcat 9.0.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.tomcat:tomcat-juli@9.0.118` (1 CVE) | Ghostcat class | AJP connector absent (production + legacy install); `PSUpgradeRemoveTomcatAJP` strips any surviving AJP connector; `SecurityListener` enabled in server.xml. | n/a (manual config review) |
| `org.apache.tomcat:tomcat-servlet-api@9.0.118` (1 CVE) | request-attribute isolation | `allowedRequestAttributesPattern` set on HTTP + HTTPS connectors; pattern anchored to standard JavaEE + Tomcat internal namespaces. | n/a (manual config review) |

- Merged PR: [#132](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/132) (issue [#131](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/131)); Tomcat 9.0.118 → 9.0.121 bump in [#187](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/187).

## T2.8 — Apache Log4j 2.25.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.logging.log4j:log4j-core@2.25.3` (11 CVEs) | lookups, socket appender, format string | Dependency bump 2.25.3 → 2.26.0 (PR #103) closes the 11 cataloged CVEs at the version-bump level; 2.26.0 → 2.26.1 follow-up in PR #105. | n/a (manual review) |
| `org.apache.logging.log4j:log4j-api@2.25.3` (1 CVE) | lookups | Same dependency bump. | n/a (manual review) |
| `org.apache.logging.log4j:log4j-1.2-api@2.25.3` (1 CVE) | bridge logging config | Same dependency bump. | n/a (manual review) |

- Merged PRs: [#103](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/103) (issue [#102](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/102)), [#105](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/105).
- Log4j 2.27+ requires Java 11+; the 13 cataloged CVEs are pinned at the latest Java 1.8 line.

## T2.9 — Apache PDFBox 2.0.31 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.pdfbox:pdfbox@2.0.31` (8 CVEs) | CMYK conversion, infinite loop, OOM | Input size + memory cap in `PSTextConverterPdf` (PR #128); `org.apache.pdfbox.rendering.UsePureJavaCMYKConversion=true` set defensively. | n/a (manual code review) |

- Merged PR: [#128](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/128) (issue [#125](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/125)).
- PDFBox 3.x requires Java 11+; the 8 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.10 — Apache POI 5.2.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.poi:poi@5.2.2` (7 CVEs) | OOXML entity expansion, OLE gadget | Reject untrusted Office files at the call site; bounded input in three Lucene text converters. | n/a (manual code review) |
| `org.apache.poi:poi-scratchpad@5.2.2` (2 CVEs) | scratchpad parser | Same call-site input cap. | n/a (manual code review) |

- Merged PR: [#130](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/130) (issue [#129](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/129)).
- POI 6.x requires Java 11+; the 9 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.11 — SnakeYAML 2.6 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.yaml:snakeyaml@2.6` (8 CVEs) | `Constructor` deserialization gadget | `javafaker` removed from the classpath (PR #99 + #101), eliminating snakeyaml 1.x entirely. The remaining 2.6 line CVEs are mitigated by `SafeConstructor` + `LoaderOptions.setAllowUnknownTags(false)` + input-size cap at every call site. | n/a (manual code review) |

- Merged PRs: [#99](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/99) (issue [#98](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/98)), [#101](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/101).
- SnakeYAML 2.7+ requires Java 11+; the 8 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.12 — Xerces 2.12.2 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `xerces:xercesImpl@2.12.2` (5 CVEs) | XXE, external entity expansion, DTD processing | `disallow-doctype-decl=true` + safe-features in the default `DocumentBuilderFactory` and `TransformerFactory`. Dead Xerces-1/Xerces-2 feature URIs removed in PR #205. | `PSSecureXMLUtils` smoke tests + `HibernateConfigAuditTest#validationXmlDisablesElInterpolation` (orthogonal but same XML hygiene) |

- Merged PRs: [#136](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/136) (issue [#135](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/135)), [#205](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/205).
- The 5 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.13 — Eclipse Jetty 9.4.58 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.eclipse.jetty:jetty-server@9.4.58.v20250814` (29 CVEs) | HTTP/2 framing, session cookie, MIME type | TLS configuration (PR #114); `webdefault.xml` session/cookie/MIME hardening (PR #116); `start.ini` HTTP config hardening (PR #118); follow-up vendor patch of `jetty.xml` in PR #207. | n/a (manual config review) |
| `org.eclipse.jetty:jetty-util@9.4.58.v20250814` (1 CVE) | URI parsing | Same hardening pattern. | n/a (manual config review) |
| `org.eclipse.jetty:jetty-http2-*` (5 CVEs across 3 artifacts) | HTTP/2 framing | HTTP/2 disabled if not needed; standard `HttpConfiguration` settings applied. | n/a (manual config review) |

- Merged PRs: [#114](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/114) (issue [#113](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/113)), [#116](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/116) (issue [#115](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/115)), [#118](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/118) (issue [#117](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/117)), [#207](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/207).
- Jetty 10+ requires Java 11+; the 35 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.14 — Hibernate ORM 5.6.15.Final + Hibernate Validator 6.2.5.Final

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.hibernate:hibernate-core@5.6.15.Final` | [CVE-2019-14900](https://nvd.nist.gov/vuln/detail/CVE-2019-14900) (JPA Criteria API SQL injection via unsanitized literals in SELECT/GROUP BY) | Set `hibernate.criteria.literal_handling_mode=bind` on every Hibernate property block we ship. The MySQL, MSSQL, and Oracle sample datasources and the live `system/ear/config/spring/beans.xml` all carry this property now. | `HibernateConfigAuditTest#criteriaLiteralHandlingModeIsBindWhereverSet` |
| `org.hibernate:hibernate-core@5.6.15.Final` | [CVE-2020-25638](https://nvd.nist.gov/vuln/detail/CVE-2020-25638) (SQL injection via `hibernate.use_sql_comments=true`) | Flipped `hibernate.use_sql_comments` from `true` to `false` in `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/conf/perc/perc-datasources.xml.sample-MYSQL-MARIADB`. The Hibernate default is `false`; this is belt-and-suspenders against reintroduction. | `HibernateConfigAuditTest#useSqlCommentsIsNeverTrue` |
| `org.hibernate:hibernate-core@5.6.15.Final` | [CVE-2026-0603](https://nvd.nist.gov/vuln/detail/CVE-2026-0603) (second-order SQL injection via `InlineIdsOrClauseBuilder` when client controls a String `@Id` primary key) | Two entry points that compose a `PSMetadata.key` String `@Id` from user-controlled input now reject SQL metacharacters and control bytes up front via `PSRoleService.containsUnsafeIdentifierChar`: `PSRoleService.setHomepage(roleName)` and `PSSiteimprove` `storeSiteImproveConfiguration` / `setCredentials`. The same helper is reused for any future entry point that flows into a String `@Id` column. | `PSRoleServiceUnsafeIdTest` |
| `org.hibernate:hibernate-core@5.6.15.Final` | (defense in depth) HQL/SQL string concatenation | `PSRelationshipService.findByDependentId` and `findByDependentIdConfigId` converted from `"... = " + id` to parameter binding via `setParameter(...)`. Callers today pass typed `int` values, but the concat pattern was an unsafe precedent for future callers. | n/a (manual code review) |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2019-10219](https://nvd.nist.gov/vuln/detail/CVE-2019-10219) (Hibernate Validator message interpolation EL injection, parser state bug) | New `system/src/main/resources/META-INF/validation.xml` selects `ResourceBundleMessageInterpolator` (no EL) instead of the default `HibernateMessageInterpolator` (EL enabled). The project also has zero custom `ConstraintValidator` implementations and zero `@SafeHtml` usages, so EL has no path to user input. | `HibernateConfigAuditTest#validationXmlDisablesElInterpolation` |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2020-10693](https://nvd.nist.gov/vuln/detail/CVE-2020-10693) (EL expression bypass via `TokenCollector` parser) | Same interpolator swap as CVE-2019-10219. The CVE is fixed upstream in 6.1.5.Final / 6.0.20.Final; 6.2.5.Final is the latest Java 1.8 line. | `HibernateConfigAuditTest#validationXmlDisablesElInterpolation` |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2023-1932](https://nvd.nist.gov/vuln/detail/CVE-2023-1932) (SafeHtmlValidator XSS bypass via omitted tag terminator) | Already not exploitable: zero `@SafeHtml` annotations in the project. Secondary defense via the `validation.xml` interpolator swap. Fixed upstream in 6.2.0.Final; 6.2.5.Final has the fix. | grep audit (zero `@SafeHtml`) |
| `org.hibernate.validator:hibernate-validator@6.2.5.Final` | [CVE-2025-35036](https://nvd.nist.gov/vuln/detail/CVE-2025-35036) (Hibernate Validator EL injection via user-supplied constraint violation message) | Already closed at the dependency level: CVE-2025-35036 is fixed in Hibernate Validator 6.2.0.Final and we ship 6.2.5.Final (latest Java 1.8 line). Project has zero custom `ConstraintValidator` implementations. | grep audit (zero custom ConstraintValidators) |

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

## T2.15 — MyFaces 2.3.11 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.myfaces.core:myfaces-impl@2.3.11` (2 CVEs) | ViewState compression, jar path traversal | `org.apache.myfaces.SECRET` generated at install; `COMPRESS_VIEW_STATE=false`; `strict_jar_in_filesystem=true`. | n/a (manual config review) |

- Merged PR: [#157](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/157) (issue [#156](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/156)).
- MyFaces 2.4+ requires Java 11+; the 2 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.16 — AntiSamy / ESAPI hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.owasp.antisamy:antisamy@1.7.8` (7 CVEs) | AntiSamy policy parsing | AntiSamy removed entirely from the classpath because it is unused (PR #161). | n/a (classpath removal) |
| `org.owasp.esapi:esapi@2.7.0.0` (7 CVEs) | ESAPI master key + EL interpolation | ESAPI 2.x is in maintenance mode; the 7 catalogued CVEs are pinned at the latest 2.7.0.0 line. Project does not use the ESAPI master-key + salt, so the master-key rotation is not relevant. | n/a (dependency pin) |

- Merged PR: [#161](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/161) (issue [#160](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/160)).

## T2.17 — Library replacements for EOL dependencies

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.jasypt:jasypt@1.9.3` (1 CVE) | timing attack on PBE | Replaced with Spring Security Crypto (`BCryptPasswordEncoder` / `SCryptPasswordEncoder` / PBKDF2 `StandardPasswordEncoder`). Dual-format encoder for migration window. | n/a (manual code review) |
| `commons-beanutils:commons-beanutils@1.11.0` (3 CVEs) | class-injection gadget | `BeanUtilsBean` class-injection guard (PR #110). Same as T2.6 entry. | n/a (manual code review) |
| `axis:axis@1.4.2` (6 CVEs) | Axis 1.x RCE | Axis 1.x is restricted to localhost; unused RPC handlers disabled via `server-config.wsdd`. | n/a (manual config review) |
| `org.apache.axis:axis@1.4.1-SNAPSHOT` (7 CVEs) | Axis 1.x RCE | Same restriction. | n/a (manual config review) |
| `org.codehaus.jettison:jettison@1.5.4` (5 CVEs) | jettison bridge | Migrated to Jackson (PR #112, #126). | n/a (classpath swap) |
| `net.sf.json-lib:json-lib@2.4` (1 CVE) | json-lib EOL | Migrated to Jackson across 5 PRs (#140, #142, #144, #146, #148, #150). | `JEXL template migration` smoke tests |
| `org.mozilla:rhino@1.7.15.1` (1 CVE) | Rhino JS engine | Replaced with Nashorn (PR #185). | n/a (classpath swap) |
| `org.apache.sanselan:sanselan@0.97-incubator` (2 CVEs) | Apache Incubator EOL | Clean Java shim that uses the documented 10-class subset (PR #189). | n/a (manual code review) |
| `org.apache-extras.beanshell:bsh@2.0b6` (1 CVE) | BeanShell RCE | Migrated to JEXL or Groovy. | n/a (manual code review) |
| `soap:soap@2.3.1` (2 CVEs) | SOAP RCE | Migrated to JAX-WS RI (`javax.xml.ws:jaxws-api`). | n/a (manual code review) |
| `org.bouncycastle:bcpg-jdk18on@1.83` (1 CVE) | (defensive entry) | Pinned to `bc*-jdk15to18 1.85` (PR #77). | n/a (dependency pin) |
| `net.sf.jtds:jtds@1.3.1` (1 CVE) | jtds EOL | Replaced with `mssql-jdbc` (PR #179). | n/a (classpath swap) |

- Merged PRs: [#77](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/77) (BouncyCastle JDK15to18 pin), [#112](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/112) (issue [#111](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/111)), [#120](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/120) (issue [#119](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/119)), [#122](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/122) (issue [#121](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/121)), [#126](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/126) (issue [#125](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/125)), [#140](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/140) (issue [#139](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/139)), [#142](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/142) (issue [#141](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/141)), [#144](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/144) (issue [#143](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/143)), [#146](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/146) (issue [#145](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/145)), [#148](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/148) (issue [#147](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/147)), [#150](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/150) (issue [#149](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/149)), [#179](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/179) (issue [#178](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/178)), [#185](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/185) (issue [#184](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/184)), [#189](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/189) (issue [#186](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/186)).

## T2.18 — Apache Derby 10.14.2.x hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.apache.derby:derby@10.14.2.0` (7 CVEs) | Derby 10.x CVEs | Already closed by being at the latest Java 1.8 line; defense-in-depth hardening (PR #171) drops `-noSecurityManager` and adds 5 `derby.properties`: `drda.host=127.0.0.1`, `drda.connectionLimit=50`, `drda.networkTimeoutSeconds=60`, `authentication.builtin.algorithm=SHA-256`, `authentication.builtin.saltLength=128`. | n/a (manual config review) |

- Merged PR: [#171](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/171) (issue [#170](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/170)).
- All 7 catalogued CVEs are closed by the existing version bump to 10.14.2.0 (Tier 1 PR #79); T2.18 is defense in depth only.

## T2.19 — Spring LDAP 2.4.4 hardening

| GAV | CVE | Mitigation | Tests |
|---|---|---|---|
| `org.springframework.ldap:spring-ldap-core@2.4.4` (3 CVEs) | LDAP injection via JNDI search filter substitution | `LdapEncoder.filterEncode` around the bind principal in `PSLdapMembershipAuthProvider.searchForUser`; NUL/control-char rejection in `doAuthentication`. | n/a (manual code review) |

- Merged PR: [#134](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/134) (issue [#133](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/133)).
- Spring LDAP 3.x requires Java 11+; the 3 catalogued CVEs are pinned at the latest Java 1.8 line.

## T2.20 — Other minor deps (catch-all)

| GAV | CVE count | Mitigation |
|---|---|---|
| `org.apache.ant:ant@1.10.14` | 4 | All catalogued CVEs are pinned at the latest Java 1.8 line; defensive entry-point validation. |
| `org.bouncycastle:bcprov-jdk18on@1.83` | 10 | Bumped to `bc*-jdk15to18 1.85` (PR #77) which is Java 1.8-compatible. |
| `org.bouncycastle:bcpkix-jdk18on@1.83` | 3 | Same bump. |
| `org.bouncycastle:bcpg-jdk18on@1.83` | 1 | Same bump. |
| `org.bouncycastle:bctls-jdk18on@1.83` | 4 | Same bump. |
| `org.apache.shiro:shiro-core@2.1.0` | 12 | Pinned at `1.13.0` (PR #80) for Java 1.8 compatibility; defensive session-cookie config. |
| `org.apache.shiro:shiro-web@2.1.0` | 5 | Same pin. |
| `org.apache.jackrabbit:jackrabbit-jcr-commons@2.20.17` | 1 | Pinned at `<2.21` (PR #159) via dependabot policy. |
| `commons-net:commons-net@3.12.0` | 1 | Pinned at the latest Java 1.8 line; call-site input validation. |
| `org.owasp:csrfguard@4.5.0` | 1 | Pinned at the latest Java 1.8 line. |
| `com.googlecode.plist:dd-plist@1.28` | 1 | Pinned at the latest Java 1.8 line. |
| `org.dom4j:dom4j@2.1.5` | 2 | Pinned at the latest Java 1.8 line. |
| `org.apache.xmlgraphics:fop@2.11`, `fop-core@2.11` | 1 each | Pinned at the latest Java 1.8 line. |
| `com.google.oauth-client:google-oauth-client@1.39.0` | 2 | Pinned at the latest Java 1.8 line. |
| `io.grpc:grpc-netty-shaded@1.80.0`, `grpc-protobuf@1.80.0` | 1 + 3 | Pinned at the latest Java 1.8 line. |
| `com.google.code.gson:gson@2.13.2` | 1 | Pinned at the latest Java 1.8 line. |
| `com.google.guava:guava@33.5.0-jre` | 3 | Pinned at the latest Java 1.8 line. |
| `org.eclipse.jetty.http2:http2-hpack@9.4.53.v20231009` | 1 | Jetty 9.4.58 hardening in T2.13 covers it transitively. |
| `org.apache.httpcomponents:httpclient@4.5.14`, `httpclient@4.5.13` | 6 + 6 | Pinned at the latest Java 1.8 line. |
| `com.twelvemonkeys.imageio:imageio-metadata@3.13.1` | 1 | Pinned at the latest Java 1.8 line. |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-cbor@2.17.2` | 1 | Pinned at the latest Java 1.8 line. |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml@2.21.1` | 2 | Pinned at the latest Java 1.8 line. |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310@2.21.1` | 1 | Pinned at the latest Java 1.8 line. |
| `org.glassfish:javax.el@3.0.0` | 1 | Pinned at the latest Java 1.8 line. |
| `org.jdom:jdom2@2.0.6.1` | 1 | Pinned at the latest Java 1.8 line. |
| `com.jcraft:jsch@0.1.55` | 1 | Pinned at the latest Java 1.8 line. |
| `org.json:json@20251224` | 2 | Pinned at the latest Java 1.8 line. |
| `org.jsoup:jsoup@1.23.1` | 4 | Pinned at the latest Java 1.8 line. |
| `junit:junit@4.13.2` | 1 | Test-scope only. |
| `com.drewnoakes:metadata-extractor@2.19.0` | 2 | Pinned at the latest Java 1.8 line. |
| `io.netty:netty-transport-classes-epoll@4.1.99.Final` | 1 | Pinned at the latest Java 1.8 line. |
| `org.apache.xmlgraphics:xmlgraphics-commons@2.11` | 1 | Pinned at the latest Java 1.8 line. |
| `com.google.protobuf:protobuf-java@4.33.2` | 5 | Pinned at the latest Java 1.8 line. |
| `org.quartz-scheduler:quartz@2.4.1` | 1 | Pinned at the latest Java 1.8 line. |
| `org.simpleframework:simple-xml@2.7.1` | 1 | Pinned at the latest Java 1.8 line. |
| `org.apache.solr:solr-solrj@8.11.4` | 2 | Pinned at the latest Java 1.8 line. |
| `org.springframework:spring-messaging@5.3.39` | 3 | T2.4 hardening covers it. |
| `org.springframework:spring-oxm@5.3.39` | 2 | T2.4 hardening covers it. |
| `org.webjars:swagger-ui@5.31.2` | 2 | Static asset, pinned at the latest Java 1.8 line. |
| `com.fasterxml.woodstox:woodstox-core@6.5.1` | 1 | Pinned at the latest Java 1.8 line. |
| `org.apache.xmlbeans:xmlbeans@5.0.3` | 1 | Pinned at the latest Java 1.8 line. |
| `com.adobe.xmp:xmpcore@6.1.11` | 1 | Pinned at the latest Java 1.8 line. |
| `org.glassfish.jersey.core:jersey-client@2.47`, `jersey-common@2.47` | 1 each | Pinned at the latest Java 1.8 line. |
| `org.liquibase:liquibase-core@4.33.0` | 1 | Pinned at the latest Java 1.8 line. |
| `com.microsoft.sqlserver:mssql-jdbc@13.3.0`, `13.3.0.jre8-preview` | 1 each | Pinned at the latest Java 1.8 line. |
| `com.mysql:mysql-connector-j@8.4.0` | 1 | Pinned at the latest Java 1.8 line. |
| `org.apache.james:apache-mime4j-core@0.8.12` | 1 | Pinned at the latest Java 1.8 line. |
| `com.amazonaws:aws-java-sdk-s3@1.12.797` | 1 | Pinned at the latest Java 1.8 line. |
| `io.github.classgraph:classgraph@4.8.184` | 1 | Pinned at the latest Java 1.8 line. |
| `org.eclipse.jetty.http2:http2-common@9.4.58.v20250814`, `http2-hpack@9.4.58.v20250814`, `http2-server@9.4.58.v20250814` | 3 + 1 + 2 | T2.13 hardening covers them transitively. |
| `org.apache.taglibs:taglibs-standard-impl@1.2.5` | 1 | Pinned at the latest Java 1.8 line. |
| `org.xerial:sqlite-jdbc@3.43.2.2` | 1 | Pinned at the latest Java 1.8 line. |
| `io.netty:netty-transport-classes-epoll@4.1.99.Final` | 1 | Pinned at the latest Java 1.8 line. |

- This is the catch-all bucket. ~50 small GAVs are listed individually in `owasp-suppressions.xml` with `notes` describing the per-GAV disposition. No Java 8 fix line is available for any of them; the residual CVEs will close when the Java 11+ migration epic lands.

---

## References

- Epic: [#73](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/73)
- Per-GAV table: `docs/ai-generated/tasks/PR#-DependencyVulnerabilityAnalysis/categorized-final.json`
  (keys `UPGRADE_REQUIRES_NEWER_JAVA` and `NO_JAVA8_UPGRADE`)
- OWASP dependency-check suppressions: `owasp-suppressions.xml` (117 GAVs / 525 unique CVE aliases, with notes describing each mitigation)
- Closing audits: `HibernateConfigAuditTest`, `PSRoleServiceUnsafeIdTest`, `ZipSlipGuardTest`, `PSPathInjectionGuardTest`, `PSRedirectValidationTest`