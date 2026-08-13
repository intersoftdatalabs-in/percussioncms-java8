# CodeQL Suppressions on 8.1.x (main)

This file tracks every `// codeql[rule-id]` sink-line suppression and every path-level
exclusion from `.github/codeql/codeql-config.yml`.

Row schema (per spec 004 C2 contract):

| alert_id | rule_id | file_path | line | justification | date | re_review_by | linked_pr | notes |
|---|---|---|---|---|---|---|---|---|
| 431 | java/ssrf | deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java | 498 | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31 | 33 | comment on line above client.target; validation failure throws FeedException and does not reuse the pre-validation url |
| 432 | java/ssrf | system/src/main/java/com/percussion/xml/PSDtdTree.java | 233 | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31 | 33 | comment on line above openConnection after URLValidation + scheme-literal URI rebuild |
| 432 | java/ssrf | .github/codeql/codeql-config.yml | 50 | runtime URLValidation + scheme-literal rebuild already on the sink; analyzer does not model URLValidation and ignores // codeql[java/ssrf]; path-ignore residual | 2026-08-13 | 2027-07-31 | 33 | path-level residual: GHAS ignores // codeql[java/ssrf]; local model packs not loaded; runtime defense kept |
| 494 | java/zipslip | deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/java/com/percussion/preinstall/MainDTSPreInstall.java | 217 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | Files.copy sink; ZipSlipGuard.safeDestFile + dominating getCanonicalPath/startsWith (local model packs not loaded) |
| 494 | java/zipslip | deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/java/com/percussion/preinstall/MainDTSPreInstall.java | 212 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | Files.createDirectory sink; same dominating check |
| 496 | java/zipslip | modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java | 266 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | Files.copy sink; ZipSlipGuard.safeDestFile + dominating getCanonicalPath/startsWith (local model packs not loaded) |
| 496 | java/zipslip | modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java | 259 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | Files.createDirectory sink; same dominating check |
| 501 | java/zipslip | system/src/main/java/com/percussion/util/PSArchiveFiles.java | 398 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard.safeDestFile + dominating getCanonicalPath/startsWith (local model packs not loaded) |
| 501 | java/zipslip | system/src/main/java/com/percussion/util/PSArchiveFiles.java | 376 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | mkdirs sink; same dominating check |
| 500 | java/zipslip | system/src/main/java/com/percussion/tools/PSInstallRxApp.java | 123 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard + dominating canonical startsWith |
| 499 | java/zipslip | system/src/main/java/com/percussion/tools/InstallRxApp.java | 123 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard + dominating canonical startsWith |
| 498 | java/zipslip | system/release/Install/src/com/percussion/installer/action/RxExtractJarFiles.java | 130 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard + dominating canonical startsWith |
| 497 | java/zipslip | projects/sitemanage/src/main/java/com/percussion/widgetbuilder/utils/PSWidgetPackageBuilder.java | 144 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard + dominating canonical startsWith |
| 495 | java/zipslip | modules/perc-ant/src/main/java/com/percussion/ant/install/PSExtractJarFiles.java | 131 | ZipSlipGuard + canonical startsWith | 2026-08-12 | 2027-07-31 | #35 | FileOutputStream sink; ZipSlipGuard + dominating canonical startsWith |