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
