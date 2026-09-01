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

import com.percussion.error.PSExceptionUtils;
import com.percussion.security.SecureStringUtils;
import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Startup process that applies the MyFaces 2.3.x hardening recommendations to the deployed {@code
 * web.xml} on first install, and leaves any operator-supplied values alone on subsequent startups.
 * This is the runtime side of the T2.15 workstream (non-upgradeable epic, parent issue #73): the
 * MyFaces 2.3.x line has no Java 8 fix for the cataloged CVEs, so the only mitigation is
 * configuration hardening.
 *
 * <p>For each of the three MyFaces context-params below, the behavior is: <b>if the param is
 * already present with a non-empty value, leave it alone; otherwise, set it to the safe default
 * (or, for the SECRET, a freshly generated cryptographic secret).</b> The generated SECRET is
 * persisted into the deployed web.xml, so it survives restarts and only rotates when the operator
 * manually changes the file (or removes the entry, which causes the next startup to generate a new
 * one).
 *
 * <ul>
 *   <li><b>{@code org.apache.myfaces.SECRET}</b> — set to a freshly generated {@link
 *       SecureStringUtils#generateRandomSecret() 64-char secret} (XML/URL-safe, ~384 bits of
 *       entropy). The default in MyFaces is a known constant, which makes the ViewState CSRF token
 *       forgeable.
 *   <li><b>{@code org.apache.myfaces.COMPRESS_VIEW_STATE}</b> — set to {@code false}. Disabling
 *       ViewState compression closes a class of decompression-related issues in MyFaces' client
 *       state serializer.
 *   <li><b>{@code org.apache.myfaces.strict_jar_in_filesystem}</b> — set to {@code true}. Restricts
 *       MyFaces' taglib/jar resolver to filesystem-relative paths so a malicious jar placed outside
 *       the expected path cannot be picked up.
 * </ul>
 *
 * <p>Registered as a Spring bean in {@code sitemanage-beans.xml} alongside the other {@link
 * IPSStartupProcess} implementations ({@code PSRxFixStartupProcess}, {@code PSRotateSecureKey},
 * etc.). Runs once at server startup, after the user service has had a chance to create the default
 * admin user (so the {@code SecureStringUtils} path mirrors the admin-password generation pattern
 * in {@code PSUserService.createPercussionUser()}).
 *
 * <p>Idempotent: re-running the process on an already-hardened install is a no-op (the file is only
 * re-serialized if at least one missing param had to be filled in).
 */
public class PSEnsureMyFacesSecret implements IPSStartupProcess {

  private static final Logger log = LogManager.getLogger(PSEnsureMyFacesSecret.class);

  /**
   * Deployed {@code web.xml} path, relative to the Rhythmyx root directory. Matches the layout used
   * elsewhere in the project (see {@code PSConfigureDatasource#m_strSpringConfigLocation} for the
   * same {@code AppServer/server/rx/deploy/rxapp.ear/rxapp.war} prefix).
   */
  static final String DEPLOYED_WEB_XML_REL_PATH =
      "AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/web.xml";

  /** MyFaces ViewState CSRF-token seed. Generated at runtime (see {@link #SAFE_DEFAULTS}). */
  static final String PARAM_SECRET = "org.apache.myfaces.SECRET";

  /** MyFaces ViewState compression toggle. */
  static final String PARAM_COMPRESS_VIEW_STATE = "org.apache.myfaces.COMPRESS_VIEW_STATE";

  /** MyFaces strict jar-in-filesystem toggle. */
  static final String PARAM_STRICT_JAR = "org.apache.myfaces.strict_jar_in_filesystem";

  /**
   * Sentinel used as the SAFE_DEFAULTS value for {@link #PARAM_SECRET} to indicate that the actual
   * value should be generated at runtime via {@link SecureStringUtils#generateRandomSecret()}.
   * Declared before the static initializer so the static block can reference it without a
   * forward-reference error.
   */
  private static final String SENTINEL_GENERATE_SECRET = "\0__GENERATE__\0";

  /**
   * Safe defaults applied when the corresponding context-param is missing or empty. Order is
   * preserved on insertion (LinkedHashMap) so the written web.xml is deterministic. The {@code
   * SECRET} entry's value is a sentinel ({@link #SENTINEL_GENERATE_SECRET}) that is replaced at
   * runtime with a freshly generated secret.
   */
  private static final Map<String, String> SAFE_DEFAULTS = new LinkedHashMap<>();

  static {
    SAFE_DEFAULTS.put(PARAM_SECRET, SENTINEL_GENERATE_SECRET);
    SAFE_DEFAULTS.put(PARAM_COMPRESS_VIEW_STATE, "false");
    SAFE_DEFAULTS.put(PARAM_STRICT_JAR, "true");
  }

  /** Indent amount, in spaces, used when re-serializing web.xml. Matches the existing format. */
  private static final String INDENT_AMOUNT = "4";

  @Override
  public void doStartupWork(Properties startupProps) {
    File webXmlFile = new File(PathUtils.getRxDir(null), DEPLOYED_WEB_XML_REL_PATH);
    if (!webXmlFile.exists()) {
      log.warn(
          "Deployed web.xml not found at {}; skipping MyFaces hardening "
              + "(T2.15). This is expected in some unit-test contexts.",
          webXmlFile);
      return;
    }
    if (!webXmlFile.canRead() || !webXmlFile.canWrite()) {
      log.warn(
          "Deployed web.xml at {} is not readable/writable; skipping MyFaces hardening",
          webXmlFile);
      return;
    }
    try {
      applyHardening(webXmlFile);
    } catch (Exception e) {
      // Never let a hardening failure prevent the server from starting. Log and move on so the
      // operator can fix the file (or the install) manually.
      log.error(
          "Failed to apply MyFaces hardening to {}: {}",
          webXmlFile,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Parses {@code webXmlFile}, fills in any missing hardening context-params, and re-serializes the
   * file if and only if at least one param had to be added or updated.
   */
  private void applyHardening(File webXmlFile) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(webXmlFile);

    Element root = doc.getDocumentElement();
    Map<String, Element> existing = collectContextParams(root);

    boolean modified = false;
    for (Map.Entry<String, String> entry : SAFE_DEFAULTS.entrySet()) {
      String paramName = entry.getKey();
      String defaultValue = entry.getValue();

      Element existingEl = existing.get(paramName);
      String existingValue = readParamValue(existingEl);

      if (existingValue != null && !existingValue.trim().isEmpty()) {
        log.info(
            "MyFaces context-param {} already set; preserving (length={})",
            paramName,
            existingValue.length());
        continue;
      }

      String valueToWrite =
          SENTINEL_GENERATE_SECRET.equals(defaultValue)
              ? SecureStringUtils.generateRandomSecret()
              : defaultValue;

      if (existingEl == null) {
        Element newParam = buildContextParamElement(doc, paramName, valueToWrite);
        Node insertionPoint = findInsertionPoint(root);
        if (insertionPoint == null) {
          root.appendChild(newParam);
        } else {
          root.insertBefore(newParam, insertionPoint);
        }
        log.info(
            "Added MyFaces context-param {} (length={}) to {}",
            paramName,
            valueToWrite.length(),
            webXmlFile);
      } else {
        writeParamValue(existingEl, valueToWrite);
        log.info(
            "Updated empty MyFaces context-param {} (length={}) in {}",
            paramName,
            valueToWrite.length(),
            webXmlFile);
      }
      modified = true;
    }

    if (modified) {
      writeXml(doc, webXmlFile);
      log.info("Persisted MyFaces hardening to {}", webXmlFile);
    } else {
      log.info("No MyFaces hardening changes required; {} is already configured", webXmlFile);
    }
  }

  /** Collect all {@code <context-param>} elements keyed by their {@code <param-name>}. */
  private static Map<String, Element> collectContextParams(Element root) {
    Map<String, Element> result = new LinkedHashMap<>();
    NodeList children = root.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) continue;
      if (!"context-param".equals(n.getNodeName())) continue;
      Element e = (Element) n;
      String name = readChildText(e, "param-name");
      if (name != null) result.put(name, e);
    }
    return result;
  }

  /** Read the text of a direct-child {@code <param-value>} element, or {@code null} if absent. */
  private static String readParamValue(Element contextParam) {
    if (contextParam == null) return null;
    return readChildText(contextParam, "param-value");
  }

  /** Read the text of a direct-child element by tag name, or {@code null} if absent/empty. */
  private static String readChildText(Element parent, String tag) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
        String text = n.getTextContent();
        return text == null ? null : text;
      }
    }
    return null;
  }

  /**
   * Find the first non-{@code context-param} child of {@code root} so new context-params can be
   * inserted in the correct schema-ordered position (per the Servlet DTD, {@code context-param}
   * must precede {@code filter}, {@code filter-mapping}, {@code servlet}, etc.). Returns {@code
   * null} if no such child exists, in which case the new element is appended.
   */
  private static Node findInsertionPoint(Element root) {
    NodeList children = root.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && !"context-param".equals(n.getNodeName())) {
        return n;
      }
    }
    return null;
  }

  /** Build a fresh {@code <context-param><param-name/><param-value/></context-param>} element. */
  private static Element buildContextParamElement(Document doc, String name, String value) {
    Element contextParam = doc.createElement("context-param");
    Element nameEl = doc.createElement("param-name");
    nameEl.appendChild(doc.createTextNode(name));
    Element valueEl = doc.createElement("param-value");
    valueEl.appendChild(doc.createTextNode(value));
    contextParam.appendChild(nameEl);
    contextParam.appendChild(valueEl);
    return contextParam;
  }

  /** Replace the text content of the direct-child {@code <param-value>} with {@code value}. */
  private static void writeParamValue(Element contextParam, String value) {
    NodeList children = contextParam.getChildNodes();
    Element valueEl = null;
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && "param-value".equals(n.getNodeName())) {
        valueEl = (Element) n;
        break;
      }
    }
    if (valueEl == null) {
      valueEl = contextParam.getOwnerDocument().createElement("param-value");
      contextParam.appendChild(valueEl);
    }
    // Clear any existing text children.
    while (valueEl.getFirstChild() != null) {
      valueEl.removeChild(valueEl.getFirstChild());
    }
    valueEl.appendChild(valueEl.getOwnerDocument().createTextNode(value));
  }

  /** Serialize {@code doc} to {@code target} with 4-space indentation matching web.xml. */
  private static void writeXml(Document doc, File target) throws Exception {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer t = tf.newTransformer();
    t.setOutputProperty(OutputKeys.INDENT, "yes");
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", INDENT_AMOUNT);
    t.transform(new DOMSource(doc), new StreamResult(target));
  }

  @Override
  public void setStartupProcessManager(IPSStartupProcessManager mgr) {
    if (mgr != null) mgr.addStartupProcess(this);
  }
}
