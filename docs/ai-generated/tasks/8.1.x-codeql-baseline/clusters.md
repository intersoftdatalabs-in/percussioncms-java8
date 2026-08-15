# CodeQL Critical+High Clusters on 8.1.x (main)

Generated: 2026-08-15T21:10:13Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 75
- Critical+High open: 65
- Cluster count: 5

## Cluster Map

| Rule | Severity | Count | Reference 004 PR | Notes |
|---|---|---|---|---|
| `js/xss-through-dom` | high | 45 | TBD | |
| `java/path-injection` | high | 9 | PR #1365 + #1362 + #1361 | 8.1.x: this PR |
| `js/incomplete-sanitization` | high | 8 | TBD | 8.1.x: PR #57 (6 path-ignore + 2 code fix) |
| `java/xss` | high | 2 | PR #1348 + #1344 + #1367 | 8.1.x: PR #55 (Tomcat sample) |
| `js/clear-text-logging` | high | 1 | TBD | 8.1.x: PR #56 |

## Per-Cluster Detail

### `js/xss-through-dom` (45 alerts)

- Alert #181 — `system/UnitTestResources/com/percussion/delivery/bw-corona.html:1310`
- Alert #180 — `system/Packages/perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js:231`
- Alert #179 — `system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1610`
- Alert #178 — `system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1606`
- Alert #177 — `system/Packages/perc.widgets.image/sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js:396`
- ... and 40 more

### `java/path-injection` (9 alerts)

- Alert #704 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:216`
- Alert #477 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:519`
- Alert #476 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:518`
- Alert #474 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:403`
- Alert #467 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:182`
- ... and 4 more

### `js/incomplete-sanitization` (8 alerts)

- Alert #246 — `system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57`
- Alert #245 — `system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57`
- Alert #244 — `system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57`
- Alert #243 — `system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49`
- Alert #242 — `system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49`
- ... and 3 more

### `java/xss` (2 alerts)

- Alert #564 — `system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:78`
- Alert #563 — `system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:77`

### `js/clear-text-logging` (1 alerts)

- Alert #334 — `modules/perc-qa-automation/frontend/tests/login.spec.js:33`
