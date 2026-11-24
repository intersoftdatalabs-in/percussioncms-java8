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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.data.PSMetadataBlogResult;
import com.percussion.delivery.metadata.data.PSMetadataDatedEntries;
import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.data.PSMetadataRestEntry;
import com.percussion.delivery.metadata.data.PSSearchResults;
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
 * Regression tests verifying that {@link PSDbMetadataProperty} {@code VALUETYPE.TEXT} properties
 * (such as {@code dcterms:abstract}) can be read from detached {@link PSDbMetadataEntry} instances
 * after the Hibernate session closes.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSMetadataTextValueDetachedTest {

  private static final String MORE_LINK_SUMMARY =
      "<p>Intro paragraph for the blog post.<a class=\"perc-more-link\"></a></p>";

  @Autowired private IPSMetadataIndexerService indexer;

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
  public void testBlogListSearchWithMoreLinkSummaryAndPlainPost() {
    PSDbMetadataEntry postWithSummary =
        createBlogPost(
            "post-with-summary.html",
            "Post With Summary",
            new Date(1700000100000L),
            MORE_LINK_SUMMARY);
    PSDbMetadataEntry postWithoutSummary =
        createBlogPost("post-plain.html", "Plain Post", new Date(1700000000000L), null);

    indexer.save(Arrays.<IPSMetadataEntry>asList(postWithSummary, postWithoutSummary));

    PSMetadataQuery query = new PSMetadataQuery();
    query.setCriteria(
        Arrays.asList(
            "type = 'page'",
            "site = 'QA-Widgets'",
            "folder LIKE '/qa-blog/%'",
            "dcterms:source = 'qa-blog-post'"));
    query.setMaxResults(10);
    query.setOrderBy("dcterms:created desc, linktext asc");
    query.setReturnTotalEntries(true);

    PSSearchResults results = restService.get(query);
    assertNotNull(results);
    assertEquals(Integer.valueOf(2), results.getTotalEntries());
    assertEquals(2, results.getResults().size());

    PSMetadataRestEntry first = results.getResults().get(0);
    assertEquals("Post With Summary", first.getLinktext());
    assertEquals(MORE_LINK_SUMMARY, first.getProperties().get("dcterms:abstract"));

    PSMetadataRestEntry second = results.getResults().get(1);
    assertEquals("Plain Post", second.getLinktext());
    assertNull(second.getProperties().get("dcterms:abstract"));
  }

  @Test
  public void testLongTextAbstractAccessibleAcrossDetachedRestAndDaoPaths() {
    StringBuilder longSummaryBuilder = new StringBuilder();
    for (int i = 0; i < 4500; i++) {
      longSummaryBuilder.append('a');
    }
    longSummaryBuilder.append("<a class=\"perc-more-link\"></a>");
    String longSummary = longSummaryBuilder.toString();

    PSDbMetadataEntry post =
        createBlogPost(
            "long-summary.html", "Long Summary Post", new Date(1700000200000L), longSummary);
    indexer.save(post);

    PSMetadataQuery query = new PSMetadataQuery();
    query.setCriteria(Arrays.asList("site = 'QA-Widgets'", "folder LIKE '/qa-blog/%'"));
    query.setMaxResults(10);
    query.setOrderBy("dcterms:created desc");
    query.setCurrentPageId(post.getPagepath());

    PSSearchResults searchResults = restService.get(query);
    assertEquals(1, searchResults.getResults().size());
    assertEquals(
        longSummary, searchResults.getResults().get(0).getProperties().get("dcterms:abstract"));

    PSMetadataBlogResult blogResult = restService.getBlog(query);
    assertNotNull(blogResult.getCurrent());
    assertEquals(longSummary, blogResult.getCurrent().getProperties().get("dcterms:abstract"));

    PSMetadataDatedEntries datedEntries = restService.getDatedEntries(query);
    assertEquals(1, datedEntries.getEvents().size());
    assertEquals(longSummary, datedEntries.getEvents().get(0).getSummary());

    IPSMetadataEntry found = indexer.findEntry(post.getPagepath());
    assertNotNull(found);
    assertEquals(longSummary, extractAbstract(found));

    List<IPSMetadataEntry> all = indexer.getAllEntries();
    assertEquals(1, all.size());
    assertEquals(longSummary, extractAbstract(all.get(0)));
  }

  private PSDbMetadataEntry createBlogPost(
      String name, String title, Date created, String summaryHtml) {
    String folder = "/qa-blog/2026/11/";
    String pagepath = "/QA-Widgets" + folder + name;
    PSDbMetadataEntry entry = new PSDbMetadataEntry(name, folder, pagepath, "page", "QA-Widgets");
    entry.setLinktext(title);
    entry.addProperty(new PSDbMetadataProperty("dcterms:title", title));
    entry.addProperty(new PSDbMetadataProperty("dcterms:created", created));
    entry.addProperty(new PSDbMetadataProperty("dcterms:source", "qa-blog-post"));
    if (summaryHtml != null) {
      entry.addProperty(new PSDbMetadataProperty("dcterms:abstract", VALUETYPE.TEXT, summaryHtml));
    }
    return entry;
  }

  private String extractAbstract(IPSMetadataEntry entry) {
    for (IPSMetadataProperty prop : new ArrayList<>(entry.getProperties())) {
      if ("dcterms:abstract".equals(prop.getName())) {
        assertEquals(VALUETYPE.TEXT, prop.getValuetype());
        assertEquals(((PSDbMetadataProperty) prop).getTextvalue(), prop.getValue());
        return prop.getStringvalue();
      }
    }
    return null;
  }
}
