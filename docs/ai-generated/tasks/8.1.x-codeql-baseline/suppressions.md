# CodeQL Suppressions on 8.1.x (main)

This file tracks every `// codeql[rule-id]` sink-line suppression and every path-level
exclusion from `.github/codeql/codeql-config.yml`.

Row schema (per spec 004 C2 contract):

| alert_id |  rule_id  |                                                      file_path                                                      | line |                                justification                                 |    date    | re_review_by | linked_pr |                                                         notes                                                          |
|----------|-----------|---------------------------------------------------------------------------------------------------------------------|------|------------------------------------------------------------------------------|------------|--------------|-----------|------------------------------------------------------------------------------------------------------------------------|
| 431      | java/ssrf | deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java | 498  | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31   | 33        | comment on line above client.target; validation failure throws FeedException and does not reuse the pre-validation url |
| 432      | java/ssrf | system/src/main/java/com/percussion/xml/PSDtdTree.java                                                              | 232  | URL rebuilt from URLValidation.validateURLString + http/https scheme literal | 2026-08-12 | 2027-07-31   | 33        | comment on line above openConnection after URLValidation + scheme-literal URI rebuild                                  |

