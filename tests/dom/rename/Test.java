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

package dom.rename;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import dom.ParserWrapper;

/**
 * Tests Document.renameNode() and UserDataHandler on renamed nodes.
 */
public class Test implements UserDataHandler {

    private short lastOperation = -1;
    private String lastKey;
    private Object lastData;
    private Node lastSource;
    private Node lastDestination;

    

    @BeforeEach
    public void setUp() {
        resetHandlerData();
    }

    private Document parseDocument() throws Exception {
        ParserWrapper parser = (ParserWrapper) Class.forName("dom.wrappers.Xerces").getDeclaredConstructor().newInstance();
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://xml.org/sax/features/validation", false);
        return parser.parse("tests/dom/rename/input.xml");
    }

    @org.junit.jupiter.api.Test
    public void testRenameElement() throws Exception {
        Document doc = parseDocument();
        NodeList elements = doc.getElementsByTagName("email");
        Element child = (Element) elements.item(0);
        assertNotNull(child);
        assertEquals("email", child.getNodeName());

        // default attribute from DTD
        Attr at = child.getAttributeNode("defaultEmailAttr");
        assertNotNull(at);
        assertEquals("defaultEmailValue", at.getValue());
        assertFalse(at.getSpecified());

        // attach user data
        child.setUserData("mydata", "yo", this);
        assertEquals("yo", (String) child.getUserData("mydata"));

        // renaming an element without a namespace URI
        Element newChild = (Element) doc.renameNode(child, null, "url");
        assertEquals("url", newChild.getNodeName());
        assertNull(newChild.getNamespaceURI());

        // old default attribute must no longer be there
        assertFalse(newChild.hasAttribute("defaultEmailAttr"));
        assertTrue(at.getSpecified());

        // new default attribute must be there
        at = newChild.getAttributeNode("defaultUrlAttr");
        assertNotNull(at);
        assertEquals("defaultUrlValue", at.getValue());
        assertFalse(at.getSpecified());

        // data must still be there
        assertEquals("yo", (String) newChild.getUserData("mydata"));
        if (newChild != child) {
            assertEquals(UserDataHandler.NODE_RENAMED, lastOperation);
            assertEquals("mydata", lastKey);
            assertEquals("yo", lastData);
            assertSame(child, lastSource);
            assertSame(newChild, lastDestination);
            resetHandlerData();
        }

        // renaming an element with a namespace URI
        Element newChild2 = (Element) doc.renameNode(newChild, "ns1", "foo");
        assertEquals("foo", newChild2.getNodeName());
        assertEquals("ns1", newChild2.getNamespaceURI());
        assertFalse(newChild2.hasAttribute("defaultUrlAttr"));
        assertEquals("yo", (String) newChild2.getUserData("mydata"));
        if (newChild2 != newChild) {
            assertEquals(UserDataHandler.NODE_RENAMED, lastOperation);
            assertEquals("mydata", lastKey);
            assertEquals("yo", lastData);
            assertSame(newChild, lastSource);
            assertSame(newChild2, lastDestination);
            resetHandlerData();
        }
    }

    @org.junit.jupiter.api.Test
    public void testRenameAttribute() throws Exception {
        Document doc = parseDocument();
        NodeList elements = doc.getElementsByTagName("email");
        Element child = (Element) elements.item(1);
        assertNotNull(child);
        assertEquals("email", child.getNodeName());

        // default attribute
        Attr at = child.getAttributeNode("defaultEmailAttr");
        assertNotNull(at);
        assertEquals("defaultEmailValue", at.getValue());
        assertFalse(at.getSpecified());

        // attach user data
        at.setUserData("mydata", "yo", this);
        assertEquals("yo", (String) at.getUserData("mydata"));

        // renaming an attribute without a namespace URI
        Attr newAt = (Attr) doc.renameNode(at, null, "foo");
        assertNotNull(newAt);
        assertEquals("foo", newAt.getNodeName());
        assertNull(newAt.getNamespaceURI());
        assertEquals("defaultEmailValue", newAt.getValue());
        assertTrue(newAt.getSpecified());
        assertTrue(child.hasAttribute("foo"));
        assertTrue(child.hasAttribute("defaultEmailAttr"));
        assertEquals("yo", (String) newAt.getUserData("mydata"));
        if (newAt != at) {
            assertEquals(UserDataHandler.NODE_RENAMED, lastOperation);
            assertEquals("mydata", lastKey);
            assertEquals("yo", lastData);
            assertSame(at, lastSource);
            assertSame(newAt, lastDestination);
            resetHandlerData();
        }

        // renaming an attribute with a namespace URI
        Attr newAt2 = (Attr) doc.renameNode(newAt, "ns1", "bar");
        assertNotNull(newAt2);
        assertEquals("bar", newAt2.getNodeName());
        assertEquals("ns1", newAt2.getNamespaceURI());
        assertEquals("defaultEmailValue", newAt2.getValue());
        assertTrue(newAt2.getSpecified());
        assertTrue(child.hasAttributeNS("ns1", "bar"));
        assertEquals("yo", (String) newAt2.getUserData("mydata"));
        if (newAt2 != newAt) {
            assertEquals(UserDataHandler.NODE_RENAMED, lastOperation);
            assertEquals("mydata", lastKey);
            assertEquals("yo", lastData);
            assertSame(newAt, lastSource);
            assertSame(newAt2, lastDestination);
            resetHandlerData();
        }
    }

    private void resetHandlerData() {
        lastOperation = -1;
        lastKey = null;
        lastData = null;
        lastSource = null;
        lastDestination = null;
    }

    public void handle(short operation, String key, Object data, Node src, Node dst) {
        lastOperation = operation;
        lastKey = key;
        lastData = data;
        lastSource = src;
        lastDestination = dst;
    }

    public static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
    public static void assertTrue(String message, boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }
    public static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
    public static void assertFalse(String message, boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }
    public static void assertNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object);
    }
    public static void assertNull(String message, Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object, message);
    }
    public static void assertNotNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object);
    }
    public static void assertNotNull(String message, Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object, message);
    }
    public static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertSame(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }
    public static void assertSame(String message, Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual, message);
    }
    public static void fail(String message) {
        org.junit.jupiter.api.Assertions.fail(message);
    }

}
