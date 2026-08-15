#!/usr/bin/env python3
"""Generate triage.md from alerts.md + suppressions.md + accepted-risks.md.

One row per open alert, disposition inferred from existing suppressions,
accepted-risks, and the 8.1.x path-ignore set in .github/codeql/codeql-config.yml.

Per spec 004 C1, the schema is:
  | # | alert_id | rule_id | severity | file_path | module_owner |
    | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
"""
from __future__ import annotations

import re
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
ALERTS = REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md"
SUPPRESSIONS = REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/suppressions.md"
ACCEPTED_RISKS = (
    REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/accepted-risks.md"
)
CODEQL_CONFIG = REPO_ROOT / ".github/codeql/codeql-config.yml"
TRIAGE = REPO_ROOT / "docs/ai-generated/tasks/8.1.x-codeql-baseline/triage.md"


def parse_alerts(text: str) -> list[dict[str, str]]:
    # An alert block: header line, then 6 indented `  - **Key:** value` lines.
    # The Message value can span multiple indented lines; we stop at the next
    # `^- \*\*Alert #` (start of next block) or end of text.
    block_pattern = re.compile(
        r"- \*\*Alert #(?P<id>\d+)\*\* — `(?P<rule>[^`]+)` \((?P<sev>[^,]+), CodeQL\)\n"
        r"(?P<body>(?:  - \*\*[^*]+:\*\*[^\n]*\n)+)",
        re.MULTILINE,
    )
    out: list[dict[str, str]] = []
    for m in block_pattern.finditer(text):
        body = m.group("body")
        kv: dict[str, str] = {}
        # Each kv line begins with two spaces, "- **Key:** value"; continuation
        # lines start with 4+ spaces (or are empty). Split body by `  - **` markers.
        parts = re.split(r"\n  - \*\*", "\n  - **" + body)
        # parts[0] is empty (preceded by header); skip the leading "  - **" split
        parts_iter = iter(parts)
        for part in parts:
            part = part.strip()
            if not part:
                continue
            # Each part now starts with "Key:** value\n..." after split
            m2 = re.match(r"(?P<key>[^*]+):\*\*\s*(?P<val>.*)", part, re.DOTALL)
            if not m2:
                continue
            key = m2.group("key").strip()
            val = m2.group("val").strip()
            kv[key] = val
        out.append(
            {
                "id": m.group("id"),
                "rule": m.group("rule"),
                "sev": m.group("sev"),
                "tool": kv.get("Tool", ""),
                "state": kv.get("State", ""),
                "created": kv.get("Created", ""),
                "url": kv.get("URL", ""),
                "path": (kv.get("Location", "").rsplit(":", 1)[0] if kv.get("Location") else ""),
                "line": (kv.get("Location", "").rsplit(":", 1)[1] if kv.get("Location") else ""),
                "msg": kv.get("Message", ""),
            }
        )
    return out


def parse_suppressions(text: str) -> dict[int, dict[str, str]]:
    """Map alert_id -> {linked_pr, file_path, justification} for sink-line/path-ignore rows."""
    result: dict[int, dict[str, str]] = {}
    for line in text.splitlines():
        line = line.strip()
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) < 9:
            continue
        try:
            alert_id = int(cells[0])
        except ValueError:
            continue
        existing = result.get(alert_id, {})
        linked_pr = cells[7].strip("`")
        if linked_pr and linked_pr not in ("TBD", "—", ""):
            existing["linked_pr"] = linked_pr
        existing["file_path"] = cells[2].strip("`")
        existing["justification"] = cells[4].strip("`")
        result[alert_id] = existing
    return result


def parse_accepted_risks(text: str) -> dict[int, dict[str, str]]:
    """Map alert_id -> {linked_pr, justification, target_milestone}."""
    result: dict[int, dict[str, str]] = {}
    for line in text.splitlines():
        line = line.strip()
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) < 8:
            continue
        try:
            alert_id = int(cells[0])
        except ValueError:
            continue
        result[alert_id] = {
            "linked_pr": cells[6].strip("`"),
            "target_milestone": cells[5].strip("`"),
            "justification": cells[4].strip("`"),
            "notes": cells[7].strip("`"),
        }
    return result


def parse_codeql_ignore_paths(text: str) -> list[tuple[str, str, str]]:
    """Return [(glob, source_comment, line_no)] for paths-ignore entries."""
    out: list[tuple[str, str, str]] = []
    in_paths_ignore = False
    last_comment = ""
    line_no = 0
    for line in text.splitlines():
        line_no += 1
        if line.strip().startswith("paths-ignore:"):
            in_paths_ignore = True
            continue
        if in_paths_ignore:
            if not line.strip():
                last_comment = ""
                continue
            if line.lstrip().startswith("#"):
                last_comment = line.lstrip("# ").strip()
                continue
            if line.strip().startswith("-"):
                glob = line.strip().lstrip("- ").strip().strip('"')
                if glob and not glob.startswith("**/"):
                    out.append((glob, last_comment, str(line_no)))
                last_comment = ""
            elif line.strip() and not line.startswith(" "):
                in_paths_ignore = False
                last_comment = ""
    return out


