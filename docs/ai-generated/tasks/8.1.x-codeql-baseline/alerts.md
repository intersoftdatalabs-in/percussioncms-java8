# Code Scanning Alerts for intersoftdatalabs-in/percussioncms-java8

State filter: open
Generated: (python port — timestamp not embedded; see file mtime)

- **Alert #610** — `java/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/610
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/translations/PSFormEncodeDecodeHelper.java:170
  - **Message:** This part of the regular expression may cause exponential backtracking on strings containing many repetitions of '\n'.
- **Alert #609** — `java/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/609
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:117
  - **Message:** This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
- **Alert #608** — `java/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/608
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:110
  - **Message:** This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
    This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.
- **Alert #607** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/607
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1878
  - **Message:** This regular expression is constructed from a user-provided value.
- **Alert #606** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/606
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1804
  - **Message:** This regular expression is constructed from a user-provided value.
- **Alert #605** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/605
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1802
  - **Message:** This regular expression is constructed from a user-provided value.
- **Alert #604** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/604
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1800
  - **Message:** This regular expression is constructed from a user-provided value.
- **Alert #603** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/603
  - **Location:** modules/utils/src/main/java/com/percussion/utils/string/PSFolderStringUtils.java:73
  - **Message:** This regular expression is constructed from a user-provided value.
    This regular expression is constructed from a user-provided value.
- **Alert #602** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/602
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSBlogPostVisitDao.java:243
  - **Message:** This regular expression is constructed from a user-provided value.
- **Alert #601** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/601
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:1195
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #600** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/600
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:1193
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #599** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/599
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:504
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #598** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/598
  - **Location:** deliverytiersuite/delivery-tier-suite/common/src/main/java/com/percussion/delivery/exceptions/PSUncaughtError.java:60
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #597** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/597
  - **Location:** deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/comments/services/PSCommentsRestService.java:375
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #596** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/596
  - **Location:** deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/comments/services/PSCommentsRestService.java:343
  - **Message:** Untrusted URL redirection depends on a user-provided value.
- **Alert #595** — `java/ldap-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/595
  - **Location:** system/src/main/java/com/percussion/security/PSJndiGroupProvider.java:200
  - **Message:** This LDAP query depends on a user-provided value.
- **Alert #594** — `java/insecure-trustmanager` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/594
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java:383
  - **Message:** This uses TrustManager, which is defined in PSSiteImporter$ and trusts any certificate.
- **Alert #593** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/593
  - **Location:** system/src/main/java/com/percussion/filetracker/PSFUDFileNode.java:427
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #592** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/592
  - **Location:** system/src/main/java/com/percussion/filetracker/PSFUDApplication.java:187
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #591** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/591
  - **Location:** system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:722
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #590** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/590
  - **Location:** system/business/src/com/percussion/share/dao/PSSerializerUtils.java:105
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #589** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/589
  - **Location:** modules/utils/src/main/java/com/percussion/xml/PSXmlDocumentBuilder.java:452
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
    XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #588** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/588
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/jexl/PSOImportJexl.java:314
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #587** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/587
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/jexl/PSOImportJexl.java:107
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #586** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/586
  - **Location:** modules/perc-checkboxtree/src/main/java/com/percussion/controls/contenteditor/checkboxtree/PSCheckboxTreeModel.java:75
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #585** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/585
  - **Location:** modules/extensions-main/src/main/java/com/percussion/xmldom/PSXmlDomUtils.java:531
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.
- **Alert #584** — `java/unsafe-hostname-verification` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/584
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java:391
  - **Message:** The hostname verifier defined by this type always accepts any certificate, even if the hostname does not match.
- **Alert #583** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/583
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSTemplateServlet.java:153
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #582** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/582
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/siteimprove/rest/PSSiteimprove.java:221
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #581** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/581
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:415
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #580** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/580
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:375
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #579** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/579
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:335
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #578** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/578
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:295
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #577** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/577
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:208
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #576** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/576
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:170
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #575** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/575
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:129
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #574** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/574
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:227
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #573** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/573
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/preview/SimpleXmlView.java:56
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #572** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/572
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/imageedit/web/SimpleXmlView.java:55
  - **Message:** Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
    Error information can be exposed to an external user.
- **Alert #571** — `java/stack-trace-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/571
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactoryException.java:135
  - **Message:** Error information can be exposed to an external user.
- **Alert #570** — `java/stack-trace-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/570
  - **Location:** deployer/src/test/java/com/percussion/webdav/test/util/PSServletRequesterTest.java:280
  - **Message:** Error information can be exposed to an external user.
- **Alert #569** — `java/static-initialization-vector` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/569
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:99
  - **Message:** A static initialization vector should not be used for encryption.
- **Alert #568** — `java/unvalidated-url-forward` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/568
  - **Location:** modules/servletutils/src/main/java/com/percussion/utils/servlet/PSServletUtils.java:247
  - **Message:** Untrusted URL forward depends on a user-provided value.
- **Alert #567** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/567
  - **Location:** system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:535
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #566** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/566
  - **Location:** system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:513
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #565** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/565
  - **Location:** system/services/src/com/percussion/services/aaclient/PSAaClientServlet.java:75
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #564** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/564
  - **Location:** system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:78
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #563** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/563
  - **Location:** system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:77
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #562** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/562
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:801
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #561** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/561
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:738
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #560** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/560
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:488
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #559** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/559
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:226
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #558** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/558
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:203
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #557** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/557
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:144
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #556** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/556
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:127
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #555** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/555
  - **Location:** projects/sitemanage/src/main/java/com/percussion/role/service/impl/PSRoleService.java:139
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #554** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/554
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSPageRestService.java:340
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #553** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/553
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/siteimprove/rest/PSSiteimprove.java:221
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #552** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/552
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:202
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #551** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/551
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:198
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #550** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/550
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:189
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #549** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/549
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:162
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #548** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/548
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:158
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #547** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/547
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:153
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #546** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/546
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:148
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #545** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/545
  - **Location:** projects/sitemanage/src/main/java/com/percussion/dashboardmanagement/service/impl/PSUserProfileRestService.java:44
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #544** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/544
  - **Location:** projects/sitemanage/src/main/java/com/percussion/dashboardmanagement/service/impl/PSDashboardService.java:86
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #543** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/543
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:529
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #542** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/542
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:483
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #541** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/541
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:232
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #540** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/540
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:2011
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #539** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/539
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1931
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #538** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/538
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1848
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #537** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/537
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1846
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #536** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/536
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:784
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #535** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/535
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:763
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
    Cross-site scripting vulnerability due to a user-provided value.
- **Alert #534** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/534
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src/main/java/com/percussion/soln/p13n/delivery/ds/web/DeliveryController.java:157
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #533** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/533
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/impl/PSMetadataRestService.java:479
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #532** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/532
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:341
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.
- **Alert #531** — `java/unsafe-deserialization` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/531
  - **Location:** system/services/src/com/percussion/services/system/impl/PSEmailMessageHandler.java:92
  - **Message:** Unsafe deserialization depends on a user-provided value.
