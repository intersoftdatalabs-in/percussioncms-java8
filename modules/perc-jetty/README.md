# perc-jetty

This Module creates Jetty install/deployment jar including dependencies, configurations and script files.

## Building

```
mvn clean install
```

## JVM security defaults (CVE-2026-114938)

`system/Tools/jetty/defaults/start.d/jvm.ini` (packaged into the Jetty distribution) sets `-XX:+IgnoreUnrecognizedVMOptions` followed by `-XX:-UseBASE64Intrinsics` under `--exec` (GH-288) as proactive defense-in-depth against **CVE-2026-114938** ("Java4Shell"):

- **JDK 8 (`1.8.0`):** JDK 8 does not include `UseBASE64Intrinsics` or the affected AVX-512 VBMI `_base64_decodeBlock` intrinsic, so `-XX:+IgnoreUnrecognizedVMOptions` is required so JDK 8 starts cleanly without aborting on an unrecognized flag.
- **JDK 17 / 21+ (if used or carried forward across an 8.2 upgrade):** Disables the AVX-512 VBMI Base64 intrinsic on unpatched runtimes (`< 17.0.21.1` / `< 21.0.13.1`) across both x86-64 and ARM64. Once running on a patched JDK (`17.0.21.1+` / `21.0.13.1+`), operators who want hardware-accelerated Base64 decoding back can add `-XX:+UseBASE64Intrinsics` below `--exec` in `jetty/base/start.d/jvm.ini`.

Audit test: `Base64IntrinsicsConfigAuditTest` in `modules/perc-security-utils`.

