# PercussionCMS v8.1.8 Release Notes - Java 8 EOL LTS Backports & Removals (Waves 1–3)

This section covers the Java 8 End-of-Life (EOL) dependency backports, coordinate relocations, and library retirements merged from `8.1.9-dev` into `v8.1.8` under Epic [#299](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/299). Merge it into the full `v8.1.8` release notes when assembling the final changelog.

## Summary of Changes

To eliminate remaining Maven Dependabot advisories while preserving full Java 8 compatibility (`javax.*` namespace and bytecode level `<= 52`), Waves 1–3 replaced unmaintained upstream EOL dependencies through two mechanisms:

1. **In-repo removals & replacements** — retired deprecated modules and replaced insecure/unmaintained libraries with JDK 8 or maintained equivalents (`org.owasp.encoder:encoder:1.4.0`, Apache HttpClient `5.6.3`).
2. **Intersoft Java 8 LTS forks** — backported upstream 2024–2026 CVE patches across 8 repositories published to GitHub Packages under `com.intsof.lts.*` (and `org.apache.derby:10.14.2.1` from Apache SVN `branches/10.14`), enforced at build time by `enforce-banned-eol-dependencies` and monitored weekly via `.github/workflows/upstream-cve-watch.yml` and `fork-manifest.json`.

## Intersoft Java 8 LTS Forks Included in v8.1.8

|         Family          |                                                                Coordinates & Tag                                                                |   Upstream Base    |                                                             Key Remediations                                                              |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **Apache Tika**         | [`com.intsof.lts.tika:*:2.9.4.1`](https://github.com/intersoftdatalabs-in/tika/releases/tag/v2.9.4.1) (`#294`)                                  | `2.9.4`            | StAX / Woodstox external-entity & DTD protections (`CVE-2025-54988`, `CVE-2025-66516`)                                                    |
| **Apache CXF**          | [`com.intsof.lts.cxf:*:3.5.11.1`](https://github.com/intersoftdatalabs-in/cxf/releases/tag/v3.5.11.1) (`#295`)                                  | `3.5.11`           | Backported 14 `3.6.x-fixes` security commits (`CVE-2026-49875`, `CVE-2026-50645`, etc.)                                                   |
| **Apache Derby**        | [`org.apache.derby:*:10.14.2.1`](https://github.com/intersoftdatalabs-in/derby/releases/tag/v10.14.2.1) (`#296`)                                | `10.14.2.0`        | Built from Apache SVN `branches/10.14` (`r1929838`, `DERBY-7147` / `CVE-2022-46337`)                                                      |
| **Apache ActiveMQ**     | [`com.intsof.lts.activemq:*:5.16.8.1`](https://github.com/intersoftdatalabs-in/activemq/releases/tag/v5.16.8.1) (`#307`)                        | `5.16.8`           | Backported 13 2026 CVEs (`CVE-2026-33227`..`CVE-2026-54475`)                                                                              |
| **Spring Security**     | [`com.intsof.lts.springframework.security:*:5.8.16.1`](https://github.com/intersoftdatalabs-in/spring-security/releases/tag/v5.8.16.1) (`#308`) | `5.8.16`           | Backported security fixes onto Spring Security `5.8.16` on JDK 8                                                                          |
| **Spring Framework**    | [`com.intsof.lts.springframework:*:5.3.39.1`](https://github.com/intersoftdatalabs-in/spring-framework/releases/tag/v5.3.39.1) (`#309`)         | `5.3.39`           | Backported 26 unique CVEs (`CVE-2016-1000027`, `CVE-2024-38816`..`CVE-2026-47884`)                                                        |
| **Apache Commons Lang** | [`com.intsof.lts.commons:commons-lang:2.6.1`](https://github.com/intersoftdatalabs-in/commons-lang/releases/tag/v2.6.1) (`#318`)                | `2.6`              | Iterative `ClassUtils.getClass` fix for `CVE-2025-48924` (Shindig 2.5 runtime dep)                                                        |
| **Eclipse Jetty**       | [`com.intsof.lts.jetty:*:9.4.65.2`](https://github.com/intersoftdatalabs-in/jetty.project/releases/tag/v9.4.65.2) (`#319`)                      | `9.4.58.v20250814` | Relocated 139 reactor POMs to `com.intsof.lts.jetty` preserving `org.eclipse.jetty.*` packages (`CVE-2024-6763` + 8 patched Jetty 9 CVEs) |

## Removed & Migrated Components

- **Deprecated Secure Membership Removed (`#297`):** Deleted `@Deprecated` `deliverytiersuite/delivery-tier-suite/secure-membership` (`perc-secure-membership`) and `sys_resources/webapps/secure`, eliminating `org.springframework.ldap:spring-ldap-core` (`CVE-2026-41720`) and `spring-security-ldap`. Replaced `jackrabbit-jcr-commons` in `PSQueryResultUtils` and `ValueUtil` in-repo (`CVE-2025-58782`).
- **Legacy Apache SOAP Removed & HttpClient 5 Migration (`#310`):** Deleted obsolete `soap:soap:2.3.1` webservice handlers/descriptors/clients and migrated all `commons-httpclient:3.1` consumers (`PSDeliveryClient`, `TLSSocketFactory`/`PSTlsSocketFactory`, DTS/JEXL/toolkit clients, `PSRestClient`, and Shindig `BasicHttpFetcher`) to Apache HttpClient `5.6.3`.
- **OWASP ESAPI Retired (`#316`):** Removed `org.owasp.esapi:esapi:2.7.0.0` (eliminating `commons-configuration:1.10`, `CVE-2025-46392`), ESAPI config resources, and installer properties; migrated `SecureStringUtils` to JDK 8 + `org.owasp.encoder:encoder:1.4.0`.

## Upgrade & Build Operator Notes

- **Customized Jetty `jvm.ini` (`#307` ActiveMQ hardening):** `system/Tools/jetty/defaults/start.d/jvm.ini` adds `-Dorg.apache.activemq.transport.VM_TRANSPORT_FACTORY_SCHEMES_ENABLED=broker,properties,xbean` under `--exec`. Upgraded installations that maintain customized `jetty/base/start.d/jvm.ini` files must retain or add this JVM option so embedded ActiveMQ `vm://localhost?brokerConfig=xbean:activemq.xml` (`perc-mq.xml` / `jetty-env.xml`) initializes cleanly.
- **Developer & CI Maven Settings:** All 8 `github-intersoft-*` repository entries are documented in `settings-template.xml` (backed by public GitHub Packages repositories and built-in `GITHUB_TOKEN` in workflows).

