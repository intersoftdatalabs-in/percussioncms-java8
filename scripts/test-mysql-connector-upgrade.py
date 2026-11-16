#!/usr/bin/env python3
"""Upgrade simulation for the MySQL Connector/J purge (issue #5).

Seeds a fake pre-upgrade DTS and CMS install containing legacy MySQL connectors, extracts the purge/install
steps from the REAL installer files (installDts.xml, install.xml), runs them with Ant, then asserts:
  * no legacy connector remains (scripts/check-legacy-mysql-connector.py is run on the result),
  * exactly the shipped mysql-connector-j jar remains, unrelated jars are untouched,
  * every removed jar is logged by Ant (the INFO log support relies on).

Usage: test-mysql-connector-upgrade.py [--keep]
Needs Java 8+, Ant 1.10 and ant-contrib: found automatically in ~/.m2 or /usr/share/java
(Debian/Ubuntu: apt-get install ant ant-contrib). Uses only the system temp dir; nothing in the repo is modified.
"""
import glob
import os
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DTS_XML = os.path.join(
    REPO,
    "deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/rxconfig/Installer/installDts.xml",
)
CMS_XML = os.path.join(REPO, "modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml")
CHECKER = os.path.join(REPO, "scripts/check-legacy-mysql-connector.py")
SHIPPED = "mysql-connector-j-8.4.0.jar"

DTS_DIRS = ["common/lib", "lib", "lib/jdbc", "perc-lib", "perc-lib/jdbc"]
LEGACY = ["mysql-connector-java-5.1.32.jar", "mysql-connector-java-5.1.12-bin.jar", "mm.mysql-2.0.14.jar",
          "mysql-connector-java-8.0.28.jar", "mysql-connector-j-8.0.33.jar", "mysql-connector.jar"]
KEEP = ["mariadb-java-client-3.5.11.jar", "ojdbc8-19.3.0.0.jar", "HikariCP-4.0.3.jar"]


