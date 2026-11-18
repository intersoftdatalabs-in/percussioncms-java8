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
package com.percussion.pagemanagement.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.percussion.designmanagement.service.impl.PSFileSystemService;
import com.percussion.utils.jsr170.PSValueFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSPageUtilsTest {

  private static final String SPORTS_ID = "102839b5-e5ca-4c84-9fd5-b5f6d646121f";
  private static final String NEWS_ID = "202cff1c-a87d-4a3f-860b-9e0382eea1e0";
  private static final String LOCAL_ID = "303dff1c-a87d-4a3f-860b-9e0382eea1e0";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testProcessFrameworkContent() {
    PSPageUtils utils = new PSPageUtils();

    // Null and empty checks
    assertEquals("", utils.processFrameworkContent(null));
    assertEquals("", utils.processFrameworkContent(""));
    assertEquals("", utils.processFrameworkContent("   "));

    // Plain text / script tags / HTML should not be modified
    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent(
            "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>"));
    assertEquals(
        "console.log(\"hello\");", utils.processFrameworkContent("console.log(\"hello\");"));

    // CDN / Web URLs ending in .js should be wrapped in script tags
    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent("https://code.jquery.com/jquery-3.7.1.min.js"));
    assertEquals(
        "<script src=\"http://code.jquery.com/jquery-3.7.1.js?v=2\"></script>",
        utils.processFrameworkContent("http://code.jquery.com/jquery-3.7.1.js?v=2"));
    assertEquals(
        "<script src=\"//code.jquery.com/ui/1.13.3/jquery-ui.min.js\"></script>",
        utils.processFrameworkContent("//code.jquery.com/ui/1.13.3/jquery-ui.min.js"));

    // Relative URLs ending in .js
    assertEquals(
        "<script src=\"/js/custom.js\"></script>", utils.processFrameworkContent("/js/custom.js"));

    // CDN / Web URLs ending in .css should be wrapped in link tags
    assertEquals(
        "<link rel=\"stylesheet\" href=\"https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css\" />",
        utils.processFrameworkContent(
            "https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css"));
    assertEquals(
        "<link rel=\"stylesheet\" href=\"//example.com/styles.css?foo=bar#baz\" />",
        utils.processFrameworkContent("//example.com/styles.css?foo=bar#baz"));

    // URLs with other extensions or not looking like JS/CSS should not be modified
    assertEquals(
        "https://example.com/image.png",
        utils.processFrameworkContent("https://example.com/image.png"));
  }

  /**
   * Regression test for issue #284 ({@code sys_assembly.vm}): {@link
   * PSPageUtils#getCategoryLabel(String)} resolves UUID paths against {@code
   * rx_resources/category/category.xml} without triggering path-traversal errors.
   */
  @Test
  public void shouldResolveCategoryLabelFromCategoryXml() throws IOException {
    PSPageUtils utils = createCategoryUtils();

    assertEquals("/Categories/News", utils.getCategoryLabel("/Categories/" + NEWS_ID));
    assertEquals(
        "/Categories/News/Local",
        utils.getCategoryLabel("/Categories/" + NEWS_ID + "/" + LOCAL_ID));
    assertEquals("/Categories/unknown-id", utils.getCategoryLabel("/Categories/unknown-id"));
  }

  /**
   * Regression test for issue #284 ({@code percCategoryList}): {@link
   * PSPageUtils#getProcessedCategories(List)} builds a sorted label tree from page category
   * properties without throwing {@link IllegalArgumentException}.
   */
  @Test
  public void shouldBuildProcessedCategoriesTreeWithoutPathTraversalFailure() throws Exception {
    PSPageUtils utils = createCategoryUtils();
    Value[] categoryValues =
        new Value[] {
          PSValueFactory.createValue((Object) ("/Categories/" + SPORTS_ID)),
          PSValueFactory.createValue((Object) ("/Categories/" + NEWS_ID))
        };

    PSRenderAsset asset = new PSRenderAsset();
    asset.setNode(createCategoryPageNode(categoryValues));

    List<PSCategoryTree> result = utils.getProcessedCategories(Collections.singletonList(asset));
    assertNotNull(result);
    assertEquals(1, result.size());
    PSCategoryTree rootCategory = result.get(0);
    assertEquals("Categories", rootCategory.getCategory());
    assertEquals(2, rootCategory.getChildren().size());
    assertEquals("News", rootCategory.getChildren().get(0).getCategory());
    assertEquals("Sports", rootCategory.getChildren().get(1).getCategory());
  }

  /**
   * Regression test for issue #285 (Blog List / Blog Post widgets): {@link
   * PSPageUtils#alphaOrderCategory(Value[])} replaces category UUID paths with labels and sorts by
   * the resolved leaf label rather than leaving raw UUIDs in place.
   */
  @Test
  public void shouldReplaceCategoryIdsWithSortedLabelsInAlphaOrderCategory() throws Exception {
    PSPageUtils utils = createCategoryUtils();
    Value[] values =
        new Value[] {
          PSValueFactory.createValue((Object) ("/Categories/" + SPORTS_ID)),
          PSValueFactory.createValue((Object) ("/Categories/" + NEWS_ID))
        };

    utils.alphaOrderCategory(values);

    assertEquals("/Categories/News", values[0].getString());
    assertEquals("/Categories/Sports", values[1].getString());
  }

  /**
   * Ensures DOCTYPE/entity declarations in {@code category.xml} are rejected by the XXE-hardened
   * {@code SAXBuilder} and fall back safely without throwing.
   */
  @Test
  public void shouldRejectCategoryXmlWithDoctypeDeclaration() throws IOException {
    String xxeXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE CategoryTree [ <!ENTITY xxe \"injected\"> ]>\n"
            + "<CategoryTree title=\"Categories\">\n"
            + "  <topLevelNodes>\n"
            + "    <Children id=\""
            + NEWS_ID
            + "\" title=\"&xxe;\" selectable=\"true\"/>\n"
            + "  </topLevelNodes>\n"
            + "</CategoryTree>\n";
    PSPageUtils utils = createCategoryUtils(xxeXml);

    assertEquals("/Categories/" + NEWS_ID, utils.getCategoryLabel("/Categories/" + NEWS_ID));
  }

  /**
   * Verifies {@link PSPageUtils#getCategoryLabel(String)} works when the JDK's bundled Xerces
   * {@code SAXParserFactory} is configured (as in {@code jetty/defaults/start.d/jvm.ini}).
   */
  @Test
  public void shouldResolveCategoryLabelWithJdkInternalSaxParserFactory() throws IOException {
    PSPageUtils utils = createCategoryUtils();
    String prevFactory = System.getProperty("javax.xml.parsers.SAXParserFactory");
    try {
      System.setProperty(
          "javax.xml.parsers.SAXParserFactory",
          "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
      assertEquals("/Categories/News", utils.getCategoryLabel("/Categories/" + NEWS_ID));
    } finally {
      if (prevFactory == null) {
        System.clearProperty("javax.xml.parsers.SAXParserFactory");
      } else {
        System.setProperty("javax.xml.parsers.SAXParserFactory", prevFactory);
      }
    }
  }

  private PSPageUtils createCategoryUtils() throws IOException {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<CategoryTree title=\"Categories\">\n"
            + "  <topLevelNodes>\n"
            + "    <Children id=\""
            + SPORTS_ID
            + "\" title=\"Sports\" selectable=\"true\"/>\n"
            + "    <Children id=\""
            + NEWS_ID
            + "\" title=\"News\" selectable=\"true\">\n"
            + "      <Child id=\""
            + LOCAL_ID
            + "\" title=\"Local\" selectable=\"true\"/>\n"
            + "    </Children>\n"
            + "  </topLevelNodes>\n"
            + "</CategoryTree>\n";
    return createCategoryUtils(xml);
  }

  private PSPageUtils createCategoryUtils(String categoryXml) throws IOException {
    File webResourcesDir = tempFolder.newFolder("web_resources");
    File categoryDir = new File(tempFolder.getRoot(), "rx_resources/category");
    if (!categoryDir.mkdirs()) {
      throw new IOException("Failed to create category directory: " + categoryDir);
    }
    Files.write(
        new File(categoryDir, "category.xml").toPath(),
        categoryXml.getBytes(StandardCharsets.UTF_8));

    PSPageUtils utils = new PSPageUtils();
    utils.setFileSystemService(new PSFileSystemService(webResourcesDir.getAbsolutePath()));
    return utils;
  }

  private static Node createCategoryPageNode(Value[] categoryValues) {
    Property categoryProp =
        (Property)
            Proxy.newProxyInstance(
                PSPageUtilsTest.class.getClassLoader(),
                new Class<?>[] {Property.class},
                (proxy, method, args) -> {
                  if ("getValues".equals(method.getName())) {
                    return categoryValues;
                  }
                  throw new UnsupportedOperationException("Unexpected Property call: " + method);
                });

    return (Node)
        Proxy.newProxyInstance(
            PSPageUtilsTest.class.getClassLoader(),
            new Class<?>[] {Node.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if ("hasProperty".equals(name)) {
                return args != null && args.length == 1 && "page_categories_tree".equals(args[0]);
              }
              if ("getProperty".equals(name)) {
                if (args != null && args.length == 1 && "page_categories_tree".equals(args[0])) {
                  return categoryProp;
                }
                return null;
              }
              throw new UnsupportedOperationException("Unexpected Node call: " + method);
            });
  }
}