- **Alert #530** — `java/unsafe-deserialization` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/530
  - **Location:** system/services/src/com/percussion/services/notification/impl/PSMessageQueueService.java:117
  - **Message:** Unsafe deserialization depends on a user-provided value.
- **Alert #529** — `java/unsafe-deserialization` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/529
  - **Location:** system/services/src/com/percussion/services/notification/impl/PSMessageQueueService.java:109
  - **Message:** Unsafe deserialization depends on a user-provided value.
- **Alert #528** — `java/unsafe-deserialization` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/528
  - **Location:** system/business/src/com/percussion/rx/publisher/impl/PSPublishHandler.java:224
  - **Message:** Unsafe deserialization depends on a user-provided value.
- **Alert #527** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/527
  - **Location:** system/services/src/com/percussion/services/contentmgr/impl/PSContentMgr.java:698
  - **Message:** This query depends on a user-provided value.
    This query depends on a user-provided value.
    This query depends on a user-provided value.
- **Alert #526** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/526
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDaoHelper.java:433
  - **Message:** This query depends on a user-provided value.
- **Alert #525** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/525
  - **Location:** modules/utils/src/main/java/com/percussion/util/PSSQLStatement.java:90
  - **Message:** This query depends on a user-provided value.
- **Alert #524** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/524
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/utils/PSOSimpleSqlQuery.java:95
  - **Message:** This query depends on a user-provided value.
- **Alert #523** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/523
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:460
  - **Message:** This query depends on a user-provided value.
- **Alert #522** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/522
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:364
  - **Message:** This query depends on a user-provided value.
- **Alert #521** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/521
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactory.java:1227
  - **Message:** This query depends on a user-provided value.
- **Alert #520** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/520
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcResultSetIteratorStep.java:100
  - **Message:** This query depends on a user-provided value.
- **Alert #519** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/519
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataQueryService.java:598
  - **Message:** This query depends on a user-provided value.
    This query depends on a user-provided value.
    This query depends on a user-provided value.
    This query depends on a user-provided value.
    This query depends on a user-provided value.
- **Alert #518** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/518
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:1460
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #517** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/517
  - **Location:** system/src/main/java/com/percussion/servlets/PSAppServlet.java:191
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #516** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/516
  - **Location:** system/servlet/src/com/percussion/webdav/method/PSPropFindMethod.java:225
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #515** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/515
  - **Location:** system/servlet/src/com/percussion/webdav/method/PSLockMethod.java:332
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #514** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/514
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImportLogViewer.java:135
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #513** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/513
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSTemplateServlet.java:85
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #512** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/512
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSTemplateInfo.java:89
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #511** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/511
  - **Location:** deliverytiersuite/delivery-tier-suite/polls/src/main/java/com/percussion/delivery/polls/services/PSPollsRestService.java:73
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #510** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/510
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/impl/PSMetadataRestService.java:118
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #509** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/509
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/impl/PSMetadataExtractorRestService.java:70
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #508** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/508
  - **Location:** deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/membership/services/impl/PSMembershipRestService.java:99
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #507** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/507
  - **Location:** deliverytiersuite/delivery-tier-suite/integrations/src/main/java/com/percussion/delivery/integrations/IntegrationsRestService.java:46
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #506** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/506
  - **Location:** deliverytiersuite/delivery-tier-suite/forms/src/main/java/com/percussion/delivery/forms/impl/PSFormRestService.java:138
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #505** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/505
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:122
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #504** — `java/http-response-splitting` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/504
  - **Location:** deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/comments/services/PSCommentsRestService.java:101
  - **Message:** This header depends on a user-provided value, which may cause a response-splitting vulnerability.
- **Alert #503** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/503
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:94
  - **Message:** Cryptographic algorithm AES/CBC/PKCS5Padding is insecure. CBC mode with PKCS#5 or PKCS#7 padding is vulnerable to padding oracle attacks. Consider using GCM instead.
- **Alert #502** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/502
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:65
  - **Message:** Cryptographic algorithm AES/CBC/PKCS5Padding is insecure. CBC mode with PKCS#5 or PKCS#7 padding is vulnerable to padding oracle attacks. Consider using GCM instead.
- **Alert #501** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/501
  - **Location:** system/src/main/java/com/percussion/util/PSArchiveFiles.java:352
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #500** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/500
  - **Location:** system/src/main/java/com/percussion/tools/PSInstallRxApp.java:85
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #499** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/499
  - **Location:** system/src/main/java/com/percussion/tools/InstallRxApp.java:85
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #498** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/498
  - **Location:** system/release/Install/src/com/percussion/installer/action/RxExtractJarFiles.java:75
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #497** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/497
  - **Location:** projects/sitemanage/src/main/java/com/percussion/widgetbuilder/utils/PSWidgetPackageBuilder.java:125
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #496** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/496
  - **Location:** modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java:238
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #495** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/495
  - **Location:** modules/perc-ant/src/main/java/com/percussion/ant/install/PSExtractJarFiles.java:73
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #494** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/494
  - **Location:** deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/java/com/percussion/preinstall/MainDTSPreInstall.java:194
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
    Unsanitized archive entry, which may contain '..', is used in a file system operation.
- **Alert #493** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/493
  - **Location:** system/src/main/java/com/percussion/xml/PSDtdTree.java:200
  - **Message:** This path depends on a user-provided value.
- **Alert #492** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/492
  - **Location:** system/src/main/java/com/percussion/server/PSServer.java:350
  - **Message:** This path depends on a user-provided value.
- **Alert #491** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/491
  - **Location:** system/src/main/java/com/percussion/process/PSProcessDaemon.java:804
  - **Message:** This path depends on a user-provided value.
- **Alert #490** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/490
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:442
  - **Message:** This path depends on a user-provided value.
- **Alert #489** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/489
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:413
  - **Message:** This path depends on a user-provided value.
- **Alert #488** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/488
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:365
  - **Message:** This path depends on a user-provided value.
- **Alert #487** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/487
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:365
  - **Message:** This path depends on a user-provided value.
- **Alert #486** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/486
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:349
  - **Message:** This path depends on a user-provided value.
- **Alert #485** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/485
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:343
  - **Message:** This path depends on a user-provided value.
- **Alert #484** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/484
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:343
  - **Message:** This path depends on a user-provided value.
- **Alert #483** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/483
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:341
  - **Message:** This path depends on a user-provided value.
- **Alert #482** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/482
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:322
  - **Message:** This path depends on a user-provided value.
- **Alert #481** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/481
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:351
  - **Message:** This path depends on a user-provided value.
- **Alert #480** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/480
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:350
  - **Message:** This path depends on a user-provided value.
