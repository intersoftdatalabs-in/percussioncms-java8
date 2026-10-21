# Issue #254: Pages by Status / Assets by Status Gadgets - Search Criteria Deserialization Error

## Problem

The "Pages by Status: Default Workflow - Pending (All Sites)" and the related
"Assets by Status" dashboard gadgets fail to load their content after login on
8.1.8. The browser console shows `POST /Rhythmyx/services/pagemanagement/page/searchPageByStatus 400 (Bad Request)` and the gadgets render a deserialization error.

### Backend error
```
HTTP 500 Internal Server Error
Cannot deserialize value of type `java.lang.String` from Array value
(token `JsonToken.START_ARRAY`)
at [Source: REDACTED ...; line: 1, column: 136] (through reference chain:
com.percussion.search.management.data.PSSearchCriteria["searchFields"]
-> java.util.LinkedHashMap["entry"])
```

### Browser stack points to
- `perc_workflow_status_gadget.js` (`loadGadget`)
- `perc_dashboard.packed.min.js` (`getAsyncSearchResult`, `makeAjaxRequest`)

## Root cause

- The Java side `com.percussion.searchmanagement.data.PSSearchCriteria.searchFields`
  is declared as `Map<String, String>` (see
  `projects/sitemanage/src/main/java/com/percussion/searchmanagement/data/PSSearchCriteria.java:42`).
- The CXF/Jackson-based REST endpoint `PSPageRestService.search(...)`
  (`/Rhythmyx/services/pagemanagement/page/searchPageByStatus`) deserializes the
  JSON request body directly into `PSSearchCriteria`. For `Map<String, String>`,
  Jackson expects a JSON object whose keys are the map's keys and whose values
  are strings:
  ```json
  "searchFields": {
    "templateid": "1",
    "sys_workflowid": "6",
    "sys_contentstateid": "4",
    "sys_contentlastmodifier": "admin1"
  }
  ```
- Instead, the dashboard gadget JS posted the search fields as a JAXB/CXF
  `Map` envelope `{ "entry": [ { "key": ..., "value": ... }, ... ] }`. Jackson
  parses the outer object as a `LinkedHashMap`, then tries to coerce the value
  of the literal key `"entry"` (an array) into a `String`, producing the
  `MismatchedInputException` shown above.

## Fix

Change the front-end gadget code so the `searchFields` property is a flat JSON
object of `key -> value` pairs, matching the `Map<String, String>` that the
backend expects.

### Files changed
1. `system/Packages/perc.gadget.pageByStatus/.../perc_workflow_status_gadget.js`
   - `loadGadget()` now converts the local `searchFields` array of `{key,value}`
     entries into a `{key: value}` object before attaching it to the
     `SearchCriteria` payload.
2. `system/Packages/perc.gadget.assetByStatus/.../PercAssetStatusGadget.js`
   - `loadGadget()` applies the same conversion.
3. `cui/widgets/search/search.viewmodel.js`
   - The Finder `Search` view model had the identical `{"entry":[...]}` pattern
     and is fixed in the same way so the front-end Finder dialog continues to
     populate `PSSearchCriteria.searchFields` correctly.

The service-layer POJO (`PSSearchCriteria`) is intentionally **not** changed;
keeping it as `Map<String, String>` matches existing callers such as
`PSSearchService.getContentIdsForSearchByStatus(...)` which iterates
`searchFields.entrySet()` to build Lucene `PSWSSearchField`s.

## Verification

1. Inspect the JSON on the wire - `searchFields` must be a flat JSON object,
   not `{"entry":[...]}`.
2. Trigger the gadgets on the CMS Dashboard - "Pages by Status" and "Assets
   by Status" - both should return successful JSON (`PagedItemList`) and render
   their tables instead of the deserialization error.
3. Open the Finder search dialog - advanced search fields should still be sent
   correctly.

## Risk

Low. JS-only change in static assets. The previous JSON shape was never
deserializable by the current Jackson configuration of the REST service, so the
old payload was already broken; the new payload is the one the backend has
always expected.

## Follow-up (NOT addressed by PR #255)

PR #255 is deliberately scoped to the three front-end files above. The same
"Jackson flat map vs. JAXB envelope" mismatch that caused #254 also appears in
other parts of the codebase. These are **out of scope for #254** and need their
own issue/triage.

### `PSMapWrapper` family

