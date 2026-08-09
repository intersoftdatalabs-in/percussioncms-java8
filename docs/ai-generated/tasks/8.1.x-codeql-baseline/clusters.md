# CodeQL Critical+High Clusters on 8.1.x (main)

Generated: 2026-08-09T22:37:02Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 610
- Critical+High open: 429
- Cluster count: 30

## Cluster Map

| Rule | Severity | Count | Reference 004 PR | Notes |
|---|---|---|---|---|
| `js/incomplete-sanitization` | high | 97 | TBD | |
| `js/xss-through-dom` | high | 95 | TBD | |
| `java/path-injection` | high | 60 | PR #1365 + #1362 + #1361 | |
| `java/xss` | high | 36 | PR #1348 + #1344 + #1367 | |
| `js/useless-regexp-character-escape` | high | 36 | TBD | |
| `java/sql-injection` | high | 9 | PR #1343 | |
| `java/xxe` | critical | 9 | TBD | |
| `js/bad-tag-filter` | high | 9 | TBD | |
| `java/zipslip` | high | 8 | PR #1341 | |
| `js/incomplete-multi-character-sanitization` | high | 8 | TBD | |
| `js/redos` | high | 8 | TBD | |
| `js/xss` | high | 8 | TBD | |
| `java/implicit-cast-in-compound-assignment` | high | 7 | TBD | |
| `java/regex-injection` | high | 6 | PR #1295 | |
| `js/code-injection` | critical | 6 | TBD | |
| `java/unsafe-deserialization` | critical | 4 | TBD | |
| `js/insecure-randomness` | high | 4 | TBD | |
| `js/unvalidated-dynamic-method-call` | high | 3 | TBD | |
| `java/polynomial-redos` | high | 2 | PR #1294 | |
| `java/ssrf` | critical | 2 | PR #1300 + #1364 | |
| `java/weak-cryptographic-algorithm` | high | 2 | TBD | |
| `js/polynomial-redos` | high | 2 | TBD | |
| `java/insecure-trustmanager` | high | 1 | PR #1297 | |
| `java/ldap-injection` | critical | 1 | PR #1345 + #1300 | |
| `java/redos` | high | 1 | PR #1333 | |
| `java/static-initialization-vector` | high | 1 | TBD | |
| `java/tainted-numeric-cast` | critical | 1 | TBD | |
| `java/unvalidated-url-forward` | high | 1 | PR #1335 | |
| `js/clear-text-logging` | high | 1 | TBD | |
| `js/double-escaping` | high | 1 | TBD | |

## Per-Cluster Detail

### `js/incomplete-sanitization` (97 alerts)

- Alert #311 — `system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954`
- Alert #310 — `system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954`
- Alert #309 — `system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:2954`
- Alert #308 — `system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:2971`
- Alert #307 — `system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:2971`
- ... and 92 more

### `js/xss-through-dom` (95 alerts)

- Alert #185 — `system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:1616`
- Alert #184 — `system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:984`
- Alert #183 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/test_ScriptSrcIO.html:249`
- Alert #182 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/scriptsrc/TestMultipart.js:55`
- Alert #181 — `system/UnitTestResources/com/percussion/delivery/bw-corona.html:1310`
- ... and 90 more

### `java/path-injection` (60 alerts)

- Alert #493 — `system/src/main/java/com/percussion/xml/PSDtdTree.java:200`
- Alert #492 — `system/src/main/java/com/percussion/server/PSServer.java:350`
- Alert #491 — `system/src/main/java/com/percussion/process/PSProcessDaemon.java:804`
- Alert #490 — `system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:442`
- Alert #489 — `system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:413`
- ... and 55 more

### `java/xss` (36 alerts)

- Alert #567 — `system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:535`
- Alert #566 — `system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:513`
- Alert #565 — `system/services/src/com/percussion/services/aaclient/PSAaClientServlet.java:75`
- Alert #564 — `system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:78`
- Alert #563 — `system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:77`
- ... and 31 more

### `js/useless-regexp-character-escape` (36 alerts)

- Alert #48 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/date/serialize.js:96`
- Alert #47 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/js/globalvariables.js:229`
- Alert #46 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/js/globalvariables.js:229`
- Alert #45 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/lib/codemirror.js:5833`
- Alert #44 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/lib/codemirror.js:5833`
- ... and 31 more

### `java/sql-injection` (9 alerts)

- Alert #527 — `system/services/src/com/percussion/services/contentmgr/impl/PSContentMgr.java:698`
- Alert #526 — `projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDaoHelper.java:433`
- Alert #525 — `modules/utils/src/main/java/com/percussion/util/PSSQLStatement.java:90`
- Alert #524 — `modules/perc-toolkit/src/main/java/com/percussion/pso/utils/PSOSimpleSqlQuery.java:95`
- Alert #523 — `modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:460`
- ... and 4 more

### `java/xxe` (9 alerts)

- Alert #593 — `system/src/main/java/com/percussion/filetracker/PSFUDFileNode.java:427`
- Alert #592 — `system/src/main/java/com/percussion/filetracker/PSFUDApplication.java:187`
- Alert #591 — `system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:722`
- Alert #590 — `system/business/src/com/percussion/share/dao/PSSerializerUtils.java:105`
- Alert #589 — `modules/utils/src/main/java/com/percussion/xml/PSXmlDocumentBuilder.java:452`
- ... and 4 more

### `js/bad-tag-filter` (9 alerts)

- Alert #333 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js:252`
- Alert #332 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js:460`
- Alert #331 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:15166`
- Alert #330 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:9433`
- Alert #329 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/xml/xml.js:79`
- ... and 4 more

### `java/zipslip` (8 alerts)

