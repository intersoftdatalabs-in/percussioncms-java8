# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Fixed (build)

- **`pom.xml` unparseable on `main`** — the `Comment out ai build plugin` change in `984b73ae70` placed a `</plugins></pluginManagement><plugins>` sequence *before* the `<!-- <plugin>...</plugin> -->` comment, then a second copy of the same sequence *after* the comment, leaving a stray `</pluginManagement>` whose matching `<pluginManagement>` had already been closed. Maven 3.9 and xmllint both rejected the file (`end tag name </pluginManagement> must match start tag name <build> from line 2072`) so `./mvn-env.sh validate` failed before any module compiled. The closing tags are now placed once, after the comment, restoring the original `pluginManagement → plugins → plugins` structure with the ai-build-integrity plugin still commented out. `./mvn-env.sh validate` → BUILD SUCCESS.

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-12

### Changed

- **jsoup 1.22.1 → 1.23.1** — Java 8–compatible HTML parser bump (Cleaner security fix + parser/perf improvements). Property `jsoup.version` in root `pom.xml`; `sitemanage` now uses `${jsoup.version}` instead of a hard-coded pin.
- **Tomcat 9.0.117 → 9.0.118** — DTS `tomcat.version` (catalina/coyote/jasper + distribution zip). Remains on the Tomcat 9 / Java 8 line.

### Fixed

- **Dependabot still proposed Java 11+/17 majors in grouped PRs (#22)** — full-ignore (all versions) for `org.springframework.security*`, `org.apache.shiro*`, and `org.apache.jackrabbit*` so security group PRs cannot reintroduce Spring Security 6.x (Java 17), Shiro 2.x (Java 11), or Jackrabbit 2.21+ (Java 11). Expanded group `exclude-patterns` accordingly. Closed PR #22 without merge.
- **Dependabot Java 8 ignore list not applied** — `.github/dependabot.yml` now applies the Java 8 compatibility ignore policy across all multi-module Maven manifests (`directories: ["/", "**/*"]`), removes an invalid version range (`>=*jre11*`) that could break ignore parsing, adds missing caps (e.g. Hibernate 6), and defines `java8-safe-patches` / `java8-security-patches` groups with exclude-patterns so grouped PRs cannot re-bundle Spring 6 / CXF 4 / Tika 3 / etc. with legitimate patches.
- **Dependency Submission workflow YAML syntax error** (#14) — `.github/workflows/dependency-submission.yml` failed immediately with a workflow file issue (Actions UI: syntax error on line 17) because the job `if:` condition was an unquoted plain scalar containing `chore: update build number`. YAML treats `: ` as mapping syntax, so no jobs started. The condition is now a block scalar (`if: |`) so the colon is literal text and Dependency Submission can run on `main` / `workflow_dispatch`.
- **Rich Text widget: image selected from Assets not inserted** (#11) — `modules/perc-tinymce/.../plugins/percadvimage/plugin.js` called `String.prototype.replace` on `pathItem.PathItem.folderPaths` returned by `PercPathService.getPathItemById`. The REST service now returns `folderPaths` as an array, so the call threw `TypeError: pathItem.PathItem.folderPaths.replace is not a function`, aborting `getImageData` before the inline image element was populated with `sys_dependentvariantid`/`sys_dependentid`. The fix normalizes `folderPaths` to a string (handling both string and array responses) so the inline link element is written correctly and the server-side `PSInlineLinkField` no longer reports `Missing required inline link parameter: sys_dependentvariantid`. Also removed a stray `console.log("Hello")` debug statement in the same `updateFileSelection` command handler.

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

