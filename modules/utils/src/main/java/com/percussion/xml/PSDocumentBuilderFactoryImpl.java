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

package com.percussion.xml;

import com.percussion.security.xml.PSSecureXMLUtils;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Percussion's project-wide {@link javax.xml.parsers.DocumentBuilderFactory} implementation.
 *
 * <p>T2.12 hardening (issue #135): the default Xerces factory returned by {@code
 * DocumentBuilderFactory.newInstance()} is unsafe for processing untrusted XML -- DTDs and external
 * entities are enabled by default, which is the underlying cause of the CVE-2024-34447 (XXE) and
 * CVE-2022-46337 / CVE-2023-39978 / CVE-2013-4002 (XML bomb) class of CVEs in {@code
 * xercesImpl:2.12.2}. The opt-in hardening path through {@link
 * PSSecureXMLUtils#getSecuredDocumentBuilderFactory(PSSecureXMLUtils.PSXmlSecurityOptions)} already
 * enforces the OWASP-recommended feature set, but the ~100 call sites in the project that call
 * {@code DocumentBuilderFactory.newInstance()} (or that resolve the JAXP system property registered
 * by {@link PSSecureXMLUtils#setupJAXPDefaults()}) would otherwise bypass it. This implementation
 * enforces the same secure defaults in its constructor so that every {@code
 * DocumentBuilderFactory.newInstance()} call in the project is safe by default.
 *
 * <p>The feature URIs are taken from {@link PSSecureXMLUtils} so the secure set is defined in
 * exactly one place; if a future patch needs to add or change a feature, the change is made here
 * AND in {@link PSSecureXMLUtils#setupJAXPDefaults()}. The constructor catches {@link
 * ParserConfigurationException} from any feature that the underlying Xerces version does not
 * recognize and logs at WARN -- the missing feature is treated as not-enforced rather than as a
 * fatal initialization error, matching {@link PSSecureXMLUtils}'s posture.
 */
public class PSDocumentBuilderFactoryImpl
    extends org.apache.xerces.jaxp.DocumentBuilderFactoryImpl {

  private static final Logger log = LogManager.getLogger(PSDocumentBuilderFactoryImpl.class);

  public PSDocumentBuilderFactoryImpl() {
    super();

    // T2.12 hardening (issue #135): enforce the secure-XML feature set on every DocumentBuilder
    // created by this factory, even when callers do not go through PSSecureXMLUtils. The URIs
    // and the no-DTD / no-external-entity posture match PSSecureXMLUtils.enableDBFFeatures.
    setXIncludeAware(PSSecureXMLUtils.XINCLUDE_AWARE);
    setExpandEntityReferences(PSSecureXMLUtils.EXPAND_ENTITY_REFERENCES);

    setFeatureSafe(XMLConstants.FEATURE_SECURE_PROCESSING, true, "FEATURE_SECURE_PROCESSING");
    setFeatureSafe(PSSecureXMLUtils.DISALLOW_DOCTYPES_FEATURE, true, "DISALLOW_DOCTYPES_FEATURE");
    setFeatureSafe(
        PSSecureXMLUtils.SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE,
        false,
        "SAX_GENERAL_EXTERNAL_ENTITIES_FEATURE");
    setFeatureSafe(
        PSSecureXMLUtils.X1_GENERAL_EXTERNAL_ENTITIES_FEATURE,
        false,
        "X1_GENERAL_EXTERNAL_ENTITIES_FEATURE");
    setFeatureSafe(
        PSSecureXMLUtils.X2_GENERAL_EXTERNAL_ENTITIES_FEATURE,
        false,
        "X2_GENERAL_EXTERNAL_ENTITIES_FEATURE");
    setFeatureSafe(
        PSSecureXMLUtils.SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE,
        false,
        "SAX_EXTERNAL_PARAMETER_ENTITIES_FEATURE");
    setFeatureSafe(PSSecureXMLUtils.LOAD_EXTERNAL_DTD, false, "LOAD_EXTERNAL_DTD");
  }

  private void setFeatureSafe(String feature, boolean value, String nameForLog) {
    try {
      super.setFeature(feature, value);
    } catch (ParserConfigurationException e) {
      log.warn(
          "T2.12 hardening: could not enforce {}={} on DocumentBuilderFactory: {}",
          nameForLog,
          value,
          e.getMessage());
    }
  }

  /**
   * Creates a new instance of a {@link DocumentBuilder} using the currently configured parameters.
   */
  @Override
  public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
    return super.newDocumentBuilder();
  }

  /**
   * Allows the user to set specific attributes on the underlying implementation.
   *
   * @param name name of attribute
   * @param value null means to remove attribute
   */
  @Override
  public void setAttribute(String name, Object value) throws IllegalArgumentException {
    super.setAttribute(name, value);
  }

  /**
   * Allows the user to retrieve specific attributes on the underlying implementation.
   *
   * @param name
   */
  @Override
  public Object getAttribute(String name) throws IllegalArgumentException {
    return super.getAttribute(name);
  }

  @Override
  public Schema getSchema() {
    return super.getSchema();
  }

  @Override
  public void setSchema(Schema grammar) {
    super.setSchema(grammar);
  }

  @Override
  public boolean isXIncludeAware() {
    return super.isXIncludeAware();
  }

  @Override
  public void setXIncludeAware(boolean state) {
    super.setXIncludeAware(state);
  }

  @Override
  public boolean getFeature(String name) throws ParserConfigurationException {
    return super.getFeature(name);
  }

  @Override
  public void setFeature(String name, boolean value) throws ParserConfigurationException {
    super.setFeature(name, value);
  }

  /**
   * Specifies that the parser produced by this code will provide support for XML namespaces. By
   * default the value of this is set to <code>false</code>
   *
   * @param awareness true if the parser produced will provide support for XML namespaces; false
   *     otherwise.
   */
  @Override
  public void setNamespaceAware(boolean awareness) {
    super.setNamespaceAware(awareness);
  }

  /**
   * Specifies that the parser produced by this code will validate documents as they are parsed. By
   * default the value of this is set to <code>false</code>.
   *
   * <p>Note that "the validation" here means <a href="http://www.w3.org/TR/REC-xml#proc-types">a
   * validating parser</a> as defined in the XML recommendation. In other words, it essentially just
   * controls the DTD validation. (except the legacy two properties defined in JAXP 1.2.)
   *
   * <p>To use modern schema languages such as W3C XML Schema or RELAX NG instead of DTD, you can
   * configure your parser to be a non-validating parser by leaving the {@link
   * #setValidating(boolean)} method <code>false</code>, then use the {@link #setSchema(Schema)}
   * method to associate a schema to a parser.
   *
   * @param validating true if the parser produced will validate documents as they are parsed; false
   *     otherwise.
   */
  @Override
  public void setValidating(boolean validating) {
    super.setValidating(validating);
  }

  /**
   * Specifies that the parsers created by this factory must eliminate whitespace in element content
   * (sometimes known loosely as 'ignorable whitespace') when parsing XML documents (see XML Rec
   * 2.10). Note that only whitespace which is directly contained within element content that has an
   * element only content model (see XML Rec 3.2.1) will be eliminated. Due to reliance on the
   * content model this setting requires the parser to be in validating mode. By default the value
   * of this is set to <code>false</code>.
   *
   * @param whitespace true if the parser created must eliminate whitespace in the element content
   *     when parsing XML documents; false otherwise.
   */
  @Override
  public void setIgnoringElementContentWhitespace(boolean whitespace) {
    super.setIgnoringElementContentWhitespace(whitespace);
  }

  /**
   * Specifies that the parser produced by this code will expand entity reference nodes. By default
   * the value of this is set to <code>true</code>
   *
   * @param expandEntityRef true if the parser produced will expand entity reference nodes; false
   *     otherwise.
   */
  @Override
  public void setExpandEntityReferences(boolean expandEntityRef) {
    super.setExpandEntityReferences(expandEntityRef);
  }

  /**
   * Specifies that the parser produced by this code will ignore comments. By default the value of
   * this is set to <code>false
   * </code>.
   *
   * @param ignoreComments <code>boolean</code> value to ignore comments during processing
   */
  @Override
  public void setIgnoringComments(boolean ignoreComments) {
    super.setIgnoringComments(ignoreComments);
  }

  /**
   * Specifies that the parser produced by this code will convert CDATA nodes to Text nodes and
   * append it to the adjacent (if any) text node. By default the value of this is set to <code>
   * false</code>
   *
   * @param coalescing true if the parser produced will convert CDATA nodes to Text nodes and append
   *     it to the adjacent (if any) text node; false otherwise.
   */
  @Override
  public void setCoalescing(boolean coalescing) {
    super.setCoalescing(coalescing);
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which are namespace
   * aware.
   *
   * @return true if the factory is configured to produce parsers which are namespace aware; false
   *     otherwise.
   */
  @Override
  public boolean isNamespaceAware() {
    return super.isNamespaceAware();
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which validate the XML
   * content during parse.
   *
   * @return true if the factory is configured to produce parsers which validate the XML content
   *     during parse; false otherwise.
   */
  @Override
  public boolean isValidating() {
    return super.isValidating();
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which ignore ignorable
   * whitespace in element content.
   *
   * @return true if the factory is configured to produce parsers which ignore ignorable whitespace
   *     in element content; false otherwise.
   */
  @Override
  public boolean isIgnoringElementContentWhitespace() {
    return super.isIgnoringElementContentWhitespace();
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which expand entity
   * reference nodes.
   *
   * @return true if the factory is configured to produce parsers which expand entity reference
   *     nodes; false otherwise.
   */
  @Override
  public boolean isExpandEntityReferences() {
    return super.isExpandEntityReferences();
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which ignores comments.
   *
   * @return true if the factory is configured to produce parsers which ignores comments; false
   *     otherwise.
   */
  @Override
  public boolean isIgnoringComments() {
    return super.isIgnoringComments();
  }

  /**
   * Indicates whether or not the factory is configured to produce parsers which converts CDATA
   * nodes to Text nodes and appends it to the adjacent (if any) Text node.
   *
   * @return true if the factory is configured to produce parsers which converts CDATA nodes to Text
   *     nodes and appends it to the adjacent (if any) Text node; false otherwise.
   */
  @Override
  public boolean isCoalescing() {
    return super.isCoalescing();
  }
}
