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
package com.percussion.services.assembly.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.MathTool;
import org.junit.Test;

/**
 * Regression test for issue #367: verifies {@code perc_processNavigationNode} in {@code
 * sys_percNavigationWidget.vm} writes {@code 'Navigate to' Title Text} ({@code
 * $titleNavigateToText}) inside the rendered {@code <a>} {@code title} attribute.
 */
public class PSSysPercNavigationWidgetMacroTest {

    private static final String VM_DIR_REL_PATH =
            "system/cms/content/applications/sys_resources/ApplicationFiles/vm";
    private static final String VELOCITY_PROPS_REL_PATH =
            "system/Tools/velocity/config/velocity.properties";

    @Test
    public void shouldIncludeNavigateToPrefixInTitleAttributeForRootAndSubmenuLinks()
            throws Exception {
        StubNavNode root = createSampleTree();

        String html = renderNav(root, "yes", "Go to:", 3, 0);

        assertTrue(
                "Expected root link title attribute with prefix, got:\n" + html,
                html.contains(" title=\"Go to: Home\">Home</a>"));
        assertTrue(
                "Expected level-1 submenu link title attribute with prefix, got:\n" + html,
                html.contains(" title=\"Go to: Section One\">Section One</a>"));
        assertTrue(
                "Expected level-2 submenu link title attribute with prefix, got:\n" + html,
                html.contains(" title=\"Go to: Child Page\">Child Page</a>"));
        assertFalse(
                "Did not expect prefix text leaked before title attribute, got:\n" + html,
                html.contains("Go to: title="));
    }

    @Test
    public void shouldUseDefaultNavigateToPrefixWithSpaceSeparator() throws Exception {
        StubNavNode root = createSampleTree();

        String html = renderNav(root, "yes", "Navigate to", 3, 0);

        assertTrue(
                "Expected default 'Navigate to' prefix in title attribute, got:\n" + html,
                html.contains(" title=\"Navigate to Home\">Home</a>"));
        assertTrue(
                "Expected default 'Navigate to' prefix on child link, got:\n" + html,
                html.contains(" title=\"Navigate to Section One\">Section One</a>"));
    }

    @Test
    public void shouldOmitLeadingSpaceWhenNavigateToTextIsEmptyWhitespaceOrNull()
            throws Exception {
        StubNavNode root = createSampleTree();

        for (String prefix : new String[] {"", "   ", null}) {
            String html = renderNav(root, "yes", prefix, 3, 0);

            assertTrue(
                    "Expected title=\"Home\" without leading space for prefix="
                            + prefix
                            + ", got:\n"
                            + html,
                    html.contains(" title=\"Home\">Home</a>"));
            assertTrue(
                    "Expected title=\"Section One\" without leading space for prefix="
                            + prefix
                            + ", got:\n"
                            + html,
                    html.contains(" title=\"Section One\">Section One</a>"));
            assertFalse(
                    "Did not expect literal reference in output for prefix=" + prefix,
                    html.contains("$titleNavigateToText"));
            assertFalse(
                    "Did not expect literal 'null' in title attribute for prefix=" + prefix,
                    html.contains("title=\"null"));
        }
    }

    @Test
    public void shouldEscapeHtmlSpecialCharactersInPrefixAndDisplayTitle() throws Exception {
        StubNavNode root =
                new StubNavNode("guid-root", 0, "News & \"Events\"", "/index.html", "SELF", "");

        String html = renderNav(root, "yes", "Go to \"Docs\" & <Home>", 3, 0);

        assertTrue(
                "Expected HTML-escaped prefix and display title in title attribute, got:\n"
                        + html,
                html.contains(
                        " title=\"Go to &quot;Docs&quot; &amp; &lt;Home&gt; News &amp;"
                                + " &quot;Events&quot;\">News &amp; &quot;Events&quot;</a>"));
    }

    @Test
    public void shouldOmitTitleAttributeWhenAddTitleAttrsIsNo() throws Exception {
        StubNavNode root = createSampleTree();

        String html = renderNav(root, "no", "Go to:", 3, 0);

        assertFalse("Did not expect any title= attribute, got:\n" + html, html.contains("title="));
        assertFalse(
                "Did not expect 'Go to:' when addtitleattributes is no, got:\n" + html,
                html.contains("Go to:"));
    }

    @Test
    public void shouldRenderTitleAttributeWithTargetAndNonZeroRootLevel() throws Exception {
        StubNavNode root = new StubNavNode("guid-root", 0, "Home", "/index.html", "ANCESTOR", "");
        StubNavNode section =
                new StubNavNode(
                        "guid-sec", 1, "External Docs", "/docs/index.html", "SELF", "_blank");
        StubNavNode child =
                new StubNavNode(
                        "guid-child", 2, "API Guide", "/docs/api/index.html", "DESCENDANT", "");
        section.addSubmenu(child);
        root.addSubmenu(section);

        String html = renderNav(root, "yes", "Go to:", 3, 1);

        assertFalse(
                "Did not expect depth-0 root node rendered when rootlevel=1, got:\n" + html,
                html.contains(">Home</a>"));
        assertTrue(
                "Expected target='_blank' followed by title attribute, got:\n" + html,
                html.contains(" target='_blank' title=\"Go to: External Docs\">External Docs</a>"));
        assertTrue(
                "Expected child link title attribute when rootlevel=1, got:\n" + html,
                html.contains(" title=\"Go to: API Guide\">API Guide</a>"));
    }

