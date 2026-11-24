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
package com.percussion.pagemanagement.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Regression tests for issue #411 verifying {@link
 * PSPageChangeHandler#extractSummaryToMoreLink(String)} matches {@code perc-blog-more-link} markers
 * whether empty, padded with spaces/NBSP, single-quoted, or self-closing, and normalizes the
 * extracted marker to {@code <span class="perc-blog-more-link"></span>}.
 */
public class PSPageChangeHandlerMoreLinkTest {

  private static final String CANONICAL_MARKER = "<span class=\"perc-blog-more-link\"></span>";

  @Test
  public void shouldExtractAndNormalizeMarkerVariants() {
    String[] markers = {
      "<span class=\"perc-blog-more-link\"></span>",
      "<span class=\"perc-blog-more-link\"> </span>",
      "<span class=\"perc-blog-more-link\">  \t\n </span>",
      "<span class=\"perc-blog-more-link\">\u00A0</span>",
      "<span class=\"perc-blog-more-link\">&nbsp;</span>",
      "<span class=\"perc-blog-more-link\">&#160;</span>",
      "<span class=\"perc-blog-more-link\">&#xA0;</span>",
      "<span class=\"perc-blog-more-link\">&#x00a0;</span>",
      "<span class=\"perc-blog-more-link\"/>",
      "<span class=\"perc-blog-more-link\" />",
      "<span class='perc-blog-more-link'> </span>",
      "<SPAN CLASS=\"perc-blog-more-link\">&NBSP;</SPAN>"
    };

    for (String marker : markers) {
      String input = "<p>Lead paragraph " + marker + " trailing body text</p>";
      assertEquals(
          "Failed for marker: " + marker,
          "<p>Lead paragraph " + CANONICAL_MARKER,
          PSPageChangeHandler.extractSummaryToMoreLink(input));
    }
  }

  @Test
  public void shouldReturnNullWhenNoEmptyMoreLinkMarkerPresent() {
    assertNull(PSPageChangeHandler.extractSummaryToMoreLink(null));
    assertNull(PSPageChangeHandler.extractSummaryToMoreLink("<p>Plain summary without marker</p>"));
    assertNull(
        PSPageChangeHandler.extractSummaryToMoreLink(
            "<p>Text <span class=\"perc-blog-more-link\">keep this text</span></p>"));
    assertNull(
        PSPageChangeHandler.extractSummaryToMoreLink(
            "<p>Text <span class=\"other-class\"> </span></p>"));
  }
}
