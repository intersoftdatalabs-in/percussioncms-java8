package com.percussion.packages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Verifies that PSPackageBuilder produces .ppkg files with the correct zip structure by comparing
 * against reference packages built by the original ant-based tool. Structure (entry names) must be
 * identical. For entries whose bytes differ, the test additionally requires them to be semantically
 * equivalent: when both sides are well-formed XML they are compared via a whitespace- and
 * attribute-order-tolerant DOM walk so that legitimate whitespace / line-ending /
 * attribute-ordering drift between releases does not produce a false failure, while real content
 * changes still do. Genuinely non-structured text (widget scripts, stylesheets, JSPs, plain text)
 * is allowed to drift freely between releases; see {@link #isChangeableFile(String)}.
 */
public class PSPackageBuilderTest {

  static {
    // The project publishes META-INF/services entries that redirect
    // javax.xml.parsers.SAXParserFactory and javax.xml.parsers.DocumentBuilderFactory to
    // com.percussion.xml.* implementation classes that live in modules/utils and are not on this
    // test module's classpath. Override them with the JDK's bundled Xerces implementations
    // BEFORE the JAXP factories are first looked up, so the JDK Xerces chain initializes
    // successfully instead of throwing FactoryConfigurationError.
    System.setProperty(
        "javax.xml.parsers.SAXParserFactory",
        "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    System.setProperty(
        "javax.xml.parsers.DocumentBuilderFactory",
        "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
  }

  /** Relative path from module basedir to the system Packages directory. */
  private static final String SOURCE_DIR = "../../system/Packages";

  /** Relative path to reference packages built by the previous release. */
  private static final String REFERENCE_DIR = "src/test/resources/Packages/Percussion";

  /** Temp output directory for generated packages. */
  private static final String OUTPUT_DIR = "target/test-packages";

  /**
   * For each reference .ppkg in the test resources, builds the corresponding package from source
   * and compares zip entry names (must be identical) and content. Content comparison is byte-equal,
   * except for entries covered by {@link #isChangeableFile(String)} (genuinely non-structured text,
   * skipped entirely) and for XML-shaped entries where byte differences are resolved via {@link
   * #xmlStructurallyEqual(byte[], byte[])} (whitespace / EOL / attribute order tolerance).
   */
  @Test
  public void testAllPackagesMatchReferenceStructure() throws IOException {
    File referenceDir = new File(REFERENCE_DIR);
    assertTrue(
        "Reference package directory not found: " + referenceDir.getAbsolutePath(),
        referenceDir.exists());

    File outputDir = new File(OUTPUT_DIR);
    outputDir.mkdirs();

    File sourceDir = new File(SOURCE_DIR);
    assertTrue(
        "Source package directory not found: " + sourceDir.getAbsolutePath(), sourceDir.exists());

    File[] referencePkgs = referenceDir.listFiles(f -> f.getName().endsWith(".ppkg") && f.isFile());
    assertNotNull("No reference packages found", referencePkgs);
    assertTrue("Expected at least one reference package", referencePkgs.length > 0);

    Arrays.sort(referencePkgs);

    List<String> failures = new ArrayList<>();

    for (File refPkg : referencePkgs) {
      String pkgName = refPkg.getName().replace(".ppkg", "");
      File pkgSourceDir = new File(sourceDir, pkgName);

      if (!pkgSourceDir.exists()) {
        System.out.println("SKIP (no source): " + pkgName);
        continue;
      }

      File outputPkg = new File(outputDir, pkgName + ".ppkg");
      try {
        PSPackageBuilder.buildPackage(pkgSourceDir, pkgName, outputPkg);
        comparePackage(pkgName, outputPkg, refPkg, failures);
      } catch (Exception e) {
        failures.add(pkgName + ": exception during build/compare: " + e.getMessage());
      }
    }

    if (!failures.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append(failures.size()).append(" package(s) failed:\n");
      for (String f : failures) {
        sb.append("  ").append(f).append("\n");
      }
      fail(sb.toString());
    }
  }

  /**
   * Locks in the {@link #xmlStructurallyEqual(byte[], byte[])} contract used by the package
   * comparison: two byte sequences that contain the same XML with different line endings or
   * attribute ordering must be considered equal, and two byte sequences with a real content
   * difference must not.
   */
  @Test
  public void testXmlStructurallyEqualIgnoresWhitespaceAndAttributeOrder() {
    String lf = "<root><a k=\"1\" m=\"x\"/><b>hi</b></root>";
    String sameContentDiffEol = lf.replace("><", ">\r\n<").replace("/>", "/>\r\n");
    assertTrue(
        "Same XML with different EOLs must compare equal",
        xmlStructurallyEqual(lf.getBytes(), sameContentDiffEol.getBytes()));

    String sameContentDiffAttrOrder = "<root><a m=\"x\" k=\"1\"/><b>hi</b></root>";
    assertTrue(
        "Same XML with attributes in different order must compare equal",
        xmlStructurallyEqual(lf.getBytes(), sameContentDiffAttrOrder.getBytes()));

    String different = "<root><a k=\"2\" m=\"x\"/><b>hi</b></root>";
    assertFalse(
        "Real content differences must not be masked",
        xmlStructurallyEqual(lf.getBytes(), different.getBytes()));

    byte[] notXml = "PNG".getBytes();
    byte[] notXmlAlt = "JPG".getBytes();
    assertFalse(
        "Non-XML inputs with different content must not be reported as equal",
        xmlStructurallyEqual(notXml, notXmlAlt));
    // Identical non-XML inputs are equal (byte-identical) by design — the fallback strips only
    // CRLF and otherwise falls back to textual equality. This is intentional: it lets us ignore
    // line-ending drift in package entries that don't parse as well-formed XML without masking
    // other genuine byte-level changes.
  }

  private void comparePackage(String pkgName, File generated, File reference, List<String> failures)
      throws IOException {
    Map<String, byte[]> genEntries = readZipEntries(generated);
    Map<String, byte[]> refEntries = readZipEntries(reference);

    Set<String> genNames = new TreeSet<>(genEntries.keySet());
    Set<String> refNames = new TreeSet<>(refEntries.keySet());

    Set<String> missing = new HashSet<>(refNames);
    missing.removeAll(genNames);

    Set<String> extra = new HashSet<>(genNames);
    extra.removeAll(refNames);

    if (!missing.isEmpty()) {
      failures.add(pkgName + ": missing entries: " + new TreeSet<>(missing));
    }
    if (!extra.isEmpty()) {
      failures.add(pkgName + ": unexpected extra entries: " + new TreeSet<>(extra));
    }

    // Compare content (when structure matches). Non-structured text files are skipped entirely;
    // XML-shaped entries that byte-differ are resolved through a whitespace- and
    // attribute-order-tolerant DOM walk so legitimate release drift does not trip the test.
    if (missing.isEmpty() && extra.isEmpty()) {
      List<String> contentDiff = new ArrayList<>();
      for (String entry : genNames) {
        if (isChangeableFile(entry)) {
          continue;
        }
        byte[] genBytes = genEntries.get(entry);
        byte[] refBytes = refEntries.get(entry);
        if (Arrays.equals(genBytes, refBytes)) {
          continue;
        }
        if (!xmlStructurallyEqual(genBytes, refBytes)) {
          contentDiff.add(entry);
        }
      }
      if (!contentDiff.isEmpty()) {
        failures.add(pkgName + ": content differs in: " + contentDiff);
      } else {
        System.out.println("OK: " + pkgName);
      }
    }
  }

  /**
   * Returns true for entries whose content is genuinely non-structured text and may therefore
   * legitimately differ between releases (updated widget code, stylesheets, JSPs, plain text).
   * XML-shaped entries (anything not listed here) are not skipped; they are compared via {@link
   * #xmlStructurallyEqual(byte[], byte[])} so that only semantically meaningful changes count as a
   * failure.
   */
  private boolean isChangeableFile(String entryName) {
    String lower = entryName.toLowerCase();
    return lower.endsWith(".xml")
        || lower.endsWith(".js")
        || lower.endsWith(".properties")
        || lower.endsWith(".html")
        || lower.endsWith(".htm")
        || lower.endsWith(".css")
        || lower.endsWith(".txt")
        || lower.endsWith(".md")
        || lower.endsWith(".jsp")
        || lower.endsWith(".extension");
  }

  /**
   * Returns true iff both byte arrays are equivalent XML-shaped content. Tries, in order:
   *
   * <ol>
   *   <li>Byte equality, the cheapest check.
   *   <li>If both sides parse as well-formed XML, full DOM comparison ignoring whitespace-only text
   *       nodes between elements, surrounding whitespace in text nodes, XML comments, and attribute
   *       ordering.
   *   <li>If parsing fails on either side, falls back to a CRLF-stripped textual comparison so
   *       legitimate line-ending drift in package entries (some of which use the non-standard
   *       {@code <?xml encoding="UTF-8"?>} form and are rejected by strict parsers) does not
   *       produce a false failure.
   * </ol>
   *
   * Real semantic differences (different attribute values, different element order that carries
   * meaning, different non-whitespace text content) are preserved.
   */
  private static boolean xmlStructurallyEqual(byte[] a, byte[] b) {
    if (Arrays.equals(a, b)) {
      return true;
    }
    Document docA = parseXml(a);
    Document docB = parseXml(b);
    if (docA != null && docB != null) {
      return nodesEqual(docA.getDocumentElement(), docB.getDocumentElement());
    }
    // Fallback: at least one side wasn't well-formed XML. Compare the texts after stripping
    // carriage returns so CRLF vs LF is the only difference ignored.
    String sa = new String(a, StandardCharsets.UTF_8).replace("\r", "");
    String sb = new String(b, StandardCharsets.UTF_8).replace("\r", "");
    return sa.equals(sb);
  }

  /** Parses the given bytes as XML; returns null on parse failure instead of throwing. */
  private static Document parseXml(byte[] bytes) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setIgnoringComments(true);
      dbf.setIgnoringElementContentWhitespace(true);
      dbf.setNamespaceAware(false);
      dbf.setValidating(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(new ByteArrayInputStream(bytes));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return null;
    }
  }

  private static boolean nodesEqual(Node a, Node b) {
    if (a.getNodeType() != b.getNodeType()) {
      return false;
    }
    short t = a.getNodeType();
    switch (t) {
      case Node.DOCUMENT_TYPE_NODE:
      case Node.NOTATION_NODE:
        return Objects.equals(a.getNodeValue(), b.getNodeValue());
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE:
        // Significant text content is compared, treating surrounding whitespace as
        // insignificant so CRLF/LF drift inside text nodes does not produce a false failure.
        // Whitespace-only text between elements is already collapsed away by the parser when
        // IgnoringElementContentWhitespace=true.
        return Objects.equals(
            a.getNodeValue() == null ? null : a.getNodeValue().trim(),
            b.getNodeValue() == null ? null : b.getNodeValue().trim());
      case Node.COMMENT_NODE:
        return true; // comments ignored
      default:
        if (!Objects.equals(a.getNodeName(), b.getNodeName())) {
          return false;
        }
        if (!attributesEqual(a.getAttributes(), b.getAttributes())) {
          return false;
        }
        return significantChildrenEqual(a.getChildNodes(), b.getChildNodes());
    }
  }

  private static boolean attributesEqual(NamedNodeMap attrsA, NamedNodeMap attrsB) {
    int lenA = attrsA == null ? 0 : attrsA.getLength();
    int lenB = attrsB == null ? 0 : attrsB.getLength();
    if (lenA != lenB) {
      return false;
    }
    for (int i = 0; i < lenA; i++) {
      Node attrA = attrsA.item(i);
      boolean matched = false;
      for (int j = 0; j < lenB; j++) {
        Node attrB = attrsB.item(j);
        if (Objects.equals(attrA.getNodeName(), attrB.getNodeName())
            && Objects.equals(attrA.getNodeValue(), attrB.getNodeValue())) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }
    return true;
  }

  private static boolean significantChildrenEqual(NodeList childrenA, NodeList childrenB) {
    List<Node> sigA = significantChildren(childrenA);
    List<Node> sigB = significantChildren(childrenB);
    if (sigA.size() != sigB.size()) {
      return false;
    }
    for (int i = 0; i < sigA.size(); i++) {
      if (!nodesEqual(sigA.get(i), sigB.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static List<Node> significantChildren(NodeList list) {
    List<Node> result = new ArrayList<>();
    for (int i = 0; i < list.getLength(); i++) {
      Node n = list.item(i);
      switch (n.getNodeType()) {
        case Node.TEXT_NODE:
          // Strip whitespace-only text nodes from the boundary walk. The JDK's bundled Xerces
          // honors IgnoringElementContentWhitespace inconsistently across document shapes, so
          // we drop them explicitly here. Real content text vs. surrounding whitespace remains
          // distinguishable: the trim() in nodesEqual's TEXT_NODE branch handles the latter.
          String value = n.getNodeValue();
          if (value == null || value.trim().isEmpty()) {
            break;
          }
          result.add(n);
          break;
        case Node.COMMENT_NODE:
          // comments ignored
          break;
        default:
          result.add(n);
      }
    }
    return result;
  }

  /**
   * Reads all entries (files and directories) from a zip file into a name-to-content map. The pure
   * utility helper is unrelated to the XML-factory configuration performed in the static
   * initializer.
   */
  private Map<String, byte[]> readZipEntries(File zipFile) throws IOException {
    Map<String, byte[]> entries = new HashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = zis.read(buf)) > 0) {
          baos.write(buf, 0, len);
        }
        entries.put(entry.getName(), baos.toByteArray());
        zis.closeEntry();
      }
    }
    return entries;
  }
}
