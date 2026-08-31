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

package schema;

import java.net.URL;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Test Schema processing
 *
 * @author Khaled Noaman, IBM
 */
public class Test {

    // feature ids
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    protected static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

    // property ids
    protected static final String SCHEMA_NONS_LOCATION_ID = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";

    public Test() {}

    @org.junit.jupiter.api.Test
    public void testSettingNoNamespaceSchemaLocation() throws Exception {
        SAXParser parser = new org.apache.xerces.parsers.SAXParser();

        parser.setFeature(NAMESPACES_FEATURE_ID, true);
        parser.setFeature(NAMESPACE_PREFIXES_FEATURE_ID, false);
        parser.setFeature(VALIDATION_FEATURE_ID, true);
        parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, false);
        parser.setFeature(DYNAMIC_VALIDATION_FEATURE_ID, false);

        parser.setProperty(SCHEMA_NONS_LOCATION_ID, "personal.xsd");

        parser.setContentHandler(new DefaultHandler());
        parser.setErrorHandler(new DefaultHandler());

        URL xmlUrl = Test.class.getClassLoader().getResource("schema/personal-schema.xml");
        String xmlUri = xmlUrl != null ? xmlUrl.toExternalForm() : "./tests/schema/personal-schema.xml";
        parser.parse(xmlUri);
    }
}