- Alert #501 — `system/src/main/java/com/percussion/util/PSArchiveFiles.java:352`
- Alert #500 — `system/src/main/java/com/percussion/tools/PSInstallRxApp.java:85`
- Alert #499 — `system/src/main/java/com/percussion/tools/InstallRxApp.java:85`
- Alert #498 — `system/release/Install/src/com/percussion/installer/action/RxExtractJarFiles.java:75`
- Alert #497 — `projects/sitemanage/src/main/java/com/percussion/widgetbuilder/utils/PSWidgetPackageBuilder.java:125`
- ... and 3 more

### `js/incomplete-multi-character-sanitization` (8 alerts)

- Alert #320 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/docs.js:268`
- Alert #319 — `deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2413`
- Alert #318 — `deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:320`
- Alert #317 — `WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:15005`
- Alert #316 — `WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14910`
- ... and 3 more

### `js/redos` (8 alerts)

- Alert #422 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/python/python.js:233`
- Alert #421 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624`
- Alert #420 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624`
- Alert #419 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/markdown/markdown.js:624`
- Alert #418 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/mode/erlang/erlang.js:346`
- ... and 3 more

### `js/xss` (8 alerts)

- Alert #63 — `system/cms/content/applications/sys_resources/ApplicationFiles/webimagefx/webimagefx.js:58`
- Alert #62 — `system/cms/content/applications/sys_resources/ApplicationFiles/mobilepreview/js/PercMobilePreview.js:9`
- Alert #61 — `cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:794`
- Alert #60 — `cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:705`
- Alert #59 — `cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:549`
- ... and 3 more

### `java/implicit-cast-in-compound-assignment` (7 alerts)

- Alert #429 — `system/src/main/java/com/percussion/server/cache/PSAutotuneCache.java:216`
- Alert #428 — `system/src/main/java/com/percussion/server/cache/PSAutotuneCache.java:199`
- Alert #427 — `system/src/main/java/com/percussion/HTTPClient/RespInputStream.java:144`
- Alert #426 — `system/src/main/java/com/percussion/HTTPClient/RespInputStream.java:140`
- Alert #425 — `system/src/main/java/com/percussion/HTTPClient/BufferedInputStream.java:115`
- ... and 2 more

### `java/regex-injection` (6 alerts)

- Alert #607 — `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1878`
- Alert #606 — `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1804`
- Alert #605 — `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1802`
- Alert #604 — `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1800`
- Alert #603 — `modules/utils/src/main/java/com/percussion/utils/string/PSFolderStringUtils.java:73`
- ... and 1 more

### `js/code-injection` (6 alerts)

- Alert #12 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/loader/2775/dojo.js:1015`
- Alert #11 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/io/RepubsubIO.js:111`
- Alert #10 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js.uncompressed.js:1127`
- Alert #9 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:617`
- Alert #8 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/hostenv_browser.js:29`
- ... and 1 more

### `java/unsafe-deserialization` (4 alerts)

- Alert #531 — `system/services/src/com/percussion/services/system/impl/PSEmailMessageHandler.java:92`
- Alert #530 — `system/services/src/com/percussion/services/notification/impl/PSMessageQueueService.java:117`
- Alert #529 — `system/services/src/com/percussion/services/notification/impl/PSMessageQueueService.java:109`
- Alert #528 — `system/business/src/com/percussion/rx/publisher/impl/PSPublishHandler.java:224`

### `js/insecure-randomness` (4 alerts)

- Alert #6 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/uuid/TimeBasedGenerator.js:197`
- Alert #5 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/uuid/TimeBasedGenerator.js:189`
- Alert #4 — `WebUI/war/views/PercCategoryView.js:880`
- Alert #3 — `WebUI/war/views/PercCategoryView.js:435`

### `js/unvalidated-dynamic-method-call` (3 alerts)

- Alert #337 — `system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:836`
- Alert #336 — `system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:518`
- Alert #335 — `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/codemirror-4.8/addon/tern/worker.js:20`

### `java/polynomial-redos` (2 alerts)

- Alert #609 — `deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:117`
- Alert #608 — `deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:110`

### `java/ssrf` (2 alerts)

- Alert #432 — `system/src/main/java/com/percussion/xml/PSDtdTree.java:204`
- Alert #431 — `deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:461`

### `java/weak-cryptographic-algorithm` (2 alerts)

- Alert #503 — `modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:94`
- Alert #502 — `modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:65`

### `js/polynomial-redos` (2 alerts)

- Alert #414 — `cui/components/requirejs-text/text.js:34`
- Alert #413 — `cui/components/requirejs-text/text.js:33`

### `java/insecure-trustmanager` (1 alerts)

- Alert #594 — `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java:383`

### `java/ldap-injection` (1 alerts)

- Alert #595 — `system/src/main/java/com/percussion/security/PSJndiGroupProvider.java:200`

### `java/redos` (1 alerts)

- Alert #610 — `modules/extensions-main/src/main/java/com/percussion/extensions/translations/PSFormEncodeDecodeHelper.java:170`

### `java/static-initialization-vector` (1 alerts)

- Alert #569 — `modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:99`

### `java/tainted-numeric-cast` (1 alerts)

- Alert #430 — `modules/utils/src/main/java/com/percussion/services/guidmgr/data/PSGuid.java:281`

### `java/unvalidated-url-forward` (1 alerts)

- Alert #568 — `modules/servletutils/src/main/java/com/percussion/utils/servlet/PSServletUtils.java:247`

### `js/clear-text-logging` (1 alerts)

- Alert #334 — `modules/perc-qa-automation/frontend/tests/login.spec.js:33`

### `js/double-escaping` (1 alerts)

- Alert #312 — `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/tests/io/test_ScriptSrcIO.html:246`
