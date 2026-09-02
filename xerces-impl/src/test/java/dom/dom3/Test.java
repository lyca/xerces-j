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

package dom.dom3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.NodeImpl;
import org.apache.xerces.xs.ElementPSVI;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSSerializer;

/**
 * DOM Level 3 tests converted to JUnit 4.
 */
public class Test implements DOMErrorHandler, LSResourceResolver {

    private int errorCounter = 0;
    private String savedRegistryProp;

    private static String resolveResourceURI(String path) {
        if (path.startsWith("tests/")) {
            path = path.substring("tests/".length());
        }
        URL res = Test.class.getClassLoader().getResource(path);
        if (res != null) {
            return res.toExternalForm();
        }
        File f = new File(path);
        if (f.exists()) {
            return f.toURI().toString();
        }
        return path;
    }

    @BeforeEach
    public void setUp() {
        errorCounter = 0;
        savedRegistryProp = System.getProperty(DOMImplementationRegistry.PROPERTY);
        System.setProperty(DOMImplementationRegistry.PROPERTY,
            "org.apache.xerces.dom.DOMImplementationSourceImpl org.apache.xerces.dom.DOMXSImplementationSourceImpl");
    }

    @AfterEach
    public void tearDown() {
        if (savedRegistryProp != null) {
            System.setProperty(DOMImplementationRegistry.PROPERTY, savedRegistryProp);
        }
        else {
            System.clearProperty(DOMImplementationRegistry.PROPERTY);
        }
    }

    private DOMImplementationLS getDOMImplementationLS() throws Exception {
        DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
        assertNotNull(impl, "DOMImplementationLS should not be null");
        return impl;
    }

    @org.junit.jupiter.api.Test
    public void testLookupNamespace() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        Document doc = builder.parseURI(resolveResourceURI("dom/dom3/input.xml"));
        NodeList ls = doc.getElementsByTagName("a:elem_a");
        NodeImpl elem = (NodeImpl) ls.item(0);

        assertEquals("ns1", elem.lookupPrefix("http://www.example.com"));
        assertTrue(elem.isDefaultNamespace("http://www.example.com"));
        assertEquals("http://www.w3.org/2001/XMLSchema-instance", elem.lookupNamespaceURI("xsi"));

        ls = doc.getElementsByTagName("bar:leaf");
        elem = (NodeImpl) ls.item(0);
        assertEquals("foo", elem.lookupPrefix("url1:"));

        ls = doc.getElementsByTagName("elem8");
        elem = (NodeImpl) ls.item(0);
        Element e1 = doc.createElementNS("b:", "p:baz");
        e1.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:x", "b:");
        elem.appendChild(e1);