    private static StubNavNode createSampleTree() {
        StubNavNode root = new StubNavNode("guid-root", 0, "Home", "/index.html", "SELF", "");
        StubNavNode section =
                new StubNavNode("guid-sec", 1, "Section One", "/section-one/index.html", "OTHER", "");
        StubNavNode child =
                new StubNavNode(
                        "guid-child",
                        2,
                        "Child Page",
                        "/section-one/child-page/index.html",
                        "OTHER",
                        "");
        section.addSubmenu(child);
        root.addSubmenu(section);
        return root;
    }

    private static String renderNav(
            StubNavNode root,
            String addTitleAttrs,
            String titleNavigateToText,
            int maxLevels,
            int rootLevel)
            throws Exception {
        File vmDir = resolveRepoFile(VM_DIR_REL_PATH);
        File propsFile = resolveRepoFile(VELOCITY_PROPS_REL_PATH);

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(propsFile)) {
            props.load(in);
        }
        props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, vmDir.getAbsolutePath());
        props.setProperty(RuntimeConstants.VM_LIBRARY, "sys_percNavigationWidget.vm");

        RuntimeInstance runtime = new RuntimeInstance();
        runtime.init(props);

        Map<String, Object> tools = new HashMap<>();
        tools.put("esc", new EscapeTool());
        tools.put("math", new MathTool());

        Map<String, Object> rx = new HashMap<>();
        rx.put("pageutils", new StubPageUtils());

        VelocityContext vc = new VelocityContext();
        vc.put("tools", tools);
        vc.put("rx", rx);
        vc.put("perc", new StubPerc());
        vc.put("linkContext", new Object());
        vc.put("rootNode", root);
        vc.put("addTitleAttrs", addTitleAttrs);
        vc.put("titleNavigateToText", titleNavigateToText);
        vc.put("maxlevels", maxLevels);
        vc.put("rootlevel", rootLevel);
        vc.put("maxRootItems", -1);
        vc.put("maxSubItems", -1);

        String template =
                "#perc_processNavigationNode($rootNode, 1, 1, $addTitleAttrs,"
                        + " $titleNavigateToText, $maxRootItems, $maxSubItems)";
        StringWriter writer = new StringWriter();
        runtime.evaluate(vc, writer, "sys_percNavigationWidget_test", template);
        return writer.toString();
    }

    private static File resolveRepoFile(String relativePath) throws IOException {
        File fromModule = new File(new File(".."), relativePath);
        if (fromModule.exists()) {
            return fromModule;
        }
        File fromRoot = new File(relativePath);
        if (fromRoot.exists()) {
            return fromRoot;
        }
        throw new IOException(
                "Could not locate " + relativePath + " from " + new File(".").getAbsolutePath());
    }

    public static class StubPageUtils {
        public String navLink(Object linkContext, StubNavNode node) {
            return node.link;
        }
    }

    public static class StubPerc {
        private final StubWidget widget = new StubWidget();

        public StubWidget getWidget() {
            return widget;
        }
    }

    public static class StubWidget {
        private final StubWidgetItem item = new StubWidgetItem();

        public StubWidgetItem getItem() {
            return item;
        }
    }

    public static class StubWidgetItem {
        public String getId() {
            return "42";
        }
    }

    public static class StubProperty {
        private final String value;

        StubProperty(String value) {
            this.value = value;
        }

        public String getString() {
            return value;
        }
    }

    public static class StubNavNode {
        private final String guid;
        private final int depth;
        private final String link;
        private final Map<String, StubProperty> properties = new HashMap<>();
        private final List<StubNavNode> submenu = new ArrayList<>();
        private StubNavNode parent;

        StubNavNode(
                String guid,
                int depth,
                String displayTitle,
                String link,
                String axis,
                String target) {
            this.guid = guid;
            this.depth = depth;
            this.link = link;
            this.parent = null;
            properties.put("nav:axis", new StubProperty(axis));
            properties.put("rx:cssClassNames", new StubProperty(""));
            properties.put("rx:no_type", new StubProperty("section"));
            properties.put("rx:no_externalurl", new StubProperty(""));
            properties.put("rx:no_target", new StubProperty(target));
            properties.put("rx:displaytitle", new StubProperty(displayTitle));
        }

        void addSubmenu(StubNavNode child) {
            child.parent = this;
            submenu.add(child);
        }

        public int getDepth() {
            return depth;
        }

        public List<StubNavNode> getNodes(String name) {
            if ("nav:submenu".equals(name)) {
                return submenu;
            }
            return Collections.emptyList();
        }

        public StubProperty getProperty(String name) {
            StubProperty prop = properties.get(name);
            return prop != null ? prop : new StubProperty("");
        }

        public StubNavNode getParent() {
            return parent;
        }

        public String getGuid() {
            return guid;
        }
    }
}
