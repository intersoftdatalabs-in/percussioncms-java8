/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.publisher.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.percussion.utils.jsr170.PSCollectionRangeIterator;
import com.percussion.utils.jsr170.PSStringValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.Test;

/** Unit tests for {@link PSQueryResultUtils} after replacing Jackrabbit's RowIteratorAdapter. */
public class PSQueryResultUtilsTest {

  private static final class StubRow implements Row {
    private final String title;

    StubRow(String title) {
      this.title = title;
    }

    @Override
    public Value[] getValues() {
      return new Value[] {new PSStringValue(title)};
    }

    @Override
    public Value getValue(String propertyName) {
      return "rx:sys_title".equals(propertyName) ? new PSStringValue(title) : null;
    }

    @Override
    public Node getNode() {
      return null;
    }

    @Override
    public Node getNode(String selectorName) {
      return null;
    }

    @Override
    public String getPath() {
      return "/" + title;
    }

    @Override
    public String getPath(String selectorName) {
      return "/" + title;
    }

    @Override
    public double getScore() {
      return 1.0;
    }

    @Override
    public double getScore(String selectorName) {
      return 1.0;
    }
  }

  private static final class StubRowIterator extends PSCollectionRangeIterator<Row>
      implements RowIterator {
    StubRowIterator(Collection<Row> rows) {
      super(rows);
    }

    @Override
    public Row nextRow() {
      return next();
    }
  }

  private static QueryResult makeQueryResult(int count) {
    final String[] cols = new String[] {"rx:sys_title"};
    final List<Row> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      rows.add(new StubRow("item-" + i));
    }
    return new QueryResult() {
      @Override
      public String[] getColumnNames() {
        return cols;
      }

      @Override
      public RowIterator getRows() {
        return new StubRowIterator(rows);
      }

      @Override
      public NodeIterator getNodes() throws RepositoryException {
        throw new UnsupportedOperationException("Not needed for test");
      }

      @Override
      public String[] getSelectorNames() {
        return new String[0];
      }
    };
  }

  @Test
  public void splitsRowsIntoChunksWithWorkingRowIteratorContract() throws Exception {
    QueryResult original = makeQueryResult(5);
    java.util.Iterator<QueryResult> chunks = PSQueryResultUtils.splitQueryResults(original, 2);

    List<QueryResult> collected = new ArrayList<>();
    while (chunks.hasNext()) {
      collected.add(chunks.next());
    }

    assertEquals(3, collected.size());

    QueryResult first = collected.get(0);
    assertArrayEquals(new String[] {"rx:sys_title"}, first.getColumnNames());
    assertArrayEquals(new String[0], first.getSelectorNames());
    RowIterator it0 = first.getRows();
    assertEquals(2L, it0.getSize());
    assertEquals(0L, it0.getPosition());
    assertTrue(it0.hasNext());
    assertEquals("item-0", it0.nextRow().getValue("rx:sys_title").getString());
    assertEquals(1L, it0.getPosition());
    assertEquals("item-1", it0.nextRow().getValue("rx:sys_title").getString());
    assertFalse(it0.hasNext());

    QueryResult second = collected.get(1);
    RowIterator it1 = second.getRows();
    it1.skip(1);
    assertEquals("item-3", it1.nextRow().getValue("rx:sys_title").getString());
    assertFalse(it1.hasNext());

    QueryResult third = collected.get(2);
    RowIterator it2 = third.getRows();
    assertEquals(1L, it2.getSize());
    assertEquals("item-4", it2.nextRow().getValue("rx:sys_title").getString());
    assertFalse(it2.hasNext());
  }

  @Test
  public void handlesEmptyQueryResult() throws Exception {
    java.util.Iterator<QueryResult> chunks =
        PSQueryResultUtils.splitQueryResults(makeQueryResult(0), 2);
    assertFalse(chunks.hasNext());
  }
}