        assertEquals("p", ((NodeImpl) e1).lookupPrefix("b:"));
        assertEquals("http://www.w3.org/2001/XMLSchema-instance", elem.lookupNamespaceURI("xsi"));
    }

    @org.junit.jupiter.api.Test
    public void testNormalizeDocumentSchemaValidation() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = builder.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("validate", Boolean.TRUE);
        Document core = builder.parseURI(resolveResourceURI("dom/dom3/schema.xml"));
        assertEquals(0, errorCounter, "No errors should be reported");

        errorCounter = 0;
        NodeList ls2 = core.getElementsByTagName("decVal");
        Element testElem = (Element) ls2.item(0);
        testElem.removeAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns");

        ls2 = core.getElementsByTagName("v02:decVal");
        testElem = (Element) ls2.item(0);
        testElem.setPrefix("myPrefix");
        Element root = core.getDocumentElement();

        Element newElem = core.createElementNS(null, "decVal");
        newElem.appendChild(core.createTextNode("string"));
        root.insertBefore(newElem, testElem);

        newElem = core.createElementNS(null, "notInSchema");
        newElem.appendChild(core.createTextNode("added new element"));
        root.insertBefore(newElem, testElem);

        root.appendChild(core.createElementNS("UndefinedNamespace", "NS1:foo"));
        config = core.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("validate", Boolean.TRUE);
        config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
        core.normalizeDocument();
        assertEquals(3, errorCounter, "3 errors should be reported");

        errorCounter = 0;
        config.setParameter("validate", Boolean.FALSE);
        config.setParameter("comments", Boolean.FALSE);
        core.normalizeDocument();
        assertEquals(0, errorCounter, "No errors should be reported");
    }

    @org.junit.jupiter.api.Test
    public void testNormalizeDocumentPSVI() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = builder.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("validate", Boolean.TRUE);
        config.setParameter("psvi", Boolean.TRUE);
        Document core = builder.parseURI(resolveResourceURI("data/personal-schema.xml"));
        assertEquals(0, errorCounter, "No errors should be reported");

        NodeList ls2 = core.getElementsByTagName("person");
        Element testElem = (Element) ls2.item(0);
        assertEquals("person", ((ElementPSVI) testElem).getElementDeclaration().getName());

        Element e1 = core.createElementNS(null, "person");
        core.getDocumentElement().appendChild(e1);
        e1.setAttributeNS(null, "id", "newEmp");
        Element e2 = core.createElementNS(null, "name");
        e2.appendChild(core.createElementNS(null, "family"));
        e2.appendChild(core.createElementNS(null, "given"));
        e1.appendChild(e2);
        e1.appendChild(core.createElementNS(null, "email"));
        Element e3 = core.createElementNS(null, "link");
        e3.setAttributeNS(null, "manager", "Big.Boss");
        e1.appendChild(e3);

        testElem.removeAttributeNode(testElem.getAttributeNodeNS(null, "contr"));

        config = core.getDomConfig();
        errorCounter = 0;
        config.setParameter("psvi", Boolean.TRUE);
        config.setParameter("error-handler", this);
        config.setParameter("validate", Boolean.TRUE);
        config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
        core.normalizeDocument();
        assertEquals(0, errorCounter, "No errors should be reported");
        assertEquals("person", ((ElementPSVI) e1).getElementDeclaration().getName());
    }

    @org.junit.jupiter.api.Test
    public void testNormalizeDocumentCoreNamespaceFixup() {
        Document doc = new DocumentImpl();
        Element root = doc.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:stylesheet");
        doc.appendChild(root);
        root.setAttributeNS("http://attr1", "xsl:attr1", "");

        Element child1 = doc.createElementNS("http://child1", "NS2:child1");
        child1.setAttributeNS("http://attr2", "NS2:attr2", "");
        root.appendChild(child1);

        Element child2 = doc.createElementNS("http://child2", "NS4:child2");
        child2.setAttributeNS("http://attr3", "attr3", "");
        root.appendChild(child2);

        Element child3 = doc.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:child3");
        child3.setAttributeNS("http://a1", "attr1", "");
        child3.setAttributeNS("http://a2", "xsl:attr2", "");
        child3.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:a1", "http://a1");
        child3.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsl", "http://a2");

        Element child4 = doc.createElementNS(null, "child4");
        child4.setAttributeNS("http://a1", "xsl:attr1", "");
        child4.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "default");
        child3.appendChild(child4);
        root.appendChild(child3);

        doc.normalizeDocument();

        assertEquals("xsl:stylesheet", root.getNodeName());
        assertEquals("http://www.w3.org/1999/XSL/Transform", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "xsl"));
        assertEquals("http://attr1", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));

        assertEquals("NS2:child1", child1.getNodeName());
        assertEquals("http://child1", child1.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS2"));
        assertEquals("http://attr2", child1.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));

        assertEquals("xsl:child3", child3.getNodeName());
        assertEquals("http://a2", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));
        assertEquals("http://a1", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "a1"));
        assertEquals("http://www.w3.org/1999/XSL/Transform", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "xsl"));

        Attr attr = child3.getAttributeNodeNS("http://a2", "attr2");
        assertNotNull(attr, "NS1:attr2 != null");
        assertEquals(5, child3.getAttributes().getLength());

        Attr temp = child4.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "xmlns");
        assertEquals("xmlns", temp.getNodeName());
        assertEquals("", temp.getNodeValue());
    }

    @org.junit.jupiter.api.Test
    public void testSerializationNamespaceFixup() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        LSSerializer writer = impl.createLSSerializer();

        Document doc = new DocumentImpl();
        Element root = doc.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:stylesheet");
        doc.appendChild(root);
        root.setAttributeNS("http://attr1", "xsl:attr1", "");

        Element child1 = doc.createElementNS("http://child1", "NS2:child1");
        child1.setAttributeNS("http://attr2", "NS2:attr2", "");
        root.appendChild(child1);

        Element child2 = doc.createElementNS("http://child2", "NS4:child2");
        child2.setAttributeNS("http://attr3", "attr3", "");
        root.appendChild(child2);

        Element child3 = doc.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:child3");
        child3.setAttributeNS("http://a1", "attr1", "");
        child3.setAttributeNS("http://a2", "xsl:attr2", "");
        child3.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:a1", "http://a1");
        child3.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsl", "http://a2");

        Element child4 = doc.createElementNS(null, "child4");
        child4.setAttributeNS("http://a1", "xsl:attr1", "");
        child4.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "default");

        child3.appendChild(child4);
        root.appendChild(child3);

        writer.getDomConfig().setParameter("namespaces", Boolean.TRUE);
        String xmlData = writer.writeToString(doc);
        Reader r = new StringReader(xmlData);
        LSInput in = impl.createLSInput();
        in.setCharacterStream(r);
        doc = builder.parse(in);

        root = doc.getDocumentElement();
        child1 = (Element) root.getFirstChild();
        child2 = (Element) child1.getNextSibling();
        child3 = (Element) child2.getNextSibling();

        assertEquals("xsl:stylesheet", root.getNodeName());
        assertEquals("http://www.w3.org/1999/XSL/Transform", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "xsl"));
        assertEquals("http://attr1", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));

        assertEquals("NS2:child1", child1.getNodeName());
        assertEquals("http://child1", child1.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS2"));
        assertEquals("http://attr2", child1.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));

        assertEquals("xsl:child3", child3.getNodeName());
        assertEquals("http://a2", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "NS1"));
        assertEquals("http://a1", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "a1"));
        assertEquals("http://www.w3.org/1999/XSL/Transform", child3.getAttributeNS("http://www.w3.org/2000/xmlns/", "xsl"));

        Attr attr = child3.getAttributeNodeNS("http://a2", "attr2");
        assertNotNull(attr);
        assertEquals(5, child3.getAttributes().getLength());
    }

    @org.junit.jupiter.api.Test
    public void testReplaceWholeText() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = builder.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("validate", Boolean.FALSE);
        config.setParameter("entities", Boolean.TRUE);
        Document doc = builder.parseURI(resolveResourceURI("dom/dom3/wholeText.xml"));

        Element test = (Element) doc.getElementsByTagName("elem").item(0);
        test.appendChild(doc.createTextNode("Address: "));
        test.appendChild(doc.createEntityReference("ent2"));
        test.appendChild(doc.createTextNode("City: "));
        test.appendChild(doc.createEntityReference("ent1"));

        NodeList ls = test.getChildNodes();
        assertEquals(5, ls.getLength());

        String compare = "Home Address: 1900 Dallas Road (East) City: Dallas. California. USA  PO #5668";
        assertEquals(compare, ((Text) ls.item(0)).getWholeText());
        assertEquals(compare, ((Text) ls.item(1)).getWholeText());

        ((NodeImpl) ls.item(0)).setReadOnly(true, true);
        Text original = (Text) ls.item(0);
        Node newNode = original.replaceWholeText("Replace with this text");
        ls = test.getChildNodes();
        assertEquals(1, ls.getLength());
        assertEquals("Replace with this text", ls.item(0).getNodeValue());
        assertNotSame(original, newNode);

        Text text = doc.createTextNode("readonly");
        ((NodeImpl) text).setReadOnly(true, true);
        text = text.replaceWholeText("Data");
        assertEquals("Data", text.getNodeValue());

        final Element finalTest = (Element) doc.getElementsByTagName("elem").item(1);
        assertThrows(DOMException.class, () -> ((Text)finalTest.getFirstChild()).replaceWholeText("can't replace"),
                "Expected DOMException (NO_MODIFICATION_ALLOWED_ERR) due to read-only element constraint inside nested entity.");
    }

    @org.junit.jupiter.api.Test
    public void testSchemaTypeAndLocation() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = builder.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("resource-resolver", this);
        config.setParameter("validate", Boolean.TRUE);
        config.setParameter("psvi", Boolean.TRUE);

        errorCounter = 0;
        builder.parseURI(resolveResourceURI("dom/dom3/both-error.xml"));
        assertEquals(4, errorCounter, "4 errors should be reported");

        errorCounter = 0;
        config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
        builder.parseURI(resolveResourceURI("dom/dom3/both.xml"));
        assertEquals(0, errorCounter, "No errors should be reported");

        errorCounter = 0;
        config.setParameter("schema-type", "http://www.w3.org/TR/REC-xml");
        builder.parseURI(resolveResourceURI("dom/dom3/both-error.xml"));
        assertEquals(3, errorCounter, "3 errors should be reported");

        // parse with validate=false to load DOM, then normalize with schema-location
        config.setParameter("validate", Boolean.FALSE);
        config.setParameter("schema-type", null);
        Document core2 = builder.parseURI(resolveResourceURI("dom/dom3/both-error.xml"));

        errorCounter = 0;
        Element root = core2.getDocumentElement();
        root.removeAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        root.removeAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "noNamespaceSchemaLocation");

        config = core2.getDomConfig();
        config.setParameter("error-handler", this);
        config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
        config.setParameter("schema-location", "personal.xsd");
        config.setParameter("resource-resolver", this);
        config.setParameter("validate", Boolean.TRUE);
        core2.normalizeDocument();
        assertEquals(1, errorCounter, "1 error should be reported");
    }

    @org.junit.jupiter.api.Test
    public void testBaseURI() throws Exception {
        DOMImplementationLS impl = getDOMImplementationLS();
        LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        Document doc = parser.parseURI(resolveResourceURI("dom/dom3/baseURI.xml"));
        Element root = doc.getDocumentElement();

        NodeList ls = doc.getElementsByTagNameNS(null, "streetNum");
        Node e = ls.item(0);
        assertTrue(((NodeImpl) e).getBaseURI().endsWith("dom/dom3/baseURI.xml"));

        ls = root.getElementsByTagNameNS(null, "header");
        Node p2 = ls.item(0);
        assertEquals("http://paragraph.com", ((NodeImpl) p2).getBaseURI());

        p2 = ls.item(1);
        assertEquals("http://paragraph.com2", ((NodeImpl) p2).getBaseURI());
    }

    public boolean handleError(DOMError error) {
        short severity = error.getSeverity();
        if (severity == DOMError.SEVERITY_ERROR) {
            errorCounter++;
        }
        return true;
    }

    public LSInput resolveResource(String type, String namespace, String publicId, String systemId, String baseURI) {
        try {
            DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
            LSInput source = impl.createLSInput();
            if (systemId != null && systemId.endsWith("personal.xsd")) {
                source.setSystemId(resolveResourceURI("data/personal.xsd"));
            }
            else if (systemId != null && systemId.endsWith("personal.dtd")) {
                source.setSystemId(resolveResourceURI("data/personal.dtd"));
            }
            else {
                return null;
            }
            return source;
        }
        catch (Exception e) {
            return null;
        }
    }
}