def matches_path_ignore(file_path: str, ignore_globs: list[tuple[str, str, str]]) -> tuple[bool, str]:
    for glob, comment, line in ignore_globs:
        if glob.endswith("/**"):
            prefix = glob[:-3]
            if file_path == prefix or file_path.startswith(prefix + "/"):
                return True, f"paths-ignore {glob} (#{comment or 'see config'}) @ line {line}"
        elif glob.startswith("**/") and glob.endswith("/**"):
            middle = glob[3:-3]
            if middle in file_path:
                return True, f"paths-ignore {glob} (#{comment or 'see config'}) @ line {line}"
        elif glob == file_path:
            return True, f"paths-ignore {glob} (#{comment or 'see config'}) @ line {line}"
    return False, ""


def module_owner(file_path: str) -> str:
    """Best-effort module_owner (top-level prefix)."""
    parts = file_path.split("/")
    if not parts:
        return ""
    head = parts[0]
    if head in ("system", "rest", "WebUI", "delivery", "deployer", "PCM-PkgMgtUI", "deliverytiersuite", "projects", "modules", "cui", "modules"):
        # Drill one more level for projects/* and modules/* and deliverytiersuite/*
        if head in ("projects", "modules", "deliverytiersuite") and len(parts) > 1:
            return f"{head}/{parts[1]}"
        return head
    return head


def disposition_for(
    alert: dict[str, str],
    suppression: dict[int, dict[str, str]] | None,
    accepted: dict[int, dict[str, str]] | None,
    ignore_globs: list[tuple[str, str, str]],
) -> tuple[str, str, str, str, str, str]:
    """Return (disposition, target_action, target_milestone, linked_pr, notes, module_owner).

    Disposition policy:
      - accepted-risk: alert documented in accepted-risks.md
      - false-positive: file_path matches an entry in codeql-config.yml paths-ignore
      - fix: everything else; linked_pr left empty (TBD = no PR yet)
    """
    aid = int(alert["id"])
    fp = alert["path"]

    if accepted and aid in accepted:
        a = accepted[aid]
        return (
            "accepted-risk",
            "no action until re-review",
            a["target_milestone"] or "2027-07-31",
            a["linked_pr"] or "TBD",
            a["justification"] + (" | " + a["notes"] if a["notes"] else ""),
            module_owner(fp),
        )

    matched, justification = matches_path_ignore(fp, ignore_globs)
    if matched:
        return (
            "false-positive",
            "no action; path-ignored in .github/codeql/codeql-config.yml",
            "merged",
            "",
            justification,
            module_owner(fp),
        )

    # Note: alerts in suppressions.md with a linked_pr but not path-ignored are
    # not yet auto-dismissed by GHAS. They still require either (a) a path-ignore
    # entry added to codeql-config.yml or (b) manual dismissal on the GHAS
    # dashboard. They are NOT marked ready-to-close here; the disposition is
    # surfaced via the notes field so the next follow-up PR can address them.
    notes = ""
    if suppression and aid in suppression:
        s = suppression[aid]
        notes = (
            "suppressions.md lists this alert under "
            + (s.get("file_path", "?") + " (linked_pr=" + s.get("linked_pr", "?") + ")")
            + "; path-ignore not yet applied"
        )

    return (
        "fix",
        "code fix required",
        "TBD",
        "",
        notes,
        module_owner(fp),
    )


def main() -> int:
    alerts = parse_alerts(ALERTS.read_text(encoding="utf-8"))
    open_alerts = [a for a in alerts if a["state"].strip() == "open"]
    suppressions = parse_suppressions(SUPPRESSIONS.read_text(encoding="utf-8"))
    accepted = parse_accepted_risks(ACCEPTED_RISKS.read_text(encoding="utf-8"))
    ignore_globs = parse_codeql_ignore_paths(CODEQL_CONFIG.read_text(encoding="utf-8"))

    counts: dict[str, int] = defaultdict(int)
    rows: list[tuple[str, ...]] = []
    for idx, a in enumerate(open_alerts, start=1):
        disposition, action, milestone, linked_pr, notes, owner = disposition_for(
            a, suppressions, accepted, ignore_globs
        )
        counts[disposition] += 1
        rows.append(
            (
                str(idx),
                a["id"],
                a["rule"],
                a["sev"].strip(),
                f"{a['path']}:{a['line']}",
                owner,
                disposition,
                action,
                milestone,
                linked_pr,
                notes,
            )
        )

    lines: list[str] = []
    lines.append("# CodeQL Triage Inventory — 8.1.x (main)")
    lines.append("")
    lines.append(
        "Every open Critical/High alert gets exactly one row here. The pipeline gates "
        "(`scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`) read "
        "this file."
    )
    lines.append("")
    lines.append(
        f"Generated: {datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}"
    )
    lines.append(f"Source: {ALERTS.relative_to(REPO_ROOT)}")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append(f"- Total open alerts: {len(open_alerts)}")
    for d, c in sorted(counts.items()):
        lines.append(f"- {d}: {c}")
    lines.append("")
    lines.append(
        "Schema (per spec 004 C1):"
    )
    lines.append("")
    lines.append(
        "| # | alert_id | rule_id | severity | file_path | module_owner | "
        "disposition (candidate) | target_action | target_milestone | linked_pr | notes |"
    )
    lines.append(
        "|---|----------|---------|----------|-----------|--------------|"
        "-------------------------|---------------|------------------|-----------|-------|"
    )
    for r in rows:
        lines.append("| " + " | ".join(r) + " |")

    TRIAGE.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Total open: {len(open_alerts)}")
    for d, c in sorted(counts.items()):
        print(f"  {d}: {c}")
    print(f"Wrote {TRIAGE.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())