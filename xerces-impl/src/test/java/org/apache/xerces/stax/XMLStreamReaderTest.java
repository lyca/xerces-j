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

package org.apache.xerces.stax;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class XMLStreamReaderTest {

    @Test
    public void testBasicParsing() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   + "<root xmlns:ns=\"http://example.com\" attr=\"value1\">\n"
                   + "  <ns:child id=\"c1\">Hello World</ns:child>\n"
                   + "  <!-- a comment -->\n"
                   + "  <?pi-target pi-data?>\n"
                   + "</root>";

        XMLInputFactory factory = new XMLInputFactoryImpl();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

        assertEquals(XMLStreamConstants.START_DOCUMENT, reader.getEventType());
        assertEquals("1.0", reader.getVersion());
        assertEquals("UTF-8", reader.getEncoding());

        // START_ELEMENT <root>
        int event = reader.nextTag();
        assertEquals(XMLStreamConstants.START_ELEMENT, event);
        assertEquals("root", reader.getLocalName());
        assertEquals(1, reader.getAttributeCount());
        assertEquals("attr", reader.getAttributeLocalName(0));
        assertEquals("value1", reader.getAttributeValue(0));
        assertEquals(1, reader.getNamespaceCount());
        assertEquals("ns", reader.getNamespacePrefix(0));
        assertEquals("http://example.com", reader.getNamespaceURI(0));

        // START_ELEMENT <ns:child>
        event = reader.nextTag();
        assertEquals(XMLStreamConstants.START_ELEMENT, event);
        assertEquals("child", reader.getLocalName());
        assertEquals("ns", reader.getPrefix());
        assertEquals("http://example.com", reader.getNamespaceURI());
        assertEquals("c1", reader.getAttributeValue(null, "id"));

        // CHARACTERS "Hello World"
        event = reader.next();
        assertEquals(XMLStreamConstants.CHARACTERS, event);
        assertEquals("Hello World", reader.getText());

        // END_ELEMENT </ns:child>
        event = reader.next();
        assertEquals(XMLStreamConstants.END_ELEMENT, event);
        assertEquals("child", reader.getLocalName());

        // nextTag skips whitespace/comments/PIs to </root>
        event = reader.nextTag();
        assertEquals(XMLStreamConstants.END_ELEMENT, event);
        assertEquals("root", reader.getLocalName());

        // END_DOCUMENT
        event = reader.next();
        assertEquals(XMLStreamConstants.END_DOCUMENT, event);
        assertFalse(reader.hasNext());

        reader.close();
    }

    @Test
    public void testGetElementText() throws Exception {
        String xml = "<item>Sample Text Value</item>";
        XMLInputFactory factory = new XMLInputFactoryImpl();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

        reader.nextTag();
        assertEquals("item", reader.getLocalName());
        String text = reader.getElementText();
        assertEquals("Sample Text Value", text);
        assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());

        reader.close();
    }

    @Test
    public void testAttributesAndMultipleElements() throws Exception {
        String xml = "<books><book id=\"1\" title=\"XML Guide\"/><book id=\"2\" title=\"Performance\"/></books>";
        XMLInputFactory factory = new XMLInputFactoryImpl();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

        reader.nextTag(); // <books>
        assertEquals("books", reader.getLocalName());

        reader.nextTag(); // <book 1>
        assertEquals("book", reader.getLocalName());
        assertEquals(2, reader.getAttributeCount());
        assertEquals("1", reader.getAttributeValue(null, "id"));
        assertEquals("XML Guide", reader.getAttributeValue(null, "title"));

        reader.nextTag(); // </book 1> or next <book 2> (empty element is start + end)
        if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
            reader.nextTag();
        }
        assertEquals("book", reader.getLocalName());
        assertEquals("2", reader.getAttributeValue(null, "id"));
        assertEquals("Performance", reader.getAttributeValue(null, "title"));

        reader.close();
    }
}
