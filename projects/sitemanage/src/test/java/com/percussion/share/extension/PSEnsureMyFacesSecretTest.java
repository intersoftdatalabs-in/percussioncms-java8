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
package com.percussion.share.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Unit tests for {@link PSEnsureMyFacesSecret}. The test uses a thread-local Rx dir pointing at a
 * {@link TemporaryFolder} so the deployed web.xml can be staged under {@code
 * AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/web.xml} without touching the real
 * install.
 */
public class PSEnsureMyFacesSecretTest {

  @Rule public TemporaryFolder tempFolder = TemporaryFolder.builder().build();

  private File rxRoot;
  private File webXmlFile;
  private File priorRxDir;

  @Before
  public void setUp() throws Exception {
    rxRoot = tempFolder.newFolder("rx");
    File webinf = new File(rxRoot, "AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF");
    assertTrue(webinf.mkdirs());
    webXmlFile = new File(webinf, "web.xml");
    assertTrue(webXmlFile.createNewFile());

    // Remember the previous static RxDir so we can restore it after the test, and set a
    // thread-local RxDir to point at our temp root so PathUtils.getRxDir() resolves there.
    priorRxDir = PathUtils.getRxDir(null);
    PathUtils.setThreadOnlyRxDir(rxRoot);
  }

  @After
  public void tearDown() {
    PathUtils.unsetThreadOnlyRxDir(rxRoot);
    PathUtils.clearRxDir();
  }

  @Test
  public void addsAllThreeParamsWhenNonePresent() throws Exception {
    writeWebXml(MINIMAL_WEB_XML);

    PSEnsureMyFacesSecret process = new PSEnsureMyFacesSecret();
    process.doStartupWork(new Properties());

    Document doc = parse(webXmlFile);
    assertEquals("false", paramValue(doc, PSEnsureMyFacesSecret.PARAM_COMPRESS_VIEW_STATE));
    assertEquals("true", paramValue(doc, PSEnsureMyFacesSecret.PARAM_STRICT_JAR));
    String secret = paramValue(doc, PSEnsureMyFacesSecret.PARAM_SECRET);
    assertNotNull(secret);
    assertEquals(64, secret.length());
  }

  @Test
  public void preservesOperatorSuppliedSecret() throws Exception {
    String operatorSecret = "operator-supplied-secret-do-not-clobber-32chars";
    writeWebXml(
        MINIMAL_WEB_XML.replace(
            "</web-app>",
            "  <context-param>\n"
                + "    <param-name>org.apache.myfaces.SECRET</param-name>\n"
                + "    <param-value>"
                + operatorSecret
                + "</param-value>\n"
                + "  </context-param>\n"
                + "</web-app>"));

    PSEnsureMyFacesSecret process = new PSEnsureMyFacesSecret();
    process.doStartupWork(new Properties());

    Document doc = parse(webXmlFile);
    assertEquals(operatorSecret, paramValue(doc, PSEnsureMyFacesSecret.PARAM_SECRET));
    // The other two are still filled in.
    assertEquals("false", paramValue(doc, PSEnsureMyFacesSecret.PARAM_COMPRESS_VIEW_STATE));
    assertEquals("true", paramValue(doc, PSEnsureMyFacesSecret.PARAM_STRICT_JAR));
  }

  @Test
  public void replacesEmptyParamValue() throws Exception {
    writeWebXml(
        MINIMAL_WEB_XML.replace(
            "</web-app>",
            "  <context-param>\n"
                + "    <param-name>org.apache.myfaces.SECRET</param-name>\n"
                + "    <param-value></param-value>\n"
                + "  </context-param>\n"
                + "</web-app>"));

    PSEnsureMyFacesSecret process = new PSEnsureMyFacesSecret();
    process.doStartupWork(new Properties());

    Document doc = parse(webXmlFile);
    String secret = paramValue(doc, PSEnsureMyFacesSecret.PARAM_SECRET);
    assertNotNull(secret);
    assertEquals(64, secret.length());
  }

  @Test
  public void isIdempotentOnAlreadyConfiguredFile() throws Exception {
    writeWebXml(MINIMAL_WEB_XML);

    PSEnsureMyFacesSecret process = new PSEnsureMyFacesSecret();
    process.doStartupWork(new Properties());

    // Capture the file content after the first run.
    String firstRun = new String(Files.readAllBytes(webXmlFile.toPath()), StandardCharsets.UTF_8);
    String firstSecret = paramValue(parse(webXmlFile), PSEnsureMyFacesSecret.PARAM_SECRET);

    // Run again. Should be a no-op: file content is identical, secret is identical.
    process.doStartupWork(new Properties());
    String secondRun = new String(Files.readAllBytes(webXmlFile.toPath()), StandardCharsets.UTF_8);
    String secondSecret = paramValue(parse(webXmlFile), PSEnsureMyFacesSecret.PARAM_SECRET);

    assertEquals(firstRun, secondRun);
    assertEquals(firstSecret, secondSecret);
  }

  @Test
  public void generatedSecretsAreDifferentAcrossFreshInvocations() throws Exception {
    // Two separate fresh files, both initially empty, both run once.
    writeWebXml(MINIMAL_WEB_XML);
    PSEnsureMyFacesSecret p1 = new PSEnsureMyFacesSecret();
    p1.doStartupWork(new Properties());
    String s1 = paramValue(parse(webXmlFile), PSEnsureMyFacesSecret.PARAM_SECRET);

    // Reset file to a clean state but reuse the same process instance (state-free).
    writeWebXml(MINIMAL_WEB_XML);
    PSEnsureMyFacesSecret p2 = new PSEnsureMyFacesSecret();
    p2.doStartupWork(new Properties());
    String s2 = paramValue(parse(webXmlFile), PSEnsureMyFacesSecret.PARAM_SECRET);

    assertNotNull(s1);
    assertNotNull(s2);
    assertNotEquals(s1, s2);
  }

  // --- helpers ---

  private void writeWebXml(String content) throws Exception {
    Files.write(webXmlFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
  }

  private static Document parse(File f) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);
    DocumentBuilder db = dbf.newDocumentBuilder();
    return db.parse(f);
  }

  /**
   * Find a {@code <context-param>} by {@code <param-name>} and return its {@code <param-value>}
   * text content, or {@code null} if not present.
   */
  private static String paramValue(Document doc, String name) throws Exception {
    XPath xp = XPathFactory.newInstance().newXPath();
    String expr = "//context-param[param-name='" + name + "']/param-value/text()";
    NodeList nl = (NodeList) xp.evaluate(expr, doc, XPathConstants.NODESET);
    if (nl.getLength() == 0) return null;
    return nl.item(0).getNodeValue();
  }

  /**
   * Minimal web.xml scaffold that the process can read and write. Uses an inline DOCTYPE so the
   * {@link DocumentBuilder} parses it without trying to fetch the Servlet DTD over the network.
   */
  private static final String MINIMAL_WEB_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
          + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
          + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee "
          + "http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\n"
          + "         version=\"2.5\">\n"
          + "  <display-name>Test</display-name>\n"
          + "</web-app>\n";
}
