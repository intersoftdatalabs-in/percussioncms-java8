# CodeQL PR Playbook — 8.1.x

Operational guide for closing CodeQL (Critical/High) alerts on the 8.1.x release line.

## Pipeline

1. **Task 0** — `scripts/fetch-gh-code-scanning-alerts.py` produces
   `docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md` (open alerts).
2. **Task 0** — `scripts/filter-stale-alerts.py` removes alerts whose source
   file is no longer in `git ls-files` (CodeQL cache lag).
3. **Task 0** — `scripts/generate-clusters.py` writes
   `docs/ai-generated/tasks/8.1.x-codeql-baseline/clusters.md` (Critical/High
   open, grouped by `rule.id`).
4. **Task 1** — `SecureStringUtils` SQL barriers + model packs
   land at `.github/codeql/models/`. Without the model packs the scanner
   keeps re-flagging the new sinks.
5. **Tasks 2-8** — one PR per CodeQL rule family. Each PR:
   - Branches from `main` as `codeql/<rule-id>-cluster`.
   - Applies the fix pattern from the 004 branch (`remotes/origin/development`).
   - Adds a JUnit 4 regression test that fails on the pre-fix code.
   - Adds a sink-line `// codeql[rule-id]` suppression only when the sink
     cannot be hardened, formatted as `// codeql[rule-id] justification: <one-line>; re-review by 2027-07-31`.
   - Updates `clusters.md` row with the PR number.
6. **Task 9** — first-party WebUI JS residuals; vendor removals only after
   `scripts/verify-distribution-archive.sh` confirms the file is not in the
   built distribution tree.
7. **Task 10** — `accepted-risks.md` with re-review dates for unfixable
   sinks.
8. **Task 12** — `CHANGELOG.md` entries per family with the
   `GH_POST_PR_COMMIT_RUN_ID` placeholder.
9. **Task 13** — sign-off PR re-runs the pipeline and asserts zero
   Critical/High open.

## Verification scripts

|                   Script                   |                      Purpose                      |
|--------------------------------------------|---------------------------------------------------|
| `scripts/fetch-gh-code-scanning-alerts.py` | CodeQL alerts → `alerts.md`                       |
| `scripts/filter-stale-alerts.py`           | Drop deleted-file rows                            |
| `scripts/generate-clusters.py`             | Critical/High → `clusters.md`                     |
| `scripts/verify-triage-inventory.sh`       | Requires `triage.md` (Task 13)                    |
| `scripts/verify-valid-fixes.sh`            | Every valid row has `linked_pr`                   |
| `scripts/verify-suppressions.py`           | Every row in `suppressions.md` is greppable       |
| `scripts/verify-pr-review-resolution.py`   | Every closing PR has zero unresolved threads      |
| `scripts/verify-distribution-archive.py`   | Vendor removals don't appear in distribution JARs |

## Model pack files (`.github/codeql/models/`)

Each `*.model.yml` declares a sanitizer barrier for the corresponding
CodeQL rule family. Packs remain in-repo for documentation; GHA rejects
local pack paths in the `packs:` input, so the runtime sanitizers +
sink-line `// codeql[...]` comments are the enforcement layer.

|              Model               |           Rule family           |
|----------------------------------|---------------------------------|
| `sql-object-name.model.yml`      | `java/sql-injection`            |
| `path-injection-guard.model.yml` | `java/path-injection`           |
| `ldap-escape.model.yml`          | `java/ldap-injection`           |
| `url-validation-ssrf.model.yml`  | `java/ssrf`                     |
| `xss-escape.model.yml`           | `java/xss`                      |
| `secure-xml-xxe.model.yml`       | `java/xxe`                      |
| `redirect-validation.model.yml`  | `java/unvalidated-url-redirect` |

## Constraints

- **Java 8** — every change must compile under `java.source.version=1.8`.
- **No `var`, no records, no `List.of`** — JDK 1.8.0 toolchain per `AGENTS.md`.
- **No silent removal** — any deleted file must be verified by
  `verify-distribution-archive.py` to not appear in the rebuilt
  `modules/perc-distribution-tree` or `modules/perc-packages` JARs/`.ppkg`.
- **No `owasp-suppressions.xml` additions** for dependency CVEs.
- **Backports are reviewable** — each PR < 800 LOC of net source change where
  possible. Path-injection (Task 6) may exceed; split by module if so.
- **Branch convention** — `codeql/<short-desc>` (no umbrella issue per
  user direction; AGENTS.md `security/<issue#>-<short-desc>` lifted).

