# CodeQL Triage Inventory — 8.1.x (main)

Every open Critical/High alert gets exactly one row here. The pipeline gates (`scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`) read this file.

Generated: 2026-08-18T00:08:10Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 9
- fix: 9

Schema (per spec 004 C1):

| # | alert_id | rule_id | severity | file_path | module_owner | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
|---|----------|---------|----------|-----------|--------------|-------------------------|---------------|------------------|-----------|-------|
| 1 | 718 | js/xss-through-dom | high | delivery/common/js/views/PercTagListView.js:123 | delivery | fix | code fix required | TBD |  |  |
| 2 | 717 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:162 | delivery | fix | code fix required | TBD |  |  |
| 3 | 716 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:152 | delivery | fix | code fix required | TBD |  |  |
| 4 | 714 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:272 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercRegistrationView.js (linked_pr=this PR); path-ignore not yet applied |
| 5 | 713 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:83 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercRegistrationView.js (linked_pr=this PR); path-ignore not yet applied |
| 6 | 712 | js/xss-through-dom | high | delivery/common/js/views/PercCategoryListView.js:199 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercCategoryListView.js (linked_pr=this PR); path-ignore not yet applied |
| 7 | 711 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:288 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercArchiveListView.js (linked_pr=this PR); path-ignore not yet applied |
| 8 | 710 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:199 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercArchiveListView.js (linked_pr=this PR); path-ignore not yet applied |
| 9 | 709 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:138 | delivery | fix | code fix required | TBD |  | suppressions.md lists this alert under delivery/common/js/views/PercArchiveListView.js (linked_pr=this PR); path-ignore not yet applied |
