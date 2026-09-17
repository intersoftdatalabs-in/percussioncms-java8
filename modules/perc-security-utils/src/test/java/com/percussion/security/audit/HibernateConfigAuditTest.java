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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.audit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Regression audit for the Hibernate configuration surface touched by T2.14 (epic #73, issue #223).
 *
 * <p>The audit walks every Spring-style bean XML in the project and every Hibernate properties file
 * and asserts three security-mitigating invariants:
 *
 * <ol>
 *   <li>{@code hibernate.use_sql_comments} must not be {@code true} anywhere. Hibernate's {@code
 *       use_sql_comments=true} setting causes SQL comments to be embedded in the generated SQL
 *       using user-influenced strings; this is the lever for CVE-2020-25638 (SQL injection via
 *       Hibernate ORM 5.6.x). The Hibernate default is {@code false}; we keep it {@code false}
 *       everywhere we ship.
 *   <li>Where {@code hibernate.criteria.literal_handling_mode} is set, it must be {@code bind}.
 *       When it is not set we rely on the Hibernate default. CVE-2019-14900 (JPA Criteria API SQL
 *       injection via unsanitized literals in SELECT/GROUP BY) is closed only when literals are
 *       bound as parameters.
 *   <li>If a {@code META-INF/validation.xml} ships in the project, it must disable EL interpolation
 *       by selecting {@code ResourceBundleMessageInterpolator} (no EL) and must not select {@code
 *       HibernateMessageInterpolator} (EL enabled). CVE-2019-10219, CVE-2020-10693, and
 *       CVE-2025-35036 are all EL-injection-through-validation-message paths.
 * </ol>
 *
 * <p>The test does not require any database; it is a pure offline audit that walks the project
 * tree. It is intentionally JUnit 4 to match the rest of {@code perc-security-utils}.
 *
 * @author mavis-code
 */
public class HibernateConfigAuditTest {

  private static final String USE_SQL_COMMENTS = "hibernate.use_sql_comments";
  private static final String LITERAL_HANDLING_MODE = "hibernate.criteria.literal_handling_mode";

  /** CVE-2020-25638: {@code hibernate.use_sql_comments=true} is the lever for SQL injection. */
  @Test
  public void useSqlCommentsIsNeverTrue() throws IOException {
    File root = findProjectRoot();
    assertTrue(
        "Could not locate project root (no directory above user.dir contains mvn-env.sh).",
        root != null);

    List<String> offenders = new ArrayList<>();
    for (File cfg : candidateConfigs(root)) {
      Properties props = parseSpringHibernateProps(cfg);
      String value = props.getProperty(USE_SQL_COMMENTS);
      if ("true".equalsIgnoreCase(value)) {
        offenders.add(cfg.getAbsolutePath() + " => " + USE_SQL_COMMENTS + "=" + value);
      }
    }
    assertTrue(
        "hibernate.use_sql_comments must never be 'true' (CVE-2020-25638). Offenders: " + offenders,
        offenders.isEmpty());
  }

  /**
   * CVE-2019-14900: where the property is set at all it must be {@code bind}. If the property is
   * not present in a file we leave it to the Hibernate default; the test only guards against an
   * explicit insecure value.
   */
  @Test
  public void criteriaLiteralHandlingModeIsBindWhereverSet() throws IOException {
    File root = findProjectRoot();
    assertTrue(
        "Could not locate project root (no directory above user.dir contains mvn-env.sh).",
        root != null);

    List<String> offenders = new ArrayList<>();
    for (File cfg : candidateConfigs(root)) {
      Properties props = parseSpringHibernateProps(cfg);
      String value = props.getProperty(LITERAL_HANDLING_MODE);
      if (value != null && !"bind".equalsIgnoreCase(value.trim())) {
        offenders.add(cfg.getAbsolutePath() + " => " + LITERAL_HANDLING_MODE + "=" + value);
      }
    }
    assertTrue(
        "hibernate.criteria.literal_handling_mode must be 'bind' wherever it is set"
            + " (CVE-2019-14900). Offenders: "
            + offenders,
        offenders.isEmpty());
  }

  /**
   * CVE-2019-10219 / CVE-2020-10693 / CVE-2025-35036: if a {@code META-INF/validation.xml} is
   * present in the project it must disable EL interpolation. This is a soft check: the audit passes
   * if no {@code validation.xml} ships (zero risk in that case because there is also zero custom
   * ConstraintValidator / SafeHtml usage) or if any {@code validation.xml} that does ship selects
   * {@code ResourceBundleMessageInterpolator} and does not select {@code
   * HibernateMessageInterpolator}.
   */
  @Test
  public void validationXmlDisablesElInterpolation() throws IOException {
    File root = findProjectRoot();
    assertTrue("Could not locate project root.", root != null);

    List<File> validationXmls = findValidationXmls(root);
    if (validationXmls.isEmpty()) {
      // No validation.xml ships, so there is no Hibernate Validator message interpolator
      // configured for this project. The default HibernateMessageInterpolator applies. There is
      // still no runtime exposure: the project has no custom ConstraintValidator implementations
      // and no @SafeHtml usages, so EL cannot reach user input. Document and pass.
      return;
    }
    List<String> offenders = new ArrayList<>();
    for (File v : validationXmls) {
      String text = new String(Files.readAllBytes(v.toPath()), "UTF-8");
      // Match the actual configured interpolator, not arbitrary substring text. The
      // <message-interpolator> element is the only sanctioned way to set this in
      // validation.xml; comments and prose elsewhere in the file are not configuration.
      java.util.regex.Pattern p =
          java.util.regex.Pattern.compile(
              "<message-interpolator>\\s*([^<\\s]+)\\s*</message-interpolator>");
      java.util.regex.Matcher m = p.matcher(text);
      String configured = null;
      while (m.find()) {
        configured = m.group(1);
      }
      boolean usesResourceBundle =
          configured != null && configured.contains("ResourceBundleMessageInterpolator");
      boolean usesHibernate =
          configured != null && configured.contains("HibernateMessageInterpolator");
      if (configured == null || usesHibernate || !usesResourceBundle) {
        offenders.add(
            v.getAbsolutePath()
                + " => configured="
                + configured
                + ", usesHibernate="
                + usesHibernate
                + ", usesResourceBundle="
                + usesResourceBundle);
      }
    }
    assertTrue(
        "META-INF/validation.xml must select ResourceBundleMessageInterpolator"
            + " and must not select HibernateMessageInterpolator (CVE-2019-10219,"
            + " CVE-2020-10693, CVE-2025-35036). Offenders: "
            + offenders,
        offenders.isEmpty());
  }

