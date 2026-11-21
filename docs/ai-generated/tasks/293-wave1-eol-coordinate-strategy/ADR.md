# ADR-293: Wave 1 EOL Java 8 Coordinate, Publishing & Scanner Guardrail Strategy

- **Status:** Accepted (`8.1.9-dev`)
- **Issue:** [#293](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/293) (under Epic [#299](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/299))
- **Prior Analysis:** [#290](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/290) / PR [#291](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/291), Wave 0 [#292](https://github.com/intersoftdatalabs-in/percussioncms-java8/issues/292) / PR [#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300)

## Context

After Wave 0 ([#300](https://github.com/intersoftdatalabs-in/percussioncms-java8/pull/300)) landed on `main` (`093a3d37b`) and updated the GitHub Dependency Graph snapshot, open Maven Dependabot alerts dropped from **84** to **62** (9 Critical, 24 High, 22 Medium, 7 Low). Remaining Wave 1 targets include dependencies pinned to EOL Java 8 lines whose upstream advisories span major versions (`Tika 3.x`, `CXF 3.6+`, `Jackrabbit 2.21+`, `Spring LDAP 3.x`), alongside `Apache Derby 10.14.2.0` whose upstream GHSA range is strictly `== 10.14.2.0` (fixed in `10.14.2.1` on Apache SVN `branches/10.14`, never published to Maven Central).

Maintaining security fixes on Java 8 (`8.1.x` / `javax.*`) requires both runtime remediation and reliable SCA / Dependabot hygiene without creating split-classpath collisions or upstream CVE blind spots.

## Decisions

### 1. Hybrid Maven Coordinate Strategy

- **Use original upstream coordinates (`org.apache.derby`)** when upstream GHSA vulnerability ranges terminate within the Java 8 branch (`== 10.14.2.0` -> `10.14.2.1`), allowing Dependabot alert `#77` to auto-close without exclusions.
- **Use relocated Intersoft coordinates (`com.intsof.lts.<family>`) with unchanged Java package namespaces** when upstream GHSA ranges cross into Java 11+/17+ major lines (`Tika`, `CXF`). Keeping Java package names (`org.apache.tika.*`, `org.apache.cxf.*`) unchanged requires zero Percussion Java/Spring XML refactoring while clearing cross-major Dependabot alerts on upstream coordinates.
- **Keep existing Jetty precedent (`org.eclipse.jetty:9.4.65.1`)** unchanged; `jetty.project` remains tracked in `fork-manifest.json` (1 residual cross-major alert `#115` `CVE-2024-6763` `<= 12.0.11`).

### 2. Per-Fork GitHub Packages Repositories

- Publish each Intersoft Java 8 LTS fork to its own GitHub Packages Maven registry under `intersoftdatalabs-in` (`https://maven.pkg.github.com/intersoftdatalabs-in/<repo>`), matching the existing [`intersoftdatalabs-in/jetty.project`](https://github.com/intersoftdatalabs-in/jetty.project) (`v9.4.65.1`) pattern.
- Each fork's `.github/workflows/release.yml` uses the repository's built-in `secrets.GITHUB_TOKEN` (`packages: write`), avoiding organization-wide write PAT secrets.
- `percussioncms-java8` configures matching `<repository>` entries in root `pom.xml`, template credentials in `settings-template.xml`, and CI server credentials in `.github/workflows/dependency-submission.yml`.

### 3. Dual Guardrails Against Relocated Coordinate Blind Spots

1. **Fail-closed split-classpath enforcement (`maven-enforcer-plugin` `<bannedDependencies>`):** Root `pom.xml` binds `enforce-banned-eol-dependencies` to the `validate` phase (`<fail>true</fail>`) so banned/removed coordinates and replaced upstream coordinates (`org.apache.tika:*` in `#294`, `org.apache.cxf:*` in `#295`) fail the build immediately if reintroduced transitively.
2. **Weekly upstream CVE monitoring (`fork-manifest.json` + `.github/workflows/upstream-cve-watch.yml`):** Every forked family records its upstream GAVs, pinned upstream base version, and patched CVEs in `fork-manifest.json`. A weekly GitHub Actions job queries OSV (`api.osv.dev`) against upstream coordinates, subtracts `patched_cves`, and opens or updates an alert issue if any new upstream vulnerability appears.

### 4. `@Deprecated` `perc-secure-membership` Disposition (`#297`)

- Retire and remove `@Deprecated` `perc-secure-membership` and `sitemanage` `webapps/secure` in `8.1.9` (`#297`), eliminating `spring-ldap-core` (`#151`) and `spring-security-ldap` from the distribution.

### 5. Severity-Ordered Wave 1 Execution Roadmap (`8.1.9-dev`)

1. **Stage 0 (`#293`):** ADR, `fork-manifest.json`, OSV CVE-watch workflow, fail-closed Enforcer `<bannedDependencies>`.
2. **Stage 1 (`#294`):** Apache Tika `2.9.4` -> `com.intsof.lts.tika:2.9.4.1` (**3 Critical**, reachable via `PDFParser` in `PSTikaTextConvertor`).
3. **Stage 2 (`#295`):** Apache CXF `3.5.11` -> `com.intsof.lts.cxf:3.5.11.1` (**1 Critical + 1 High**, active REST/SOAP stack).
4. **Stage 3 (`#296`):** Apache Derby `10.14.2.1` from Apache SVN `branches/10.14` (**1 Critical**, localhost-only DRDA / SHA-256 auth).
5. **Stage 4 (`#297`):** Retire `@Deprecated` `perc-secure-membership` + `webapps/secure` (**1 High**) and replace `jackrabbit-jcr-commons` in-repo (**1 Medium**).
6. **Wave 2 (`#298`):** Strategic evaluation for `Spring Framework 5.3.39` / `Spring Security 5.8.16` / `ActiveMQ 5.16.8`.

