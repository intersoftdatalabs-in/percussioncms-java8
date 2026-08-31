/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.search.lucene.textconverter;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts the text from a supplied input stream corresponding to a PDF file. It uses PDFBox to
 * extract the text from the PDF Document. The following are the limitations.
 *
 * <ul>
 *   <li>Extracts only text and no meta data like author or created date etc.
 *   <li>If the document is password protected, the parser will attempt to use the empty password;
 *       if the document opens, text is extracted, otherwise an exception is raised and surfaced as
 *       a {@link PSExtensionProcessingException}.
 *   <li>The input stream is bounded at {@value #MAX_PDF_BYTES} bytes (see {@link
 *       BoundedInputStream}); oversized inputs are rejected before the parser is invoked.
 *   <li>The PDFBox memory working set is capped at {@value #MAX_PDF_BYTES} bytes ({@link
 *       MemoryUsageSetting#setupMixed(long)}); large allocations spill to a temp file.
 *   <li>PDFBox is run in pure-Java CMYK mode (no native deps for CMYK color conversion).
 * </ul>
 */
public class PSTextConverterPdf implements IPSLuceneTextConverter {

  /**
   * Hard cap on the input PDF size and on the PDFBox working-set. 64 MiB matches the value commonly
   * used for full-text indexing of moderate documents. Tunable in one place; raise with caution
   * (each request holds the full doc tree in memory or in a temp file under {@code
   * java.io.tmpdir}).
   */
  private static final long MAX_PDF_BYTES = 64L * 1024L * 1024L;

  static {
    // T2.9 hardening (issue #127): PDFBox recommends the pure-Java CMYK path for
    // server-side processing (no JNI / native-image dependency, fewer CVEs in the
    // native bridge). Set once on class load; idempotent.
    System.setProperty(
        "org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", Boolean.TRUE.toString());
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.search.lucene.textconverter.IPSLuceneTextConverter#getConvertedText(java.io.
   * InputStream, java.lang.String)
   */
  public String getConvertedText(InputStream is, String mimetype)
      throws PSExtensionProcessingException {
    if (is == null) throw new IllegalArgumentException("is must not be null");

    // T2.9 hardening (issue #127): bound the input before it reaches PDFBox. An
    // oversized (or maliciously streamed) PDF would otherwise be loaded into memory
    // in full and either OOM the JVM or run a multi-minute parse.
    BoundedInputStream bounded = new BoundedInputStream(is, MAX_PDF_BYTES);

    String resultText = "";
    PDDocument pdfDocument = null;
    try {
      // T2.9 hardening (issue #127): cap the parser's working set. The default
      // load(InputStream) allows unbounded memory; setupMixed() spills to a temp
      // file when the in-memory cache exceeds the threshold.
      pdfDocument = PDDocument.load(bounded, MemoryUsageSetting.setupMixed(MAX_PDF_BYTES));
      PDFTextStripper stripper = new PDFTextStripper();
      StringWriter writer = new StringWriter();
      stripper.writeText(pdfDocument, writer);
      resultText = writer.getBuffer().toString();
    } catch (Exception e) {
      throw new PSExtensionProcessingException(m_className, e);
    } finally {
      if (pdfDocument != null) {
        try {
          pdfDocument.close();
        } catch (IOException e) {
          throw new PSExtensionProcessingException(m_className, e);
        }
      }
    }
    return resultText;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
   * java.io.File)
   */
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {}

  /** A member variable to hold the name of this class. */
  private String m_className = getClass().getName();
}
