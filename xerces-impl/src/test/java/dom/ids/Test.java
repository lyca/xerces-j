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

package dom.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A simple program to test Document.getElementById() and the management
 * of ID attributes.
 */
public class Test {

    //
    // Constants
    //

    // feature ids

    protected static final String NAMESPACES_FEATURE_ID =
        "http://xml.org/sax/features/namespaces";

    protected static final String VALIDATION_FEATURE_ID =
        "http://xml.org/sax/features/validation";

    protected static final String SCHEMA_VALIDATION_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema";

    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema-full-checking";

    protected static final String DEFERRED_DOM_FEATURE_ID =
        "http://apache.org/xml/features/dom/defer-node-expansion";

    protected static final boolean DEFAULT_NAMESPACES = true;

    protected static final boolean DEFAULT_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

    protected static final boolean DEFAULT_DEFERRED_DOM = true;

    //
    // Public methods
    //

    @org.junit.jupiter.api.Test
    public void testGetElementById() throws Exception {

        DOMParser parser = new DOMParser();
        parser.setFeature(NAMESPACES_FEATURE_ID, DEFAULT_NAMESPACES);
        parser.setFeature(VALIDATION_FEATURE_ID, DEFAULT_VALIDATION);
        parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, DEFAULT_SCHEMA_VALIDATION);
        parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, DEFAULT_SCHEMA_FULL_CHECKING);
        parser.setFeature(DEFERRED_DOM_FEATURE_ID, DEFAULT_DEFERRED_DOM);

        URL resource = getClass().getClassLoader().getResource("dom/ids/input.xml");
        assertNotNull(resource, "input.xml not found");
        Document doc = null;
        try {
            parser.parse(resource.toExternalForm());
            doc = parser.getDocument();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Element el = doc.getElementById("one.worker");
        assertNotNull(el, "el != null");
        Element el2 = doc.getElementById("one.worker there");
        assertNull(el2, "el2 == null");

        if (el != null) {
            assertEquals("one.worker", el.getAttribute("id"), "el.getAttribute(\"id\")");
            el.setAttribute("id", "my.worker");
            el2 = doc.getElementById("my.worker");
            assertSame(el, el2, "el2 == el");

            el2 = doc.getElementById("one.worker");
            assertNull(el2, "el2 == null");
            el.removeAttribute("id");
            el2 = doc.getElementById("my.worker");
            assertNull(el2, "el2 == null");
        }

        // find default id attribute and check its value
        NodeList elementList = doc.getElementsByTagName("person");
        Element testEmployee = (Element)elementList.item(1);
        Attr id = testEmployee.getAttributeNode("id2");
        assertEquals("id02", id.getNodeValue(), "value == 'id02'");

        Element elem = doc.getElementById("id02");
        assertEquals("person", elem.getNodeName(), "return by id 'id02'");

        // remove default attribute and check on retrieval what its value
        Attr removedAttr = testEmployee.removeAttributeNode(id);
        String value = testEmployee.getAttribute("id2");
        assertEquals("default.id", value, "value='default.id'");

        elem = doc.getElementById("default.id");
        assertNotNull(elem, "elem by id 'default.id'");

        elem = doc.getElementById("id02");
        assertNull(elem, "elem by id '02'");

        Element person = (Element)doc.getElementsByTagNameNS(null, "person").item(0);
        person.removeAttribute("id");
        person.removeAttribute("id2");
        person.setAttributeNS(null, "idAttr", "eb0009");
        person.setIdAttribute("idAttr", true);

        elem = doc.getElementById("eb0009");
        assertNotNull(elem, "elem by id 'eb0009'");

        doc.getDocumentElement().removeChild(person);
        elem = doc.getElementById("eb0009");
        assertNull(elem, "element with id 'eb0009 removed'");

        doc.getDocumentElement().appendChild(person);
        elem = doc.getElementById("eb0009");
        assertNotNull(elem, "elem by id 'eb0009'");
        Attr attr = (Attr)person.getAttributeNode("idAttr");
        assertTrue(attr.isId(), "attribute is id");

        person.setIdAttribute("idAttr", false);
        elem = doc.getElementById("eb0009");
        assertNull(elem, "element with id 'eb0009 removed'");

        assertFalse(attr.isId(), "attribute is not id");

    }

}
