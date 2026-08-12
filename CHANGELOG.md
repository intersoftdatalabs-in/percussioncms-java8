# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.8 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Fixed (Task 8 — tainted-numeric-cast)

- **`java/tainted-numeric-cast` in PSGuid 3-component constructor** (#430, critical) — added an explicit int-range guard around the long->int narrowing cast in `PSGuid.assemble(String, PSTypeEnum, boolean)`. The `typeid` component was previously parsed as long and cast directly to int for `PSTypeEnum.valueOf` lookup, which CodeQL flags as a tainted narrowing conversion. The new guard rejects out-of-range input with a clear `IllegalArgumentException` (`Type id out of range for PSTypeEnum ordinal: ...`) instead of the previous generic `type cannot be null` error from `assemble()`. Behaviour for valid in-range input is unchanged.
- **Tainted-numeric-cast regression test** — `modules/utils/src/test/java/com/percussion/services/guidmgr/data/PSGuidTaintedNumericCastTest.java` (JUnit 4, 4 cases): in-range accept, above-Integer.MAX_VALUE reject, below-Integer.MIN_VALUE reject, in-range-but-unmatched-ordinal reject.

## [8.1.8 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Fixed

- **EC2 instance metadata detection on IMDSv2-only hosts** (#1) — `PSPubServerService.isEC2Instance()` and `PSAmazonS3DeliveryHandler.isEC2Instance()` used to probe `http://169.254.169.254/latest/meta-data/` with a plain IMDSv1-style GET. On Amazon Linux 2023+ and other AMIs with `HttpTokens=required`, that probe fails and the host is treated as non-EC2, forcing operators to set static Access Key / Secret even when using an EC2 instance profile (with or without Assume Role). Both probe paths now delegate to a new `PSEc2InstanceMetadataClient` (`system/business/.../PSEc2InstanceMetadataClient.java`) that performs the IMDSv2 token flow (`PUT /latest/api/token` with `X-aws-ec2-metadata-token-ttl-seconds`, then `GET` with `X-aws-ec2-metadata-token`) and falls back to IMDSv1 only when the token endpoint is not available. The result is cached for the JVM lifetime, the first probe is gated by a `CountDownLatch` so concurrent callers wait for the same result instead of racing two IMDS probes, and `PSPubServerService` / `PSAmazonS3DeliveryHandler` are pure delegates (no second JVM cache layer).
- **S3 publish server save rejects empty AWS credentials on non-EC2 hosts** (#1) — `PSPubServerService.validatePropertiesByDriver` no longer rejects missing Access Key / Secret Key on save. Operators can leave them empty when relying on an EC2 instance profile (with or without Assume Role + ARN). The runtime `PSAmazonS3DeliveryHandler.getAmazonS3Client` now fails fast with an explicit `PSDeliveryException` when neither EC2 nor Assume Role is available and the static keys are blank, so the publish error is clear instead of a confusing auth failure on the first S3 API call. The UI (PercPublishMinuetView) downgrades the post-save success footer alert to a warning alert when S3 is selected and the Access Key / Secret Key are empty, so operators are still informed that the publish will fail at runtime on non-EC2 hosts.

### Added

- **IMDSv2-aware metadata client** — `system/business/src/com/percussion/rx/delivery/impl/PSEc2InstanceMetadataClient.java` with `MetadataTransport` indirection for testability, JVM-lifetime result cache, IMDSv2 → IMDSv1 fallback, `CountDownLatch` for concurrent first-call, and a `resetCache()` hook for tests / ops.
- **IMDSv2 unit tests** — `system/Testing/src/com/percussion/rx/delivery/impl/PSEc2InstanceMetadataClientTest.java` covering IMDSv2 success, IMDSv1 fallback when token PUT is rejected, non-EC2 connection-refused, IMDSv2 metadata GET failure with IMDSv1 also failing, result caching, concurrent first-call (single probe), and `resetCache()` re-probing.

### Notes

- AL2023+ defaults require `HttpPutResponseHopLimit >= 2` when running inside a container; operate-side documentation should mention this alongside the code fix.
- The optional follow-up to support DefaultCredentialsProvider / IRSA-style auth for non-EC2 hosts is not in scope here and remains a separate work item.
- Legacy `WebUI/war/views/PublishView.js` does not share the missing-S3-credentials warning; the warning is only surfaced in the Minuet (`PercPublishMinuetView.js`) save path.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Added (Task 5 — zip-slip defense)

- **ZipSlipGuard helper** — `modules/perc-security-utils/src/main/java/com/percussion/security/io/ZipSlipGuard.java`. A focused subset of the development-branch `PathValidation`: canonical-path containment check (CWE-22/CWE-23). Resolves both `/` and `\` as separators (Windows-built installers emit backslashes), strips leading separators so absolute path entries normalize to a relative position under the extract root, and rejects any remaining path whose canonical form escapes the root.
- **ZipSlipGuard unit tests** — `modules/perc-security-utils/src/test/java/com/percussion/security/io/ZipSlipGuardTest.java`, JUnit 4, 9 cases (flat file, deep nesting, parent traversal, backslash traversal, absolute path normalization, mid-path traversal, null extract dir, null entry name, empty entry name).

### Fixed (Task 5 — zip-slip sinks)

All 8 open CodeQL `java/zipslip` High alerts on 8.1.x closed by routing archive entries through `ZipSlipGuard.safeDestFile(extractDir, entryName)` before any `mkdirs` / `FileOutputStream` / `Files.copy`, plus an analyzer-visible dominating canonical-path containment check at each sink (CodeQL does not load local model packs, so `ZipSlipGuard` alone is invisible to `java/zipslip`):

| Alert | Sink | Module |
|---|---|---|
| #501 | `PSArchiveFiles.java:352` | `system/` |
| #500 | `PSInstallRxApp.java:85` | `system/tools/` |
| #499 | `InstallRxApp.java:85` | `system/tools/` |
| #498 | `RxExtractJarFiles.java:75` | `system/release/Install/` |
| #497 | `PSWidgetPackageBuilder.java:125` | `projects/sitemanage/` |
| #496 | `Main.java:238` | `modules/perc-distribution-tree/` |
| #495 | `PSExtractJarFiles.java:73` | `modules/perc-ant/` |
| #494 | `MainDTSPreInstall.java:194` | `deliverytiersuite/.../delivery-tier-distribution/` |

### Notes

- Per-task fix pattern derived from 004 spec PR #1341 (`e22848f496`); uses the same canonical-path containment guarantee as `PathValidation.constructSafePath`.
- Scope rationale: `ZipSlipGuard` is intentionally a 70-line focused subset (canonical-path containment only). The full `PathValidation` (532 lines, with `combineSafePaths`, `validatePathWithinDirectory`, etc.) is brought forward as part of Task 6 path-injection residual work where it is needed at a larger scale.
- No Maven dependency change; `*.version` properties untouched per the Java 8 stack constraint.
- Per AGENTS.md, `Version.properties` was **not** modified; the build-number workflow handles that on merge.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Fixed

- **DTS `rotateKey` push failure logging** (#3) — `PSDeliveryClient` was logging a multi-KB Tomcat HTML error page and `PSDeliveryInfoService` only logged the admin base URL, leaving operators unable to tell that the real failing call was `PUT <admin>/feeds/rss/rotateKey` (HTTP 405). The ERROR line now includes method + full URL + HTTP status + a short reason (HTML body truncated to first line, max 200 chars) and the WARN text now points operators at the DTS feeds app, `deliverymanager` credentials, and `availableServices`. A new `PSDeliveryHttpErrorSupport` helper owns the truncation/formatting with a dedicated JUnit test, and `PSDeliveryClientException` now carries `statusCode` / `httpMethod` / `requestUrl` / `responseSnippet` so callers can render structured diagnostics without parsing the message.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Added (Task 3 — SSRF defense)

- **URLValidation helper** — `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLValidation.java` (and `URLValidationConfig`, `URLGlobMatcher`, `URLListFileLoader`) brought forward from the development branch. Rejects `file:` and other non-`http(s)` schemes, hard-blocks reserved / cloud metadata hosts (`169.254.169.254`, `metadata.google.internal`, etc.), allows loopback on any port, allows non-private public hosts on ports 80/443/-1, and supports an additive allow-list from `rxconfig/Server/allowedUrls.properties`. Backported to Java 8 by rewriting the three Java 11+ APIs (`String.isBlank`, `Path.of`, `InputStream.readAllBytes`) to Java 8 equivalents so the module still compiles under `java.source.version=1.8`.
- **URLValidation unit tests** — `URLValidationTest` and `URLGlobMatcherTest` in `modules/perc-security-utils/src/test/java/.../validation/` (JUnit 4 ports of the development-branch Jupiter suite; 20 tests cover baseline allow, hard/default blocks, allow-list enablement, and input validation).
- **DTD-URL SSRF guard** — `system/src/main/java/com/percussion/xml/PSDtdTree.java` now feeds the http/https URL branch through `URLValidation.validateURLString(...)` and rebuilds the URL with an explicit scheme literal before `openConnection()`. GHAS does not honor `// codeql[java/ssrf]` and does not model `URLValidation`, so the file is a documented `paths-ignore` residual in `.github/codeql/codeql-config.yml` (alert #432). Runtime validation is unchanged.
- **Feed metadata-service SSRF guard** — `deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java` runs the constructed metadata-service URL through `URLValidation.validateURLString(...)` and rebuilds the URI with an explicit scheme literal before `client.target(...)`. Validation failure now aborts with `FeedException` instead of falling through to `client.target` with the pre-validation URL. A `// codeql[java/ssrf]` comment on the line immediately above the sink closes alert #431.
- **PSDtdTree SSRF regression test** — `system/Testing/src/com/percussion/xml/PSDtdTreeSsrfTest.java` (4 cases) covers AWS metadata rejection, RFC1918 private-host rejection, `file://` scheme rejection, and the loopback baseline allow (negative-assertion form so the test is green regardless of whether the loopback port has a live listener).

### Notes

- Per-task fix pattern is derived from the 004 branch (PR #1300 `b8b1c96003` — same CodeQL `java/ssrf` rule, same rebuild-with-scheme-literal pattern; PR #1205/`#1302` `697dd655f0` — origin of `URLValidation` on the development branch).
- GHA rejects local `packs:` paths, so the in-repo `url-validation-ssrf` model pack is documentation only. Runtime `URLValidation` + scheme-literal rebuild remain the enforcement layer. GHAS also ignores `// codeql[java/ssrf]` (on or above the sink), so `PSDtdTree.java` is a playbook path-level residual in `codeql-config.yml` `paths-ignore`.
- CodeQL alerts #431 (`PSFeedService`) and #432 (`PSDtdTree`) are the only `java/ssrf` Critical+High alerts on `main` as of 2026-08-12.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Added (Task 2 — LDAP injection)

- **LDAP filter escape helper** — `system/src/main/java/com/percussion/security/PSJndiUtils.java#escapeLdapFilterValue(String)` package-private method that hex-escapes RFC 4515 reserved characters (`\`, `(`, `)`, NUL) and UTF-8 multi-byte sequences. The `*` wildcard is intentionally preserved so the existing `'%' → '*'` translation done by `processFilter` keeps working end-to-end.
- **Filter-string hardening** — `PSJndiUtils.getFilterString` now escapes each user-supplied filter value before composing it into the LDAP search filter, closing the injection vector where an attacker who can control a filter argument could break out of the parenthesised comparison and append arbitrary clauses (e.g. `*)(uid=*` turning `cn=foo` into `(|(cn=*)(uid=*))`).
- **LDAP injection regression test** — `system/Testing/src/com/percussion/security/PSJndiUtilsLdapInjectionTest.java` (9 JUnit 4 tests): reserved-char escape, wildcard preservation, ASCII passthrough, UTF-8 byte-by-byte escape, null/empty inputs, and the end-to-end `*)(uid=*` injection vector.

### Fixed (build)

- **`pom.xml` unparseable on `main`** — the `Comment out ai build plugin` change in `984b73ae70` placed a `</plugins></pluginManagement><plugins>` sequence *before* the `<!-- <plugin>...</plugin> -->` comment, then a second copy of the same sequence *after* the comment, leaving a stray `</pluginManagement>` whose matching `<pluginManagement>` had already been closed. Maven 3.9 and xmllint both rejected the file (`end tag name </pluginManagement> must match start tag name <build> from line 2072`) so `./mvn-env.sh validate` failed before any module compiled. The closing tags are now placed once, after the comment, restoring the original `pluginManagement → plugins → plugins` structure with the ai-build-integrity plugin still commented out. `./mvn-env.sh validate` → BUILD SUCCESS.

### Notes

- Per-task fix pattern is derived from the 004 branch (PR #1345 `e8489b698a` — same CodeQL `java/ldap-injection` rule, same fix family).
- CodeQL alert #595 (`system/src/main/java/com/percussion/security/PSJndiGroupProvider.java:200`) is the only `java/ldap-injection` Critical+High alert on `main` as of 2026-08-12.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Changed

- **jsoup 1.22.1 → 1.23.1** — Java 8–compatible HTML parser bump (Cleaner security fix + parser/perf improvements). Property `jsoup.version` in root `pom.xml`; `sitemanage` now uses `${jsoup.version}` instead of a hard-coded pin.
- **Tomcat 9.0.117 → 9.0.118** — DTS `tomcat.version` (catalina/coyote/jasper + distribution zip). Remains on the Tomcat 9 / Java 8 line.

### Fixed

- **Dependabot still proposed Java 11+/17 majors in grouped PRs (#22)** — full-ignore (all versions) for `org.springframework.security*`, `org.apache.shiro*`, and `org.apache.jackrabbit*` so security group PRs cannot reintroduce Spring Security 6.x (Java 17), Shiro 2.x (Java 11), or Jackrabbit 2.21+ (Java 11). Expanded group `exclude-patterns` accordingly. Closed PR #22 without merge.
- **Dependabot Java 8 ignore list not applied** — `.github/dependabot.yml` now applies the Java 8 compatibility ignore policy across all multi-module Maven manifests (`directories: ["/", "**/*"]`), removes an invalid version range (`>=*jre11*`) that could break ignore parsing, adds missing caps (e.g. Hibernate 6), and defines `java8-safe-patches` / `java8-security-patches` groups with exclude-patterns so grouped PRs cannot re-bundle Spring 6 / CXF 4 / Tika 3 / etc. with legitimate patches.
- **Dependency Submission workflow YAML syntax error** (#14) — `.github/workflows/dependency-submission.yml` failed immediately with a workflow file issue (Actions UI: syntax error on line 17) because the job `if:` condition was an unquoted plain scalar containing `chore: update build number`. YAML treats `: ` as mapping syntax, so no jobs started. The condition is now a block scalar (`if: |`) so the colon is literal text and Dependency Submission can run on `main` / `workflow_dispatch`.
- **Rich Text widget: image selected from Assets not inserted** (#11) — `modules/perc-tinymce/.../plugins/percadvimage/plugin.js` called `String.prototype.replace` on `pathItem.PathItem.folderPaths` returned by `PercPathService.getPathItemById`. The REST service now returns `folderPaths` as an array, so the call threw `TypeError: pathItem.PathItem.folderPaths.replace is not a function`, aborting `getImageData` before the inline image element was populated with `sys_dependentvariantid`/`sys_dependentid`. The fix normalizes `folderPaths` to a string (handling both string and array responses) so the inline link element is written correctly and the server-side `PSInlineLinkField` no longer reports `Missing required inline link parameter: sys_dependentvariantid`. Also removed a stray `console.log("Hello")` debug statement in the same `updateFileSelection` command handler.
- **Editor / preview broken on responsive themes (8.1.7 `perc_decoration.css` !important)** (#2) — `WebUI/war/css/perc_decoration.css` no longer forces `120/240/360/480px` vspan heights with `!important` / `min-height: 0 !important`, and no longer declares `.hspan_*` column widths at all. Decoration loads before the site theme in the editor/preview header (see `PSHTMLHeaderImporterTest` link ordering), so column widths are owned by the theme (default `160/640/800/960px`; responsive `auto`) and vspan sizing is left to the theme's own `min-height` floors (GH-757 still intact in `system/.../default_theme/theme.css`). Customer themes that set `height/width: auto` on spans (e.g. `cccc-theme.css`) now render correctly in CMS chrome. Regression covered by new `VspanFooterAlignmentCssTest` (asserts decoration has no `!important` fixed vspan grid, no `min-height: 0`, and no `.hspan_*` declarations; theme.css still owns published `min-height` floors for #757).

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-10

### Added (Task 6 — path-injection sitemanage)

- **Shared path guards** — `modules/perc-security-utils/src/main/java/com/percussion/security/io/PSPathInjectionGuard.java` with `requireSafeFileName`, `requireUnderBase`, `requireUnderBasePath`, `containsForbiddenCharacters` (CodeQL `java/path-injection` barriers, backported from the 004 spec).
- **Path-guard regression test** — `modules/perc-security-utils/src/test/java/com/percussion/security/io/PSPathInjectionGuardTest.java` (23 JUnit 4 tests covering accept/reject paths, traversal, NUL bytes, base containment).
- **PSThemeService input guards** — `requireSafeFileName(themeName)` at `getNewThemeFolder`, `getThemeFolder`, `getThumbUrl`, `create`, `createFromDefault`; new `safeSessionSegment(String)` static helper to sanitize user session IDs into path-safe segments (used by `clearCacheRegionCSS`).
- **PSRegionCSSFileService internal validator** — new `requireSafeFilePath(String)` helper that rejects NUL bytes and embedded `..` traversal segments while allowing multi-segment paths. Applied at every public method that takes a `filePath`.
- **PSFileSystemService base containment** — `getFile(String path)` and `getChildren(String path)` now use `requireUnderBase(rootDirectory, path)` to verify the resolved path is contained within the per-service root.
- **PSSiteConfigUtils site-name guards** — `requireSafeFileName(sitename)` at every public method that takes a `sitename` (the value that flows into `new File(getSitesConfigPath(), sitename)` and similar).

### Removed (admin)

- **`ai-build-integrity-maven-plugin`** — removed from `pom.xml` `<pluginManagement>` and `<plugins>`, plus the `ai-build-integrity.plugin.version` property. The plugin was hashing AI-generated docs files and rejecting every `scripts/fetch-gh-code-scanning-alerts.py` re-run as "tampered". Its purpose (AI resource audit) is redundant with the CodeQL analyzer-of-record workflow we are bringing forward in Task 1.

### Notes

- Per-task fix patterns are derived from the 004 branch (PR #1362 multi-pass residual batch — `1c2fed9418`).
- 23 alerts in `projects/sitemanage/` addressed by structural guards above; 25 alerts remain in 11 files (PSFileSystemPathItemService, PSAssetService, PSCloudService, PSRenderLinkService, PSWebResourcesRestService, PSImportThemeHelper, PSCSSParser, AssetAdaptor, PSSiteDataService, etc.) and will land in a follow-up PR. The 12 alerts in `system/` are tracked for the Task 6 system-side split.
- The current PR is intentionally reviewable-sized; the full Task 6 cluster (60 alerts) is split per the plan.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-09

### Added (Task 1)

- **CodeQL analyzer of record** — brought forward `.github/workflows/codeql.yml` from `development` and adapted for the 8.1.x release line (`main` branch trigger, `intersoftdatalabs-in/percussioncms-java8` repo).
- **Custom sanitizer barrier models** — `.github/codeql/models/` with 7 model packs (`sql-object-name`, `path-injection-guard`, `ldap-escape`, `url-validation-ssrf`, `redirxect-validation`, `secure-xml-xxe`, `xss-escape`). Per the dev branch's note, GHA rejects local pack paths in the `packs:` input, so the runtime sanitizers + sink-line `// codeql[...]` comments are the enforcement layer; the model packs are the audit trail.
- **Shared SQL guards** — added `SecureStringUtils.requireSqlObjectName`, `requireSqlObjectNameOrNull`, `requireSafeMetadataToken`, `requireSingleSqlStatement`, `requireFactorySqlStatement` (CodeQL `java/sql-injection` barriers, backported from PR #1343 with pattern fields `SQL_OBJECT_NAME` and `SAFE_METADATA_TOKEN`).
- **Junit 4 regression test** — `modules/perc-security-utils/src/test/java/com/percussion/security/SecureStringUtilsSqlInjectionTest` (7 tests covering accept/reject paths for each new method).
- **Empty triage scaffolding** — `triage.md`, `suppressions.md`, `accepted-risks.md`, `codeql-pr-playbook.md`, `README.md` under `docs/ai-generated/tasks/8.1.x-codeql-baseline/`.
- **Harness self-test** — `scripts/test_verify_suppressions.py` proves the suppression verifier can detect a missing `// codeql[...]` anchor.

### Fixed (Tasks 0+1)

- `scripts/verify-suppressions.py` — off-by-one parsing for the suppressions schema (data rows do not have a leading `#` column; the script was reading cells[1] as alert_id but the schema starts at cells[0]).
- `scripts/verify-suppressions.py` — calls `filter-stale-alerts.py` (hyphen) by the correct filename, matching the file on disk.
- `scripts/gh-preflight.py` — accepts SSH origin URLs (containing `:<repo>.git`) in addition to `https://github.com/<repo>`.

### Notes

- CodeQL Advanced only scans Java + JavaScript/TypeScript (no Actions/C#/Python/etc.).
- Default Setup and Code Quality remain disabled per development branch's analyzer-of-record policy.
- Spotless check passes on the modified module; full PMD/verify run is required for the first family PR per the plan.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-09

### Added

- Ported cross-platform Python verification scripts from `development` for the 8.1.x CodeQL Critical+High mitigation track: `scripts/fetch-gh-code-scanning-alerts.py`, `scripts/filter-stale-alerts.py`, `scripts/gh-preflight.py`, `scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`, `scripts/verify-suppressions.py`, `scripts/verify-pr-review-resolution.py`, `scripts/verify-distribution-archive.py`, plus `scripts/generate-clusters.py` (8.1.x-local cluster summary) and `scripts/requirements-dev.txt`. Defaults target `intersoftdatalabs-in/percussioncms-java8` and `docs/ai-generated/tasks/8.1.x-codeql-baseline/`.
- Initial baseline of open CodeQL alerts on `main` (2026-08-09): 610 open, 429 Critical+High across 30 rule clusters. Cluster map written to `docs/ai-generated/tasks/8.1.x-codeql-baseline/clusters.md`; raw API dump to `docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md`; stale-cache filter returns 0 (all paths present in `git ls-files`).

## [8.1.7 Build 947] - 2026-07-03

### Fixed

- Fixed the "Category Name" placeholder text overlapping user input when adding a new category in Admin → Categories. The default title "New Category" was being written into the input's actual `value` in `displayCategoryDetails()` (`PercCategoryView.js`), with a fragile `.select()` call relied on to let the first keystroke overwrite it; any click or focus timing issue broke the selection and left the literal text behind. Added a real `placeholder` attribute (new `perc.ui.perc.categories@New Category` i18n key) to `#perc-category-name-field` in `percCategories.jsp`, and updated `displayCategoryDetails()` to leave the field genuinely empty for new, unsaved categories so the browser natively shows/hides the placeholder as the user types.
- Fixed a `TypeError: Cannot read properties of null (reading 'setTitle')` thrown from the category name field's `keyup` handler after dismissing the "You must change the category name." validation alert in `PercCategoryView.js`. The alert reused the shared `alertDialog()` helper, whose `okCallBack` unconditionally rebuilt the category tree via `controller.getCategories()`; since the user was still mid-edit on the unsaved node, the rebuild left no active node (blocked by `beforeActivate` while `editing` was still true) while the name field stayed enabled, so the next keystroke crashed. Added a `validationAlert()` helper that shows the same message without reloading the tree, and used it for this check so the in-progress edit/active node is preserved.

## [8.1.7 Build 928] - 2026-06-25

### Fixed

- Fixed PSPackageBuilderTest#testAllPackagesMatchReferenceStructure test failure by syncing reference .ppkg files with source content type labels (#1153). Updated perc.widget.registration and perc.widget.secureLogin reference packages to mark content types as "(Deprecated)".

## [8.1.7 Build 929] - 2026-06-26

### Fixed

- Fixed misleading "Path not found" error dialog that appeared immediately after a folder create or rename (#867). After rename, `update_btn` in `perc_delete_page_button.js` would call `open_path` on the new path while the JCR was still indexing it; the existing 3x200ms client retry was insufficient and the error handler showed a false-positive alert. Increased the client retry to 6x300ms in `perc_path_manager.js` and made `update_btn` silently disable the delete button on lookup failure (the path was just navigated to, so it must exist) instead of showing the error dialog.

## [8.1.7 Build 922] - 2026-06-25

### Fixed

- Fixed duplicate validation error displays and incorrect error status responses when converting a folder to a section (#866). Updated `PSValidationExceptionMapper`, `PSBeanValidationExceptionMapper`, and `PSSpringValidationExceptionMapper` to map validation errors to standard `400 BAD_REQUEST` instead of `500 INTERNAL_SERVER_ERROR`. Fixed `PercServiceUtils.js` to use an `else if` for `globalError` parsing to prevent duplicate extraction of the same error message when both `globalErrors` and `globalError` are populated in the response.

## [8.1.7 Build 921] - 2026-06-25

### Fixed

- Fixed a folder creation race condition and exception mapping issue where folder creation would display a "Path not found" error popup even though folder creation succeeded in the database (#867). Registered `PSPathServiceExceptionMapper` as a provider under CXF, mapped path service exceptions implementing `IPSNotFoundException` to a `404 NOT_FOUND` status code, and added a short retry loop with a 100ms sleep when lookup of the newly created folder path fails.

## [8.1.7 Build 920] - 2026-06-24

### Fixed

- Fixed footer widget appearing in wrong location in the CMS page editor (#757). The `vspan_X` region height rules in `perc_decoration.css` now use `!important` to assert fixed heights in the editor iframe, preventing `theme.css` `min-height` values from allowing sidebar regions to overflow and displace the footer region.

## [8.1.7 Build 919] - 2026-06-24

### Fixed

- Fixed server startup crash (StringIndexOutOfBoundsException) by restoring date format to `buildNumber` and mapping the sequential run number to `buildId`. Retained defensive robustness checks in `PSFormatVersion` and `PSLogHandler` to prevent future build version format crashes.

## [8.1.7 Build 917] - 2026-06-24

### Added

- Added GitHub Actions workflow to automatically update and commit the build number in `Version.properties` files and `CHANGELOG.md` upon pushes to `development-8.1.x`.

### Changed

- Updated `Version.properties` buildNumber to use `917` placeholder and updated `AGENTS.md` guidelines to use the automated workflow instead of manual increments.

### Fixed

- Fixed a JavaScript TypeError ("Cannot read properties of null (reading 'scrollHeight')") on the Admin Console page by ensuring DOM elements exist before referencing them (#906).

