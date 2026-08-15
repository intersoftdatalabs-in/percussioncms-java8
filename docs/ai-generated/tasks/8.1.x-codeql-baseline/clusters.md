# CodeQL Critical+High Clusters on 8.1.x (main)

Generated: 2026-08-15T21:14:20Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 25
- Critical+High open: 25
- Cluster count: 3

## Cluster Map

| Rule | Severity | Count | Reference 004 PR | Notes |
|---|---|---|---|---|
| `js/xss-through-dom` | high | 14 | TBD | 8.1.x: post-merge residual |
| `java/path-injection` | high | 9 | PR #1365 + #1362 + #1361 | 8.1.x: post-merge residual |
| `js/incomplete-sanitization` | high | 2 | TBD | 8.1.x: post-merge residual |

## Per-Cluster Detail

### `js/xss-through-dom` (14 alerts)

- Alert #143 — `delivery/common/js/views/PercTagListView.js:115`
- Alert #142 — `delivery/common/js/views/PercRssView.js:144`
- Alert #141 — `delivery/common/js/views/PercRssView.js:140`
- Alert #140 — `delivery/common/js/views/PercRssView.js:64`
- Alert #139 — `delivery/common/js/views/PercRegistrationView.js:260`
- ... and 9 more

### `java/path-injection` (9 alerts)

- Alert #704 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:216`
- Alert #477 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:519`
- Alert #476 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:518`
- Alert #474 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:403`
- Alert #467 — `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:182`
- ... and 4 more

### `js/incomplete-sanitization` (2 alerts)

- Alert #706 — `cui/widgets/app/app.viewmodel.js:88`
- Alert #705 — `cui/widgets/app/app.viewmodel.js:88`