`com.percussion.share.data.PSMapWrapper`
(`projects/sitemanage/src/main/java/com/percussion/share/data/PSMapWrapper.java:54`)
declares `entries` as a plain `Map<String, String>` with **no JAXB adapter
annotation**. Under the active `JacksonContextResolver`
(`projects/sitemanage/src/main/java/com/percussion/sitemanage/json/JacksonContextResolver.java`,
which sets `WRAP_ROOT_VALUE`/`UNWRAP_ROOT_VALUE` and pairs
`JaxbAnnotationIntrospector` with `JacksonAnnotationIntrospector`), the wire
format is therefore the flat `{"psmap":{"entries":{"k1":"v1"}}}`.

But several front-end callers still speak the legacy JAXB envelope:

| Call site | File:line | Behavior |
|---|---|---|
| `encryptString` / `decryptString` | `WebUI/war/services/PercUtilService.js:64,162` | **sends** `{"psmap":{"entries":{"entry":{"key":"string","value":str}}}}` and **reads** `result.data.psmap.entries.entry.value` |
| `encryptStrings` / `decryptStrings` | `system/Packages/perc.widget.form/.../PercFormController.js:1778` | sends the same envelope |
| `getSaaSSiteNames` / `getCopySiteInfo` | `WebUI/war/widgets/perc_site_map.js:628,651`, `WebUI/war/plugins/PercCopySiteDialog.js:39` | **reads** `psmap.entries` via `convertMapToArray` |
| Google Analytics setup gadget | `system/Packages/perc.gadget.googleSetup/.../perc_google_setup_gadget.xml:548-549` | **reads** `result.data.psmap.entries.entry` via `$.perc_utils.convertCXFArray` |

Server endpoints involved:
- `PSUtilityRestService.encryptString / decryptString / encryptStrings / decryptStrings`
  (`projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSUtilityRestService.java:98,113,129,147`)
- `PSSiteDataRestService.getCopySiteInfo / getSaaSSiteNames`
  (`projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:242,261`)
- `PSMonitor.getStats()`
  (`projects/sitemanage/src/main/java/com/percussion/monitor/service/PSMonitor.java:30`)

### `PSPathItem.typeProperties`

`PSPathItem.typeProperties`
(`projects/sitemanage/src/main/java/com/percussion/pathmanagement/data/PSPathItem.java:54`)
reuses the same `PSMapWrapper` shape. Its reader is
`WebUI/war/widgets/PercSiteTemplatesWidget.js:700`
(`data.childrenInPage[j].typeProperties.entries`), so **any** normalization of
the `PSMapWrapper` wire format must update this reader too.

### Not affected: `PSPathItem.displayProperties`

`PSPathItem.displayProperties` (`PSPathItem.java:50`) is annotated
`@XmlJavaTypeAdapter(PSMapAdapter.class)` and adapts through
`PSPathItemDisplayProperties` whose field is renamed `@XmlElement(name="column")`
(`.../PSPathItemDisplayProperties.java:32`). `JacksonJaxbJsonProvider` (configured
at `projects/sitemanage/src/main/resources/.../projects/sitemanage-beans.xml:216`)
honors both, so the `columnData.column` readers at
`WebUI/war/plugins/PercDataTableUtil.js:52`,
`WebUI/war/widgets/PercFinderListView/PercFinderListView.js:188` and
`WebUI/war/widgets/PercSiteTemplatesWidget.js:662` are **not** part of this bug
family. Worth watching, but no evidence today that it is broken.

### Recommended triage

Open a sibling issue, e.g.
"**#254 sibling: `PSMapWrapper` / `PSPathItem.typeProperties` endpoints inconsistent
with Jackson wire format**", and decide between two options as a single unit:

- **(a)** change the JS callers to send/read flat maps everywhere (the same fix
  PR #255 applied), or
- **(b)** keep the JAXB envelope by adding an adapter so the CXF/Jackson provider
  emits/consumes `{"entry":[...]}` for `Map<String,String>`-shaped POJOs.

Do not change the server side first without aligning the JS: the send/read
asymmetry above means a unilateral server-side change would silently swap the
failure mode rather than fix it.

### Testing caveat for any future fix

`PSUtilityRestService` is covered by
`projects/sitemanage/src/test/java/com/percussion/utils/web/service/PSUtilityServiceRestTest.java`,
but those tests exercise the Java-side `PSMapWrapper` **directly** and therefore
**bypass HTTP**. They will not detect a JS-vs-Jackson wire-format mismatch. A
follow-up fix needs a test that posts a real JSON body to the endpoint to be
meaningful.
