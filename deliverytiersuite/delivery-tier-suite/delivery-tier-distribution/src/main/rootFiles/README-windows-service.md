# Delivery Tier Windows Service Notes

## JVM security defaults (CVE-2026-114938)

Because Windows DTS services run via Apache Commons Daemon Procrun (`tomcat9.exe` in `--StartMode jvm`, which bypasses `setenv.bat`), `DTSProductionService.bat` and `DTSStagingService.bat` pass `-XX:+IgnoreUnrecognizedVMOptions;-XX:-UseBASE64Intrinsics` directly in Procrun `++JvmOptions` (GH-288) as proactive defense-in-depth against **CVE-2026-114938** ("Java4Shell").

- **JDK 8 (`1.8.0`):** JDK 8 is unaffected by CVE-2026-114938 and does not define `UseBASE64Intrinsics`, so `-XX:+IgnoreUnrecognizedVMOptions` is required before `-XX:-UseBASE64Intrinsics` so service startup stays clean.
- **JDK 17 / 21+:** Protects deployments running DTS under an unpatched JDK 17/21+ runtime (`< 17.0.21.1` / `< 21.0.13.1`) or service registrations carried forward across an upgrade.

For existing Windows services installed before this change, append the option pair in place without reinstalling, then restart the service:

```bat
tomcat9.exe //US//PercussionProductionDTS ++JvmOptions "-XX:+IgnoreUnrecognizedVMOptions;-XX:-UseBASE64Intrinsics"
tomcat9.exe //US//PercussionStagingDTS ++JvmOptions "-XX:+IgnoreUnrecognizedVMOptions;-XX:-UseBASE64Intrinsics"
```
