# CodeQL Triage Inventory — 8.1.x (main)

Every open Critical/High alert gets exactly one row here. The pipeline gates (`scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`) read this file.

Generated: 2026-08-18T00:08:10Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 9
- fix: 9

Schema (per spec 004 C1):

| # | alert_id |      rule_id       | severity |                      file_path                       | module_owner | disposition (candidate) |   target_action   | target_milestone | linked_pr |                                                                                                  notes                                                                                                   |
|---|----------|--------------------|----------|------------------------------------------------------|--------------|-------------------------|-------------------|------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | 718      | js/xss-through-dom | high     | delivery/common/js/views/PercTagListView.js:121      | delivery     | fix                     | code fix required | TBD              |           | code fix landed: replaced in-repo percSafeUrl() wrapper with an inline /^\s*(?:javascript                                                                                                                | vbscript | data)\s*:/i regex test immediately before the .attr("href", href) sink; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert. |
| 2 | 717      | js/xss-through-dom | high     | delivery/common/js/views/PercBlogPostView.js:160     | delivery     | fix                     | code fix required | TBD              |           | code fix landed: same inline regex pattern as #718 on the category href; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                                                        |
| 3 | 716      | js/xss-through-dom | high     | delivery/common/js/views/PercBlogPostView.js:150     | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the tag href; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                                |
| 4 | 714      | js/xss-through-dom | high     | delivery/common/js/views/PercRegistrationView.js:266 | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the confirmation-page window.location value; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert. |
| 5 | 713      | js/xss-through-dom | high     | delivery/common/js/views/PercRegistrationView.js:78  | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the redirect window.location.href value; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.     |
| 6 | 712      | js/xss-through-dom | high     | delivery/common/js/views/PercCategoryListView.js:195 | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test in the href expression passed to $("<a>").attr("href", ...); suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                                 |
| 7 | 711      | js/xss-through-dom | high     | delivery/common/js/views/PercArchiveListView.js:283  | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the flat-list month href; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                    |
| 8 | 710      | js/xss-through-dom | high     | delivery/common/js/views/PercArchiveListView.js:194  | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the hierarchical month href; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                 |
| 9 | 709      | js/xss-through-dom | high     | delivery/common/js/views/PercArchiveListView.js:133  | delivery     | fix                     | code fix required | TBD              |           | code fix landed: inline regex test on the hierarchical year href; sink-line // codeql[rule-id] marker removed; suppressions.md row dropped. Awaits next GHAS scan to dismiss the alert.                  |

