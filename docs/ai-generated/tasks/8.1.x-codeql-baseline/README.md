# 8.1.x CodeQL Baseline

This directory holds the working state for the 8.1.x CodeQL Critical+High
mitigation track. The source of truth is the GitHub code-scanning dashboard
for `intersoftdatalabs-in/percussioncms-java8` (the GH issues UI is intentionally
not used — per user direction, the GH scan alerts themselves are the tracker).

## Files

|          File           |                               Purpose                                |
|-------------------------|----------------------------------------------------------------------|
| `alerts.md`             | Raw open-alert dump (610 rows, 2026-08-09)                           |
| `alerts-stale-cache.md` | Alerts whose source file is no longer in `git ls-files` (0 rows)     |
| `clusters.md`           | Critical/High open grouped by `rule.id` (429 rows, 30 clusters)      |
| `triage.md`             | One row per open Critical/High alert (empty; populated by tasks 2-8) |
| `suppressions.md`       | Inline `// codeql[...]` and path-level exclusion index               |
| `accepted-risks.md`     | Sinks that cannot be closed without legacy removal or Java 9+        |
| `codeql-pr-playbook.md` | Per-task workflow guide                                              |

## Pipeline

See `codeql-pr-playbook.md` for the full task order. Task 0 (this commit) built
the scripts and the initial baseline. Tasks 1-13 follow.
