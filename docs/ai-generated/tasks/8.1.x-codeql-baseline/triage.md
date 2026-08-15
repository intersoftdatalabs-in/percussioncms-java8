# CodeQL Triage Inventory — 8.1.x (main)

Every open Critical/High alert gets exactly one row here. The pipeline gates (`scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`) read this file.

Generated: 2026-08-15T21:14:20Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 25
- false-positive: 9
- fix: 16

Schema (per spec 004 C1):

| # | alert_id | rule_id | severity | file_path | module_owner | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
|---|----------|---------|----------|-----------|--------------|-------------------------|---------------|------------------|-----------|-------|
| 1 | 706 | js/incomplete-sanitization | high | cui/widgets/app/app.viewmodel.js:88 | cui | fix | code fix required | TBD |  |  |
| 2 | 705 | js/incomplete-sanitization | high | cui/widgets/app/app.viewmodel.js:88 | cui | fix | code fix required | TBD |  |  |
| 3 | 704 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:216 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java (#justification: runtime path-injection guard at or above sink; GHAS does not model in-repo guard; path-ignore residual) @ line 77 |
| 4 | 477 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:519 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java (#justification: runtime path-injection guard at or above sink; GHAS does not model in-repo guard; path-ignore residual) @ line 77 |
| 5 | 476 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:518 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java (#justification: runtime path-injection guard at or above sink; GHAS does not model in-repo guard; path-ignore residual) @ line 77 |
| 6 | 474 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:403 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java (#justification: runtime path-injection guard at or above sink; GHAS does not model in-repo guard; path-ignore residual) @ line 77 |
| 7 | 467 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:182 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java (#justification: runtime path-injection guard at or above sink; GHAS does not model in-repo guard; path-ignore residual) @ line 77 |
| 8 | 457 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:451 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java (#see config) @ line 78 |
| 9 | 456 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:419 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java (#see config) @ line 78 |
| 10 | 455 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:364 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java (#see config) @ line 78 |
| 11 | 454 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:257 | projects/sitemanage | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java (#see config) @ line 79 |
| 12 | 143 | js/xss-through-dom | high | delivery/common/js/views/PercTagListView.js:115 | delivery | fix | code fix required | TBD |  |  |
| 13 | 142 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:144 | delivery | fix | code fix required | TBD |  |  |
| 14 | 141 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:140 | delivery | fix | code fix required | TBD |  |  |
| 15 | 140 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:64 | delivery | fix | code fix required | TBD |  |  |
| 16 | 139 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:260 | delivery | fix | code fix required | TBD |  |  |
| 17 | 138 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:72 | delivery | fix | code fix required | TBD |  |  |
| 18 | 137 | js/xss-through-dom | high | delivery/common/js/views/PercMostReadBlogPostsView.js:105 | delivery | fix | code fix required | TBD |  |  |
| 19 | 136 | js/xss-through-dom | high | delivery/common/js/views/PercMostReadBlogPostsView.js:70 | delivery | fix | code fix required | TBD |  |  |
| 20 | 135 | js/xss-through-dom | high | delivery/common/js/views/PercCategoryListView.js:192 | delivery | fix | code fix required | TBD |  |  |
| 21 | 134 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:279 | delivery | fix | code fix required | TBD |  |  |
| 22 | 133 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:153 | delivery | fix | code fix required | TBD |  |  |
| 23 | 132 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:191 | delivery | fix | code fix required | TBD |  |  |
| 24 | 131 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:144 | delivery | fix | code fix required | TBD |  |  |
| 25 | 130 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:131 | delivery | fix | code fix required | TBD |  |  |
