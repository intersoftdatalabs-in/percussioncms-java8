# ADR-298: Wave 2 In-House Java 8 LTS Strategy (`com.intsof.lts.*`) for ActiveMQ 5.16, Spring Security 5.8 & Spring Framework 5.3

- **Status:** Accepted (`8.1.9-dev`)
- **Issue:** [#298](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/298) (under Epic [#299](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/299))
- **Prior Analysis & Wave 1 Work:** [#290](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/290) / PR [#291](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/291), Wave 0 [#292](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/292) / PR [#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300), [ADR-293](../293-wave1-eol-coordinate-strategy/ADR.md) (PR [#301](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/301)), and Wave 1 PRs [#302](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/302)–[#305](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/305)

## Context

After Wave 0 ([#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300)) landed on `main` (`093a3d37b`), open Maven Dependabot alerts dropped from **84** to **62** (9 Critical, 24 High, 22 Medium, 7 Low). Wave 1 Stages 1–4 ([PR #302](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/302) Tika `2.9.4.1`, [PR #303](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/303) CXF `3.5.11.1`, [PR #304](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/304) Derby `10.14.2.1`, [PR #305](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/305) retiring `perc-secure-membership` + replacing `jackrabbit-jcr-commons`) eliminate **8** more alerts (**5 Critical, 2 High, 1 Medium**), leaving **54** open Maven Dependabot alerts in the residual post-Wave-1 baseline:

| Residual Group | Open Alerts | Breakdown by Artifact / CVE Profile | Java 8 Upgrade Boundary |
|---|---:|---|---|
| **Apache ActiveMQ `5.16.8`** | **16** | `activemq-broker` (12), `activemq-client` (4) across 13 distinct 2026 CVEs | `5.17+` / `5.19.8+` requires **JDK 11+** (or JDK 17+ for `6.x`) |
| **Spring Security `5.8.16`** | **5** | `spring-security-web` (3), `spring-security-core` (1), `spring-security-crypto` (1 — `CVE-2025-22228` `#119` mitigated in Wave 0) | `5.8.17+` is commercial paywall; `6.x`/`7.x` requires **JDK 17+ & `jakarta.*`** |
| **Spring Framework `5.3.39`** | **27** | `spring-webmvc` (16), `spring-expression` (4), `spring-context` (2), `spring-core` (2), `spring-web` (2), `spring-jms` (1) | `5.3.40+` is Broadcom commercial paywall; `6.x`/`7.x` requires **JDK 17+ & `jakarta.*`** |
| **Wave 2 Core Frameworks Subtotal** | **48 (88.9%)** | Three core frameworks targeted by Wave 2 Stages 1–3 | Relocated in-house `com.intsof.lts.*` Java 8 LTS builds |
| **Bucket 3 Legacy / Cross-Major** | **6 (11.1%)** | `soap:soap@2.3.1` (2: `#29`, `#31`), `commons-httpclient:3.1` (1: `#32`), `commons-lang:2.6` (1: `#41`), `commons-configuration:1.10` (1: `#120`), `jetty-http` (`#115` `CVE-2024-6763`, `<= 12.0.11`) | `soap` & `commons-httpclient` migrated in Wave 2 Stage 4; `esapi:2.7.0.0` transitives & Jetty 12 track to `8.2.x` |

---

## Codebase Reachability Baseline (Defense-in-Depth Retained)

Existing application and configuration mitigations remain active as defense-in-depth alongside our LTS builds:

1. **Apache ActiveMQ `5.16.8`:** [perc-mq.xml](../../../../system/Tools/jetty/defaults/etc/perc-mq.xml) and [activemq.xml](../../../../system/Tools/jetty/defaults/etc/activemq/activemq.xml) enforce an in-VM broker (`vm://localhost`) with `<transportConnectors/>` empty, `advisorySupport=false`, and no Jolokia web console.
2. **Spring Framework `5.3.39`:** Zero codebase references to `XsltView`, `ScriptTemplateView`, `HttpInvoker`, `UriComponentsBuilder`, or `MappingJackson2MessageConverter`; active surfaces are MVC `@RequestMapping` dispatchers, static resources, SpEL, bean/web binding, and `JmsTemplate`.
3. **Spring Security `5.8.16`:** Zero references to `CookieRequestCache`, `X509`, or `DaoAuthenticationProvider`; `PSSecurityHeadersFilter` enforces HTTP security headers independently, and `LengthValidatingBCryptEncoder` (Wave 0) enforces the 72-byte BCrypt input ceiling.

---

## Decision: Reject Commercial Third-Party LTS; Compete With In-House `com.intsof.lts.*` Builds

### Rejected Option — Third-Party Commercial LTS (HeroDevs NES / Broadcom Spring Enterprise)
We **explicitly reject** procuring third-party commercial Java 8 LTS subscriptions (HeroDevs Never-Ending Support or Broadcom Tanzu Spring Enterprise). Intersoft already operates a proven Maven/GitHub Packages LTS build-and-watch pipeline (`intersoftdatalabs-in/jetty.project`, `tika`, `cxf`, `derby` + `fork-manifest.json` + `.github/workflows/upstream-cve-watch.yml` + fail-closed `maven-enforcer-plugin` bans). Building and publishing our own Java 8 LTS artifacts eliminates external licensing lock-in and demonstrates direct engineering parity on the Java 8 LTS line.

### Accepted Strategy — Wave 2 In-House `com.intsof.lts.*` Execution Plan

Intersoft will build, verify on JDK 8, and publish relocated **`com.intsof.lts.*`** Java 8 LTS artifacts (preserving `javax.*` binary & Java package compatibility) to per-fork GitHub Packages repositories, backed by `fork-manifest.json` upstream OSV monitoring and fail-closed `maven-enforcer-plugin` `<bannedDependencies>` rules, executed in four ordered stages:

| Stage | Target Family / Legacy Dep | Target Coordinate / Action | Alerts Covered | Toolchain & Patch Source |
|---|---|---|---|---|
| **Wave 2 — Stage 1** | **Apache ActiveMQ `5.16.8`** | `com.intsof.lts.activemq:5.16.8.1` (`intersoftdatalabs-in/activemq@v5.16.8.1`) | **16** (`activemq-broker` 12, `activemq-client` 4; 13 distinct 2026 CVEs) | **Native Maven JDK 8 build**; backport public Apache `5.19.x` / `main` commits for all 13 CVEs |
| **Wave 2 — Stage 2** | **Spring Security `5.8.16`** | `com.intsof.lts.springframework.security:5.8.16.1` (`intersoftdatalabs-in/spring-security@v5.8.16.1`) | **5** (`spring-security-web` 3, `spring-security-core` 1, `spring-security-crypto` 1) | **Gradle 7.5 standalone build** emitting JDK 8 bytecode (`major version: 52`) & `javax.*` packages; establishes reusable Gradle-to-GitHub-Packages publishing pipeline |
| **Wave 2 — Stage 3** | **Spring Framework `5.3.39`** | `com.intsof.lts.springframework:5.3.39.1` (`intersoftdatalabs-in/spring-framework@v5.3.39.1`) | **27** (`spring-webmvc` 16, `spring-expression` 4, `spring-context` 2, `spring-core` 2, `spring-web` 2, `spring-jms` 1) | Reuses Stage 2 **Gradle 7.5** pipeline (`JDK 17` build JDK targeting `JavaLanguageVersion.of(8)` bytecode, Develocity/commercial plugins stripped); backport public `6.x`/`7.x` patches onto `javax.*` and author independent JDK 8 fixes for advisory-only CVEs |
| **Wave 2 — Stage 4** | **Bucket 3 Legacy Removals (`soap`, `commons-httpclient`)** | Remove Apache SOAP `soap:soap:2.3.1` & migrate remaining `commons-httpclient:3.1` call sites to Apache HttpClient 5.x | **3** (`soap` 2 [1C, 1H], `commons-httpclient` 1 [1M]) | In-repo removal / migration + `enforce-banned-eol-dependencies` bans |

### Retained Guardrails & Defense-in-Depth
1. Keep runtime mitigations documented in `docs/SECURITY-MITIGATIONS.md` and `owasp-suppressions.xml` for defense-in-depth (including ActiveMQ in-VM `vm://localhost` lock and `PSSecurityHeadersFilter` / `LengthValidatingBCryptEncoder`).
2. Record every forked artifact family, upstream GAVs, upstream pinned base version, and patched CVEs in `fork-manifest.json` so `.github/workflows/upstream-cve-watch.yml` continuously alerts on any future upstream disclosure.
3. Ban replaced upstream coordinates (`org.apache.activemq:*`, `org.springframework.security:*`, `org.springframework:*`, `soap:soap`, `commons-httpclient:commons-httpclient`) in root `pom.xml` `enforce-banned-eol-dependencies` (`<fail>true</fail>`) as each stage lands.
4. Keep `esapi:2.7.0.0` transitives (`commons-lang:2.6` `#41`, `commons-configuration:1.10` `#120`) and `jetty-http` (`#115` `CVE-2024-6763`) documented in `docs/SECURITY-MITIGATIONS.md` alongside optional CycloneDX VEX attestations until `8.2.x` (`JDK 17+`).

---

> Co-Authored by Antigravity CLI using Orchestrator (Neon) with agent Antigravity.
