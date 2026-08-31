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

package com.percussion.search.lucene.textconverter;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.search.lucene.IPSLuceneConstants;
import com.percussion.server.PSServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.microsoft.rtf.RTFParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.parser.xml.XMLParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

/**
 * Uses Apache Tika to extract text from many different document types. This is added as a
 * replacement for most of the old system text converters
 *
 * <p>T2.1 hardening (issue #137, completing the #92 input-cap work): the parser set used by this
 * converter is an explicit allowlist built from {@link CompositeParser} with seven individual
 * parsers (text, HTML, XML, PDF, legacy Office, OOXML, RTF). For any media type not claimed by one
 * of those parsers, Tika's {@code CompositeParser} falls back to {@code EmptyParser} and the
 * document is parsed as empty. The previous implementation used {@code AutoDetectParser}, which
 * loads every parser Tika discovers on the classpath via the standard service-loader mechanism --
 * including image OCR, audio transcription, video metadata parsers, and any parser added in a
 * future Tika minor upgrade. The strict allowlist closes the file-type- confusion CVE class in
 * {@code tika-parsers-standard-package:2.9.4}: a document with a misleading Content-Type header is
 * parsed by no parser (rather than by an unexpected parser), and the addition of a new Tika parser
 * does not silently enter the trusted surface.
 *
 * <p>The allowlist is built once, lazily, from the {@link TikaConfig} loaded by {@link
 * #getTikaConfig()}, so the existing {@code tika-config.xml} restrictions (which exclude the {@code
 * ExecutableParser} and {@code SQLite3Parser} and the {@code image/jpeg}, {@code application/pdf},
 * and {@code application/x-sqlite3} MIME types from the default parser) remain in effect. The
 * strict allowlist above is the *outer* surface; the configured parser is the *inner* surface; both
 * must allow a type for it to be parsed.
 *
 * <p>The original T2.1 work (#92) added the {@link
 * com.percussion.security.io.PSTikaCap#truncate(InputStream)} input cap and the {@code
 * TikaInputStream.get(File)} streaming path. This PR layers the parser allowlist on top of those
 * defenses. Recursive embedded-document parsing is intentionally preserved -- the indexer needs the
 * text from inside OOXML / OLE2 compound documents, so we do not set a custom {@code
 * EmbeddedDocumentExtractor} that returns {@code false} from {@code shouldParseEmbedded()}.
 */
public class PSTikaTextConvertor implements IPSLuceneTextConverter {
  /** Reference to log for this class */
  private static final Logger ms_log = LogManager.getLogger(PSTikaTextConvertor.class);

  /**
   * Server property to set the write limit. This is the maximum number of characters to store in
   * the String sent to the indexer. This can be very large for a large document If a document is
   * over this limit it will only index the text before the limit.
   */
  private static final String INDEX_WRITE_LIMIT = "indexWriteLimit";

  /** Creating a new TikaConfig object takes a long time, so we will create a singleton */
  private static TikaConfig m_tikaConfig = null;

  /**
   * Strict parser allowlist (T2.1 hardening, issue #137). Built once from the {@link TikaConfig}
   * loaded by {@link #getTikaConfig()}, used in place of {@code AutoDetectParser} in {@link
   * #getConvertedText(InputStream, String)}. See the class Javadoc for the security contract.
   */
  private static Parser m_strictParser = null;

  /*
   * Default write limit, just under 10M
   */
  private static int writeLimit = 5000000;

  private static Set<String> m_mimeTypes = new HashSet<String>();

