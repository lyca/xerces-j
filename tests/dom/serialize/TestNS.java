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

package dom.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.apache.xerces.dom.DOMImplementationImpl;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

/**
 * Tests DOM Serializer namespace fixup algorithm.
 */
public class TestNS {

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestNS.class);
    }

    @org.junit.Test
    public void testNamespaceFixupSerialization() throws Exception {
        DocumentImpl doc = new DocumentImpl();
        Element root = doc.createElementNS("urn:root", "r:root");
        doc.appendChild(root);

        Element e1 = doc.createElementNS("http://rsa2", "xx:child");
        e1.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xx", "http://rsa2");
        e1.setAttributeNS("http://attrUri", "xx:attr", "value");
        root.appendChild(e1);

        Element e2 = doc.createElementNS("http://childDefault", "child2");
        root.appendChild(e2);

        DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationImpl.getDOMImplementation();
        LSSerializer serializer = impl.createLSSerializer();
        serializer.getDomConfig().setParameter("namespaces", Boolean.TRUE);
        String serialized = serializer.writeToString(doc);

        assertNotNull(serialized);
        assertTrue("Serialized output should contain root element", serialized.contains("root"));

        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.parse(new InputSource(new StringReader(serialized)));
        Document parsedDoc = parser.getDocument();
        assertNotNull(parsedDoc);
        assertEquals("root", parsedDoc.getDocumentElement().getLocalName());
    }

    @org.junit.Test
    public void testAttributeNamespaceSynthesis() throws Exception {
        DocumentImpl doc = new DocumentImpl();
        Element root = doc.createElementNS("urn:test", "t:root");
        doc.appendChild(root);

        Element child = doc.createElementNS("urn:test", "t:elem");
        Attr attr = doc.createAttributeNS("http://attrUri", "attrWithoutPrefix");
        child.setAttributeNode(attr);
        root.appendChild(child);

        DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationImpl.getDOMImplementation();
        LSSerializer serializer = impl.createLSSerializer();
        serializer.getDomConfig().setParameter("namespaces", Boolean.TRUE);
        String xml = serializer.writeToString(doc);

        assertNotNull(xml);
        assertTrue("Synthesized NS prefix or declaration should be present", xml.contains("http://attrUri"));

        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.parse(new InputSource(new StringReader(xml)));
        Document parsed = parser.getDocument();
        Element parsedChild = (Element) parsed.getDocumentElement().getElementsByTagNameNS("urn:test", "elem").item(0);
        assertNotNull(parsedChild);
        Attr parsedAttr = parsedChild.getAttributeNodeNS("http://attrUri", "attrWithoutPrefix");
        assertNotNull(parsedAttr);
    }
}
