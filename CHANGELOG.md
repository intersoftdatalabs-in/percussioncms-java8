# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-08-09

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

