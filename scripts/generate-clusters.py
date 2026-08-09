#!/usr/bin/env python3
"""Generate clusters.md from alerts.md (critical+high only, grouped by rule.id)."""
from __future__ import annotations

import re
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
ALERTS = REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md"
CLUSTERS = REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/clusters.md"

# Reference 004 PR mapping (from .kilo/plans/1786311971129-codeql-critical-high-mitigation-8.1.x.md)
REFERENCE_004_PR = {
    "java/ldap-injection": "PR #1345 + #1300",
    "java/ssrf": "PR #1300 + #1364",
    "java/sql-injection": "PR #1343",
    "java/zipslip": "PR #1341",
    "java/path-injection": "PR #1365 + #1362 + #1361",
    "java/xss": "PR #1348 + #1344 + #1367",
    "java/regex-injection": "PR #1295",
    "java/insecure-trustmanager": "PR #1297",
    "java/unsafe-hostname-verification": "PR #1342",
    "java/unvalidated-url-forward": "PR #1335",
    "java/unvalidated-url-redirection": "PR #1344",
    "java/polynomial-redos": "PR #1294",
    "java/redos": "PR #1333",
    "java/stack-trace-exposure": "PR #1275 + #1346",
    "java/error-message-exposure": "n/a (8.2 follow-up)",
}


def parse_alerts(text: str) -> list[dict[str, str]]:
    pattern = re.compile(
        r"- \*\*Alert #(?P<id>\d+)\*\* — `(?P<rule>[^`]+)` \((?P<sev>[^,]+), CodeQL\)\n"
        r"  - \*\*Tool:\*\* (?P<tool>.*?)\n"
        r"  - \*\*State:\*\* (?P<state>.*?)\n"
        r"  - \*\*Created:\*\* (?P<created>.*?)\n"
        r"  - \*\*URL:\*\* (?P<url>.*?)\n"
        r"  - \*\*Location:\*\* (?P<path>[^:]+):(?P<line>\d+)\n"
        r"  - \*\*Message:\*\* (?P<msg>.*?)(?=\n\n|\Z)",
        re.DOTALL,
    )
    return [m.groupdict() for m in pattern.finditer(text)]


def main() -> int:
    text = ALERTS.read_text(encoding="utf-8")
    alerts = parse_alerts(text)
    crit_high = [a for a in alerts if a["sev"].strip() in ("critical", "high") and a["state"].strip() == "open"]
    clusters: dict[str, list[dict[str, str]]] = defaultdict(list)
    for a in crit_high:
        clusters[a["rule"]].append(a)

    rule_order = sorted(clusters.keys(), key=lambda r: (-len(clusters[r]), r))

    lines: list[str] = []
    lines.append("# CodeQL Critical+High Clusters on 8.1.x (main)")
    lines.append("")
    lines.append(f"Generated: {datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}")
    lines.append(f"Source: {ALERTS.relative_to(REPO_ROOT)}")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append(f"- Total open alerts: {len(alerts)}")
    lines.append(f"- Critical+High open: {len(crit_high)}")
    lines.append(f"- Cluster count: {len(clusters)}")
    lines.append("")
    lines.append("## Cluster Map")
    lines.append("")
    lines.append("| Rule | Severity | Count | Reference 004 PR | Notes |")
    lines.append("|---|---|---|---|---|")
    for rule in rule_order:
        rows = clusters[rule]
        sev = "critical" if any("critical" in r["sev"] for r in rows) else "high"
        cnt = len(rows)
        ref = REFERENCE_004_PR.get(rule, "TBD")
        lines.append(f"| `{rule}` | {sev} | {cnt} | {ref} | |")
    lines.append("")
    lines.append("## Per-Cluster Detail")
    lines.append("")
    for rule in rule_order:
        rows = clusters[rule]
        lines.append(f"### `{rule}` ({len(rows)} alerts)")
        lines.append("")
        for r in rows[:5]:
            lines.append(f"- Alert #{r['id']} — `{r['path']}:{r['line']}`")
        if len(rows) > 5:
            lines.append(f"- ... and {len(rows) - 5} more")
        lines.append("")

    CLUSTERS.write_text("\n".join(lines), encoding="utf-8")
    print(f"Total open: {len(alerts)}")
    print(f"Critical+High open: {len(crit_high)}")
    print(f"Cluster count: {len(clusters)}")
    print(f"Wrote {CLUSTERS.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
