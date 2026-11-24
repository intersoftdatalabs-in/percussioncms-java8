/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.data.PSSearchResults;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Regression tests for {@link PSMetadataQuery#getTotalMaxResults()} and server {@code queryLimit}
 * enforcement on both total result counts and paginated slices (#428).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSMetadataTotalMaxResultsTest {

  private static final String SITE = "QA-Widgets";
  private static final String FOLDER = "/qa-pal/";

  @Autowired private IPSMetadataIndexerService indexer;

  @Autowired private IPSMetadataQueryService queryService;

  @Autowired private IPSMetadataRestService restService;

  @BeforeEach
  public void setUp() {
    indexer.deleteAllMetadataEntries();
  }

  @AfterEach
  public void tearDown() {
    indexer.deleteAllMetadataEntries();
  }

  @Test
  public void testTotalMaxResultsCapsTotalAndBlocksPagesBeyondCap() {
    seedPages("Alpha", "Bravo", "Charlie");

    // Page 1: 1 per page, max 2 total out of 3 matching pages -> totalEntries must be 2.
    PSMetadataQuery page1Query = createPageQuery(1, 2, 0, true, "linktext_lower asc");
    PSSearchResults page1 = restService.get(page1Query);
    assertNotNull(page1);
    assertEquals(Integer.valueOf(2), page1.getTotalEntries());
    assertEquals(1, page1.getResults().size());
    assertEquals("QA PAL Alpha", page1.getResults().get(0).getLinktext());

    // Page 2: second and final allowed page.
    PSMetadataQuery page2Query = createPageQuery(1, 2, 1, false, "linktext_lower asc");
    PSSearchResults page2 = restService.get(page2Query);
    assertNotNull(page2);
    assertNull(page2.getTotalEntries());
    assertEquals(1, page2.getResults().size());
    assertEquals("QA PAL Bravo", page2.getResults().get(0).getLinktext());

    // Page 3 (direct request beyond totalMaxResults): returns 0 rows and capped totalEntries.
    PSMetadataQuery page3Query = createPageQuery(1, 2, 2, true, "linktext_lower asc");
    PSSearchResults page3 = restService.get(page3Query);
    assertNotNull(page3);
    assertEquals(Integer.valueOf(2), page3.getTotalEntries());
    assertTrue(page3.getResults().isEmpty());

    PSMetadataQuery page3NoTotalQuery = createPageQuery(1, 2, 2, false, "linktext_lower asc");
    PSSearchResults page3NoTotal = restService.get(page3NoTotalQuery);
    assertNotNull(page3NoTotal);
    assertNull(page3NoTotal.getTotalEntries());
    assertTrue(page3NoTotal.getResults().isEmpty());
  }

  @Test
  public void testPartialLastPageCappedAtRemainingTotalMaxResults() {
    seedPages("Alpha", "Bravo", "Charlie", "Delta", "Echo");

    // Sort on property (dcterms:created desc) to also exercise SORTTYPE.PROPERTY path.
    PSMetadataQuery page1Query =
        createPageQuery(2, 3, 0, true, "dcterms:created desc, linktext_lower asc");
    PSSearchResults page1 = restService.get(page1Query);
    assertEquals(Integer.valueOf(3), page1.getTotalEntries());
    assertEquals(2, page1.getResults().size());
    assertEquals("QA PAL Echo", page1.getResults().get(0).getLinktext());
    assertEquals("QA PAL Delta", page1.getResults().get(1).getLinktext());

    // Page 2 starts at index 2 with totalMaxResults 3 -> only 1 row remaining, not 2.
    PSMetadataQuery page2Query =
        createPageQuery(2, 3, 2, false, "dcterms:created desc, linktext_lower asc");
    PSSearchResults page2 = restService.get(page2Query);
    assertNull(page2.getTotalEntries());
    assertEquals(1, page2.getResults().size());
    assertEquals("QA PAL Charlie", page2.getResults().get(0).getLinktext());

    // Page 3 starts at index 4 >= 3 -> empty.
    PSMetadataQuery page3Query =
        createPageQuery(2, 3, 4, false, "dcterms:created desc, linktext_lower asc");
    PSSearchResults page3 = restService.get(page3Query);
    assertTrue(page3.getResults().isEmpty());
  }

  @Test
  public void testTotalMaxResultsLargerThanActualMatchesPreservesActualCount() {
    seedPages("Alpha", "Bravo", "Charlie");

    PSMetadataQuery page1Query = createPageQuery(2, 10, 0, true, "linktext_lower asc");
    PSSearchResults page1 = restService.get(page1Query);
    assertEquals(Integer.valueOf(3), page1.getTotalEntries());
    assertEquals(2, page1.getResults().size());

    PSMetadataQuery page2Query = createPageQuery(2, 10, 2, false, "linktext_lower asc");
    PSSearchResults page2 = restService.get(page2Query);
    assertEquals(1, page2.getResults().size());
    assertEquals("QA PAL Charlie", page2.getResults().get(0).getLinktext());
  }

  @Test
  public void testMaxResultsLargerThanTotalMaxResultsStillHonorsTotalCapAndStartIndex()
      throws Exception {
    seedPages("Alpha", "Bravo", "Charlie", "Delta", "Echo");

    PSMetadataQuery page1Query = createPageQuery(5, 2, 0, true, "linktext_lower asc");
    PSPair<List<IPSMetadataEntry>, Integer> page1 = queryService.executeQuery(page1Query);
    assertEquals(Integer.valueOf(2), page1.getSecond());
    assertEquals(2, page1.getFirst().size());
    assertEquals("QA PAL Alpha", page1.getFirst().get(0).getLinktext());
    assertEquals("QA PAL Bravo", page1.getFirst().get(1).getLinktext());

    PSMetadataQuery offsetQuery = createPageQuery(5, 2, 1, false, "linktext_lower asc");
    PSPair<List<IPSMetadataEntry>, Integer> offsetPage = queryService.executeQuery(offsetQuery);
    assertEquals(1, offsetPage.getFirst().size());
    assertEquals("QA PAL Bravo", offsetPage.getFirst().get(0).getLinktext());
  }

  @Test
  public void testServerQueryLimitCapsTotalAndPagesWhenClientRequestsMore() throws Exception {
    seedPages("Alpha", "Bravo", "Charlie", "Delta", "Echo");

    Integer originalLimit = queryService.getQueryLimit();
    queryService.setQueryLimit(3);
    try {
      PSMetadataQuery page1Query = createPageQuery(2, 10, 0, true, "linktext_lower asc");
      PSPair<List<IPSMetadataEntry>, Integer> page1 = queryService.executeQuery(page1Query);
      assertEquals(Integer.valueOf(3), page1.getSecond());
      assertEquals(2, page1.getFirst().size());

      PSMetadataQuery page2Query = createPageQuery(2, 10, 2, false, "linktext_lower asc");
      PSPair<List<IPSMetadataEntry>, Integer> page2 = queryService.executeQuery(page2Query);
      assertEquals(1, page2.getFirst().size());
      assertEquals("QA PAL Charlie", page2.getFirst().get(0).getLinktext());

      PSMetadataQuery page3Query = createPageQuery(2, 10, 4, false, "linktext_lower asc");
      PSPair<List<IPSMetadataEntry>, Integer> page3 = queryService.executeQuery(page3Query);
      assertTrue(page3.getFirst().isEmpty());
    } finally {
      queryService.setQueryLimit(originalLimit);
    }
  }

  private void seedPages(String... suffixes) {
    List<IPSMetadataEntry> entries = new ArrayList<>();
    long baseTime = 1700000000000L;
    for (int i = 0; i < suffixes.length; i++) {
      String suffix = suffixes[i];
      String name = suffix.toLowerCase() + ".html";
      String pagepath = "/" + SITE + FOLDER + name;
      String linkText = "QA PAL " + suffix;
      PSDbMetadataEntry entry = new PSDbMetadataEntry(name, FOLDER, pagepath, "page", SITE);
      entry.setLinktext(linkText);
      entry.addProperty(new PSDbMetadataProperty("dcterms:title", linkText));
      entry.addProperty(
          new PSDbMetadataProperty("dcterms:created", new Date(baseTime + i * 2000L)));
      entries.add(entry);
    }
    indexer.save(entries);
  }

  private PSMetadataQuery createPageQuery(
      int maxResults, int totalMaxResults, int startIndex, boolean returnTotal, String orderBy) {
    PSMetadataQuery query = new PSMetadataQuery();
    query.setCriteria(
        Arrays.asList("type = 'page'", "site = '" + SITE + "'", "linktext LIKE '%QA PAL%'"));
    query.setMaxResults(maxResults);
    query.setTotalMaxResults(totalMaxResults);
    query.setStartIndex(startIndex);
    query.setReturnTotalEntries(returnTotal);
    query.setOrderBy(orderBy);
    return query;
  }
}
