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

package com.percussion.delivery;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.PSMetadataExtractorService;
import com.percussion.delivery.metadata.rdfa.PSTripleHandler;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PSMetadataExtractorServiceTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testgetNamespace(){
        PSMetadataExtractorService svc = new PSMetadataExtractorService();

        PSTripleHandler handler = new PSTripleHandler();

        handler.getNamespace("");
    }

    @Test
    public void testgetPlainProperty(){
        PSMetadataExtractorService svc = new PSMetadataExtractorService();

        PSTripleHandler handler = new PSTripleHandler();

        handler.getPlainPropertyName("");
    }


    @Test
    public void testNoAbstract() throws IOException {
        InputStream is = PSMetadataExtractorServiceTests.class.getResourceAsStream(
                "/com/percussion/delivery/no-abstract.html");

        try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {

            PSMetadataExtractorService svc = new PSMetadataExtractorService();
            PSMetadataEntry entry = svc.process(inputStreamReader, "text/html",
                    "/Sites/test/no-abstract.html", null);

            assertNotNull(entry);
            assertEquals("page", entry.getType());
            HashMap map = new HashMap();

            for (IPSMetadataProperty prop : entry.getProperties()) {
                map.put(prop.getName(), prop.getValue());
            }

            assertEquals("test",map.get("dcterms:source"));
        }

    }

    @Test
    public void testbwcorona() throws IOException {
        InputStream is = PSMetadataExtractorServiceTests.class.getResourceAsStream(
                "/com/percussion/delivery/bw-corona.html");

        try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {

            PSMetadataExtractorService svc = new PSMetadataExtractorService();
            PSMetadataEntry entry = svc.process(inputStreamReader,"text/html",
                    "/Sites/www.bw.edu/bw-corona.html",null);

            assertNotNull(entry);
            assertEquals("page", entry.getType());
            HashMap map = new HashMap();

            for(IPSMetadataProperty prop : entry.getProperties()){
                map.put(prop.getName(),prop.getValue());
            }
            assertEquals("standard-flex",map.get("dcterms:source"));
            assertEquals("2020-04-27T09:50:00",map.get("dcterms:created"));
            assertEquals("2020-03-16T17:02:57",map.get("dcterms:modified"));
            assertEquals("Modified Spring Semester Schedule",entry.getLinktext());
            assertEquals("/Categories/Event Types/BW Seasons",map.get("perc:category"));
            assertEquals("1200",map.get("og:image:width"));
            assertEquals("630",map.get("og:image:height"));
            assertEquals("Baldwin Wallace University",map.get("og:site_name"));
            assertEquals("Coronavirus Updates | Baldwin Wallace University",map.get("og:title"));
            assertEquals("Find out the latest updates concerning the Coronavirus and Baldwin Wallace University.",map.get("og:description"));
            assertEquals("https://www.bw.edu/advisory/coronavirus/",map.get("og:url"));
            assertEquals("https://www.bw.edu/Assets/social-media/social-share-default.jpg",map.get("og:image"));
            assertEquals("en_US",map.get("og:locale"));
            assertEquals("summary_large_image",map.get("twitter:card"));
            assertEquals("Coronavirus Updates | Baldwin Wallace University",map.get("twitter:title"));
            assertEquals("Find out the latest updates concerning the Coronavirus and Baldwin Wallace University.",map.get("twitter:description"));
            assertEquals("https://www.bw.edu/Assets/social-media/social-share-default.jpg",map.get("twitter:image"));
            assertEquals("banner image for Coronavirus Update",map.get("twitter:image:alt"));
            assertEquals("<div class=\"rxbodyfield\">\n" +
                    " <p><img alt=\"banner image for Coronavirus Update\" height=\"121\" src=\"/Assets/home-page/seasons/seasons-coronavirus.jpg\" title=\"Coronavirus Update\" width=\"402\"></p>\n" +
                    " <p>BW has instituted a Modified Spring Semester Schedule in response to Novel COVID-19 (Coronavirus). <strong>There are currently no campus-associated cases of COVID-19.</strong></p>\n" +
                    "</div>",map.get("dcterms:abstract"));
            assertEquals("article",map.get("og:type"));

        }
    }

    @Test
    public void testEntityAndScriptHandling() throws IOException {
        InputStream is = PSMetadataExtractorServiceTests.class.getResourceAsStream(
                "/com/percussion/delivery/entity-test.html");

        try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {

            PSMetadataExtractorService svc = new PSMetadataExtractorService();
            PSMetadataEntry entry = svc.process(inputStreamReader, "text/html",
                    "/Sites/test/entity-test.html", null);

            assertNotNull(entry);
            HashMap<String, String> map = new HashMap<>();

            for (IPSMetadataProperty prop : entry.getProperties()) {
                map.put(prop.getName(), prop.getValue());
            }

            // Check title
            assertEquals("Comprehensive Test Title & More", map.get("dcterms:title"));
            
            // Check description - entities should be resolved by the parser
            // &copy; -> ©, &euro; -> €, &nbsp; -> non-breaking space (char 160)
            String description = map.get("dcterms:description");
            assertNotNull(description);
            
            // Check abstract
            String abstractText = map.get("dcterms:abstract");
            assertNotNull(abstractText);
        }
    }

    /**
     * Issue #4: HTML with unbound XML-style prefixes (Google CSE {@code gcse:search} without
     * {@code xmlns:gcse}) must not throw out of {@link PSMetadataExtractorService}; RDFa/dcterms
     * present elsewhere on the page must still be extracted.
     */
    @Test
    public void testUnboundPrefixGcseSearch() throws IOException {
        InputStream is = PSMetadataExtractorServiceTests.class.getResourceAsStream(
                "/com/percussion/delivery/unbound-prefix-gcse.html");
        assertNotNull("unbound-prefix-gcse.html fixture must be on the test classpath", is);

        try (InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            PSMetadataExtractorService svc = new PSMetadataExtractorService();
            PSMetadataEntry entry = svc.process(inputStreamReader, "text/html",
                    "/Sites/test/error.html", null);

            assertNotNull(entry);
            assertEquals("page", entry.getType());
            assertEquals("/Sites/test/error.html", entry.getPagepath());

            HashMap<String, String> map = new HashMap<>();
            for (IPSMetadataProperty prop : entry.getProperties()) {
                map.put(prop.getName(), prop.getValue());
            }

            assertEquals("gcse-fixture", map.get("dcterms:source"));
            assertEquals("CSE Page Title", map.get("dcterms:title"));
            assertEquals("Page with Google CSE embed", map.get("dcterms:description"));

            String abstractText = map.get("dcterms:abstract");
            assertNotNull("dcterms:abstract should still be extracted", abstractText);
            assertTrue("abstract content should survive prefix sanitize: " + abstractText,
                    abstractText.contains("Abstract survives unbound vendor prefix strip"));
        }
    }

    /**
     * Minimal inline HTML: unbound {@code gcse:search} alone must not throw; page still gets a
     * default type of {@code page}.
     */
    @Test
    public void testUnboundPrefixOnlyDoesNotThrow() {
        String html = "<!DOCTYPE html><html><head><title>x</title></head>"
                + "<body><gcse:search></gcse:search><p>ok</p></body></html>";
        PSMetadataExtractorService svc = new PSMetadataExtractorService();
        Reader reader = new StringReader(html);
        PSMetadataEntry entry;
        try {
            entry = svc.process(reader, "text/html", "/Sites/test/minimal-gcse.html", null);
        } catch (RuntimeException ex) {
            throw new AssertionError("unbound gcse: prefix must not fail metadata extraction", ex);
        }
        assertNotNull(entry);
        assertEquals("page", entry.getType());
        assertEquals("/Sites/test/minimal-gcse.html", entry.getPagepath());
    }

    /**
     * Unit-level coverage for the SAX unbound-prefix detection helper.
     *
     * <p>Detection requires either a {@link org.xml.sax.SAXParseException} (or
     * SAX-typed throwable) whose message contains {@code "not bound"}. A
     * plain {@link RuntimeException} wrapping the same message must NOT be
     * trusted — that would let unrelated RDF/SAX diagnostics that happen to
     * mention "prefix x for element" silently drop every RDFa triple on the
     * page.
     */
    @Test
    public void testIsUnboundPrefixParseFailureDetectsGcse() {
        Throwable ex = new org.xml.sax.SAXParseException(
                "The prefix \"gcse\" for element \"gcse:search\" is not bound.",
                "https://example.com/error.html", null, 0, 0);
        assertTrue(PSMetadataExtractorService.isUnboundPrefixParseFailure(ex));
        assertEquals("gcse", PSMetadataExtractorService.extractUnboundPrefix(ex));
    }

    @Test
    public void testIsUnboundPrefixParseFailureIgnoresUntypedThrowableWithSameMessage() {
        // A bare RuntimeException carrying the same wording must NOT be
        // tolerated — otherwise non-unbound RDF/SAX diagnostics that happen
        // to contain the words "prefix" and "not bound" would silently drop
        // every subsequent RDFa triple on the page.
        Throwable ex = new RuntimeException(
                "The prefix \"gcse\" for element \"gcse:search\" is not bound.");
        assertFalse(PSMetadataExtractorService.isUnboundPrefixParseFailure(ex));
    }

    @Test
    public void testIsUnboundPrefixParseFailureIgnoresOtherErrors() {
        Throwable ex = new RuntimeException("Some unrelated parse error");
        assertFalse(PSMetadataExtractorService.isUnboundPrefixParseFailure(ex));
        assertEquals(null, PSMetadataExtractorService.extractUnboundPrefix(ex));
    }

    @Test
    public void testIsUnboundPrefixParseFailureIgnoresUnboundPrefixInCauseChainOfUntypedThrowable() {
        // SAXParseException is in the cause chain of a plain RuntimeException
        // — should still be detected by walking the chain.
        Throwable root = new org.xml.sax.SAXParseException(
                "The prefix \"vendor\" for element \"vendor:widget\" is not bound.",
                "https://example.com/error.html", null, 0, 0);
        Throwable wrapper = new RuntimeException("wrapping", root);
        assertTrue(PSMetadataExtractorService.isUnboundPrefixParseFailure(wrapper));
        assertEquals("vendor", PSMetadataExtractorService.extractUnboundPrefix(wrapper));
    }

    /**
     * Direct unit test for {@link PSMetadataExtractorService#stripUnboundPrefixedMarkup(Document, String)}.
     *
     * <p>This is the only way to assert that the sanitizer itself is what
     * preserves {@code dcterms:*} metadata that appears after the unbound
     * markup. Without the strip, the SAX parser would throw on
     * {@code <gcse:search>} and never reach trailing {@code dcterms:*}
     * nodes; the integration test alone cannot distinguish "strip worked"
     * from "strip didn't run but the catch swallowed the throw".
     */
    @Test
    public void testStripUnboundPrefixedMarkupRemovesGcseAndVendorButKeepsDcterms() {
        String html = "<!DOCTYPE html><html xmlns:foo=\"http://example.com/foo\">"
                + "<head>"
                + "  <meta property=\"dcterms:title\" content=\"title\"/>"
                + "</head>"
                + "<body>"
                + "  <p property=\"dcterms:abstract\">trailing abstract</p>"
                + "  <gcse:search data-id=\"x\">cse</gcse:search>"
                + "  <span vendor:data-id=\"123\">body</span>"
                + "  <foo:bar baz=\"qux\">declared</foo:bar>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        PSMetadataExtractorService svc = new PSMetadataExtractorService();
        svc.stripUnboundPrefixedMarkup(doc, "/Sites/test/strip-fixture.html");

        // <gcse:search> and <vendor:...> element+attribute must be gone.
        // (Jsoup's CSS selector parser does not support tag names with `:`, so
        // we walk the tree directly for the bound-prefix assertion.)
        boolean hasGcseSearch = false;
        boolean hasVendorDataIdAttr = false;
        boolean hasFooBar = false;
        for (org.jsoup.nodes.Element el : doc.getAllElements()) {
            String tag = el.tagName();
            if ("gcse:search".equals(tag)) {
                hasGcseSearch = true;
            }
            if ("foo:bar".equals(tag)) {
                hasFooBar = true;
            }
            if (el.hasAttr("vendor:data-id")) {
                hasVendorDataIdAttr = true;
            }
        }
        assertFalse("gcse:search element should have been stripped", hasGcseSearch);
        assertFalse("vendor:data-id attribute should have been stripped", hasVendorDataIdAttr);
        assertTrue("foo:bar element with declared xmlns:foo must survive", hasFooBar);

        // dcterms:* metadata is intact.
        assertEquals("title",
                doc.select("meta[property='dcterms:title']").attr("content"));
        assertEquals("trailing abstract",
                doc.select("[property='dcterms:abstract']").text());
    }
}