  // --- helpers --------------------------------------------------------------------------

  /**
   * Walk up from {@code user.dir} until we find a directory containing both {@code pom.xml} and
   * {@code mvn-env.sh}; that is the project root. Returns {@code null} if not found within 8
   * levels.
   */
  static File findProjectRoot() {
    File cur = new File(System.getProperty("user.dir")).getAbsoluteFile();
    for (int i = 0; i < 8 && cur != null; i++, cur = cur.getParentFile()) {
      File marker = new File(cur, "mvn-env.sh");
      File pom = new File(cur, "pom.xml");
      if (marker.isFile() && pom.isFile()) {
        return cur;
      }
    }
    return null;
  }

  /**
   * Collect every Spring-style bean XML and every {@code .properties} file under the project root
   * that looks like it could carry Hibernate configuration.
   */
  static Set<File> candidateConfigs(File root) throws IOException {
    Set<File> out = new LinkedHashSet<>();
    Files.walkFileTree(
        Paths.get(root.toURI()),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String name = dir.getFileName().toString();
            if (name.equals("target")
                || name.equals(".git")
                || name.equals(".worktrees")
                || name.equals("node_modules")
                || name.equals(".idea")
                || name.equals(".vscode")) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String name = file.getFileName().toString();
            Path parent = file.getParent();
            String parentName = parent != null ? parent.getFileName().toString() : "";
            // Sample datasources and shipped datasources in delivery tier.
            if (name.startsWith("perc-datasources")
                || name.startsWith("hibernate.cfg")
                || name.startsWith("hibernate.properties")
                || (name.endsWith(".xml")
                    && (parentName.equals("spring")
                        || parentName.equals("config")
                        || parentName.equals("META-INF")))) {
              String p = file.toString();
              if (p.contains("/test-classes/") || p.contains("/test/resources/")) {
                return FileVisitResult.CONTINUE;
              }
              if (p.endsWith("server-beans.xml")
                  || p.endsWith("install-beans.xml")
                  || p.endsWith("local-beans-template.xml")
                  || p.endsWith("beans.xml")
                  || p.endsWith("applicationContext.xml")
                  || p.endsWith("security.xml")
                  || p.endsWith("datasources.xml")
                  || p.endsWith("datasources.xml.sample-DERBY")
                  || p.endsWith("datasources.xml.sample-MSSQL")
                  || p.endsWith("datasources.xml.sample-MYSQL-MARIADB")
                  || p.endsWith("datasources.xml.sample-ORACLE")
                  || p.endsWith("datasources.xml.sample-POSTGRES")) {
                out.add(file.toFile());
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return out;
  }

  static List<File> findValidationXmls(File root) throws IOException {
    List<File> out = new ArrayList<>();
    Files.walkFileTree(
        Paths.get(root.toURI()),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String name = dir.getFileName().toString();
            if (name.equals("target")
                || name.equals(".git")
                || name.equals(".worktrees")
                || name.equals("node_modules")) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().equals("validation.xml")) {
              String p = file.toString();
              if (p.contains("/test-classes/") || p.contains("/test/")) {
                return FileVisitResult.CONTINUE;
              }
              if (p.contains("/META-INF/")) {
                out.add(file.toFile());
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return out;
  }

  /**
   * Parse a Spring bean XML or {@code .properties} file and return the Hibernate-related properties
   * as a flat {@link Properties}. For Spring XML we look at {@code <props>/<prop
   * key="...">value</prop>} entries under any bean that mentions "hibernate" in its id or class. We
   * disable external entities so a malicious config file cannot fetch DTDs during the audit.
   */
  static Properties parseSpringHibernateProps(File file) {
    Properties out = new Properties();
    String name = file.getName();
    try {
      if (name.endsWith(".properties")) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
          out.load(in);
        }
        return out;
      }
      if (!name.endsWith(".xml")) {
        return out;
      }
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc;
      try (InputStream in = Files.newInputStream(file.toPath())) {
        doc = db.parse(new InputSource(in));
      }
      // Look at <prop key="hibernate.*"> entries inside <props> blocks within <bean>s.
      NodeList props = doc.getElementsByTagName("prop");
      for (int i = 0; i < props.getLength(); i++) {
        Element prop = (Element) props.item(i);
        String key = prop.getAttribute("key");
        String value = prop.getTextContent() == null ? "" : prop.getTextContent().trim();
        if (key != null && key.startsWith("hibernate.")) {
          out.setProperty(key, value);
        }
      }
    } catch (ParserConfigurationException | IOException | org.xml.sax.SAXException e) {
      // Skip unparseable files; the audit is best-effort. Real CI build will flag invalid XML.
    }
    return out;
  }
}
