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
package com.percussion.services.guidmgr.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tainted-numeric-cast regression test for {@link PSGuid}. Verifies that the string-form
 * constructor explicitly validates the type-id component is within the int range before the
 * narrowing cast (CodeQL java/tainted-numeric-cast alert #430 on 8.1.x).
 */
public class PSGuidTaintedNumericCastTest {

  @Test
  public void acceptsInRangeTypeId() {
    // PSTypeEnum values have small positive ordinals; a typical 3-component GUID must parse
    // cleanly.
    PSGuid g = new PSGuid("1-2-3");
    assertNotNull(g);
    assertEquals(2, g.getType());
  }

  @Test
  public void rejectsTypeIdAboveIntMax() {
    // 5_000_000_000L is > Integer.MAX_VALUE (~2.147e9). The new guard rejects with an explicit
    // "out of range" message before the narrowing cast; without the guard the value silently
    // wraps to a wrapped int and reaches PSTypeEnum.valueOf() with a null result.
    try {
      new PSGuid("1-5000000000-3");
      fail("expected IllegalArgumentException for out-of-int-range typeid");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "Type id out of range for PSTypeEnum ordinal: 5000000000", expected.getMessage());
    }
  }

  @Test
  public void rejectsTypeIdBelowIntMin() {
    // The public string form is hyphen-delimited. Apache Commons Lang StringUtils.split
    // collapses consecutive '-' so "1--5000000000-3" tokenizes as host=1, typeid=5000000000,
    // uuid=3 (the minus is a delimiter, not a sign). The value is still outside the int
    // range and must hit the same guard as rejectsTypeIdAboveIntMax.
    try {
      new PSGuid("1--5000000000-3");
      fail("expected IllegalArgumentException for out-of-int-range typeid");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "Type id out of range for PSTypeEnum ordinal: 5000000000", expected.getMessage());
    }
  }

  @Test
  public void rejectsTypeIdWithUnmatchedOrdinal() {
    // In-range but no PSTypeEnum has this ordinal - must throw IllegalArgumentException from
    // assemble() with null type (existing behaviour preserved by the explicit guard).
    try {
      new PSGuid("1-999999-3");
      fail("expected IllegalArgumentException for unmatched ordinal");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }
}
