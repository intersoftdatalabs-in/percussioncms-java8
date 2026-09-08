# Issue 199: sitemanage — fix 3 failing unit tests

## Scope

Two production code changes in `projects/sitemanage` plus one test isolation
fix in `projects/sitemanage/src/test`. No cross-module impact.

## Files changed

- `projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSDispatchingPathService.java`
  - `PathNormalizer.normalizePath`: replace `notNull(path, "Path cannot be null")` with an explicit
    `if (path == null) throw new IllegalArgumentException("Path cannot be null");` so the
    implementation honours the `IPSPathNormalizer.normalizePath(...) throws IllegalArgumentException`
    contract and the `shouldFailOnNormalizeNullPath` test expectation.
  - `PathMatch.toFullPath`: same fix — replace `notNull(relativePath, ...)` with an explicit
    `IllegalArgumentException` throw to satisfy `shouldFailToReturnProperFullPathIfGivenRelativePathIsNull`.
- `projects/sitemanage/src/test/java/com/percussion/itemmanagement/service/impl/PSItemServiceTest.java`
  - Add `@Rule TemporaryFolder temporaryFolder`.
  - In `setUp`: `PathUtils.clearRxDir()` then `PathUtils.setThreadOnlyRxDir(temporaryFolder.getRoot())`.
  - In `tearDown`: `PathUtils.unsetThreadOnlyRxDir(temporaryFolder.getRoot())` then
    `PathUtils.clearRxDir()`.
  - This is the same isolation pattern already used by
    `projects/sitemanage/src/test/java/com/percussion/share/extension/PSEnsureMyFacesSecretTest.java`
    and is needed because earlier test classes in the surefire JVM set `rxdeploydir` to a
    JUnit `TemporaryFolder` root that is deleted after their class finishes, leaving a stale,
    nonexistent `rxdeploydir` for any later class that constructs an `Rx`-aware service.

## Verification

`./mvn-env.sh test -pl projects/sitemanage` after the change:

```
[INFO] Tests run: 314, Failures: 0, Errors: 0, Skipped: 17
[INFO] BUILD SUCCESS
```

The 17 skipped tests are the pre-existing `@Ignore`d tests, unchanged from the prior baseline.

`./mvn-env.sh spotless:check -pl projects/sitemanage` is clean.
