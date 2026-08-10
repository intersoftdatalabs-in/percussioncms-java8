#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Harness self-test for scripts/verify-suppressions.py.

Creates a temporary source file with a `// codeql[rule-id]` line and a
matching ``suppressions.md`` row, then asserts that the script
returns 0. Then mutates the source to remove the anchor and asserts
the script returns 1. Cleans up afterwards.

This proves the grep + window + justification logic in
verify-suppressions.py actually catches a missing suppression before
the next family PR relies on it.
"""
from __future__ import annotations

import os
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRIPT = REPO_ROOT / "scripts" / "verify-suppressions.py"


def run(*args: str) -> tuple[int, str]:
    env = os.environ.copy()
    env.pop("GH_TOKEN", None)
    proc = subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        shell=False,
        check=False,
        capture_output=True,
        text=True,
        env=env,
        cwd=REPO_ROOT,
    )
    return proc.returncode, (proc.stdout or "") + (proc.stderr or "")


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="verify-suppressions-fixture-") as tmp:
        tmp_path = Path(tmp)
        src = tmp_path / "FakeSink.java"
        supp = tmp_path / "suppressions.md"

        # 1. Source has the anchor; suppression row matches.
        src.write_text(
            "// codeql[java/sql-injection] justification: harness self-test;\n"
            "public class FakeSink {}\n",
            encoding="utf-8",
        )
        supp.write_text(
            (
                "| 9999 | `java/sql-injection` | `"
                + str(src)
                + "` | 1 | harness self-test | 2026-08-09 | 2027-07-31 | #TEST | ok |\n"
            ),
            encoding="utf-8",
        )
        rc, out = run(
            "--suppressions",
            str(supp),
            "--source-root",
            str(tmp_path),
        )
        if rc != 0:
            print(f"FAIL: harness did not pass with valid suppression: {out}")
            return 1

        # 2. Source missing the anchor; suppression row should fail.
        src.write_text(
            "public class FakeSink { /* no codeql comment */ }\n",
            encoding="utf-8",
        )
        rc, out = run(
            "--suppressions",
            str(supp),
            "--source-root",
            str(tmp_path),
        )
        if rc == 0:
            print(
                "FAIL: harness did not detect missing // codeql[java/sql-injection] anchor. "
                f"stdout/stderr: {out}"
            )
            return 1

        print("PASS: verify-suppressions.py detects valid + missing anchors")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
