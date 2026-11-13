#!/usr/bin/env python3
"""Fail if a legacy MySQL Connector/J (com.mysql.jdbc, 3.x-7.x) is present. Issue #5.

Usage:
  check-legacy-mysql-connector.py            # scan git-tracked files in the repo
  check-legacy-mysql-connector.py DIR [...]  # scan unpacked install/distribution dirs
  check-legacy-mysql-connector.py --self-test

Detects by file name (mysql-connector-java-*, mm.mysql*) and by jar content
(com/mysql/jdbc/ConnectionImpl.class exists only in the legacy driver; the 8.x
jar only ships a deprecated com.mysql.jdbc.Driver shim).
"""
import fnmatch
import os
import subprocess
import sys
import tempfile
import zipfile

NAME_PATTERNS = ["mysql-connector-java-*", "mysql-connector-java.jar", "mm.mysql*"]
LEGACY_CLASS = "com/mysql/jdbc/ConnectionImpl.class"


def legacy_reason(path):
    base = os.path.basename(path)
    if any(fnmatch.fnmatch(base, p) for p in NAME_PATTERNS):
        return "legacy file name"
    if base.endswith(".jar") and os.path.isfile(path):
        try:
            with zipfile.ZipFile(path) as z:
                if LEGACY_CLASS in z.namelist():
                    return "contains legacy com.mysql.jdbc driver classes"
        except zipfile.BadZipFile:
            pass  # e.g. an unfetched Git LFS pointer; name check above still applies
    return None


def candidates(args):
    if args:
        for d in args:
            for root, _, files in os.walk(d):
                for f in files:
                    yield os.path.join(root, f)
    else:
        out = subprocess.check_output(["git", "ls-files"], universal_newlines=True)
        for f in out.splitlines():
            if f.endswith(".jar") or fnmatch.fnmatch(os.path.basename(f), "mysql-connector*"):
                yield f


def scan(args):
    bad = [(p, r) for p in candidates(args) for r in [legacy_reason(p)] if r]
    for p, r in bad:
        print("LEGACY MYSQL CONNECTOR: %s (%s)" % (p, r))
    return 1 if bad else 0


def self_test():
    with tempfile.TemporaryDirectory() as d:
        good = os.path.join(d, "mysql-connector.jar")
        with zipfile.ZipFile(good, "w") as z:
            z.writestr("com/mysql/jdbc/Driver.class", b"")
            z.writestr("com/mysql/cj/jdbc/Driver.class", b"")
        assert scan([d]) == 0, "modern driver must pass"
        with zipfile.ZipFile(os.path.join(d, "renamed.jar"), "w") as z:
            z.writestr(LEGACY_CLASS, b"")
        assert scan([d]) == 1, "legacy classes under a generic name must fail"
        open(os.path.join(d, "mysql-connector-java-5.1.32.jar"), "w").close()
        assert scan([d]) == 1, "legacy file name must fail"
    print("self-test OK")
    return 0


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-test"]:
        sys.exit(self_test())
    sys.exit(scan(sys.argv[1:]))
