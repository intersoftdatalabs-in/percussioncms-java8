# CodeQL Critical+High Clusters on 8.1.x (main)

Generated: 2026-08-18T00:08:10Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 9
- Critical+High open: 9
- Cluster count: 1

## Cluster Map

| Rule | Severity | Count | Reference 004 PR | Notes |
|---|---|---|---|---|
| `js/xss-through-dom` | high | 9 | this PR | 8.1.x: code fix in this PR; inlined /^\s*(?:javascript|vbscript|data)\s*:/i regex test at every flag-free href/location sink; sink-line // codeql[rule-id] markers removed; suppressions.md rows for #709-#714 and #716-#718 dropped (the 3 closed-by-#62 rows for #707/#708/#715 also dropped as stale). Awaits next GHAS scan to dismiss. |

## Per-Cluster Detail

### `js/xss-through-dom` (9 alerts)

- Alert #718 — `delivery/common/js/views/PercTagListView.js:121`
- Alert #717 — `delivery/common/js/views/PercBlogPostView.js:160`
- Alert #716 — `delivery/common/js/views/PercBlogPostView.js:150`
- Alert #714 — `delivery/common/js/views/PercRegistrationView.js:266`
- Alert #713 — `delivery/common/js/views/PercRegistrationView.js:78`
- Alert #712 — `delivery/common/js/views/PercCategoryListView.js:195`
- Alert #711 — `delivery/common/js/views/PercArchiveListView.js:283`
- Alert #710 — `delivery/common/js/views/PercArchiveListView.js:194`
- Alert #709 — `delivery/common/js/views/PercArchiveListView.js:133`