def jar(path, legacy=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("com/mysql/jdbc/ConnectionImpl.class" if legacy else "META-INF/x", b"")


def find_ant_cp():
    """Ant 1.10 + launcher + ant-contrib from ~/.m2 (dev machines) or /usr/share/java (apt: ant, ant-contrib)."""
    wanted = [
        ("ant", ["~/.m2/repository/org/apache/ant/ant/1.10.*/ant-1.10.*.jar", "/usr/share/java/ant.jar"]),
        ("ant-launcher", ["~/.m2/repository/org/apache/ant/ant-launcher/1.10.*/ant-launcher-1.10.*.jar",
                          "/usr/share/java/ant-launcher.jar"]),
        ("ant-contrib", ["~/.m2/repository/ant-contrib/ant-contrib/1.0b3/ant-contrib-1.0b3.jar",
                         "/usr/share/java/ant-contrib.jar"]),
    ]
    jars = []
    for name, patterns in wanted:
        found = [x for pat in patterns for x in sorted(glob.glob(os.path.expanduser(pat))) if not x.endswith("-sources.jar")]
        if not found:
            sys.exit("cannot find %s (looked in ~/.m2 and /usr/share/java)" % name)
        jars.append(found[-1])
    return os.pathsep.join(jars)


def java():
    home = os.environ.get("JAVA_HOME")
    for c in ([home + "/bin/java"] if home else []) + glob.glob("/usr/lib/jvm/*8*/bin/java") + ["java"]:
        if shutil.which(c) or os.path.isfile(c):
            return c
    sys.exit("no java found")


def run_ant(buildfile, props, target):
    cmd = [java(), "-cp", find_ant_cp(), "org.apache.tools.ant.launch.Launcher", "-f", buildfile]
    cmd += ["-D%s=%s" % kv for kv in props.items()] + [target]
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
    return p.returncode, p.stdout


def dts_fragment():
    """Elements from the 'Purging obsolete MySQL' echo up to and including the mapper copy."""
    root = ET.parse(DTS_XML).getroot()
    for parent in root.iter():
        kids = list(parent)
        start = next((i for i, k in enumerate(kids) if k.tag == "echo" and "Purging obsolete MySQL" in (k.text or k.get("message", ""))), None)
        if start is None:
            continue
        end = next(i for i, k in enumerate(kids) if i > start and k.tag == "copy" and k.find("mapper") is not None)
        return "".join(ET.tostring(k, encoding="unicode") for k in kids[start:end + 1])
    sys.exit("DTS purge block not found in installDts.xml (structure changed?)")


def cms_target():
    root = ET.parse(CMS_XML).getroot()
    t = next(t for t in root.iter("target") if t.get("name") == "install_jdbc_drivers")
    return ET.tostring(t, encoding="unicode")


def build_file(path, name, body, extra_props=""):
    open(path, "w").write(
        '<project name="%s" default="x" xmlns:ac="antlib:net.sf.antcontrib">\n'
        '<taskdef resource="net/sf/antcontrib/antlib.xml"/>\n%s\n</project>\n' % (name, body))


def checks(results, name, ok, detail=""):
    results.append((name, ok, detail))
    print("  [%s] %s %s" % ("PASS" if ok else "FAIL", name, detail))


def ls(d):
    return sorted(os.listdir(d)) if os.path.isdir(d) else []


def test_dts(tmp, results):
    print("DTS upgrade simulation")
    inst, src = os.path.join(tmp, "dts"), os.path.join(tmp, "dts-src")
    srv = os.path.join(inst, "Deployment/Server")
    for d in DTS_DIRS:
        for f in LEGACY:
            jar(os.path.join(srv, d, f), legacy=not f.startswith("mysql-connector-j-8"))
    for f in KEEP:
        jar(os.path.join(srv, "common/lib", f))
    jar(os.path.join(src, "Deployment/Server/common/lib", SHIPPED))
    bf = os.path.join(tmp, "dts-build.xml")
    build_file(bf, "dts", '<target name="x">%s</target>' % dts_fragment())
    rc, out = run_ant(bf, {"install.dir": inst, "staging.dir": "", "install.src": src}, "x")
    checks(results, "ant exits 0", rc == 0, "" if rc == 0 else "\n" + out[-1500:])
    if "--verbose" in sys.argv:
        print(out)
    for d in DTS_DIRS:
        left = [f for f in ls(os.path.join(srv, d)) if f.startswith(("mysql", "mm.mysql"))]
        want = sorted([SHIPPED, "mysql-connector.jar"]) if d == "common/lib" else []
        checks(results, "%s mysql jars == %s" % (d, want), left == want, "" if left == want else "got %s" % left)
    checks(results, "unrelated jars kept", all(f in ls(os.path.join(srv, "common/lib")) for f in KEEP))
    log_ok = all(re.search(r"Deleting .*%s" % re.escape(f), out) for f in LEGACY[:3])
    checks(results, "removed jars are logged (Ant 'Deleting <path>')", log_ok)
    rc2 = subprocess.run([sys.executable, "-I", CHECKER, srv], stdout=subprocess.PIPE, universal_newlines=True)
    checks(results, "legacy-connector checker passes on result", rc2.returncode == 0, rc2.stdout.strip())


def test_cms(tmp, results):
    print("CMS upgrade simulation")
    inst, src = os.path.join(tmp, "cms"), os.path.join(tmp, "cms-src")
    jdbc = os.path.join(inst, "jetty/base/lib/jdbc")
    for f in LEGACY + ["ojdbc8-19.3.0.0.jar"]:
        jar(os.path.join(jdbc, f), legacy=f.startswith(("mysql-connector-java", "mm.mysql")))
    jar(os.path.join(inst, "AppServer/server/rx/lib/mysql-connector-java-5.1.7-bin.jar"), legacy=True)
    jar(os.path.join(inst, "AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/lib/mysql-connector-java-3.1.jar"), legacy=True)
    jar(os.path.join(src, "jetty/base/lib/jdbc/mysql-connector.jar"))
    jar(os.path.join(src, "jetty/base/lib/jdbc/derby-10.14.2.0.jar"))
    bf = os.path.join(tmp, "cms-build.xml")
    build_file(bf, "cms", cms_target().replace('<target name="install_jdbc_drivers"', '<target name="x"'))
    rc, out = run_ant(bf, {"install.dir": inst, "install.src": src, "customer.jdbc.drivers": jdbc}, "x")
    checks(results, "ant exits 0", rc == 0, "" if rc == 0 else "\n" + out[-1500:])
    left = [f for f in ls(jdbc) if f.startswith(("mysql", "mm.mysql"))]
    checks(results, "jetty/base/lib/jdbc mysql jars == [mysql-connector.jar]", left == ["mysql-connector.jar"], "got %s" % left)
    checks(results, "shipped jar is the new one (not legacy)", os.path.getsize(os.path.join(jdbc, "mysql-connector.jar")) == os.path.getsize(os.path.join(src, "jetty/base/lib/jdbc/mysql-connector.jar")))
    checks(results, "removed jars are logged", all(re.search(r"Deleting .*%s" % re.escape(f), out) for f in LEGACY[:3]))
    rc2 = subprocess.run([sys.executable, "-I", CHECKER, jdbc], stdout=subprocess.PIPE, universal_newlines=True)
    checks(results, "legacy-connector checker passes on result", rc2.returncode == 0, rc2.stdout.strip())


def main():
    tmp = tempfile.mkdtemp(prefix="perc-mysql-upgrade-")
    results = []
    try:
        test_dts(tmp, results)
        test_cms(tmp, results)
    finally:
        if "--keep" in sys.argv:
            print("kept", tmp)
        else:
            shutil.rmtree(tmp, ignore_errors=True)
    failed = [r for r in results if not r[1]]
    print("%d checks, %d failed" % (len(results), len(failed)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
