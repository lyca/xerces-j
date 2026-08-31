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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.xerces.dom.DOMImplementationImpl;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

/**
 * Tests XMLSerializer and LSSerializer default namespace handling.
 */
public class TestXmlns {

    

    private Document createTestDocument() {
        DocumentImpl document = new DocumentImpl();
        document.setXmlEncoding("utf-8");

        Element outerNode = document.createElement("outer");
        outerNode.setAttribute("xmlns", "myuri:");
        document.appendChild(outerNode);

        Element innerNode = document.createElement("inner");
        outerNode.appendChild(innerNode);
        return document;
    }

    @org.junit.jupiter.api.Test
    public void testXMLSerializerDefaultNamespace() throws Exception {
        Document doc = createTestDocument();
        Writer writer = new StringWriter();
        OutputFormat format = new OutputFormat();
        format.setEncoding("utf-8");
        Serializer serializer = SerializerFactory.getSerializerFactory("xml").makeSerializer(writer, format);
        serializer.asDOMSerializer().serialize(doc);

        String result = writer.toString();
        assertNotNull(result);
        assertTrue("Output should contain outer element", result.contains("<outer"));
        assertTrue("Output should contain xmlns attribute", result.contains("xmlns=\"myuri:\""));
        assertTrue("Output should contain inner element", result.contains("<inner"));
    }

    @org.junit.jupiter.api.Test
    public void testLSSerializerDefaultNamespace() throws Exception {
        Document doc = createTestDocument();
        DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationImpl.getDOMImplementation();
        LSSerializer serializer = impl.createLSSerializer();
        DOMConfiguration config = serializer.getDomConfig();
        config.setParameter("namespaces", Boolean.TRUE);

        String result = serializer.writeToString(doc);
        assertNotNull(result);
        assertTrue("Output should contain outer element", result.contains("outer"));
        assertTrue("Output should contain inner element", result.contains("inner"));
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
