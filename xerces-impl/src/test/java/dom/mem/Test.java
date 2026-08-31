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

package dom.mem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.xerces.dom.DOMImplementationImpl;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.NodeImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

/**
 * Various DOM functionality and regression tests.
 */
public class Test {

    @org.junit.jupiter.api.Test
    public void testCreateDocumentAndNodes() {
        Document doc = new DocumentImpl();
        assertNotNull(doc);

        Element el = doc.createElement("Doc02Element");
        assertNotNull(el);

        DocumentFragment frag = doc.createDocumentFragment();
        assertNotNull(frag);

        Text text = doc.createTextNode("Doc02TextNode");
        assertNotNull(text);

        Comment comment = doc.createComment("Doc02Comment");
        assertNotNull(comment);

        CDATASection cdataSec = doc.createCDATASection("Doc02CDataSection");
        assertNotNull(cdataSec);

        DocumentType docType = doc.getImplementation().createDocumentType("Doc02DocumentType", null, null);
        assertNotNull(docType);

        Notation notation = ((DocumentImpl) doc).createNotation("Doc02Notation");
        assertNotNull(notation);

        ProcessingInstruction pi = doc.createProcessingInstruction("Doc02PITarget", "Doc02PIData");
        assertNotNull(pi);

        Attr attribute = doc.createAttribute("Doc02Attribute");
        assertNotNull(attribute);

        EntityReference er = doc.createEntityReference("Doc02EntityReference");
        assertNotNull(er);

        NodeList nodeList = doc.getElementsByTagName("*");
        assertNotNull(nodeList);
    }

    @org.junit.jupiter.api.Test
    public void testDocTreeAndSiblings() {
        Document doc = new DocumentImpl();
        Element rootEl = doc.createElement("Doc03RootElement");
        doc.appendChild(rootEl);

        Text textNode = doc.createTextNode("Doc03 text stuff");
        assertNull(rootEl.getFirstChild());
        assertNull(rootEl.getLastChild());

        rootEl.appendChild(textNode);
        assertSame(textNode, rootEl.getFirstChild());
        assertSame(textNode, rootEl.getLastChild());
        assertNull(textNode.getNextSibling());
        assertNull(textNode.getPreviousSibling());

        Text textNode2 = doc.createTextNode("Doc03 text stuff");
        rootEl.appendChild(textNode2);
        assertSame(textNode2, textNode.getNextSibling());
        assertNull(textNode2.getNextSibling());
        assertNull(textNode.getPreviousSibling());
        assertSame(textNode, textNode2.getPreviousSibling());

        assertSame(textNode, rootEl.getFirstChild());
        assertSame(textNode2, rootEl.getLastChild());
    }

    @org.junit.jupiter.api.Test
    public void testAttributes() {
        Document doc = new DocumentImpl();
        Element rootEl = doc.createElement("RootElement");
        doc.appendChild(rootEl);

        Attr attr01 = doc.createAttribute("Attr01");
        rootEl.setAttributeNode(attr01);
        Attr attr02 = doc.createAttribute("Attr01");
        rootEl.setAttributeNode(attr02);

        Attr attr03 = doc.createAttribute("Attr03");
        rootEl.setAttributeNode(attr03);
        attr03.setValue("Attr03Value1");
        assertEquals("Attr03Value1", attr03.getValue());
        attr03.setValue("Attr03Value2");
        assertEquals("Attr03Value2", attr03.getValue());
    }

    @org.junit.jupiter.api.Test
    public void testTextSplitAndNormalize() {
        Document doc = new DocumentImpl();
        Element rootEl = doc.createElement("RootElement");
        doc.appendChild(rootEl);

        Text txt1 = doc.createTextNode("Hello Goodbye");
        rootEl.appendChild(txt1);

        txt1.splitText(6);
        assertEquals(2, rootEl.getChildNodes().getLength());
        rootEl.normalize();
        assertEquals(1, rootEl.getChildNodes().getLength());
        assertEquals("Hello Goodbye", rootEl.getFirstChild().getNodeValue());
    }

