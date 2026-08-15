# CodeQL Triage Inventory — 8.1.x (main)

Every open Critical/High alert gets exactly one row here. The pipeline gates (`scripts/verify-triage-inventory.py`, `scripts/verify-valid-fixes.py`) read this file.

Generated: 2026-08-15T20:01:00Z
Source: docs/ai-generated/tasks/8.1.x-codeql-baseline/alerts.md

## Summary

- Total open alerts: 75
- false-positive: 2
- fix: 73

Schema (per spec 004 C1):

| # | alert_id | rule_id | severity | file_path | module_owner | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
|---|----------|---------|----------|-----------|--------------|-------------------------|---------------|------------------|-----------|-------|
| 1 | 704 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:216 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 2 | 564 | java/xss | high | system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:78 | system | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore system/release/tomcat/Tomcat/webapps/tomcat-docs/** (#justification: vendored Tomcat sample app (Hello.java + JSP demos); runtime defense not applicable) @ line 44 |
| 3 | 563 | java/xss | high | system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:77 | system | false-positive | no action; path-ignored in .github/codeql/codeql-config.yml | merged |  | paths-ignore system/release/tomcat/Tomcat/webapps/tomcat-docs/** (#justification: vendored Tomcat sample app (Hello.java + JSP demos); runtime defense not applicable) @ line 44 |
| 4 | 477 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:519 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 5 | 476 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:518 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 6 | 474 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:403 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 7 | 467 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:182 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 8 | 457 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:451 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 9 | 456 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:419 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 10 | 455 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:364 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 11 | 454 | java/path-injection | high | projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:257 | projects/sitemanage | fix | code fix required | TBD |  |  |
| 12 | 398 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/delivery/bw-corona.html:730 | system | fix | code fix required | TBD |  |  |
| 13 | 397 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/xsl/encoding/Yahoo-EUC-JP.xhtm:46 | system | fix | code fix required | TBD |  |  |
| 14 | 396 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/delivery/bw-corona.html:1317 | system | fix | code fix required | TBD |  |  |
| 15 | 395 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/delivery/bw-corona.html:1318 | system | fix | code fix required | TBD |  |  |
| 16 | 394 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/delivery/bw-corona.html:1321 | system | fix | code fix required | TBD |  |  |
| 17 | 393 | js/functionality-from-untrusted-source | medium | system/UnitTestResources/com/percussion/delivery/bw-corona.html:1322 | system | fix | code fix required | TBD |  |  |
| 18 | 334 | js/clear-text-logging | high | modules/perc-qa-automation/frontend/tests/login.spec.js:33 | modules/perc-qa-automation | fix | code fix required | TBD |  |  |
| 19 | 246 | js/incomplete-sanitization | high | system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57 | system | fix | code fix required | TBD |  |  |
| 20 | 245 | js/incomplete-sanitization | high | system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57 | system | fix | code fix required | TBD |  |  |
| 21 | 244 | js/incomplete-sanitization | high | system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:57 | system | fix | code fix required | TBD |  |  |
| 22 | 243 | js/incomplete-sanitization | high | system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49 | system | fix | code fix required | TBD |  |  |
| 23 | 242 | js/incomplete-sanitization | high | system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49 | system | fix | code fix required | TBD |  |  |
| 24 | 241 | js/incomplete-sanitization | high | system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:49 | system | fix | code fix required | TBD |  |  |
| 25 | 235 | js/incomplete-sanitization | high | cui/widgets/app/app.viewmodel.js:87 | cui | fix | code fix required | TBD |  |  |
| 26 | 234 | js/incomplete-sanitization | high | cui/widgets/app/app.viewmodel.js:87 | cui | fix | code fix required | TBD |  |  |
| 27 | 206 | js/unsafe-jquery-plugin | medium | system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:256 | system | fix | code fix required | TBD |  |  |
| 28 | 181 | js/xss-through-dom | high | system/UnitTestResources/com/percussion/delivery/bw-corona.html:1310 | system | fix | code fix required | TBD |  |  |
| 29 | 180 | js/xss-through-dom | high | system/Packages/perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js:231 | system | fix | code fix required | TBD |  |  |
| 30 | 179 | js/xss-through-dom | high | system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1610 | system | fix | code fix required | TBD |  |  |
| 31 | 178 | js/xss-through-dom | high | system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1606 | system | fix | code fix required | TBD |  |  |
| 32 | 177 | js/xss-through-dom | high | system/Packages/perc.widgets.image/sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js:396 | system | fix | code fix required | TBD |  |  |
| 33 | 176 | js/xss-through-dom | high | system/Packages/perc.widgets.image/SupportFile-rx_resources/widgets/image/js/jquery.imageAssetControl.js:396 | system | fix | code fix required | TBD |  |  |
| 34 | 175 | js/xss-through-dom | high | system/Packages/perc.widget.socialButtons/sys__UserDependency--rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:132 | system | fix | code fix required | TBD |  |  |
| 35 | 174 | js/xss-through-dom | high | system/Packages/perc.widget.socialButtons/SupportFile-rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:135 | system | fix | code fix required | TBD |  |  |
| 36 | 173 | js/xss-through-dom | high | system/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:227 | system | fix | code fix required | TBD |  |  |
| 37 | 172 | js/xss-through-dom | high | system/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:84 | system | fix | code fix required | TBD |  |  |
| 38 | 171 | js/xss-through-dom | high | system/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:226 | system | fix | code fix required | TBD |  |  |
| 39 | 170 | js/xss-through-dom | high | system/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:84 | system | fix | code fix required | TBD |  |  |
| 40 | 169 | js/xss-through-dom | high | system/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js:447 | system | fix | code fix required | TBD |  |  |
| 41 | 168 | js/xss-through-dom | high | system/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:181 | system | fix | code fix required | TBD |  |  |
| 42 | 167 | js/xss-through-dom | high | system/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:163 | system | fix | code fix required | TBD |  |  |
| 43 | 166 | js/xss-through-dom | high | system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:50 | system | fix | code fix required | TBD |  |  |
| 44 | 165 | js/xss-through-dom | high | system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:49 | system | fix | code fix required | TBD |  |  |
| 45 | 164 | js/xss-through-dom | high | system/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:48 | system | fix | code fix required | TBD |  |  |
| 46 | 163 | js/xss-through-dom | high | system/Packages/perc.gadget.siteframework/sys__UserDependency--cm/gadgets/repository/PercSiteFrameworkGadget/perc_sitewide_framework_gadget.js:120 | system | fix | code fix required | TBD |  |  |
| 47 | 162 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:488 | system | fix | code fix required | TBD |  |  |
| 48 | 161 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:487 | system | fix | code fix required | TBD |  |  |
| 49 | 160 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:486 | system | fix | code fix required | TBD |  |  |
| 50 | 159 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:473 | system | fix | code fix required | TBD |  |  |
| 51 | 158 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:472 | system | fix | code fix required | TBD |  |  |
| 52 | 157 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:471 | system | fix | code fix required | TBD |  |  |
| 53 | 156 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:490 | system | fix | code fix required | TBD |  |  |
| 54 | 155 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:489 | system | fix | code fix required | TBD |  |  |
| 55 | 154 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:488 | system | fix | code fix required | TBD |  |  |
| 56 | 153 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:475 | system | fix | code fix required | TBD |  |  |
| 57 | 152 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:474 | system | fix | code fix required | TBD |  |  |
| 58 | 151 | js/xss-through-dom | high | system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:473 | system | fix | code fix required | TBD |  |  |
| 59 | 143 | js/xss-through-dom | high | delivery/common/js/views/PercTagListView.js:115 | delivery | fix | code fix required | TBD |  |  |
| 60 | 142 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:144 | delivery | fix | code fix required | TBD |  |  |
| 61 | 141 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:140 | delivery | fix | code fix required | TBD |  |  |
| 62 | 140 | js/xss-through-dom | high | delivery/common/js/views/PercRssView.js:64 | delivery | fix | code fix required | TBD |  |  |
| 63 | 139 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:260 | delivery | fix | code fix required | TBD |  |  |
| 64 | 138 | js/xss-through-dom | high | delivery/common/js/views/PercRegistrationView.js:72 | delivery | fix | code fix required | TBD |  |  |
| 65 | 137 | js/xss-through-dom | high | delivery/common/js/views/PercMostReadBlogPostsView.js:105 | delivery | fix | code fix required | TBD |  |  |
| 66 | 136 | js/xss-through-dom | high | delivery/common/js/views/PercMostReadBlogPostsView.js:70 | delivery | fix | code fix required | TBD |  |  |
| 67 | 135 | js/xss-through-dom | high | delivery/common/js/views/PercCategoryListView.js:192 | delivery | fix | code fix required | TBD |  |  |
| 68 | 134 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:279 | delivery | fix | code fix required | TBD |  |  |
| 69 | 133 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:153 | delivery | fix | code fix required | TBD |  |  |
| 70 | 132 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:191 | delivery | fix | code fix required | TBD |  |  |
| 71 | 131 | js/xss-through-dom | high | delivery/common/js/views/PercBlogPostView.js:144 | delivery | fix | code fix required | TBD |  |  |
| 72 | 130 | js/xss-through-dom | high | delivery/common/js/views/PercArchiveListView.js:131 | delivery | fix | code fix required | TBD |  |  |
| 73 | 90 | js/html-constructed-from-input | medium | system/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:1327 | system | fix | code fix required | TBD |  |  |
| 74 | 89 | js/html-constructed-from-input | medium | system/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:255 | system | fix | code fix required | TBD |  |  |
| 75 | 88 | js/html-constructed-from-input | medium | system/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:255 | system | fix | code fix required | TBD |  |  |