- **Alert #479** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/479
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:305
  - **Message:** This path depends on a user-provided value.
- **Alert #478** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/478
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:259
  - **Message:** This path depends on a user-provided value.
- **Alert #477** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/477
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:495
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #476** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/476
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:494
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #475** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/475
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:409
  - **Message:** This path depends on a user-provided value.
- **Alert #474** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/474
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:382
  - **Message:** This path depends on a user-provided value.
- **Alert #473** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/473
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:359
  - **Message:** This path depends on a user-provided value.
- **Alert #472** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/472
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:320
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #471** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/471
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:237
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #470** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/470
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:210
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #469** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/469
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:199
  - **Message:** This path depends on a user-provided value.
- **Alert #468** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/468
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:355
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #467** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/467
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:166
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #466** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/466
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:353
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #465** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/465
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:340
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #464** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/464
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:330
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #463** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/463
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:125
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #462** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/462
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:320
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #461** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/461
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:319
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #460** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/460
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:293
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #459** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/459
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:291
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #458** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/458
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:494
  - **Message:** This path depends on a user-provided value.
- **Alert #457** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/457
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:451
  - **Message:** This path depends on a user-provided value.
- **Alert #456** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/456
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:419
  - **Message:** This path depends on a user-provided value.
- **Alert #455** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/455
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:364
  - **Message:** This path depends on a user-provided value.
- **Alert #454** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/454
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:257
  - **Message:** This path depends on a user-provided value.
- **Alert #453** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/453
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:262
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #452** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/452
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:237
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #451** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/451
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:227
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #450** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/450
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:221
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #449** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/449
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:212
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #448** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/448
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:163
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #447** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/447
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSRenderLinkService.java:680
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #446** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/446
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:334
  - **Message:** This path depends on a user-provided value.
- **Alert #445** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/445
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:332
  - **Message:** This path depends on a user-provided value.
- **Alert #444** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/444
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:318
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #443** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/443
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:268
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #442** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/442
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:268
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #441** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/441
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:147
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #440** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/440
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:102
  - **Message:** This path depends on a user-provided value.
- **Alert #439** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/439
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:102
  - **Message:** This path depends on a user-provided value.
- **Alert #438** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/438
  - **Location:** projects/sitemanage/src/main/java/com/percussion/cloudservice/impl/PSCloudService.java:238
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #437** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/437
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java:2024
  - **Message:** This path depends on a user-provided value.
- **Alert #436** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/436
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java:1984
  - **Message:** This path depends on a user-provided value.
    This path depends on a user-provided value.
- **Alert #435** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/435
  - **Location:** projects/sitemanage/src/main/java/com/percussion/apibridge/AssetAdaptor.java:1110
  - **Message:** This path depends on a user-provided value.
- **Alert #434** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/434
  - **Location:** projects/sitemanage/src/main/java/com/percussion/apibridge/AssetAdaptor.java:1110
  - **Message:** This path depends on a user-provided value.
- **Alert #433** — `java/insecure-cookie` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/433
  - **Location:** modules/p13n-api/src/main/java/com/percussion/soln/p13n/tracking/web/CookieGenerator.java:189
  - **Message:** Cookie is added to response without the 'secure' flag being set.
- **Alert #432** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/432
  - **Location:** system/src/main/java/com/percussion/xml/PSDtdTree.java:204
  - **Message:** Potential server-side request forgery due to a user-provided value.
- **Alert #431** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/431
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:461
  - **Message:** Potential server-side request forgery due to a user-provided value.
