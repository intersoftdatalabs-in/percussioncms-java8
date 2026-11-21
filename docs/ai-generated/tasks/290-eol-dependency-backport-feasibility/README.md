# Stuck Java 8 Vulnerable Dependencies & EOL Backport Feasibility Study

**Tracking Issue:** [#290](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/290)  
**Baseline:** `main` (`00952044934b5b6762b50bd609d5235b0b9353d0`), analyzed `2026-11-19`  
**Precedent:** [`intersoftdatalabs-in/jetty.project`](https://github.com/intersoftdatalabs-in/jetty.project) (`v9.4.65.1`, GitHub Packages)

## Study Deliverables

- [`README.md`](README.md) — Executive summary, Feasibility & ROI Decision Matrix, and phased roadmap (this document)
- [`inventory.md`](inventory.md) — Reconciled Maven Java 8 vulnerability inventory across Dependabot, GitHub SBOM, OSV.dev, and `owasp-suppressions.xml`
- [`inventory.json`](inventory.json) — Machine-readable inventory (all 84 open alerts, 9 dismissed alerts, 34 OSV-matched GAVs, 117 suppression reconciliations)
- [`feasibility-study.md`](feasibility-study.md) — Cross-cutting coordinate/scanner/GitHub Packages architecture & 9-point feasibility scorecards for all stuck families

---

## Headline Findings

1. **Current live exposure is far cleaner than August 2026 — and 27% is solvable without any new fork:**
   - Out of **768 unique external Maven GAVs** in the live SBOM, GitHub Dependabot reports **84 open alerts across 32 packages** (**11 Critical, 32 High, 30 Medium, 11 Low**).
   - OSV.dev matches those 32 packages plus `org.apache.sanselan:sanselan@0.97-incubator` (2 High), and NVD/Red Hat records show one active non-Dependabot High CVE on `hibernate-core@5.6.15.Final` (`CVE-2026-0603`).
   - **23 of the 84 open Dependabot alerts (27.4%) require zero new EOL forks** ("Wave 0"): direct Java 8 updates (`guava`, `zookeeper`, `jakarta.el:3.0.4`), BOM alignment/exclusions (`jetty 9.4.57` vs existing `9.4.65.1` fork, `http2-common` optional transitive, `bc*-jdk18on` duplicate), and dead dependency removal (`shiro` in `shindig-uber`).
2. **Forking & backporting shines for low-churn Maven libraries (`Tika`, `CXF`, `Derby`, `Jetty`), not high-churn Gradle frameworks (`Spring 5.3`):**
   - **Tika `2.9.4`** (3 Critical XXE alerts, genuinely reachable via `PDFParser` in `PSTikaTextConvertor`) and **CXF `3.5.11`** (1 Critical XXE + 1 High DoS, active REST/SOAP stack) have clean Maven JDK 8 builds and surgical upstream patches.
   - **Derby `10.14.2.0`** (1 Critical alert `#77`) already has its `CVE-2022-46337` fix committed in Apache SVN `branches/10.14` — Apache simply never cut a Maven Central `10.14.2.1` release.
   - **Spring Framework `5.3.39`** (27 alerts) + **Spring Security `5.8.16`** (5 alerts) + **ActiveMQ `5.16.8`** (16 alerts) carry **48 of the 84 alerts**, but most Critical/High Spring paths (`XsltView`, `HttpInvoker`, `ScriptTemplateView`, `MappingJackson2MessageConverter`, `CookieRequestCache`, `X509`) have **0 call sites** in Percussion, ActiveMQ runs `vm://localhost` with `<transportConnectors/>` empty, Spring 5.3 requires Gradle + JDK 17 + private Broadcom fixes, and CVE churn is high.
3. **Why `jetty.project` (`9.4.65.1`) still shows open Dependabot alerts — and how to reach zero alerts:**
   - **Short-term fix:** Import `jetty-bom:9.4.65.1` in root `pom.xml`, exclude `http2-*` from `solr-solrj`, and add GitHub Packages auth to `dependency-submission.yml`.
   - **Zero-Dependabot scanner dilemma:** Because GHSA ranges for ActiveMQ (`< 5.19.8`), CXF (`< 3.6.12`), Tika (`<= 3.2.1`), Spring (`<= 5.3.49` / `< 6.0.0`), and even `jetty-http` `CVE-2024-6763` (`<= 12.0.11`) cross major lines, bumping versions under original `groupId`s (Strategy A) **will not clear Dependabot alerts**. Reaching true zero-alert scans requires either **Strategy B (relocated `groupId` `com.intsof.lts.*` + `maven-enforcer-plugin` `<bannedDependencies>` + weekly OSV/GHSA upstream CVE-watch CI + VEX)** or **Strategy D (original `groupId` + automated VEX/Dependabot dismissals)**.

---

## Feasibility & ROI Decision Matrix

|                    Family (Shipped Version)                    | Open Alerts (C/H/M/L) |             Real Risk in Percussion             | Build Tool (JDK) | Backport Complexity  | Initial Effort | Ongoing Churn |       Dependabot Clears via Version Bump?       |                      Recommendation                       |
|----------------------------------------------------------------|----------------------:|-------------------------------------------------|------------------|----------------------|----------------|---------------|-------------------------------------------------|-----------------------------------------------------------|
| **Guava `33.7.1-jre`**                                         |           1 (0/1/0/0) | Low–Med                                         | N/A              | N/A                  | 0.5h           | —             | Yes (`33.7.2-jre` / `33.8.0-jre`)               | **Wave 0: Direct Java 8 bump**                            |
| **ZooKeeper `3.7.2`**                                          |           1 (0/0/1/0) | Low                                             | N/A              | N/A                  | 0.5d           | —             | Yes (`3.8.4+` / `3.9.2+` `[52]`)                | **Wave 0: Direct Java 8 bump** (verify `CloudSolrClient`) |
| **GlassFish `javax.el 3.0.0`**                                 |           1 (0/0/1/0) | Low (EL off)                                    | N/A              | N/A                  | 0.5d           | —             | Yes (`jakarta.el:3.0.4` `[51]`)                 | **Wave 0: Swap to `jakarta.el:3.0.4`**                    |
| **Bouncy Castle `jdk18on`**                                    |           6 (2/2/2/0) | None (`1.85` pinned)                            | N/A              | N/A                  | 0.5h           | —             | Yes (exclude from Tika)                         | **Wave 0: Exclude duplicate coordinate**                  |
| **Jetty HTTP/2 `9.4.53`**                                      |           3 (0/2/1/0) | None (unused)                                   | N/A              | N/A                  | 0.5h           | —             | Yes (exclude from `solr-solrj`)                 | **Wave 0: Exclude unused `http2-*`**                      |
| **Apache Shiro `1.13.0`**                                      |           6 (0/1/2/3) | None (dead/excluded)                            | Maven (JDK 8)    | Low–Med              | 0.5d           | Low–Med       | Yes (remove unused dep)                         | **Wave 0: Remove from `shindig-uber`**                    |
| **Jetty 9.4 Core (`9.4.65.1`)**                                |           5 (0/2/2/1) | Fixed in fork; `9.4.57` leak                    | Maven (JDK 8)    | Low                  | 0.5d           | Low–Med       | 4/5 auto-clear; `CVE-2024-6763` needs VEX/reloc | **Wave 0 wiring + Extend Existing Fork**                  |
| **Apache Tika `2.9.4`**                                        |           3 (3/0/0/0) | **High** (`PDFParser` active)                   | Maven (JDK 8)    | Low–Med              | 2–3d           | Low           | Needs Strategy B or D                           | **Wave 1: EOL Fork (Recommended)**                        |
| **Apache CXF `3.5.11`**                                        |           2 (1/1/0/0) | **High** (REST/SOAP active)                     | Maven (JDK 8)    | Low–Med              | 2–4d           | Low–Med       | Needs Strategy B or D                           | **Wave 1: EOL Fork (Recommended)**                        |
| **Apache Derby `10.14.2.0`**                                   |           1 (1/0/0/0) | Low (no LDAP auth)                              | Ant (JDK 8)      | **Trivial** (in SVN) | 1–2d           | Very Low      | **Yes (`10.14.2.1` clears `#77`)**              | **Wave 1: Fork/Publish if closing `#77` matters**         |
| **Jackrabbit `2.20.17`**                                       |           1 (0/0/1/0) | Very Low (`RowIteratorAdapter`)                 | Maven (JDK 8)    | Low                  | 0.5–1d         | Very Low      | Needs replace or Strategy B/D                   | **Wave 1: Replace in-repo (or tiny fork)**                |
| **Spring LDAP `2.4.4`**                                        |           1 (0/1/0/0) | Low (`@Deprecated` module)                      | Gradle           | Low                  | 0.5–2d         | Low           | Retire module or Strategy B/D                   | **Retire `perc-secure-membership` / App guard**           |
| **ActiveMQ `5.16.8`**                                          |         16 (0/12/4/0) | Low (`vm://localhost` only)                     | Maven (JDK 8)    | Med–High             | 1.5–2w         | **High**      | Needs Strategy B or D                           | **Wave 2: Conditional / Prefer Config+VEX**               |
| **Spring Framework `5.3.39`**                                  |         27 (2/8/12/5) | Mixed (0 hits on Criticals)                     | Gradle (JDK 17)  | **High**             | 2–3+w          | **Very High** | Needs Strategy B or D                           | **Wave 2: Commercial LTS vs Java 17**                     |
| **Spring Security `5.8.16`**                                   |           5 (1/1/2/1) | Low–Med                                         | Gradle (JDK 11+) | Med–High             | 1–2w           | Med–High      | Needs Strategy B or D                           | **Wave 2: Pair w/ Spring 5.3 + BCrypt guard**             |
| **Hibernate ORM `5.6.15.Final`**                               |            0 (1H NVD) | Mitigated (T2.14)                               | Gradle (JDK 8)   | High (LGPL-2.1)      | 3–5d           | Low           | Dependabot already 0                            | **Keep Mitigated (Do Not Fork)**                          |
| **Legacy Dead EOL (`soap`, `httpclient 3`, `sanselan`, etc.)** |   5 (1/1/2/1) +2H OSV | Mixed (`soap` 14 files, `httpclient3` 32 files) | Dead / Attic     | N/A                  | N/A            | None          | Remove / Migrate                                | **Do Not Fork — Migrate / Exclude**                       |

---

## Recommended Phased Roadmap

1. **Wave 0 — Immediate Quick Wins (Eliminates 23+ Dependabot Alerts + Leaks Without New Forks):**
   - Bump `guava.version` → `33.7.2-jre`/`33.8.0-jre`, `zookeeper.version` → `3.8.6+`/`3.9.2+` (post-`CloudSolrClient` test), and `javax.el:3.0.0` → `org.glassfish:jakarta.el:3.0.4`.
   - Import `org.eclipse.jetty:jetty-bom:${jetty.version}` (`9.4.65.1`) in root [pom.xml](../../../../pom.xml), exclude `org.eclipse.jetty.http2:*` and `jetty-alpn-openjdk8-client` on `solr-solrj`, and add GitHub Packages `settings.xml` auth to [.github/workflows/dependency-submission.yml](../../../../.github/workflows/dependency-submission.yml).
   - Exclude `bc*-jdk18on` from `tika-parsers-standard-package`, exclude `antisamy` from root `esapi:2.7.0.0`, exclude `sanselan` from `shindig-gadgets` in `shindig-uber`, remove unused `shiro-web` from `shindig-uber` (after Shindig smoke test), add empty-password + 72-byte BCrypt validation guards, and clean up unattached legacy POMs (`p13n-api`, `segmentation-api`, `redirect-tool`) and stale `owasp-suppressions.xml` entries.
2. **Wave 1 — High-ROI EOL Forks (Maven/Ant, Low Churn, High Security Impact):**
   - Fork and backport **`apache/tika` (`2.9.4` → `2.9.4.1`)** and **`apache/cxf` (`3.5.11` → `3.5.11.1`)**, plus publish **`apache/derby` `10.14` (`10.14.2.1`)** if closing Critical alert `#77` via version bump is desired.
   - Replace `jackrabbit-jcr-commons` (`RowIteratorAdapter`/`ValueFactoryImpl`) directly in-repo (or trivial 7-file fork) and retire `@Deprecated` `perc-secure-membership` if unused.
   - Standardize publishing into a single consolidated GitHub Packages repo (`intersoftdatalabs-in/java8-lts-packages`) and decide between **Strategy B** (`com.intsof.lts.*` + `<bannedDependencies>` + weekly OSV CVE-watch) vs. **Strategy D** (original `groupId` + automated VEX/Dependabot dismissals).
3. **Wave 2 — Strategic Build-vs-Buy Decision (`Spring 5.3` / `Spring Security 5.8` / `ActiveMQ 5.16`):**
   - Avoid in-house full-fork maintenance of Spring 5.3 / Spring Security 5.8 / ActiveMQ 5.16 unless backed by dedicated capacity; compare **HeroDevs NES / Broadcom Spring Enterprise** against accelerating migration to the **Java 17+ `percussioncms`** line while keeping T2.2/T2.4 runtime mitigations enforced.