    @org.junit.jupiter.api.Test
    public void testNamedNodeMap() {
        Document doc = new DocumentImpl();
        NamedNodeMap nnm = doc.getAttributes();
        assertNull(nnm);

        Element el = doc.createElement("NamedNodeMap01");
        NamedNodeMap nnm2 = el.getAttributes();
        assertNotNull(nnm2);
        assertEquals(0, nnm2.getLength());
    }

    @org.junit.jupiter.api.Test
    public void testImportNode() {
        Document doc1 = new DocumentImpl();
        Document doc2 = new DocumentImpl();

        Element el1 = doc1.createElement("abc");
        doc1.appendChild(el1);
        assertNotNull(el1.getParentNode());
        el1.setAttribute("foo", "foovalue");

        Node el2 = doc2.importNode(el1, true);
        assertNull(el2.getParentNode());
        assertEquals("abc", el2.getNodeName());
        assertSame(doc2, el2.getOwnerDocument());
        assertEquals("foovalue", ((Element) el2).getAttribute("foo"));
        assertNotSame(doc1, doc2);
    }

    @org.junit.jupiter.api.Test
    public void testGetLength() {
        Document doc = new DocumentImpl();
        Text tx = doc.createTextNode("Hello");
        Element el = doc.createElement("abc");
        el.appendChild(tx);

        int textLength = tx.getLength();
        assertEquals(5, textLength);

        NodeList nl = tx.getChildNodes();
        assertEquals(0, nl.getLength());

        nl = el.getChildNodes();
        assertEquals(1, nl.getLength());
    }

    @org.junit.jupiter.api.Test
    public void testNodeList() {
        Document doc = new DocumentImpl();
        NodeList nl = doc.getChildNodes();
        assertNotNull(nl);
        assertEquals(0, nl.getLength());

        Element el = doc.createElement("NodeList01");
        doc.appendChild(el);
        assertEquals(1, nl.getLength());
    }

    @org.junit.jupiter.api.Test
    public void testNameValidity() {
        Document doc = new DocumentImpl();
        try {
            doc.createElement("!@@ bad element name");
            fail("Expected DOMException.INVALID_CHARACTER_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.INVALID_CHARACTER_ERR, e.code);
        }
    }

    @org.junit.jupiter.api.Test
    public void testCloningWithAttributes() {
        Document doc = new DocumentImpl();
        Element root = doc.createElement("CTestRoot");
        root.setAttribute("CTestAttr", "CTestAttrValue");

        assertEquals("CTestAttrValue", root.getAttribute("CTestAttr"));

        Element cloned = (Element) root.cloneNode(true);
        Attr a = cloned.getAttributeNode("CTestAttr");
        assertNotNull(a);
        assertEquals("CTestAttrValue", a.getValue());
    }

    @org.junit.jupiter.api.Test
    public void testCloningDefaultAttributes() {
        Document doc = new DocumentImpl();
        Element root = doc.createElement("CTestRoot");
        root.setAttribute("attr", "attrValue");
        Attr attr = root.getAttributeNode("attr");
        ((org.apache.xerces.dom.AttrImpl) attr).setSpecified(false);
        root.setAttribute("attr2", "attr2Value");

        Element cloned = (Element) root.cloneNode(true);
        Attr a = cloned.getAttributeNode("attr");
        assertFalse(a.getSpecified());
        a = cloned.getAttributeNode("attr2");
        assertTrue(a.getSpecified());

        a = (Attr) attr.cloneNode(true);
        assertTrue(a.getSpecified());
    }

    @org.junit.jupiter.api.Test
    public void testHasFeature() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        assertTrue(impl.hasFeature("XML", "2.0"));
        assertTrue(impl.hasFeature("XML", null));
        assertTrue(impl.hasFeature("XML", "1.0"));
        assertTrue(impl.hasFeature("Traversal", null));
        assertTrue(impl.hasFeature("Events", null));
        assertTrue(impl.hasFeature("MutationEvents", null));
        assertTrue(impl.hasFeature("Range", null));

        assertFalse(impl.hasFeature("HTML", null));
        assertFalse(impl.hasFeature("Views", null));
        assertFalse(impl.hasFeature("StyleSheets", null));
        assertFalse(impl.hasFeature("CSS", null));
        assertFalse(impl.hasFeature("CSS2", null));
        assertFalse(impl.hasFeature("UIEvents", null));
        assertFalse(impl.hasFeature("MouseEvents", null));
        assertFalse(impl.hasFeature("HTMLEvents", null));
    }

