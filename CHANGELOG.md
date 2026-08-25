# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [Unreleased]

### Fixed

- Fixed Linux SysV / init.d `stop` for Percussion CMS leaving the Jetty JVM running (#6). `rxjetty.sh` had two bugs in the stop path: a UID check (`$UID -eq 2` was almost always false, so the `start-stop-daemon` branch was dead code) and the use of `SIGHUP` (which Java does not handle as a stop signal) instead of `SIGTERM`. The stop path now resolves the actual Jetty PID via a new `find_jetty_pid` helper that tolerates stale or missing PID files by scanning `/proc/*/cmdline` for a Java process matching `JETTY_BASE`, signals it `TERM` and escalates to `KILL` after `STOP_TIMEOUT` (default 60s), and re-verifies the JVM is gone before exiting 0. Stops are now idempotent: a second `service <name> stop` against a stopped service is a no-op rather than a failure.
- Fixed init.d service starting before MySQL/MariaDB on reboot (#6). The shipped LSB header only declared `$local_fs $network`, and the chkconfig priority was `20 80` (start very early). The header now declares `Required-Start: $local_fs $network $remote_fs` and `Should-Start: $named mysqld mysql mariadb`, and the chkconfig line is raised to `2345 99 01` so the CMS starts as one of the last services on boot. `install-jetty-service.sh` now also registers `update-rc.d` with explicit late priorities `start 99 2 3 4 5 . stop 01 0 1 6 .` (and uses `K01` on the Solaris fallback) so SysV systems without chkconfig get the same ordering.
- Fixed missing wait-for-database logic for installs with a remote DB (e.g. AWS RDS) that have no local `mysqld` init script for the CMS to order against. `rxjetty.sh` now exposes `WAIT_FOR_DB_HOST` / `WAIT_FOR_DB_PORT` / `WAIT_FOR_DB_TIMEOUT` / `WAIT_FOR_DB_INTERVAL` env vars (documented in the script header). When `WAIT_FOR_DB_HOST` is set, the start path probes `host:port` with `nc -z` (bash `/dev/tcp` fallback) until the timeout elapses before launching Jetty. A non-fatal warning is logged if the wait times out so a CMS without a reachable DB still comes up for diagnosis. `install-jetty-service.sh` now ships these vars (empty by default) in the `/etc/default/<service>` template and prints a hint at the end of install about configuring them for remote DBs.

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

