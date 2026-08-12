# Percussion CMS (Java 8 / 8.1.x)

**Actively maintained by [Intersoft Data Labs](https://www.intsof.com)** · Apache 2.0 · Formerly Percussion CM1 / Rhythmyx / CM System

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%20(JDK%201.8)-orange.svg)](https://openjdk.org/)
[![Latest Release](https://img.shields.io/github/v/release/intersoftdatalabs-in/percussioncms-java8?label=Latest%20Release)](https://github.com/intersoftdatalabs-in/percussioncms-java8/releases)
[![Dependency Submission](https://github.com/intersoftdatalabs-in/percussioncms-java8/actions/workflows/dependency-submission.yml/badge.svg?branch=main)](https://github.com/intersoftdatalabs-in/percussioncms-java8/actions/workflows/dependency-submission.yml)

> **This repository is the Java 8 LTS home for the 8.1.x product line.**  
> Default branch: **`main`**. All code **must remain compatible with JDK 1.8.0**.  
> Active platform modernization (Java 21 / 8.2) lives in  
> **[intersoftdatalabs-in/percussioncms](https://github.com/intersoftdatalabs-in/percussioncms)**.

---

## What is Percussion CMS?

Percussion CMS is a mature, enterprise-grade, decoupled / headless-capable content management system with roots going back to 1999. It unifies the content production strengths of the original CM1 with the deep development and integration capabilities of Rhythmyx.

It was designed from the beginning for multi-channel delivery — websites, static sites, XML/JSON feeds, databases, and custom channels — with strong workflow, permissions, and extensibility for both marketers and developers.

**Smart architecture. Smart APIs. Smart UI.**

Intersoft Data Labs assumed full responsibility for support, maintenance, and ongoing development of the Percussion CMS product line in July 2023 after Percussion Software ended commercial support. This repository is the official open-source home of the **Java 8 / 8.1.x** product line under the Apache 2.0 license.

---

## Current Status (August 2026)

| Version / line | Status | Notes |
|----------------|--------|--------|
| **8.1.7** | Current stable release on this repo | Security hardening, WCAG-oriented accessibility work, Google Analytics 4, REST fixes, and ongoing 8.1.x maintenance |
| **8.1.x (`main`)** | Active maintenance (this repository) | JDK **1.8 only**. Security fixes, regression fixes, and carefully capped dependency updates |
| **8.2 / Java 21** | Active development (separate repo) | See [percussioncms](https://github.com/intersoftdatalabs-in/percussioncms) — not built from this tree |

**Stay on a current 8.1.x release** when you must remain on Java 8. Recent 8.1.x builds include important security patches.

If you are planning a platform upgrade (Java 21, modern UI work, broader dependency refreshes), use the main [percussioncms](https://github.com/intersoftdatalabs-in/percussioncms) repository.

---

## What can you do with it?

- Create and manage one or more websites (small sites to large multi-site deployments)
- Re-purpose content to databases, XML/JSON channels, or other delivery endpoints
- Generate static websites
- Enforce editorial control through robust workflows and fine-grained permissions
- Extend the platform with custom applications, templates, and integrations
- Run fully decoupled / headless or hybrid delivery models

---

## How do I get it?

**Binaries and installers** for the Java 8 line are published on this project’s  
[Releases page](https://github.com/intersoftdatalabs-in/percussioncms-java8/releases).

The latest stable **8.1.x** release is always featured there.  
(The broader product release history also appears under  
[percussioncms/releases](https://github.com/intersoftdatalabs-in/percussioncms/releases).)

### Commercial Support & Services

**Intersoft Data Labs** is the exclusive commercial support and maintenance provider for Percussion CMS (all versions of CMS and Rhythmyx) since July 2023.

- Production support and SLAs
- Upgrade and migration assistance (including paths toward newer lines)
- Custom development and integrations
- Hosting / managed services options

Contact: [inquire@intsof.com](mailto:inquire@intsof.com) · [intsof.com](https://www.intsof.com) · [Support Portal](https://percussionsupport.intsof.com)

Documentation: [percussioncmshelp.intsof.com](https://percussioncmshelp.intsof.com)

---

## I was a Percussion customer — is this where I download 8.1.x updates?

**Yes, for Java 8 / 8.1.x.** Use the [Releases page](https://github.com/intersoftdatalabs-in/percussioncms-java8/releases) on this GitHub project.

- Technical support: [Intersoft support portal](https://percussionsupport.intsof.com)
- Product docs: [percussioncmshelp.intsof.com](https://percussioncmshelp.intsof.com)
- Community discussion: [percussioncmscommunity.intsof.com](https://percussioncmscommunity.intsof.com)

---

## Building from Source

### Requirements

| Requirement | Notes |
|-------------|--------|
| **JDK 8** | Required. Amazon Corretto 8 or Eclipse Temurin 8 recommended. Set `JAVA_HOME` / `JAVA_HOME_8` to a **1.8** install only. |
| **Git** | Including Git LFS (`git lfs pull` before building modules that use LFS) |
| **Maven** | Prefer the repo wrapper + env scripts below (do not assume a system Maven on a newer JDK) |

This line **must not** introduce language features, APIs, or dependencies that require Java 11+.

### Recommended build commands

Use the environment scripts so Maven always runs under JDK 8:

```bash
# Linux / macOS — set once in your shell profile, e.g.:
#   export JAVA_HOME_8=/usr/lib/jvm/java-1.8.0-amazon-corretto
./mvn-env.sh clean install

# Faster local iteration
./mvn-env.sh clean install -DskipTests

# Single module
./mvn-env.sh clean install modules/perc-toolkit
```

```batch
REM Windows — set JAVA_HOME_8 to your JDK 8 home, then:
mvn-env.bat clean install
```

If you use the wrapper directly, ensure `JAVA_HOME` already points at JDK 8:

```bash
./mvnw clean install
```

Agent and contributor conventions (modules, formatting, Git workflow) are summarized in [AGENTS.md](AGENTS.md) and the [Contributor Guide](CONTRIBUTING.md).

### Install & run (after a successful build)

**CMS distribution**

```bash
java -jar ./modules/perc-distribution-tree/target/perc-distribution-tree.jar <installation-directory>
# Example:
java -jar ./modules/perc-distribution-tree/target/perc-distribution-tree.jar /opt/Percussion
```

**Delivery Tier (DTS)**

```bash
java -jar ./deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution*.jar <installation-directory>
```

Start Jetty from the install:

```bash
# Linux / macOS
cd <installation-directory>/jetty/ && ./StartJetty.sh

# Windows
cd <installation-directory>\jetty\ && StartJetty.bat
```

Default CMS URL: `http://localhost:9992/`

Default Admin & Contributor users are generated with dynamically generated temporary passwords at startup.  These are written to <installation-directory>/var/config/generated/passwords.  These passwords should be changed and the password file deleted after post installation login. 

---

## Key modules

| Module | Path | Description |
|--------|------|-------------|
| CMS core | `system` | Server core |
| WebUI | `WebUI` | Primary UI |
| sitemanage | `projects/sitemanage` | Backend for the primary UI |
| rest | `rest` | Public REST API |
| DTS | `deliverytiersuite/delivery-tier-suite` | Delivery-tier services |
| Distribution | `modules/perc-distribution-tree` | Installable CMS distribution |

---

## Contributing

We welcome contributions that keep the **8.1.x / Java 8** line healthy: bug fixes, security hardening, tests, and docs.

Please see:

- [CONTRIBUTING.md](CONTRIBUTING.md) — process and orientation  
- [AGENTS.md](AGENTS.md) — coding, build, and PR conventions for this repo  

**Do not** land Java 11+ only dependencies or language features on `main`.  
For Java 21 / 8.2 work, open PRs against [percussioncms](https://github.com/intersoftdatalabs-in/percussioncms).

---

## Related repositories

| Repository | Role |
|------------|------|
| **[percussioncms-java8](https://github.com/intersoftdatalabs-in/percussioncms-java8)** (this repo) | Java 8 LTS — **8.1.x** maintenance |
| **[percussioncms](https://github.com/intersoftdatalabs-in/percussioncms)** | Active product line — **Java 21 / 8.2** development |

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.

---

**Maintained with care by Intersoft Data Labs**  
Questions? Open an issue or reach out via the [support portal](https://percussionsupport.intsof.com).
