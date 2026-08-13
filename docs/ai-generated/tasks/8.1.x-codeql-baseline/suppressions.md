# CodeQL Suppressions on 8.1.x (main)

This file tracks every `// codeql[rule-id]` sink-line suppression and every path-level
exclusion from `.github/codeql/codeql-config.yml`.

Row schema (per spec 004 C2 contract):

| alert_id |  rule_id  |                                                      file_path                                                      | line |                                justification                                 |    date    | re_review_by | linked_pr |                                                         notes                                                          |
|----------|-----------|---------------------------------------------------------------------------------------------------------------------|------|------------------------------------------------------------------------------|------------|--------------|-----------|------------------------------------------------------------------------------------------------------------------------|
| 431      | java/ssrf | deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java | 498  | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31   | 33        | comment on line above client.target; validation failure throws FeedException and does not reuse the pre-validation url |
| 432      | java/ssrf | system/src/main/java/com/percussion/xml/PSDtdTree.java                                                              | 233  | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31   | 33        | comment on line above openConnection after URLValidation + scheme-literal URI rebuild                                  |
| 432      | java/ssrf | .github/codeql/codeql-config.yml                                                                                    | 50   | runtime URLValidation + scheme-literal rebuild already on the sink; analyzer does not model URLValidation and ignores // codeql[java/ssrf]; path-ignore residual | 2026-08-13 | 2027-07-31   | 33        | path-level residual: GHAS ignores // codeql[java/ssrf]; local model packs not loaded; runtime defense kept |