    @org.junit.jupiter.api.Test
    public void testCreateDocumentType() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();

        String qName = "foo:docName";
        String pubId = "pubId";
        String sysId = "http://sysId";

        DocumentType dt = impl.createDocumentType(qName, pubId, sysId);
        assertNotNull(dt);
        assertEquals(Node.DOCUMENT_TYPE_NODE, dt.getNodeType());
        assertEquals(qName, dt.getNodeName());
        assertNull(dt.getNamespaceURI());
        assertNull(dt.getPrefix());
        assertNull(dt.getLocalName());
        assertEquals(pubId, dt.getPublicId());
        assertEquals(sysId, dt.getSystemId());
        assertNull(dt.getInternalSubset());
        assertNull(dt.getOwnerDocument());

        assertEquals(0, dt.getEntities().getLength());
        assertEquals(0, dt.getNotations().getLength());

        qName = "docName";
        dt = impl.createDocumentType(qName, pubId, sysId);
        assertNotNull(dt);
        assertEquals(qName, dt.getNodeName());

        try {
            impl.createDocumentType("<docName", pubId, sysId);
            fail("Expected INVALID_CHARACTER_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.INVALID_CHARACTER_ERR, e.code);
        }

        try {
            impl.createDocumentType(":docName", pubId, sysId);
            fail("Expected NAMESPACE_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.NAMESPACE_ERR, e.code);
        }

        try {
            impl.createDocumentType("docName:", pubId, sysId);
            fail("Expected NAMESPACE_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.NAMESPACE_ERR, e.code);
        }

        try {
            impl.createDocumentType("<doc::Name", pubId, sysId);
            fail("Expected NAMESPACE_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.NAMESPACE_ERR, e.code);
        }

        try {
            impl.createDocumentType("<doc:N:ame", pubId, sysId);
            fail("Expected NAMESPACE_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.NAMESPACE_ERR, e.code);
        }
    }

    @org.junit.jupiter.api.Test
    public void testCreateDocument() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();

        String qName = "foo:docName";
        String pubId = "pubId";
        String sysId = "http://sysId";

        DocumentType dt = impl.createDocumentType(qName, pubId, sysId);
        String docNSURI = "http://document.namespace";
        Document doc = impl.createDocument(docNSURI, qName, dt);

        assertSame(doc, dt.getOwnerDocument());
        assertNull(doc.getOwnerDocument());
        assertEquals(Node.DOCUMENT_NODE, doc.getNodeType());
        assertSame(dt, doc.getDoctype());
        assertEquals("#document", doc.getNodeName());
        assertNull(doc.getNodeValue());

        Element el = doc.getDocumentElement();
        assertEquals("docName", el.getLocalName());
        assertEquals(docNSURI, el.getNamespaceURI());
        assertEquals(qName, el.getNodeName());
        assertSame(doc, el.getOwnerDocument());
        assertSame(doc, el.getParentNode());
        assertEquals("foo", el.getPrefix());
        assertEquals(qName, el.getTagName());
        assertFalse(el.hasChildNodes());

        try {
            impl.createDocument(docNSURI, qName, dt);
            fail("Expected WRONG_DOCUMENT_ERR");
        } catch (DOMException e) {
            assertEquals(DOMException.WRONG_DOCUMENT_ERR, e.code);
        }
    }

    @org.junit.jupiter.api.Test
    public void testCreateElementNS() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        String qName = "foo:docName";
        DocumentType dt = impl.createDocumentType(qName, "pubId", "http://sysId");
        String docNSURI = "http://document.namespace";
        Document doc = impl.createDocument(docNSURI, qName, dt);
        Element rootEl = doc.getDocumentElement();

        Element ela = doc.createElementNS("http://nsa", "a:ela");
        Element elb = doc.createElementNS("http://nsb", "elb");
        Element elc = doc.createElementNS(null, "elc");

        rootEl.appendChild(ela);
        rootEl.appendChild(elb);
        rootEl.appendChild(elc);

        assertEquals("a:ela", ela.getNodeName());
        assertEquals("http://nsa", ela.getNamespaceURI());
        assertEquals("a", ela.getPrefix());
        assertEquals("ela", ela.getLocalName());
        assertEquals("a:ela", ela.getTagName());

        assertEquals("elb", elb.getNodeName());
        assertEquals("http://nsb", elb.getNamespaceURI());
        assertNull(elb.getPrefix());
        assertEquals("elb", elb.getLocalName());
        assertEquals("elb", elb.getTagName());

        assertEquals("elc", elc.getNodeName());
        assertNull(elc.getNamespaceURI());
        assertNull(elc.getPrefix());
        assertEquals("elc", elc.getLocalName());
        assertEquals("elc", elc.getTagName());

        assertDOMException(() -> doc.createElementNS("http://nsa", "<a"), DOMException.INVALID_CHARACTER_ERR);
        assertDOMException(() -> doc.createElementNS("http://nsa", ":a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS("http://nsa", "a:"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS("http://nsa", "a::a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS("http://nsa", "a:a:a"), DOMException.NAMESPACE_ERR);

        String xmlURI = "http://www.w3.org/XML/1998/namespace";
        assertEquals(xmlURI, doc.createElementNS(xmlURI, "xml:a").getNamespaceURI());

        assertDOMException(() -> doc.createElementNS("http://nsa", "xml:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS("", "xml:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS(null, "xml:a"), DOMException.NAMESPACE_ERR);

        assertDOMException(() -> doc.createElementNS("http://nsa", "xmlns"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createElementNS(xmlURI, "xmlns"), DOMException.NAMESPACE_ERR);
        assertNull(doc.createElementNS(null, "noNamespace").getNamespaceURI());
        assertDOMException(() -> doc.createElementNS(null, "xmlns:a"), DOMException.NAMESPACE_ERR);

        assertEquals("http://nsa", doc.createElementNS("http://nsa", "foo:a").getNamespaceURI());
        assertDOMException(() -> doc.createElementNS(null, "foo:a"), DOMException.NAMESPACE_ERR);

        // Change prefix
        Element elem = doc.createElementNS("http://nsa", "foo:a");
        elem.setPrefix("bar");
        assertEquals("bar:a", elem.getNodeName());
        assertEquals("http://nsa", elem.getNamespaceURI());
        assertEquals("bar", elem.getPrefix());
        assertEquals("a", elem.getLocalName());
        assertEquals("bar:a", elem.getTagName());

        elem = doc.createElementNS("http://nsa", "a");
        assertNull(elem.getPrefix());
        elem.setPrefix("bar");
        assertEquals("bar:a", elem.getNodeName());
        assertEquals("http://nsa", elem.getNamespaceURI());
        assertEquals("bar", elem.getPrefix());
        assertEquals("a", elem.getLocalName());
        assertEquals("bar:a", elem.getTagName());

        elem = doc.createElementNS(xmlURI, "foo:a");
        elem.setPrefix("xml");
        Element elemForXmlErr = doc.createElementNS("http://nsa", "foo:a");
        assertDOMException(() -> elemForXmlErr.setPrefix("xml"), DOMException.NAMESPACE_ERR);

        elem.setPrefix("xmlns");
        Element elemNoNS = doc.createElementNS(null, "a");
        assertDOMException(() -> elemNoNS.setPrefix("foo"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.setPrefix("foo"), DOMException.NAMESPACE_ERR);
    }

    @org.junit.jupiter.api.Test
    public void testCreateAttributeNS() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        DocumentType dt = impl.createDocumentType("foo:docName", "pubId", "http://sysId");
        Document doc = impl.createDocument("http://document.namespace", "foo:docName", dt);

        Attr attra = doc.createAttributeNS("http://nsa", "a:attra");
        Attr attrb = doc.createAttributeNS("http://nsb", "attrb");
        Attr attrc = doc.createAttributeNS(null, "attrc");

        assertEquals("a:attra", attra.getNodeName());
        assertEquals("http://nsa", attra.getNamespaceURI());
        assertEquals("a", attra.getPrefix());
        assertEquals("attra", attra.getLocalName());
        assertEquals("a:attra", attra.getName());
        assertNull(attra.getOwnerElement());

        assertEquals("attrb", attrb.getNodeName());
        assertEquals("http://nsb", attrb.getNamespaceURI());
        assertNull(attrb.getPrefix());
        assertEquals("attrb", attrb.getLocalName());
        assertEquals("attrb", attrb.getName());
        assertNull(attrb.getOwnerElement());

        assertEquals("attrc", attrc.getNodeName());
        assertNull(attrc.getNamespaceURI());
        assertNull(attrc.getPrefix());
        assertEquals("attrc", attrc.getLocalName());
        assertEquals("attrc", attrc.getName());
        assertNull(attrc.getOwnerElement());

        assertDOMException(() -> doc.createAttributeNS("http://nsa", "<a"), DOMException.INVALID_CHARACTER_ERR);
        assertDOMException(() -> doc.createAttributeNS("http://nsa", ":a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "a:"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "a::a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "a:a:a"), DOMException.NAMESPACE_ERR);

        String xmlURI = "http://www.w3.org/XML/1998/namespace";
        assertEquals(xmlURI, doc.createAttributeNS(xmlURI, "xml:a").getNamespaceURI());
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "xml:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("", "xml:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS(null, "xml:a"), DOMException.NAMESPACE_ERR);

        String xmlnsURI = "http://www.w3.org/2000/xmlns/";
        assertEquals(xmlnsURI, doc.createAttributeNS(xmlnsURI, "xmlns").getNamespaceURI());
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "xmlns"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS(xmlURI, "xmlns"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("", "xmlns"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS(null, "xmlns"), DOMException.NAMESPACE_ERR);

        assertEquals(xmlnsURI, doc.createAttributeNS(xmlnsURI, "xmlns:a").getNamespaceURI());
        assertDOMException(() -> doc.createAttributeNS("http://nsa", "xmlns:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS(xmlURI, "xmlns:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS("", "xmlns:a"), DOMException.NAMESPACE_ERR);
        assertDOMException(() -> doc.createAttributeNS(null, "xmlns:a"), DOMException.NAMESPACE_ERR);

        assertEquals("http://nsa", doc.createAttributeNS("http://nsa", "foo:a").getNamespaceURI());
        assertDOMException(() -> doc.createAttributeNS(null, "foo:a"), DOMException.NAMESPACE_ERR);

        // Change prefix
        Attr attr = doc.createAttributeNS("http://nsa", "foo:a");
        attr.setPrefix("bar");
        assertEquals("bar:a", attr.getNodeName());
        assertEquals("http://nsa", attr.getNamespaceURI());
        assertEquals("bar", attr.getPrefix());
        assertEquals("a", attr.getLocalName());
        assertEquals("bar:a", attr.getName());

        attr = doc.createAttributeNS("http://nsa", "a");
        assertNull(attr.getPrefix());
        attr.setPrefix("bar");
        assertEquals("bar:a", attr.getNodeName());
        assertEquals("http://nsa", attr.getNamespaceURI());
        assertEquals("bar", attr.getPrefix());
        assertEquals("a", attr.getLocalName());
        assertEquals("bar:a", attr.getName());

        attr = doc.createAttributeNS(xmlURI, "foo:a");
        attr.setPrefix("xml");
        Attr attrForXmlErr = doc.createAttributeNS("http://nsa", "foo:a");
        assertDOMException(() -> attrForXmlErr.setPrefix("xml"), DOMException.NAMESPACE_ERR);

        Attr attrForXmlnsErr = doc.createAttributeNS("http://nsa", "foo:a");
        assertDOMException(() -> attrForXmlnsErr.setPrefix("xmlns"), DOMException.NAMESPACE_ERR);

        Attr attrXmlns = doc.createAttributeNS(xmlnsURI, "xmlns");
        assertDOMException(() -> attrXmlns.setPrefix("xml"), DOMException.NAMESPACE_ERR);

        Attr attrNoNS = doc.createAttributeNS(null, "a");
        assertDOMException(() -> attrNoNS.setPrefix("foo"), DOMException.NAMESPACE_ERR);
    }

    @org.junit.jupiter.api.Test
    public void testElementsByTagNameAndNS() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        DocumentType dt = impl.createDocumentType("foo:docName", "pubId", "http://sysId");
        Document doc = impl.createDocument("http://document.namespace", "foo:docName", dt);
        Element rootEl = doc.getDocumentElement();

        Element ela = doc.createElementNS("http://nsa", "a:ela");
        rootEl.appendChild(ela);
        Element elb = doc.createElementNS("http://nsb", "elb");
        rootEl.appendChild(elb);
        Element elc = doc.createElementNS(null, "elc");
        rootEl.appendChild(elc);
        Element eld = doc.createElementNS("http://nsa", "d:ela");
        rootEl.appendChild(eld);
        Element ele = doc.createElementNS("http://nse", "elb");
        rootEl.appendChild(ele);

        NodeList nl = doc.getElementsByTagName("a:ela");
        assertEquals(1, nl.getLength());
        assertSame(ela, nl.item(0));

        nl = doc.getElementsByTagName("elb");
        assertEquals(2, nl.getLength());
        assertSame(elb, nl.item(0));
        assertSame(ele, nl.item(1));

        nl = doc.getElementsByTagName("d:ela");
        assertEquals(1, nl.getLength());
        assertSame(eld, nl.item(0));

        nl = doc.getElementsByTagNameNS(null, "elc");
        assertEquals(1, nl.getLength());
        assertSame(elc, nl.item(0));

        nl = doc.getElementsByTagNameNS("http://nsa", "ela");
        assertEquals(2, nl.getLength());
        assertSame(ela, nl.item(0));
        assertSame(eld, nl.item(1));

        nl = doc.getElementsByTagNameNS(null, "elb");
        assertEquals(0, nl.getLength());

        nl = doc.getElementsByTagNameNS("http://nsb", "elb");
        assertEquals(1, nl.getLength());
        assertSame(elb, nl.item(0));

        nl = doc.getElementsByTagNameNS("*", "elb");
        assertEquals(2, nl.getLength());
        assertSame(elb, nl.item(0));
        assertSame(ele, nl.item(1));

        nl = doc.getElementsByTagNameNS("http://nsa", "*");
        assertEquals(2, nl.getLength());
        assertSame(ela, nl.item(0));
        assertSame(eld, nl.item(1));

        nl = doc.getElementsByTagNameNS("*", "*");
        assertEquals(6, nl.getLength());
        assertNull(nl.item(6));

        nl = rootEl.getElementsByTagNameNS("*", "*");
        assertEquals(5, nl.getLength());

        nl = doc.getElementsByTagNameNS("http://nsa", "d:ela");
        assertEquals(0, nl.getLength());

        // Live NodeList tests
        nl = doc.getElementsByTagNameNS("*", "*");
        NodeList nla = ela.getElementsByTagNameNS("*", "*");
        assertEquals(6, nl.getLength());
        assertEquals(0, nla.getLength());

        rootEl.removeChild(elc);
        assertEquals(5, nl.getLength());
        assertEquals(0, nla.getLength());

        ela.appendChild(elc);
        assertEquals(6, nl.getLength());
        assertEquals(1, nla.getLength());
    }

    @org.junit.jupiter.api.Test
    public void testAttributesAndNamedNodeMaps() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        DocumentType dt = impl.createDocumentType("foo:docName", "pubId", "http://sysId");
        Document doc = impl.createDocument("http://document.namespace", "foo:docName", dt);
        Element rootEl = doc.getDocumentElement();

        Attr attra = doc.createAttributeNS("http://nsa", "a:attra");
        rootEl.setAttributeNodeNS(attra);
        Attr attrb = doc.createAttributeNS("http://nsb", "attrb");
        rootEl.setAttributeNodeNS(attrb);
        Attr attrc = doc.createAttributeNS(null, "attrc");
        rootEl.setAttributeNodeNS(attrc);
        Attr attrd = doc.createAttributeNS("http://nsa", "d:attra");
        rootEl.setAttributeNodeNS(attrd);
        Attr attre = doc.createAttributeNS("http://nse", "attrb");
        rootEl.setAttributeNodeNS(attre);

        assertEquals("a:attra", attra.getNodeName());
        assertEquals("http://nsa", attra.getNamespaceURI());
        assertEquals("attra", attra.getLocalName());
        assertEquals("a:attra", attra.getName());
        assertEquals(Node.ATTRIBUTE_NODE, attra.getNodeType());
        assertEquals("", attra.getNodeValue());
        assertEquals("a", attra.getPrefix());
        assertTrue(attra.getSpecified());
        assertEquals("", attra.getValue());
        assertNull(attra.getOwnerElement());

        NamedNodeMap nnm = rootEl.getAttributes();
        assertEquals(4, nnm.getLength());
        assertSame(attrd, nnm.getNamedItemNS("http://nsa", "attra"));
        assertSame(attrb, nnm.getNamedItemNS("http://nsb", "attrb"));
        assertSame(attre, nnm.getNamedItemNS("http://nse", "attrb"));
        assertSame(attrc, nnm.getNamedItemNS(null, "attrc"));
        assertNull(nnm.getNamedItemNS(null, "attra"));
        assertNull(nnm.getNamedItemNS("http://nsa", "attrb"));
    }

    @org.junit.jupiter.api.Test
    public void testTextContentAndUserData() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        DocumentType dt = impl.createDocumentType("foo", "PubId", "SysId");
        Document doc = impl.createDocument(null, "foo", dt);

        assertNull(((NodeImpl) doc).getTextContent());
        assertNull(((NodeImpl) dt).getTextContent());

        ((NodeImpl) doc).setTextContent("foo");
        ((NodeImpl) dt).setTextContent("foo");

        NodeImpl el = (NodeImpl) doc.getDocumentElement();
        assertEquals("", el.getTextContent());
        el.setTextContent("yo!");
        Node t = el.getFirstChild();
        assertNotNull(t);
        assertEquals(Node.TEXT_NODE, t.getNodeType());
        assertEquals("yo!", t.getNodeValue());
        assertEquals("yo!", el.getTextContent());

        Comment c = doc.createComment("dummy");
        el.appendChild(c);

        NodeImpl el2 = (NodeImpl) doc.createElement("bar");
        el2.setTextContent("bye now");
        el.appendChild(el2);
        assertEquals("yo!bye now", el.getTextContent());

        NodeImpl el3 = (NodeImpl) doc.createElement("test");
        el.appendChild(el3);
        NodeImpl empty = (NodeImpl) doc.createElement("empty");
        el3.appendChild(empty);
        assertNotNull(el3.getTextContent());

        empty.setTextContent("hello");
        assertEquals(1, empty.getChildNodes().getLength());

        empty.setTextContent(null);
        assertEquals(0, empty.getChildNodes().getLength());
        empty.setTextContent("");
        assertEquals(0, empty.getChildNodes().getLength());

        class MyHandler implements UserDataHandler {
            boolean fCalled;
            Node fNode;
            String fKey;
            Object fData;

            MyHandler(String key, Object data, Node node) {
                this.fCalled = false;
                this.fKey = key;
                this.fData = data;
                this.fNode = node;
            }

            public void handle(short operation, String key, Object data, Node src, Node dst) {
                assertEquals(UserDataHandler.NODE_CLONED, operation);
                assertSame(fKey, key);
                assertSame(fData, data);
                assertSame(fNode, src);
                assertNotNull(dst);
                assertEquals(fNode.getNodeType(), dst.getNodeType());
                fCalled = true;
            }
        }

        el.setUserData("mykey", c, null);
        el.setUserData("mykey2", el2, null);
        assertSame(c, el.getUserData("mykey"));
        assertSame(el2, el.getUserData("mykey2"));
        el.setUserData("mykey", null, null);
        assertNull(el.getUserData("mykey"));
        el.setUserData("mykey2", null, null);
        assertNull(el.getUserData("mykey2"));

        MyHandler h = new MyHandler("mykey", c, el);
        el.setUserData("mykey", c, h);
        MyHandler h2 = new MyHandler("mykey2", el2, el);
        el.setUserData("mykey2", el2, h2);
        el.cloneNode(false);
        assertTrue(h.fCalled);
        assertTrue(h2.fCalled);

        el.setTextContent("zapped!");
        Node t2 = el.getFirstChild();
        assertEquals("zapped!", t2.getNodeValue());
        assertNull(t2.getNextSibling());
    }

    @org.junit.jupiter.api.Test
    public void testIsEqualNode() {
        DOMImplementation impl = DOMImplementationImpl.getDOMImplementation();
        Document doc = impl.createDocument(null, "root", null);
        NodeImpl root = (NodeImpl) doc.getDocumentElement();

        NodeImpl n1 = (NodeImpl) doc.createElement("el");
        n1.setTextContent("yo!");
        NodeImpl n2 = (NodeImpl) doc.createElement("el");
        n2.setTextContent("yo!");
        assertTrue(n1.isEqualNode(n2));

        n2.setTextContent("yoyo!");
        assertFalse(n1.isEqualNode(n2));

        n1.setTextContent("yoyo!");
        ((Element) n1).setAttribute("a1", "v1");
        ((Element) n1).setAttributeNS("uri", "a2", "v2");
        ((Element) n2).setAttribute("a1", "v1");
        ((Element) n2).setAttributeNS("uri", "a2", "v2");
        assertTrue(n1.isEqualNode(n2));

        Element elem = doc.createElementNS(null, "e2");
        root.appendChild(elem);
        Attr attr = doc.createAttributeNS("http://attr", "attr1");
        elem.setAttributeNode(attr);

        elem.setAttributeNS("http://attr", "p:attr1", "v2");
        Attr attr2 = elem.getAttributeNodeNS("http://attr", "attr1");
        assertEquals("p:attr1", attr2.getNodeName());
        assertEquals("v2", attr2.getNodeValue());

        elem.setAttributeNS("http://attr", "attr1", "v2");
        attr2 = elem.getAttributeNodeNS("http://attr", "attr1");
        assertEquals("attr1", attr2.getNodeName());

        ((Element) n2).setAttribute("a1", "v2");
        assertFalse(n1.isEqualNode(n2));

        root.appendChild(n1);
        root.appendChild(n2);

        NodeImpl clone = (NodeImpl) root.cloneNode(true);
        assertTrue(clone.isEqualNode(root));
    }

    private interface DOMAction {
        void run() throws DOMException;
    }

    private void assertDOMException(DOMAction action, short expectedCode) {
        try {
            action.run();
            fail("Expected DOMException with code " + expectedCode);
        } catch (DOMException e) {
            assertEquals(expectedCode, e.code, "Wrong DOMException code");
        }
    }

}
