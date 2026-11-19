# ADR-298: Wave 2 Strategy for Spring Framework 5.3.39, Spring Security 5.8.16 & ActiveMQ 5.16.8 on Java 8

- **Status:** Proposed (Decision Record for `8.1.9+` / `8.2.x` Roadmap)
- **Issue:** [#298](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/298) (under Epic [#299](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/299))
- **Prior Analysis & Wave 1 Work:** [#290](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/290) / PR [#291](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/291), Wave 0 [#292](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/292) / PR [#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300), [ADR-293](../293-wave1-eol-coordinate-strategy/ADR.md) (PR [#301](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/301)), and Wave 1 PRs [#302](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/302)–[#305](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/305)

## Context

After Wave 0 ([#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300)) landed on `main` (`093a3d37b`), open Maven Dependabot alerts dropped from **84** to **62** (9 Critical, 24 High, 22 Medium, 7 Low). Wave 1 Stages 1–4 ([PR #302](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/302) Tika `2.9.4.1`, [PR #303](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/303) CXF `3.5.11.1`, [PR #304](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/304) Derby `10.14.2.1`, [PR #305](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/305) retiring `perc-secure-membership` + replacing `jackrabbit-jcr-commons`) eliminate **8** more alerts (**5 Critical, 2 High, 1 Medium**), leaving **54** open Maven Dependabot alerts in the residual post-Wave-1 baseline:

| Residual Group | Open Alerts | Breakdown by Artifact / CVE Profile | Java 8 Upgrade Boundary |
|---|---:|---|---|
| **Spring Framework `5.3.39`** | **27** | `spring-webmvc` (16), `spring-expression` (4), `spring-context` (2), `spring-core` (2), `spring-web` (2), `spring-jms` (1) | `5.3.40+` is Broadcom commercial paywall; `6.x`/`7.x` requires **JDK 17+ & `jakarta.*`** |
| **Apache ActiveMQ `5.16.8`** | **16** | `activemq-broker` (12), `activemq-client` (4) | `5.17+` / `5.19.8+` requires **JDK 11+** (or JDK 17+ for `6.x`) |
| **Spring Security `5.8.16`** | **5** | `spring-security-web` (3), `spring-security-core` (1), `spring-security-crypto` (1 — `CVE-2025-22228` `#119` already mitigated in Wave 0) | `5.8.17+` is commercial paywall; `6.x`/`7.x` requires **JDK 17+ & `jakarta.*`** |
| **Wave 2 Subtotal** | **48 (88.9%)** | Three high-churn core frameworks | Cannot bump cleanly without JDK 17+ / `jakarta.*` or commercial LTS |
| **Bucket 3 Residual Legacy / Cross-Major** | **6 (11.1%)** | `soap:soap@2.3.1` (2: `#29`, `#31`), `commons-httpclient:3.1` (1: `#32`), `commons-lang:2.6` (1: `#41`), `commons-configuration:1.10` (1: `#120`), `jetty-http` (`#115` `CVE-2024-6763`, `<= 12.0.11`) | Attic/EOL legacy migrations (`soap`, `commons-httpclient`) or `esapi:2.7.0.0` transitives (`commons-lang`, `commons-configuration`) |

Unlike Tika (`2.9.4.1`), CXF (`3.5.11.1`), and Derby (`10.14.2.1`), **Spring Framework `5.3.x`**, **Spring Security `5.8.x`**, and **ActiveMQ `5.16.x`** carry **high ongoing CVE churn (~40+ advisories/year combined)**, proprietary/missing public patch commits on commercial `5.3.40+`/`5.8.17+` branches (`13` Spring Framework CVEs publish only `spring.io` advisory pages without public 5.3.x commits), and JDK 17 + Develocity/Gradle CI dependencies (`spring-framework` and `spring-security` upstream Gradle builds exclude Linux JDK 8).

---

## Codebase Reachability Summary (Verified in `#290`)

Before committing engineering effort to fork three large frameworks on Java 8, the `#290` feasibility study audited actual Percussion call paths across all Critical/High Wave 2 advisories:

1. **Apache ActiveMQ `5.16.8` (16 alerts) — Low Reachability in Default Deployment:**
   - [perc-mq.xml](../../../../system/Tools/jetty/defaults/etc/perc-mq.xml) and [activemq.xml](../../../../system/Tools/jetty/defaults/etc/activemq/activemq.xml) bind an **in-VM broker only** (`vm://localhost`), with `<transportConnectors/>` empty, `advisorySupport=false`, and no Jolokia web console exposed.
   - Remote wire-framing / OpenWire / AMQP / MQTT / Jolokia CVEs (`CVE-2026-34197`, `CVE-2026-53917`, etc.) are unreachable unless an operator explicitly configures external transport connectors.
2. **Spring Framework `5.3.39` (27 alerts) — Major Critical/High Sinks Unreferenced:**
   - Codebase searches found **0 references** to `XsltView` (`CVE-2026-47884` Critical), `ScriptTemplateView` (`CVE-2026-22737`), `HttpInvoker` (`CVE-2016-1000027` Critical), `UriComponentsBuilder` (`CVE-2026-41854`), or `MappingJackson2MessageConverter` (`CVE-2026-41855` High — `required-server-beans.xml` uses `JmsTemplate` without Jackson converters).
   - Active surfaces are `@RequestMapping` MVC dispatchers, static resource handling, SpEL evaluation, and core bean/web binding (documented in `docs/SECURITY-MITIGATIONS.md`).
3. **Spring Security `5.8.16` (5 alerts) — Mitigated / Unreferenced Sinks:**
   - Codebase searches found **0 usages** of `CookieRequestCache` (`CVE-2026-41706`), Spring Security `X509` (`CVE-2026-47838`), or `DaoAuthenticationProvider` (`CVE-2026-22746`).
   - `PSSecurityHeadersFilter` runs independently in `web.xml` alongside `<security:headers defaults-disabled="true"/>` (`CVE-2026-22732` defense-in-depth).
   - `CVE-2025-22228` (`BCryptPasswordEncoder` >72-byte silent truncation, `#119`) was mitigated app-side in Wave 0 (`#292` / PR `#300`) via `LengthValidatingBCryptEncoder` in `PSMembershipPasswordEncryptorFactory` (`PSMembershipPasswordEncryptorFactoryTest`).

---

## Options Evaluated

### Option 1 — Runtime Mitigations + CycloneDX VEX + Accelerate Java 17 (`8.2.x`) *(Recommended Default)*
Keep the `8.1.x` Java 8 LTS line on upstream `Spring 5.3.39`, `Spring Security 5.8.16`, and `ActiveMQ 5.16.8` with documented runtime/config hardening (`docs/SECURITY-MITIGATIONS.md` + `owasp-suppressions.xml`), publish machine-readable **CycloneDX VEX (`not_affected` / `affected_mitigated`)** statements in `8.1.9+` release artifacts, and direct engineering capacity toward migrating `percussioncms` to **JDK 17+ / Jakarta (`8.2.x`)** where Spring 6.x, Spring Security 6.x, and ActiveMQ 5.19+/6.x resolve all 48 alerts natively.

| Dimension | Assessment |
|---|---|
| **Engineering Effort** | **Low** (1–2 days to generate & verify CycloneDX VEX from existing audit tests + `SECURITY-MITIGATIONS.md`; zero fork build maintenance) |
| **Ongoing Churn** | **None** on `8.1.x` build infrastructure; periodic VEX triage on newly published CVEs |
| **Dependabot / SCA Dashboard** | Open alerts remain visible in GitHub Dependabot unless dismissed via GraphQL/API or gated with VEX during release compliance reviews |
| **Risk / ROI** | **Highest ROI** — avoids maintaining a fragile 3-framework, 13-proprietary-CVE backport fork while shipping `8.2.x` on JDK 17 |

### Option 2 — Commercial Java 8 LTS Support *(Contractual Zero-Alert Fallback)*
License commercial Java 8 EOL support (**HeroDevs Never-Ending Support [NES]** for Spring 5.3 / Spring Security 5.8 / ActiveMQ 5.16, or **Broadcom Tanzu Spring Enterprise**) and consume their drop-in Maven artifacts if customer security agreements require **zero open Dependabot / Snyk / Trivy findings** on `8.1.x` prior to customer adoption of `8.2.x`.

| Dimension | Assessment |
|---|---|
| **Engineering Effort** | **Low–Medium** (BOM/repository wiring, smoke regression testing, customer-facing license compliance) |
| **Ongoing Churn** | **Vendor-managed** (vendor tracks Broadcom/upstream CVEs, backports `jakarta.*` changes to `javax.*`, and publishes signed Java 8 releases) |
| **Dependabot / SCA Dashboard** | Cleared when vendor coordinates/VEX feeds match SCA database (verify scanner recognition during PoC) |
| **Risk / ROI** | **Better build-vs-buy economics** than an internal Spring fork whenever zero-CVE scanner parity is a hard sales/compliance requirement |

### Option 3 — Selective In-House Backports *(Strictly Gated Exception Policy Only)*
Maintain an internal fork (`com.intsof.lts.springframework` / `com.intsof.lts.activemq`) **only on an exception basis** for individual CVEs that meet ALL three criteria:
1. Severity is **Critical or High**, AND
2. Exploitability is **proven reachable** in Percussion's actual servlet/MVC/SpEL/in-VM-JMS call paths, AND
3. Mitigation cannot be cleanly enforced at an application filter/configuration layer or in a single self-contained class (unlike `LengthValidatingBCryptEncoder` in Wave 0 or the in-VM `vm://localhost` lock in ActiveMQ).

| Dimension | Assessment |
|---|---|
| **Engineering Effort** | **Very High** (2–3+ weeks initial Gradle 7.5 / JDK 17 toolchain + `jakarta.*` -> `javax.*` backporting across 5.3.x + ActiveMQ; reverse-engineering 13 non-public Broadcom fixes) |
| **Ongoing Churn** | **Unsustainable** (~15–20+ Spring CVEs/yr + ActiveMQ CVEs requiring continuous `jakarta.*` backports and regression tests) |
| **Dependabot / SCA Dashboard** | Relocated `com.intsof.lts.*` coordinates require `fork-manifest.json` + OSV watch, still leaving proprietary non-OSV Broadcom advisories harder to track automatically |
| **Risk / ROI** | **Poor ROI** for a general fork; strictly reserve for emergency isolated zero-day hotfixes if Option 1 filter mitigation is insufficient |

---

## Decision & Action Plan

1. **Adopt Option 1 as the standard `8.1.9+` engineering policy** and retain **Option 2 (HeroDevs NES / Broadcom Spring Enterprise)** as the pre-approved commercial procurement lever if enterprise customer contracts mandate scanner-zero Java 8 builds before upgrading to `8.2.x`.
2. **Do not fork full Spring Framework `5.3.39`, Spring Security `5.8.16`, or ActiveMQ `5.16.8` in-house** (`Option 3` stays strictly limited to isolated, proven-reachable emergency patches that cannot be mitigated at runtime).
3. **8.1.9 Follow-Up Deliverables (Non-Blocking):**
   - Generate a release **CycloneDX VEX** JSON bundle in CI mapping the 48 Wave 2 alerts (`not_affected` for unused Spring/ActiveMQ surfaces; `in_triage` / `resolved_with_pedigree` / runtime-mitigated for `CVE-2025-22228` and ActiveMQ in-VM isolation).
   - Track remaining **Bucket 3 legacy code migrations** as standalone cleanups on `8.2.x` / backlog:
     - `soap:soap@2.3.1` (`#29`, `#31`) -> complete CXF JAX-WS migration for the remaining 14 `PSWebServiceAgent` call sites or remove dead stubs.
     - `commons-httpclient:3.1` (`#32`) -> migrate the remaining HttpClient 3.x call sites to Apache HttpClient 5.x (already managed in root `pom.xml`).
     - `esapi:2.7.0.0` transitives (`commons-lang:2.6` `#41`, `commons-configuration:1.10` `#120`) and `jetty-http` (`#115` `CVE-2024-6763`) -> retire ESAPI 2.x / upgrade Jetty to `12.x` on `8.2.x` (JDK 17+).

---

> Co-Authored by Antigravity CLI using Orchestrator (Neon) with agent Antigravity.
