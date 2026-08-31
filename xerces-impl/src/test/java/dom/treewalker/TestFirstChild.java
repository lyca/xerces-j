/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dom.treewalker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Tests TreeWalker.firstChild() and TreeWalker.nextSibling().
 */
public class TestFirstChild {

    private Document createTestDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setValidating(false);
        dfactory.setNamespaceAware(true);

        DocumentBuilder db = dfactory.newDocumentBuilder();
        Document doc = db.newDocument();
        Element root = doc.createElement("RootElement");
        Element e1 = doc.createElement("Element1");
        Element e2 = doc.createElement("Element2");
        Element e3 = doc.createElement("Element3");
        Text e3t = doc.createTextNode("Text in Element3");

        e3.appendChild(e3t);
        root.appendChild(e1);
        root.appendChild(e2);
        root.appendChild(e3);
        doc.appendChild(root);
        return doc;
    }

    @Test
    public void testTreeWalkerFirstChildAndNextSibling() throws Exception {
        Document doc = createTestDocument();
        TreeWalker treewalker = ((DocumentTraversal) doc).createTreeWalker(
            doc, NodeFilter.SHOW_ALL, null, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        processTreeWalker(treewalker, pw);
        pw.flush();

        String expected = "<RootElement><Element1></Element1><Element2></Element2><Element3>Text in Element3</Element3></RootElement>";
        assertEquals(expected, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testTreeWalkerStepByStep() throws Exception {
        Document doc = createTestDocument();
        TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(
            doc, NodeFilter.SHOW_ALL, null, true);

        assertEquals("#document", tw.getCurrentNode().getNodeName());
        Node root = tw.firstChild();
        assertNotNull(root);
        assertEquals("RootElement", root.getNodeName());

        Node e1 = tw.firstChild();
        assertNotNull(e1);
        assertEquals("Element1", e1.getNodeName());
        assertNull(tw.firstChild());

        tw.setCurrentNode(e1);
        Node e2 = tw.nextSibling();
        assertNotNull(e2);
        assertEquals("Element2", e2.getNodeName());
        assertNull(tw.firstChild());

        tw.setCurrentNode(e2);
        Node e3 = tw.nextSibling();
        assertNotNull(e3);
        assertEquals("Element3", e3.getNodeName());

        Node e3t = tw.firstChild();
        assertNotNull(e3t);
        assertEquals("Text in Element3", e3t.getNodeValue());
        assertNull(tw.nextSibling());
    }

    private void processTreeWalker(TreeWalker treewalker, PrintWriter printwriter) {
        Node currentNode = treewalker.getCurrentNode();

        switch (currentNode.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                printwriter.print(currentNode.getNodeValue());
                break;
            case Node.ENTITY_REFERENCE_NODE:
            case Node.DOCUMENT_NODE:
            case Node.ELEMENT_NODE:
            default:
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    printwriter.print('<');
                    printwriter.print(currentNode.getNodeName());
                    printwriter.print(">");
                }

                Node node1 = treewalker.firstChild();
                while (node1 != null) {
                    processTreeWalker(treewalker, printwriter);
                    treewalker.setCurrentNode(node1);
                    node1 = treewalker.nextSibling();
                }

                treewalker.setCurrentNode(currentNode);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    printwriter.print("</");
                    printwriter.print(currentNode.getNodeName());
                    printwriter.print(">");
                }
                break;
        }
    }

}
