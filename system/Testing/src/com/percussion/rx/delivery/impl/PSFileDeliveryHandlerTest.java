/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.percussion.rx.delivery.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.percussion.rx.delivery.IPSDeliveryResult;
import com.percussion.rx.delivery.IPSDeliveryResult.Outcome;
import com.percussion.rx.delivery.impl.PSBaseDeliveryHandler.Item;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSPurgableTempFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Unit tests for {@link PSFileDeliveryHandler}. */
public class PSFileDeliveryHandlerTest {

  private static final String XLSX_MIME =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String PPTX_MIME =
      "application/vnd.openxmlformats-officedocument.presentationml.presentation";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final List<PSPurgableTempFile> tempFiles = new ArrayList<>();
  private final PSFileDeliveryHandler handler = new PSFileDeliveryHandler();

  @After
  public void tearDown() {
    for (PSPurgableTempFile f : tempFiles) {
      f.delete();
    }
  }

  private static byte[] buildOoxmlLikePayload() {
    // Include ZIP magic header (PK\x03\x04), 0xA0 at byte offset 22 (from #325),
    // and every byte value 0x00..0xFF.
    byte[] payload = new byte[32 + 256];
    payload[0] = 0x50;
    payload[1] = 0x4B;
    payload[2] = 0x03;
    payload[3] = 0x04;
    for (int i = 4; i < 32; i++) {
      payload[i] = (byte) (i * 7);
    }
    payload[22] = (byte) 0xA0;
    for (int b = 0; b < 256; b++) {
      payload[32 + b] = (byte) b;
    }
    return payload;
  }

  private PSPurgableTempFile writeTempFile(String suffix, byte[] bytes) throws IOException {
    PSPurgableTempFile tempFile = new PSPurgableTempFile("delivery-test-", suffix, null);
    tempFiles.add(tempFile);
    try (OutputStream os = new FileOutputStream(tempFile)) {
      os.write(bytes);
    }
    return tempFile;
  }

  private void assertDeliversFileBranchIdentically(String name, String mimeType, byte[] payload)
      throws Exception {
    PSPurgableTempFile tempFile = writeTempFile("-" + name, payload);
    File dest = new File(tempFolder.getRoot(), "file-branch/" + name);
    Item item = handler.new Item(new PSGuid(), tempFile, null, mimeType, 100L, false, 1L, null, 0);

    IPSDeliveryResult result = handler.doDelivery(item, 1L, dest.getAbsolutePath());

    assertEquals("Expected DELIVERED for " + mimeType, Outcome.DELIVERED, result.getOutcome());
    assertTrue("Destination should exist for " + name, dest.isFile());
    assertArrayEquals(
        "Delivered bytes must match input for " + mimeType,
        payload,
        Files.readAllBytes(dest.toPath()));
  }

  private void assertDeliversStreamBranchIdentically(String name, String mimeType, byte[] payload)
      throws Exception {
    File dest = new File(tempFolder.getRoot(), "stream-branch/" + name);
    Item item =
        handler.new Item(
            new PSGuid(), null, payload, null, payload.length, mimeType, 101L, false, 1L, null, 0);

    IPSDeliveryResult result = handler.doDelivery(item, 1L, dest.getAbsolutePath());

    assertEquals("Expected DELIVERED for " + mimeType, Outcome.DELIVERED, result.getOutcome());
    assertTrue("Destination should exist for " + name, dest.isFile());
    assertArrayEquals(
        "Delivered stream bytes must match input for " + mimeType,
        payload,
        Files.readAllBytes(dest.toPath()));
  }

  @Test
  public void testOoxmlAssetsDeliveredByteIdenticallyViaTempFile() throws Exception {
    byte[] ooxmlPayload = buildOoxmlLikePayload();
    assertDeliversFileBranchIdentically("excel.xlsx", XLSX_MIME, ooxmlPayload);
    assertDeliversFileBranchIdentically("word.docx", DOCX_MIME, ooxmlPayload);
    assertDeliversFileBranchIdentically("ppt.pptx", PPTX_MIME, ooxmlPayload);
  }

  @Test
  public void testOoxmlAssetsDeliveredByteIdenticallyViaInMemoryStream() throws Exception {
    byte[] ooxmlPayload = buildOoxmlLikePayload();
    assertDeliversStreamBranchIdentically("excel.xlsx", XLSX_MIME, ooxmlPayload);
    assertDeliversStreamBranchIdentically("word.docx", DOCX_MIME, ooxmlPayload);
    assertDeliversStreamBranchIdentically("ppt.pptx", PPTX_MIME, ooxmlPayload);
  }

  @Test
  public void testOtherBinaryMimeTypesContainingXmlOrHtmlAreNotCorrupted() throws Exception {
    byte[] payload = buildOoxmlLikePayload();
    assertDeliversFileBranchIdentically("page.html.gz", "text/x-gzip-html", payload);
    assertDeliversFileBranchIdentically("doc.sxw", "application/vnd.sun.xml.writer", payload);
    assertDeliversStreamBranchIdentically("help.chm", "application/vnd.ms-htmlhelp", payload);
  }

  @Test
  public void testUtf8AndNonUtf8HtmlAndXmlDeliveredByteIdentically() throws Exception {
    byte[] utf8Html =
        "<!DOCTYPE html><html><body>Copyright © € — 日本語</body></html>"
            .getBytes(StandardCharsets.UTF_8);
    byte[] utf8Xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>€ — 日本語</root>"
        .getBytes(StandardCharsets.UTF_8);
    // ISO-8859-1 / Windows-1252 HTML with raw 0xA0 (&nbsp;) and 0xE9 ('é') bytes
    byte[] latin1Html = "<html><body>Caf\u00e9\u00a0Test</body></html>"
        .getBytes(StandardCharsets.ISO_8859_1);

    assertDeliversFileBranchIdentically("utf8.html", "text/html", utf8Html);
    assertDeliversStreamBranchIdentically("utf8.xml", "application/xml", utf8Xml);
    assertDeliversFileBranchIdentically("latin1.html", "text/html; charset=ISO-8859-1", latin1Html);
  }

  @Test
  public void testNullMimeTypeDeliversByteIdentically() throws Exception {
    byte[] payload = buildOoxmlLikePayload();
    assertDeliversFileBranchIdentically("no-mime-file.bin", null, payload);
    assertDeliversStreamBranchIdentically("no-mime-stream.bin", null, payload);
  }

  @Test
  public void testFailureWithNullFileReturnsFailedWithoutNpe() throws Exception {
    File existingDir = tempFolder.newFolder("dest-is-dir");
    byte[] payload = new byte[] {1, 2, 3, 4};
    Item item =
        handler.new Item(
            new PSGuid(), null, payload, null, payload.length, XLSX_MIME, 102L, false, 1L, null, 0);

    IPSDeliveryResult result = handler.doDelivery(item, 1L, existingDir.getAbsolutePath());

    assertEquals(Outcome.FAILED, result.getOutcome());
    assertNotNull(result.getFailureMessage());
  }
}
