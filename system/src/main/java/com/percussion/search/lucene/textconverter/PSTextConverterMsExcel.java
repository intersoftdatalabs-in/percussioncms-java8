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
package com.percussion.search.lucene.textconverter;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.search.lucene.IPSLuceneConstants;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Extracts the text from input stream corresponding to Microsoft Excel file using POI api. Gets the
 * plain text from the cells that represent the String from all the sheets. Appends the cell text
 * with a blank space. The input is bounded at {@value #MAX_INPUT_BYTES} bytes (see {@link
 * PSTextConverterUtils#readAndCap}) before the POI parsers are invoked; oversized inputs are
 * rejected with an exception before any parser can be loaded.
 */
public class PSTextConverterMsExcel implements IPSLuceneTextConverter {

  /**
   * Hard cap on the input Excel file size. 64 MiB matches the value used by the T2.9 PDFBox
   * hardening (#128) and is well above any reasonable text-extraction workload for full-text
   * indexing. Tunable in one place; raise with caution (the resulting byte array is held in memory
   * for the duration of the parse).
   */
  private static final long MAX_INPUT_BYTES = 64L * 1024L * 1024L;

  /*
   * (non-Javadoc)
   *
   * @see com.percussion.search.lucene.textconverter.IPSLuceneTextConverter#getConvertedText(java.io.InputStream,
   * java.lang.String)
   */
  public String getConvertedText(InputStream is, String mimetype)
      throws PSExtensionProcessingException {
    if (is == null) throw new IllegalArgumentException("is must not be null");

    String resultText = "";
    try {
      // T2.10 hardening: read into a bounded byte array first so a crafted or oversized Excel
      // workbook is rejected before POI's HSSF (legacy .xls) or XSSF (.xlsx) parser can be
      // instantiated. Both parsers buffer the full workbook and have been the subject of OOM /
      // resource-exhaustion CVEs on 5.2.x; this caps the attack surface at MAX_INPUT_BYTES.
      byte[] data = PSTextConverterUtils.readAndCap(is, MAX_INPUT_BYTES);
      try (InputStream bounded = new ByteArrayInputStream(data)) {
        POITextExtractor ext = getTextExtractor(mimetype, bounded);
        resultText = ext.getText();
      }
    } catch (IOException e) {
      throw new PSExtensionProcessingException(m_className, e);
    }
    return resultText;
  }

  private POITextExtractor getTextExtractor(String mimetype, InputStream is) throws IOException {
    ExcelExtractor extractor;
    if (IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_EXCEL.equalsIgnoreCase(mimetype)
        || IPSLuceneConstants.MIME_TYPE_APPLICATION_BY_VNDMSEXCEL.equalsIgnoreCase(mimetype)) {
      extractor = new org.apache.poi.hssf.extractor.ExcelExtractor(new HSSFWorkbook(is));
    } else {
      extractor = new XSSFExcelExtractor(new XSSFWorkbook(is));
    }

    extractor.setIncludeSheetNames(false);
    extractor.setIncludeCellComments(false);

    return (POITextExtractor) extractor;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
   * java.io.File)
   */
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {}

  /** A memeber variable to hold the name of this class. */
  private String m_className = getClass().getName();
}
