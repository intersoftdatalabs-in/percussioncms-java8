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

package com.percussion.utils.string;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.percussion.security.SecureStringUtils;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSFolderStringUtilsTest {

  @Rule public TemporaryFolder temporaryFolder = TemporaryFolder.builder().build();

  @Test
  public void testFolderStringUtils() throws IOException {

    File parentA = temporaryFolder.newFolder("parentA");
    File parentB = temporaryFolder.newFolder("parentB");
    File childA = temporaryFolder.newFolder("parentA", "childA");

    assertFalse(SecureStringUtils.isChildOfFilePath(parentA.toPath(), parentB.toPath()));
    assertTrue(SecureStringUtils.isChildOfFilePath(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isChildOfFilePath(parentB.toPath(), childA.toPath()));

    assertTrue(SecureStringUtils.isSameFileAs(parentA.toPath(), parentA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), parentB.toPath()));
  }

  /**
   * Regression test for the {@code java/regex-injection} CodeQL cluster (alerts #602-#607). The
   * post-fix {@link PSFolderStringUtils#getFolderPatterns(String)} splits each path on the
   * documented {@code %} wildcard and wraps every literal segment in {@link Pattern#quote}, so
   * regex meta-characters in user input must be treated as literals rather than pattern syntax.
   */
  @Test
  public void testGetFolderPatternsTreatsMetaCharactersAsLiterals() {
    String[] adversarialInputs = {
      "a.b.c", // '.' must be literal, not "any char"
      "site(name)", // '(' and ')' must be literal
      "site$name", // '$' must be literal
      "site^name", // '^' must be literal
      "site|name", // '|' must be literal
      "site[abc]name", // character class brackets must be literal
      "site+name", // '+' must be literal
      "site%name", // '%' is the documented wildcard
    };

    for (String adversarial : adversarialInputs) {
      Pattern[] patterns = PSFolderStringUtils.getFolderPatterns(adversarial);
      assertTrue("expected at least one pattern for input: " + adversarial, patterns.length == 1);
      String compiled = patterns[0].pattern();
      // The pattern source must contain a \Q..\E wrapper (Pattern.quote marker) so that
      // regex-injection-aware scanners (CodeQL) recognize the input as quoted.
      assertTrue(
          "Pattern.quote was not applied for input: '" + adversarial + "'",
          compiled.contains("\\Q"));

      Pattern p = patterns[0];
      String literalInput = adversarial + "/";
      assertTrue(
          "literal match should succeed for: '" + adversarial + "'",
          p.matcher(literalInput).matches());

      // A sentinel that shares the structure but swaps each meta-char for 'X' must NOT match
      // if the meta-chars are properly quoted (except '%', which is the documented wildcard).
      String sentinel = adversarial.replaceAll("[^A-Za-z0-9/%]", "X").replace('%', 'X') + "/";
      boolean shouldMatch = adversarial.contains("%");
      if (shouldMatch) {
        // '%' wildcards still match; only assert non-wildcard inputs fail the sentinel.
        continue;
      }
      assertFalse(
          "regex meta characters must not match arbitrary chars for input: '" + adversarial + "'",
          p.matcher(sentinel).matches());
    }
  }

  /**
   * Verifies the documented {@code %} wildcard still compiles to {@code .*} and matches arbitrary
   * intermediate path segments after the regex-injection fix.
   */
  @Test
  public void testGetFolderPatternsPreservesPercentWildcard() {
    Pattern[] patterns = PSFolderStringUtils.getFolderPatterns("foo%bar");
    assertTrue("expected exactly one pattern", patterns.length == 1);
    Pattern p = patterns[0];
    assertTrue("pattern should contain the .* wildcard", p.pattern().contains(".*"));
    assertTrue(p.matcher("fooanythingbar/").matches());
    assertTrue(p.matcher("foobar/").matches()); // % matches empty
    assertFalse(p.matcher("foo/ba/").matches()); // too short
  }

  /**
   * Verifies the {@code ;} separator still splits into one pattern per path and that blank input
   * yields an empty (never null) array.
   */
  @Test
  public void testGetFolderPatternsMultiplePathsAndBlankInput() {
    Pattern[] patterns = PSFolderStringUtils.getFolderPatterns("/a/b;c/d");
    assertTrue("expected two patterns", patterns.length == 2);
    assertTrue(patterns[0].matcher("/a/b/").matches());
    assertTrue(patterns[1].matcher("c/d/").matches());

    assertTrue(PSFolderStringUtils.getFolderPatterns(null).length == 0);
    assertTrue(PSFolderStringUtils.getFolderPatterns("").length == 0);
    assertTrue(PSFolderStringUtils.getFolderPatterns("   ").length == 0);
  }
}
