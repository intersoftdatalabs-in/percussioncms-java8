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
package com.percussion.soln.jcr.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.junit.Test;

/**
 * Unit tests for {@link ValueUtil} backed by {@link com.percussion.utils.jsr170.PSValueFactory}.
 */
public class ValueUtilTest {

  @Test
  public void roundTripsAllSupportedPropertyTypes() throws Exception {
    Value strVal = ValueUtil.dataToJCRValue(new ValueData("hello"));
    assertEquals(PropertyType.STRING, strVal.getType());
    assertEquals("hello", ValueUtil.jcrValueToData(strVal).getStringData());

    Value boolVal = ValueUtil.dataToJCRValue(new ValueData(true));
    assertEquals(PropertyType.BOOLEAN, boolVal.getType());
    assertTrue(ValueUtil.jcrValueToData(boolVal).getBooleanData());

    Calendar cal = new GregorianCalendar(2026, Calendar.NOVEMBER, 19, 12, 30, 0);
    Value dateVal = ValueUtil.dataToJCRValue(new ValueData(cal));
    assertEquals(PropertyType.DATE, dateVal.getType());
    assertEquals(
        cal.getTimeInMillis(), ValueUtil.jcrValueToData(dateVal).getDateData().getTimeInMillis());

    Value doubleVal = ValueUtil.dataToJCRValue(new ValueData(3.5d));
    assertEquals(PropertyType.DOUBLE, doubleVal.getType());
    assertEquals(3.5d, ValueUtil.jcrValueToData(doubleVal).getDoubleData(), 0.0001d);

    Value longVal = ValueUtil.dataToJCRValue(new ValueData(42L));
    assertEquals(PropertyType.LONG, longVal.getType());
    assertEquals(Long.valueOf(42L), ValueUtil.jcrValueToData(longVal).getLongData());
  }

  @Test
  public void convertsListsAndObjects() throws Exception {
    Value[] values =
        ValueUtil.dataToJCRValues(Arrays.asList(new ValueData("a"), new ValueData(9L)));
    assertEquals(2, values.length);
    assertEquals("a", values[0].getString());
    assertEquals(9L, values[1].getLong());

    assertEquals(PropertyType.LONG, ValueUtil.createValueData(Integer.valueOf(7)).getType());
    assertEquals(PropertyType.DOUBLE, ValueUtil.createValueData(Float.valueOf(1.25f)).getType());
  }
}