  static {
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_PDF);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_MSWORD);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSWORD_DOC);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSWORD_TEMPLATE);

    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_VNDMSEXCEL);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_EXCEL);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSEXCEL_SHEET);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSEXCEL_TEMPLATE);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_VNDMSPOWERPOINT);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_MSPOWERPOINT);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSPOWERPOINT_PRES);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_OPENXML_MSPOWERPOINT_TEMPLATE);
    m_mimeTypes.add(StringUtils.lowerCase(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_RTF));
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_TEXT_BY_HTML);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_XHTML);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_XML);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_TEXT_BY_XML);
    m_mimeTypes.add(IPSLuceneConstants.MIME_TYPE_PLAIN_BY_TEXT);
  }

  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {}

  private static synchronized void getTikaConfig() throws PSExtensionProcessingException {
    if (m_tikaConfig == null) {
      try {
        m_tikaConfig = new TikaConfig();

        if (PSServer.getServerProps() != null) {
          String prop = PSServer.getServerProps().getProperty(INDEX_WRITE_LIMIT);
          if (prop != null) {
            try {
              writeLimit = Integer.parseInt(prop);
            } catch (NumberFormatException e) {
              ms_log.error(
                  "property "
                      + INDEX_WRITE_LIMIT
                      + " in server.properties is not an integer "
                      + prop);
            }
          }
        }
      } catch (MimeTypeException e) {
        throw new PSExtensionProcessingException("Cannot get TikaConfig", e);
      } catch (IOException e) {
        throw new PSExtensionProcessingException("Cannot get TikaConfig", e);
      } catch (TikaException e) {
        throw new PSExtensionProcessingException("Cannot get TikaConfig", e);
      }
    }
  }

  /**
   * Returns the strict-parser {@link CompositeParser} used in place of {@code AutoDetectParser} for
   * the indexer. The allowlist is exactly seven parsers: text, HTML, XML, PDF, legacy Office
   * (single parser handles .doc / .xls / .ppt via the OLE2 / POIFS container), OOXML (single parser
   * handles .docx / .xlsx / .pptx), and RTF. For any media type not claimed by one of those
   * parsers, {@link CompositeParser} falls back to {@code EmptyParser} and the document is parsed
   * as empty.
   *
   * <p>Built once per JVM and cached. Initialization order matters: {@link #m_tikaConfig} must be
   * populated by {@link #getTikaConfig()} before this is called.
   */
  private static synchronized Parser getStrictParser() {
    if (m_strictParser == null) {
      List<Parser> parsers = new ArrayList<>(7);
      // T2.1 hardening (issue #137): the seven parsers below are the entire surface this
      // project exposes to Tika. Any new Tika parser added in a future minor upgrade (image
      // OCR, audio, video, anything) MUST be added here explicitly to be reachable from the
      // indexer. This is the "explicit allowlist" pattern recommended by OWASP for file-format
      // detection surfaces.
      parsers.add(new TXTParser()); // text/*
      parsers.add(new HtmlParser()); // text/html, application/xhtml+xml
      parsers.add(new XMLParser()); // text/xml, application/xml, application/rdf+xml
      parsers.add(new PDFParser()); // application/pdf
      parsers.add(new OfficeParser()); // legacy application/msword, application/vnd.ms-excel,
      // application/vnd.ms-powerpoint
      parsers.add(new OOXMLParser()); // modern OOXML (docx/xlsx/pptx and templates)
      parsers.add(new RTFParser()); // application/rtf

      m_strictParser =
          new CompositeParser(
              m_tikaConfig.getMediaTypeRegistry(), Collections.unmodifiableList(parsers));
    }
    return m_strictParser;
  }

  public String getConvertedText(InputStream is, String mimetype)
      throws PSExtensionProcessingException {
    if (!m_mimeTypes.contains(mimetype)) return "";

    getTikaConfig();

    Parser parser = getStrictParser();
    Metadata metadata = new Metadata();
    metadata.set(HttpHeaders.CONTENT_TYPE, mimetype);
    WriteOutContentHandler handler = new WriteOutContentHandler(writeLimit);
    BodyContentHandler bodyhandler = new BodyContentHandler(handler);

    try (TikaInputStream tis =
        TikaInputStream.get(com.percussion.security.io.PSTikaCap.truncate(is))) {
      // getFile() Forces tika to stream to temporary file. parse uses
      // hasFile to decide whether processing should be done
      // using file or in memory. We want to preserve memory.
      // TikaInputStream.get(File file) also sets hasFile.
      tis.getFile();
      parser.parse(tis, bodyhandler, metadata, new ParseContext());
    } catch (SAXException e) {
      ms_log.warn(
          "Document text is larger than current index write limit of "
              + INDEX_WRITE_LIMIT
              + " chars. Only text up to this will be indexed, you can increase limit by setting"
              + " indexWriteLimit property in server.properties file. Increasing will use more"
              + " memory.");
      throw new PSExtensionProcessingException(e.getMessage(), e);
    } catch (Exception e) {
      ms_log.warn(
          "Document cannot be indexed, set debug trace to see full stack: ",
          ExceptionUtils.getRootCause(e));
      ms_log.debug("Document cannot be indexed", e);
      throw new PSExtensionProcessingException(e.getMessage(), e);
    }
    /*
     * Some documents can cause tika and the underlying parser to allocate way
     * too much memory We may not be able to allocate enough memory to handle
     * so we will try and recover and move on to next document. You should not
     * normally try and catch Errors, as they are usually non-recoverable. In
     * this case it is our only option or we will have to stop everything. If
     * the error is caused by an attempted allocation of a massive amount of
     * memory, that memory may be freed up and we can continue. it is possible
     * that another thread fails to allocate because of this and we cannot
     * recover those threads. A safer solution would to be to handle the
     * parsing on a separate jvm process so the server is not affected.
     */
    catch (OutOfMemoryError e) {
      ms_log.error(
          "Out of memory error while processing document while indexing you may need to increase"
              + " the java heap allocated to the CM1 server. This may just be a document that"
              + " cannot be handled currently by the underlying Tika processor. This Document will"
              + " be skipped");
    } finally {
      closeSilently(is);
    }

    return bodyhandler.toString();
  }

  private void closeSilently(InputStream stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.close();
    } catch (IOException e) {
      ms_log.debug("Failed to close stream", e);
    }
  }
}
