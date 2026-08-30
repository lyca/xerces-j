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

package dom.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dom.ParserWrapper;

/**
 * A java serialization test. Parses a document, serializes it, then reloads
 * it.
 */
public class Test {

    private static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    private static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    private static final String SCHEMA_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    private static final String DEFAULT_PARSER_NAME = "dom.wrappers.Xerces";

    @org.junit.Test
    public void testInMemorySerialization() throws Exception {
        Document document = createSampleDocument();
        byte[] bytes = serializeToBytes(document);
        Document deserializedDoc = deserializeFromBytes(bytes);
        assertNotNull(deserializedDoc);

        Element root = deserializedDoc.getDocumentElement();
        assertNotNull(root);
        assertEquals("personnel", root.getNodeName());
        assertEquals("boo", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "foo"));

        Document emptyDoc = new DocumentImpl();
        Node imported = emptyDoc.importNode(root, true);
        assertNotNull(imported);
        assertEquals("personnel", imported.getNodeName());
    }

    @org.junit.Test
    public void testFileSerialization() throws Exception {
        Document document = createSampleDocument();
        File tempFile = File.createTempFile("xerces_dom_ser_", ".ser");
        tempFile.deleteOnExit();

        try {
            serializeToFile(document, tempFile);
            Document deserializedDoc = deserializeFromFile(tempFile);
            assertNotNull(deserializedDoc);

            Element root = deserializedDoc.getDocumentElement();
            assertNotNull(root);
            assertEquals("personnel", root.getNodeName());
            assertEquals("boo", root.getAttributeNS("http://www.w3.org/2000/xmlns/", "foo"));

            Document emptyDoc = new DocumentImpl();
            Node imported = emptyDoc.importNode(root, true);
            assertNotNull(imported);
            assertEquals("personnel", imported.getNodeName());
        } finally {
            tempFile.delete();
        }
    }

    private Document createSampleDocument() throws Exception {
        ParserWrapper parser = (ParserWrapper) Class.forName(DEFAULT_PARSER_NAME).getDeclaredConstructor().newInstance();
        parser.setFeature(NAMESPACES_FEATURE_ID, true);
        parser.setFeature(VALIDATION_FEATURE_ID, true);
        parser.setFeature(SCHEMA_FEATURE_ID, true);

        Document document = parser.parse("data/personal-schema.xml");
        assertNotNull(document);
        document.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:foo", "boo");
        return document;
    }

    private byte[] serializeToBytes(Document document) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(document);
        }
        return baos.toByteArray();
    }

    private Document deserializeFromBytes(byte[] bytes) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Document) in.readObject();
        }
    }

    private void serializeToFile(Document document, File file) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(document);
        }
    }

    private Document deserializeFromFile(File file) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Document) in.readObject();
        }
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(Test.class);
    }
}