- **Alert #430** — `java/tainted-numeric-cast` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/430
  - **Location:** modules/utils/src/main/java/com/percussion/services/guidmgr/data/PSGuid.java:281
  - **Message:** This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a user-provided value, potentially causing truncation.
    This cast to a narrower type depends on a [user-provided value](4
- **Alert #429** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/429
  - **Location:** system/src/main/java/com/percussion/server/cache/PSAutotuneCache.java:216
  - **Message:** Implicit cast of source type double to narrower destination type long.
- **Alert #428** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/428
  - **Location:** system/src/main/java/com/percussion/server/cache/PSAutotuneCache.java:199
  - **Message:** Implicit cast of source type double to narrower destination type long.
- **Alert #427** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/427
  - **Location:** system/src/main/java/com/percussion/HTTPClient/RespInputStream.java:144
  - **Message:** Implicit cast of source type long to narrower destination type int.
- **Alert #426** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/426
  - **Location:** system/src/main/java/com/percussion/HTTPClient/RespInputStream.java:140
  - **Message:** Implicit cast of source type long to narrower destination type int.
- **Alert #425** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/425
  - **Location:** system/src/main/java/com/percussion/HTTPClient/BufferedInputStream.java:115
  - **Message:** Implicit cast of source type long to narrower destination type int.
- **Alert #424** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/424
  - **Location:** system/Testing/src/com/percussion/test/http/HttpRequest.java:406
  - **Message:** Implicit cast of source type long to narrower destination type int.
- **Alert #423** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:23:01Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/423
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java:581
  - **Message:** Implicit cast of source type long to narrower destination type int.
- **Alert #422** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/422
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/python/python.js:233
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '#' and containing many repetitions of '#'.
- **Alert #421** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/421
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '\t(' and containing many repetitions of '\\\\'.
- **Alert #420** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/420
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '\t'' and containing many repetitions of '\\\\'.
- **Alert #419** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/419
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '\t"' and containing many repetitions of '\\\\'.
- **Alert #418** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/418
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/erlang/erlang.js:346
  - **Message:** This part of the regular expression may cause exponential backtracking on strings containing many repetitions of '\n'.
- **Alert #417** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/417
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2413
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<script' and containing many repetitions of '\t'.
- **Alert #416** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/416
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:31
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<' and containing many repetitions of '\t'.
- **Alert #415** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/415
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:320
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<script' and containing many repetitions of '\t'.
- **Alert #414** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/414
  - **Location:** cui/components/requirejs-text/text.js:34
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.
    This regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\t'.
    This regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\t'.
    This regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.
    This regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\t'.
    This regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\t'.
- **Alert #413** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/413
  - **Location:** cui/components/requirejs-text/text.js:33
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<?xml\tversion="a' and with many repetitions of '0'.
    This regular expression that depends on library input may run slow on strings starting with '<?xml\tversion="a' and with many repetitions of '0'.
- **Alert #412** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/412
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_xdomainTooltip.html:16
  - **Message:** Script loaded using unencrypted connection.
- **Alert #411** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/411
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_xdomainSimpleDropdownDatePicker.html:11
  - **Message:** Script loaded using unencrypted connection.
- **Alert #410** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/410
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_xdomainDropdownDatePicker.html:11
  - **Message:** Script loaded using unencrypted connection.
- **Alert #409** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/409
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_xdomainFisheyeList.html:16
  - **Message:** Script loaded using unencrypted connection.
- **Alert #408** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/408
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_remoteLocalMix.html:16
  - **Message:** Script loaded using unencrypted connection.
- **Alert #407** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/407
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_remoteFail.html:12
  - **Message:** Script loaded using unencrypted connection.
- **Alert #406** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/406
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_remoteDelayed.html:12
  - **Message:** Script loaded using unencrypted connection.
- **Alert #405** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/405
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/xdomain/test_remote.html:12
  - **Message:** Script loaded using unencrypted connection.
- **Alert #404** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/404
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2775/test_i18nDebug.html:20
  - **Message:** Script loaded using unencrypted connection.
- **Alert #403** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/403
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2775/test_i18n.html:19
  - **Message:** Script loaded using unencrypted connection.
- **Alert #402** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/402
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2683/test_delayedLocalLoadDebug.html:19
  - **Message:** Script loaded using unencrypted connection.
- **Alert #401** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/401
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2683/test_delayedLocalLoad.html:18
  - **Message:** Script loaded using unencrypted connection.
- **Alert #400** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/400
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/data/old/test_delicious.html:13
  - **Message:** Script loaded using unencrypted connection.
- **Alert #399** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/399
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/data/old/test_data.html:12
  - **Message:** Script loaded using unencrypted connection.
- **Alert #398** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/398
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:730
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #397** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/397
  - **Location:** system/UnitTestResources/com/percussion/xsl/encoding/Yahoo-EUC-JP.xhtm:46
  - **Message:** Script loaded using unencrypted connection.
- **Alert #396** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/396
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:1317
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #395** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/395
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:1318
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #394** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/394
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:1321
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #393** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/393
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:1322
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #392** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/392
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:55
  - **Message:** Script loaded using unencrypted connection.
- **Alert #391** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/391
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:56
  - **Message:** Script loaded using unencrypted connection.
- **Alert #390** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/390
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:57
  - **Message:** Script loaded using unencrypted connection.
- **Alert #389** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/389
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:30
  - **Message:** Script loaded using unencrypted connection.
- **Alert #388** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/388
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:31
  - **Message:** Script loaded using unencrypted connection.
- **Alert #387** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/387
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:32
  - **Message:** Script loaded using unencrypted connection.
- **Alert #386** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/386
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:38
  - **Message:** Script loaded using unencrypted connection.
- **Alert #385** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/385
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:39
  - **Message:** Script loaded using unencrypted connection.
- **Alert #384** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/384
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:42
  - **Message:** Script loaded using unencrypted connection.
- **Alert #383** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/383
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:45
  - **Message:** Script loaded using unencrypted connection.
- **Alert #382** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/382
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:63
  - **Message:** Script loaded using unencrypted connection.
- **Alert #381** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/381
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:64
  - **Message:** Script loaded using unencrypted connection.
- **Alert #380** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/380
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:65
  - **Message:** Script loaded using unencrypted connection.
- **Alert #379** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/379
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:71
  - **Message:** Script loaded using unencrypted connection.
- **Alert #378** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/378
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:72
  - **Message:** Script loaded using unencrypted connection.
- **Alert #377** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/377
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:75
  - **Message:** Script loaded using unencrypted connection.
- **Alert #376** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/376
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:78
  - **Message:** Script loaded using unencrypted connection.
- **Alert #375** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/375
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:87
  - **Message:** Script loaded using unencrypted connection.
- **Alert #374** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/374
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:88
  - **Message:** Script loaded using unencrypted connection.
- **Alert #373** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/373
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:89
  - **Message:** Script loaded using unencrypted connection.
- **Alert #372** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/372
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:95
  - **Message:** Script loaded using unencrypted connection.
- **Alert #371** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/371
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:96
  - **Message:** Script loaded using unencrypted connection.
- **Alert #370** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/370
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:99
  - **Message:** Script loaded using unencrypted connection.
- **Alert #369** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/369
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:102
  - **Message:** Script loaded using unencrypted connection.
- **Alert #368** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/368
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:107
  - **Message:** Script loaded using unencrypted connection.
- **Alert #367** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/367
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:108
  - **Message:** Script loaded using unencrypted connection.
- **Alert #366** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/366
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:109
  - **Message:** Script loaded using unencrypted connection.
- **Alert #365** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/365
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:115
  - **Message:** Script loaded using unencrypted connection.
- **Alert #364** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/364
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:116
  - **Message:** Script loaded using unencrypted connection.
- **Alert #363** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/363
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:119
  - **Message:** Script loaded using unencrypted connection.
- **Alert #362** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/362
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:122
  - **Message:** Script loaded using unencrypted connection.
- **Alert #361** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/361
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_ws.html:343
  - **Message:** Script loaded using unencrypted connection.
- **Alert #360** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/360
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_sw.html:639
  - **Message:** Script loaded using unencrypted connection.
- **Alert #359** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/359
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_cc.html:628
  - **Message:** Script loaded using unencrypted connection.
- **Alert #358** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/358
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/webapp/WEB-INF/pages/importtest_post.jsp:52
  - **Message:** Script loaded using unencrypted connection.
- **Alert #357** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/357
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/user/pages/DispatchTemplateGenerator.jsp:31
  - **Message:** Script loaded using unencrypted connection.
    Script loaded from content delivery network with no integrity check.
- **Alert #356** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/356
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/user/pages/DispatchTemplateGenerator.jsp:32
  - **Message:** Script loaded using unencrypted connection.
    Script loaded from content delivery network with no integrity check.
- **Alert #355** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/355
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/slim/index.html:9
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #354** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/354
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/slim/index.html:10
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #353** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/353
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:16
  - **Message:** Script loaded using unencrypted connection.
- **Alert #352** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/352
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:17
  - **Message:** Script loaded using unencrypted connection.
- **Alert #351** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/351
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:18
  - **Message:** Script loaded using unencrypted connection.
- **Alert #350** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/350
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:19
  - **Message:** Script loaded using unencrypted connection.
- **Alert #349** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/349
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:20
  - **Message:** Script loaded using unencrypted connection.
- **Alert #348** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/348
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:21
  - **Message:** Script loaded using unencrypted connection.
- **Alert #347** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/347
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:22
  - **Message:** Script loaded using unencrypted connection.
- **Alert #346** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/346
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:23
  - **Message:** Script loaded using unencrypted connection.
- **Alert #345** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/345
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:24
  - **Message:** Script loaded using unencrypted connection.
- **Alert #344** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/344
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/tern.html:25
  - **Message:** Script loaded using unencrypted connection.
- **Alert #343** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/343
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/requirejs.html:10
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #342** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/342
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/demo/merge.html:11
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #341** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/341
  - **Location:** cui/components/twitter-bootstrap-3.0.0/getting-started.html:120
  - **Message:** Script loaded from content delivery network with no integrity check.
- **Alert #340** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/340
  - **Location:** cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:4
  - **Message:** Iframe loaded using unencrypted connection.
- **Alert #339** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/339
  - **Location:** cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:7
  - **Message:** Iframe loaded using unencrypted connection.
- **Alert #338** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/338
  - **Location:** cui/components/twitter-bootstrap-3.0.0/_includes/footer.html:7
  - **Message:** Script loaded using unencrypted connection.
- **Alert #337** — `js/unvalidated-dynamic-method-call` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/337
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:836
  - **Message:** Invocation of method with user-controlled name may dispatch to unexpected target and cause an exception.
- **Alert #336** — `js/unvalidated-dynamic-method-call` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/336
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:518
  - **Message:** Invocation of method with user-controlled name may dispatch to unexpected target and cause an exception.
- **Alert #335** — `js/unvalidated-dynamic-method-call` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/335
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/addon/tern/worker.js:20
  - **Message:** Invocation of method with user-controlled name may dispatch to unexpected target and cause an exception.
- **Alert #334** — `js/clear-text-logging` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/334
  - **Location:** modules/perc-qa-automation/frontend/tests/login.spec.js:33
  - **Message:** This logs sensitive data returned by process environment as clear text.
- **Alert #333** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/333
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js:252
  - **Message:** This regular expression does not match script end tags like </script >.
- **Alert #332** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/332
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js:460
  - **Message:** This regular expression does not match script end tags like </script >.
- **Alert #331** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/331
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:15166
  - **Message:** This regular expression does not match script end tags like </script >.
- **Alert #330** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/330
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:9433
  - **Message:** This regular expression does not match script end tags like </script >.
- **Alert #329** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/329
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/xml/xml.js:79
  - **Message:** This regular expression only parses --> and not --!> as a HTML comment end tag.
- **Alert #328** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/328
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2413
  - **Message:** This regular expression does not match upper case <SCRIPT> tags.
- **Alert #327** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/327
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:320
  - **Message:** This regular expression does not match upper case <SCRIPT> tags.
- **Alert #326** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/326
  - **Location:** cui/components/knockoutjs/src/utils.domManipulation.js:2
  - **Message:** This regular expression does not match comments containing newlines.
- **Alert #325** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/325
  - **Location:** cui/components/knockoutjs/dist/knockout.debug.js:771
  - **Message:** This regular expression does not match comments containing newlines.
- **Alert #324** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/324
  - **Location:** cui/components/jquery-migrate/jquery-migrate.js:745
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.
- **Alert #323** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/323
  - **Location:** cui/components/jquery-migrate/jquery-migrate.js:736
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.
- **Alert #322** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/322
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:745
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.
- **Alert #321** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/321
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:736
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.
- **Alert #320** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/320
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/docs.js:268
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #319** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/319
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2413
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #318** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/318
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:320
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #317** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/317
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:15005
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #316** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/316
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14910
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #315** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/315
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:6134
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #314** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/314
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5883
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #313** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/313
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:1498
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.
- **Alert #312** — `js/double-escaping` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/312
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/test_ScriptSrcIO.html:246
  - **Message:** This replacement may double-escape '&' characters from here.
- **Alert #311** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/311
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954
  - **Message:** This does not escape backslash characters in the input.
- **Alert #310** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/310
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954
  - **Message:** This does not escape backslash characters in the input.
- **Alert #309** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/309
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954
  - **Message:** This does not escape backslash characters in the input.
- **Alert #308** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/308
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:2971
  - **Message:** This does not escape backslash characters in the input.
- **Alert #307** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/307
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:2971
  - **Message:** This does not escape backslash characters in the input.
- **Alert #306** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/306
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:2971
  - **Message:** This does not escape backslash characters in the input.
- **Alert #305** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/305
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:2962
  - **Message:** This does not escape backslash characters in the input.
- **Alert #304** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/304
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:2962
  - **Message:** This does not escape backslash characters in the input.
- **Alert #303** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/303
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:2962
  - **Message:** This does not escape backslash characters in the input.
- **Alert #302** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/302
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #301** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/301
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #300** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/300
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #299** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/299
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #298** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/298
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #297** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/297
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #296** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/296
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #295** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/295
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #294** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/294
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1093
  - **Message:** This does not escape backslash characters in the input.
- **Alert #293** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/293
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1094
  - **Message:** This does not escape backslash characters in the input.
- **Alert #292** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/292
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1094
  - **Message:** This does not escape backslash characters in the input.
- **Alert #291** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/291
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1094
  - **Message:** This does not escape backslash characters in the input.
- **Alert #290** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/290
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #289** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/289
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #288** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/288
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1087
  - **Message:** This does not escape backslash characters in the input.
- **Alert #287** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/287
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1088
  - **Message:** This does not escape backslash characters in the input.
- **Alert #286** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/286
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1088
  - **Message:** This does not escape backslash characters in the input.
- **Alert #285** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/285
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1088
  - **Message:** This does not escape backslash characters in the input.
- **Alert #284** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/284
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #283** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/283
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #282** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/282
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #281** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/281
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #280** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/280
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #279** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/279
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #278** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/278
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #277** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/277
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #276** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/276
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #275** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/275
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #274** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/274
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #273** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/273
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #272** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/272
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:484
  - **Message:** This does not escape backslash characters in the input.
- **Alert #271** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/271
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:484
  - **Message:** This does not escape backslash characters in the input.
- **Alert #270** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/270
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:484
  - **Message:** This does not escape backslash characters in the input.
- **Alert #269** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/269
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #268** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/268
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #267** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/267
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #266** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/266
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #265** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/265
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:483
  - **Message:** This does not escape backslash characters in the input.
- **Alert #264** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/264
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:482
  - **Message:** This does not escape backslash characters in the input.
- **Alert #263** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/263
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #262** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/262
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #261** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/261
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:469
  - **Message:** This does not escape backslash characters in the input.
- **Alert #260** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/260
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:470
  - **Message:** This does not escape backslash characters in the input.
- **Alert #259** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/259
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:470
  - **Message:** This does not escape backslash characters in the input.
- **Alert #258** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/258
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:470
  - **Message:** This does not escape backslash characters in the input.
- **Alert #257** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/257
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2775/dojo.js:2251
  - **Message:** This does not escape backslash characters in the input.
- **Alert #256** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/256
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/scriptsrc/TestMultipart.js:82
  - **Message:** This does not escape backslash characters in the input.
- **Alert #255** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/255
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/flash/unit_tests_comm.html:59
  - **Message:** This replaces only the first occurrence of ">".
- **Alert #254** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/254
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/flash/unit_tests_comm.html:58
  - **Message:** This replaces only the first occurrence of "<".
- **Alert #253** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/253
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/string/extras.js:141
  - **Message:** This does not escape backslash characters in the input.
- **Alert #252** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/252
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:5392
  - **Message:** This does not escape backslash characters in the input.
- **Alert #251** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/251
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:3424
  - **Message:** This does not escape backslash characters in the input.
- **Alert #250** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/250
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/flash.js:1033
  - **Message:** This does not escape backslash characters in the input.
- **Alert #249** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/249
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/flash.js:1032
  - **Message:** This does not escape backslash characters in the input.
- **Alert #248** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/248
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/flash.js:1026
  - **Message:** This replaces only the first occurrence of "\\".
- **Alert #247** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/247
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/debug/deep.html:225
  - **Message:** This replaces only the first occurrence of "\r".
- **Alert #246** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/246
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57
  - **Message:** This replaces only the first occurrence of "<".
- **Alert #245** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/245
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57
  - **Message:** This replaces only the first occurrence of ">".
- **Alert #244** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/244
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57
  - **Message:** This replaces only the first occurrence of '"'.
- **Alert #243** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/243
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49
  - **Message:** This replaces only the first occurrence of "<".
- **Alert #242** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/242
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49
  - **Message:** This replaces only the first occurrence of ">".
- **Alert #241** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/241
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49
  - **Message:** This replaces only the first occurrence of '"'.
- **Alert #240** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/240
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/test/mode_test.js:70
  - **Message:** This replaces only the first occurrence of '&'.
- **Alert #239** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/239
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/test/mode_test.js:70
  - **Message:** This replaces only the first occurrence of '<'.
- **Alert #238** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/238
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/test/index.html:120
  - **Message:** This replaces only the first occurrence of /\[<&\]/.
- **Alert #237** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/237
  - **Location:** cui/components/jquery-ui/jquery-ui.js:17945
  - **Message:** This does not escape backslash characters in the input.
- **Alert #236** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/236
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/log4javascript/log4javascript_uncompressed.js:941
  - **Message:** This does not escape backslash characters in the input.
- **Alert #235** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/235
  - **Location:** cui/widgets/app/app.viewmodel.js:87
  - **Message:** This replaces only the first occurrence of /\[\\[\]/.
- **Alert #234** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/234
  - **Location:** cui/widgets/app/app.viewmodel.js:87
  - **Message:** This replaces only the first occurrence of /\[\\]\]/.
- **Alert #233** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/233
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:1166
  - **Message:** This does not escape backslash characters in the input.
- **Alert #232** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/232
  - **Location:** WebUI/war/widgets/perc_site_map.js:2193
  - **Message:** This replaces only the first occurrence of "{{".
- **Alert #231** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/231
  - **Location:** WebUI/war/services/PercUserService.js:169
  - **Message:** This replaces only the first occurrence of "%".
- **Alert #230** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/230
  - **Location:** WebUI/war/plugins/perc_utils.js:1674
  - **Message:** This replaces only the first occurrence of /'/.
- **Alert #229** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/229
  - **Location:** WebUI/war/plugins/perc_css_utils.js:118
  - **Message:** This replaces only the first occurrence of "\"".
- **Alert #228** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/228
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:165
  - **Message:** This does not escape backslash characters in the input.
- **Alert #227** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/227
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:165
  - **Message:** This does not escape backslash characters in the input.
- **Alert #226** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/226
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:162
  - **Message:** This does not escape backslash characters in the input.
- **Alert #225** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/225
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:162
  - **Message:** This does not escape backslash characters in the input.
- **Alert #224** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/224
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/momentjs/moment-with-locales.js:810
  - **Message:** This replaces only the first occurrence of '\\'.
- **Alert #223** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/223
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-testrunner/testrunner.js:778
  - **Message:** This does not escape backslash characters in the input.
- **Alert #222** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/222
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:17945
  - **Message:** This does not escape backslash characters in the input.
- **Alert #221** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/221
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:8347
  - **Message:** This does not escape backslash characters in the input.
- **Alert #220** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/220
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:4541
  - **Message:** This replaces only the first occurrence of '"'.
- **Alert #219** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/219
  - **Location:** WebUI/war/app/includes/siteimprove_integration.html:137
  - **Message:** This does not escape backslash characters in the input.
- **Alert #218** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/218
  - **Location:** WebUI/war/app/includes/siteimprove_integration.html:118
  - **Message:** This replaces only the first occurrence of "\\".
- **Alert #217** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/217
  - **Location:** WebUI/war/api/lib/handlebars-1.0.0.js:912
  - **Message:** This replaces only the first occurrence of "\n".
- **Alert #216** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/216
  - **Location:** WebUI/war/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\[\\[\]/.
- **Alert #215** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/215
  - **Location:** WebUI/war/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\[\\]\]/.
- **Alert #214** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/214
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:574
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.
- **Alert #213** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/213
  - **Location:** cui/components/knockoutjs/src/google-closure-compiler-utils.js:11
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.
- **Alert #212** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/212
  - **Location:** cui/components/knockoutjs/dist/knockout.debug.js:42
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.
- **Alert #211** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/211
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.js:318
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.
- **Alert #210** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/210
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.js:318
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.
- **Alert #209** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/209
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:1779
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.
- **Alert #208** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/208
  - **Location:** WebUI/war/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:8793
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.
- **Alert #207** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/207
  - **Location:** WebUI/war/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:8747
  - **Message:** Properties are copied from obj2 to obj1 without guarding against prototype pollution.
- **Alert #206** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/206
  - **Location:** system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:256
  - **Message:** Potential XSS vulnerability in the '$.fn.qtip' plugin.
- **Alert #205** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/205
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/jquery.treeview.js:215
  - **Message:** Potential XSS vulnerability in the '$.fn.treeview' plugin.
- **Alert #204** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/204
  - **Location:** cui/components/jquery-ui/jquery-ui.js:912
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.
- **Alert #203** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/203
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:524
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.
- **Alert #202** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/202
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/collapse.js:31
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.
- **Alert #201** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/201
  - **Location:** WebUI/war/widgets/PercSimpleMenu.js:94
  - **Message:** Potential XSS vulnerability in the '$.fn.percSimpleMenu' plugin.
- **Alert #200** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/200
  - **Location:** WebUI/war/plugins/perc_utils.js:1081
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #199** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/199
  - **Location:** WebUI/war/plugins/perc_utils.js:1080
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #198** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/198
  - **Location:** WebUI/war/plugins/perc_utils.js:1077
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #197** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/197
  - **Location:** WebUI/war/plugins/perc_utils.js:1076
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #196** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/196
  - **Location:** WebUI/war/plugins/perc_utils.js:1075
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #195** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/195
  - **Location:** WebUI/war/plugins/perc_utils.js:1075
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.
- **Alert #194** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/194
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:79
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.
- **Alert #193** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/193
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:75
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.
- **Alert #192** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/192
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:437
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.
- **Alert #191** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/191
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:473
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.
- **Alert #190** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/190
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:585
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.
- **Alert #189** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/189
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:566
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.
- **Alert #188** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/188
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.ui-deps.js:912
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.
- **Alert #187** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/187
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:907
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.
- **Alert #186** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/186
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:912
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.
- **Alert #185** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/185
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:1616
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #184** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/184
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:984
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #183** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/183
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/test_ScriptSrcIO.html:249
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #182** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/182
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/scriptsrc/TestMultipart.js:55
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #181** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/181
  - **Location:** system/UnitTestResources/com/percussion/delivery/bw-corona.html:1310
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #180** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/180
  - **Location:** system/Packages/perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js:231
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #179** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/179
  - **Location:** system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1610
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #178** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/178
  - **Location:** system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1606
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #177** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/177
  - **Location:** system/Packages/perc.widgets.image/sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js:396
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #176** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/176
  - **Location:** system/Packages/perc.widgets.image/SupportFile-rx_resources/widgets/image/js/jquery.imageAssetControl.js:396
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #175** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/175
  - **Location:** system/Packages/perc.widget.socialButtons/sys__UserDependency--rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:132
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #174** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/174
  - **Location:** system/Packages/perc.widget.socialButtons/SupportFile-rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:135
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #173** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/173
  - **Location:** system/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:227
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #172** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/172
  - **Location:** system/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:84
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #171** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/171
  - **Location:** system/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:226
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #170** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/170
  - **Location:** system/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:84
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #169** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/169
  - **Location:** system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js:447
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #168** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/168
  - **Location:** system/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:181
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #167** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/167
  - **Location:** system/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:163
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #166** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/166
  - **Location:** system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:50
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #165** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/165
  - **Location:** system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:49
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #164** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/164
  - **Location:** system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:48
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #163** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/163
  - **Location:** system/Packages/perc.gadget.siteframework/sys__UserDependency--cm/gadgets/repository/PercSiteFrameworkGadget/perc_sitewide_framework_gadget.js:120
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #162** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/162
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:488
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #161** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/161
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:487
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #160** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/160
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:486
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #159** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/159
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:473
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #158** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/158
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:472
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #157** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/157
  - **Location:** system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:471
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #156** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/156
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:490
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #155** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/155
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:489
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #154** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/154
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:488
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #153** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/153
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:475
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #152** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/152
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:474
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #151** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/151
  - **Location:** system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:473
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #150** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/150
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_w.html:85
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #149** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/149
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_w.html:61
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #148** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/148
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/rx_resources/js/cropper_custom.js:81
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #147** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/147
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/plugin.js:141
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #146** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/146
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/editor_plugin.js:93
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #145** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/145
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/index.html:63
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #144** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/144
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-form/jquery.form.js:383
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #143** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/143
  - **Location:** delivery/common/js/views/PercTagListView.js:115
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #142** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/142
  - **Location:** delivery/common/js/views/PercRssView.js:144
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #141** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/141
  - **Location:** delivery/common/js/views/PercRssView.js:140
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #140** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/140
  - **Location:** delivery/common/js/views/PercRssView.js:64
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #139** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/139
  - **Location:** delivery/common/js/views/PercRegistrationView.js:260
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #138** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/138
  - **Location:** delivery/common/js/views/PercRegistrationView.js:72
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #137** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/137
  - **Location:** delivery/common/js/views/PercMostReadBlogPostsView.js:105
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #136** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/136
  - **Location:** delivery/common/js/views/PercMostReadBlogPostsView.js:70
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #135** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/135
  - **Location:** delivery/common/js/views/PercCategoryListView.js:192
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #134** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/134
  - **Location:** delivery/common/js/views/PercArchiveListView.js:279
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #133** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/133
  - **Location:** delivery/common/js/views/PercBlogPostView.js:153
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #132** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/132
  - **Location:** delivery/common/js/views/PercArchiveListView.js:191
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #131** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/131
  - **Location:** delivery/common/js/views/PercBlogPostView.js:144
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #130** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/130
  - **Location:** delivery/common/js/views/PercArchiveListView.js:131
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #129** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/129
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tooltip.js:254
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #128** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/128
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1790
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #127** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/127
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1329
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #126** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/126
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tab.js:51
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #125** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/125
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1058
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #124** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/124
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/modal.js:230
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #123** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/123
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:785
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #122** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/122
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:662
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #121** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/121
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #120** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/120
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/dropdown.js:112
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #119** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/119
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:471
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #118** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/118
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/collapse.js:169
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #117** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/117
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/collapse.js:165
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #116** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/116
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/carousel.js:196
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #115** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/115
  - **Location:** cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:106
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #114** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/114
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/alert.js:40
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #113** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/113
  - **Location:** cui/components/knockoutjs/build/knockout-raw.js:23
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #112** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/112
  - **Location:** WebUI/war/widgets/perc_page_edit_dialog.js:283
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #111** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/111
  - **Location:** WebUI/war/widgets/PercInlineEditDataTable.js:268
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #110** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/110
  - **Location:** WebUI/war/widgets/PercInlineEditDataTable.js:220
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #109** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/109
  - **Location:** WebUI/war/widgetbuilder/js/views/PercWidgetFieldsViews.js:140
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #108** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/108
  - **Location:** WebUI/war/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js:55
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #107** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/107
  - **Location:** WebUI/war/views/PercUserView.js:681
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #106** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/106
  - **Location:** WebUI/war/views/PercUserView.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #105** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/105
  - **Location:** WebUI/war/views/PercIFrameView.js:232
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #104** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/104
  - **Location:** WebUI/war/views/PercIFrameView.js:48
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #103** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/103
  - **Location:** WebUI/war/views/PercChangeTemplateDialog.js:199
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #102** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/102
  - **Location:** WebUI/war/views/PercCSSGalleryView.js:66
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #101** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/101
  - **Location:** WebUI/war/services/PercServiceUtils.js:87
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #100** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/100
  - **Location:** WebUI/war/plugins/perc_utils.js:622
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #99** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/99
  - **Location:** WebUI/war/plugins/perc_utils.js:604
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #98** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/98
  - **Location:** WebUI/war/plugins/perc_template_layout_helper.js:243
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #97** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/97
  - **Location:** WebUI/war/plugins/PercRedirectHandler.js:266
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #96** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/96
  - **Location:** WebUI/war/plugins/PercListEditorWidget.js:301
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
    DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #95** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/95
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js:1098
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #94** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/94
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.js:1099
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #93** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/93
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js:363
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #92** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/92
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js:39
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #91** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/91
  - **Location:** WebUI/war/classes/perc_template_layout_class.js:95
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.
- **Alert #90** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/90
  - **Location:** system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:1327
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #89** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/89
  - **Location:** system/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:255
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #88** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/88
  - **Location:** system/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:255
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #87** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/87
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-form/jquery.form.js:312
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #86** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/86
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:473
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #85** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/85
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:473
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #84** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/84
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:341
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #83** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/83
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:341
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #82** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/82
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:339
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #81** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/81
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:339
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #80** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/80
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #79** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/79
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #78** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/78
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #77** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/77
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #76** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/76
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2666
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #75** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/75
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2653
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #74** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/74
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:182
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #73** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/73
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:165
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #72** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/72
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js:54
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #71** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/71
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:324
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #70** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/70
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:321
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #69** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/69
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:318
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #68** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/68
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:310
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #67** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/67
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:295
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #66** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/66
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:672
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #65** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/65
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:482
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #64** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/64
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:212
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
    This HTML construction which depends on library input might later allow cross-site scripting.
- **Alert #63** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/63
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/webimagefx/webimagefx.js:58
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #62** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/62
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/mobilepreview/js/PercMobilePreview.js:9
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #61** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/61
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:794
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #60** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/60
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:705
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
    Cross-site scripting vulnerability due to user-provided value.
- **Alert #59** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/59
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:549
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #58** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/58
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:545
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #57** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/57
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:168
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #56** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/56
  - **Location:** cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:40
  - **Message:** Cross-site scripting vulnerability due to user-provided value.
- **Alert #55** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/55
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js:202
  - **Message:** Suspicious character range that overlaps with \w in the same character class, and is equivalent to \["#$%&'()*+,\-.\/0-9:\].
- **Alert #54** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/54
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js:410
  - **Message:** Suspicious character range that overlaps with \w in the same character class, and is equivalent to \["#$%&'()*+,\-.\/0-9:\].
- **Alert #53** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/53
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:15116
  - **Message:** Suspicious character range that overlaps with \w in the same character class, and is equivalent to \["#$%&'()*+,\-.\/0-9:\].
- **Alert #52** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/52
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:9389
  - **Message:** Suspicious character range that overlaps with \w in the same character class, and is equivalent to \["#$%&'()*+,\-.\/0-9:\].
- **Alert #51** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/51
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/test/mode_test.js:31
  - **Message:** Suspicious character range that overlaps with \w in the same character class, and is equivalent to \[&'()*+,\-.\/0-9:;<=>?@A-Z\\[\\\\]^_\].
- **Alert #50** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/50
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/css/css.js:212
  - **Message:** Suspicious character range that overlaps with a-f in the same character class, and is equivalent to \[A-Z\\[\\\\]^_`a-f\].
- **Alert #49** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/49
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/css/css.js:212
  - **Message:** Suspicious character range that overlaps with a-f in the same character class, and is equivalent to \[A-Z\\[\\\\]^_`a-f\].
- **Alert #48** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/48
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/date/serialize.js:96
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #47** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/47
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/js/globalvariables.js:229
  - **Message:** The escape sequence '\}}' is equivalent to just '}}', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #46** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/46
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/js/globalvariables.js:229
  - **Message:** The escape sequence '\{{' is equivalent to just '{{', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #45** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/45
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/lib/codemirror.js:5833
  - **Message:** The escape sequence '\s' is equivalent to just 's', so the sequence is not a character class when it is used in a regular expression.
- **Alert #44** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/44
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/lib/codemirror.js:5833
  - **Message:** The escape sequence '\s' is equivalent to just 's', so the sequence is not a character class when it is used in a regular expression.
- **Alert #43** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/43
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #42** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/42
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #41** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/41
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #40** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/40
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #39** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/39
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #38** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/38
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #37** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/37
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #36** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/36
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #35** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/35
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #34** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/34
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #33** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/33
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #32** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/32
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #31** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/31
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #30** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/30
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #29** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/29
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #28** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/28
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #27** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/27
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #26** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/26
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #25** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/25
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #24** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/24
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #23** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/23
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #22** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/22
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/fortran/fortran.js:115
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #21** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/21
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/addon/fold/xml-fold.js:18
  - **Message:** The escape sequence '\-' is equivalent to just '-', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #20** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/20
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/addon/fold/xml-fold.js:18
  - **Message:** The escape sequence '\.' is equivalent to just '.', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #19** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/19
  - **Location:** cui/components/knockoutjs/dist/knockout.debug.js:2001
  - **Message:** The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
    The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
    The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
- **Alert #18** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/18
  - **Location:** cui/components/knockoutjs/src/binding/expressionRewriting.js:23
  - **Message:** The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
    The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
    The escape sequence '\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.
- **Alert #17** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/17
  - **Location:** WebUI/war/views/PercCommonMinuetView.js:182
  - **Message:** The escape sequence '\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #16** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/16
  - **Location:** WebUI/war/views/PercCommonMinuetView.js:181
  - **Message:** The escape sequence '\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.
    The escape sequence '\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #15** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/15
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\}}' is equivalent to just '}}', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #14** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/14
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\}}' is equivalent to just '}}', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #13** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/13
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\{{' is equivalent to just '{{', so the sequence may still represent a meta-character when it is used in a regular expression.
- **Alert #12** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/12
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2775/dojo.js:1015
  - **Message:** This code execution depends on a user-provided value.
- **Alert #11** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/11
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/io/RepubsubIO.js:111
  - **Message:** This code execution depends on a user-provided value.
- **Alert #10** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/10
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:1127
  - **Message:** This code execution depends on a user-provided value.
- **Alert #9** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/9
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:617
  - **Message:** This code execution depends on a user-provided value.
- **Alert #8** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/8
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/hostenv_browser.js:29
  - **Message:** This code execution depends on a user-provided value.
- **Alert #7** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/7
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/iframe_history.html:21
  - **Message:** This code execution depends on a user-provided value.
- **Alert #6** — `js/insecure-randomness` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/6
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/uuid/TimeBasedGenerator.js:197
  - **Message:** This uses a cryptographically insecure random number generated at Math.random() in a security context.
- **Alert #5** — `js/insecure-randomness` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/5
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/uuid/TimeBasedGenerator.js:189
  - **Message:** This uses a cryptographically insecure random number generated at Math.random() in a security context.
    This uses a cryptographically insecure random number generated at Math.random() in a security context.
- **Alert #4** — `js/insecure-randomness` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/4
  - **Location:** WebUI/war/views/PercCategoryView.js:880
  - **Message:** This uses a cryptographically insecure random number generated at Math.random() in a security context.
- **Alert #3** — `js/insecure-randomness` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/3
  - **Location:** WebUI/war/views/PercCategoryView.js:435
  - **Message:** This uses a cryptographically insecure random number generated at Math.random() in a security context.
- **Alert #2** — `js/identity-replacement` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/2
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/hostenv_dashboard.js:189
  - **Message:** This replaces ''' with itself.
- **Alert #1** — `js/identity-replacement` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-08-03T23:19:27Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms-java8/security/code-scanning/1
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/hostenv_dashboard.js:184
  - **Message:** This replaces ''' with itself.

