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

package jaxp;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests JAXP 1.2 properties
 */
public class PropertyTest extends DefaultHandler {

    private SAXParserFactory spf;

    private String getPersonalSchemaUri() {
        URL res = PropertyTest.class.getClassLoader().getResource("jaxp/data/personal-schema.xml");
        return res != null ? res.toExternalForm() : "xerces-impl/src/test/resources/jaxp/data/personal-schema.xml";
    }

    @BeforeEach
    public void setUp() {
        spf = SAXParserFactory.newInstance();
        spf.setValidating(true);
        spf.setNamespaceAware(true);
    }

    @Test
    public void testValidSchemaSource() throws Exception {
        SAXParser parser = spf.newSAXParser();
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
            "http://www.w3.org/2001/XMLSchema");
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaSource",
            new String[] { "personal.xsd", "ipo.xsd" });
        parser.parse(getPersonalSchemaUri(), new DefaultHandler());
    }

    @Test
    public void testInvalidSchemaSourceMismatch() throws Exception {
        SAXParser parser = spf.newSAXParser();
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
            "http://www.w3.org/2001/XMLSchema");
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaSource",
            new String[] { "address.xsd", "ipo.xsd" });

        try {
            parser.parse(getPersonalSchemaUri(), new DefaultHandler());
            fail("Parsing with mismatched schemas should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected: duplicate / conflicting target namespace schemas
        }
    }

    @Test
    public void testInvalidSchemaSourceExtraXsd() throws Exception {
        SAXParser parser = spf.newSAXParser();
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
            "http://www.w3.org/2001/XMLSchema");
        parser.setProperty(
            "http://java.sun.com/xml/jaxp/properties/schemaSource",
            new String[] { "personal.xsd", "ipo.xsd", "a.xsd" });

        try {
            parser.parse(getPersonalSchemaUri(), new DefaultHandler());
            fail("Parsing with erroneous schema source should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected: duplicate / conflicting target namespace schemas
        }
    }

}
