# CodeQL Accepted Risks — 8.1.x (main)

Critical/High alerts that **cannot be closed** without removing a legacy-still-in-use
component or upgrading past Java 8. Each row has a re-review date (default 2027-07-31, aligns
with the 8.3 re-review window per the 004 spec C4 contract).

Schema (per spec 004 C4):

| alert_id | rule_id | severity | file_path:line | justification | re_review_by | linked_pr | notes |
|----------|---------|----------|----------------|---------------|--------------|-----------|-------|
| 502 | java/weak-cryptographic-algorithm | high | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:65 | Deprecated class still wired as fallback decryption in 3 callers; AES/CBC/PKCS5Padding vulnerable to padding oracle; GCM migration requires re-encrypting stored credentials and updating PSAesCBC + callers to a SecretKey-derived PSAESGCMKey path. Re-review at 8.3 cut to confirm migration plan. | 2027-07-31 | TBD | Accept until legacy crypto migration |
| 503 | java/weak-cryptographic-algorithm | high | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:94 | Same deprecated class as #502; second Cipher.getInstance sink. | 2027-07-31 | TBD | Accept until legacy crypto migration |
| 569 | java/static-initialization-vector | high | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:99 | Hardcoded IV in deprecated class; static IV with CBC is a known weakness. Resolved by AES-GCM migration that prepends a random nonce (PSAESGCMEncryptor.encrypt already does this). | 2027-07-31 | TBD | Accept until legacy crypto migration |
