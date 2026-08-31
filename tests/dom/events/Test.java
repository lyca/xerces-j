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

package dom.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;

/**
 * DOM Mutation Events test converted to JUnit 4.
 */
public class Test {

    private EventReporter reporter;
    private Document doc;

    

    @BeforeEach
    public void setUp() {
        reporter = new EventReporter();
        doc = new DocumentImpl();
        reportAllMutations(doc);
    }

    @org.junit.jupiter.api.Test
    public void testElementAndAttrMutationEvents() {
        reporter.on();
        Element root = doc.createElement("Root");
        reportAllMutations(root);
        doc.appendChild(root);

        assertTrue("DOMNodeInserted event should be recorded", reporter.getEventsOfType("DOMNodeInserted").size() > 0);
        assertTrue("DOMSubtreeModified event should be recorded", reporter.getEventsOfType("DOMSubtreeModified").size() > 0);

        reporter.clear();
        Element e0 = doc.createElement("E0");
        reportAllMutations(e0);
        root.appendChild(e0);

        Attr a0 = doc.createAttribute("A0");
        reportAllMutations(a0);
        a0.setNodeValue("val0");
        e0.setAttributeNode(a0);

        List<EventReporter.EventRecord> attrEvents = reporter.getEventsOfType("DOMAttrModified");
        assertTrue("DOMAttrModified event should be recorded", attrEvents.size() > 0);
        boolean foundAdd = false;
        for (EventReporter.EventRecord r : attrEvents) {
            if ("A0".equals(r.attrName) && r.attrChange == MutationEvent.ADDITION) {
                foundAdd = true;
                break;
            }
        }
        assertTrue("Attr addition should be reported", foundAdd);

        reporter.clear();
        a0.setNodeValue("Updated A0");
        List<EventReporter.EventRecord> modEvents = reporter.getEventsOfType("DOMAttrModified");
        assertTrue("DOMAttrModified should be fired on value change", modEvents.size() > 0);

        reporter.clear();
        NamedNodeMap nnm = e0.getAttributes();
        nnm.removeNamedItem("A0");
        List<EventReporter.EventRecord> remEvents = reporter.getEventsOfType("DOMAttrModified");
        assertTrue("DOMAttrModified should be fired on attribute removal", remEvents.size() > 0);
    }

    @org.junit.jupiter.api.Test
    public void testTreeAddAndRemoveMutationEvents() {
        Element root = doc.createElement("Root");
        reportAllMutations(root);
        doc.appendChild(root);

        Element lateAdd = doc.createElement("lateAdd");
        reportAllMutations(lateAdd);
        Element child = doc.createElement("lateAdd_E0");
        reportAllMutations(child);
        lateAdd.appendChild(child);

        reporter.clear();
        reporter.on();
        root.appendChild(lateAdd);

        List<EventReporter.EventRecord> insertedIntoDoc = reporter.getEventsOfType("DOMNodeInsertedIntoDocument");
        assertTrue("DOMNodeInsertedIntoDocument should be recorded on append to tree", insertedIntoDoc.size() > 0);

        reporter.clear();
        root.removeChild(lateAdd);
        List<EventReporter.EventRecord> removedFromDoc = reporter.getEventsOfType("DOMNodeRemovedFromDocument");
        assertTrue("DOMNodeRemovedFromDocument should be recorded on remove from tree", removedFromDoc.size() > 0);
    }

    @org.junit.jupiter.api.Test
    public void testCharacterDataModifiedEvents() {
        Element root = doc.createElement("Root");
        reportAllMutations(root);
        doc.appendChild(root);

        Text t = doc.createTextNode("fo");
        reportAllMutations(t);
        root.appendChild(t);

        reporter.clear();
        reporter.on();
        t.insertData(1, "o");

        List<EventReporter.EventRecord> charEvents = reporter.getEventsOfType("DOMCharacterDataModified");
        assertTrue("DOMCharacterDataModified should be fired on insertData", charEvents.size() > 0);
        EventReporter.EventRecord first = charEvents.get(0);
        assertEquals("fo", first.prevValue);
        assertEquals("foo", first.newValue);
    }

    private void reportAllMutations(Node n) {
        String[] evtNames = {
            "DOMSubtreeModified", "DOMAttrModified", "DOMCharacterDataModified",
            "DOMNodeInserted", "DOMNodeRemoved",
            "DOMNodeInsertedIntoDocument", "DOMNodeRemovedFromDocument",
        };

        EventTarget t = (EventTarget) n;
        for (int i = 0; i < evtNames.length; i++) {
            t.addEventListener(evtNames[i], reporter, true);
            t.addEventListener(evtNames[i], reporter, false);
        }
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
