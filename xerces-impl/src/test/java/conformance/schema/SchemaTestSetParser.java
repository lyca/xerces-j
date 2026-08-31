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
package conformance.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parser for W3C XML Schema TestSet (.testSet) XML files.
 */
public class SchemaTestSetParser {

    public static SchemaTestSet parse(File testSetFile) throws Exception {
        if (!testSetFile.exists()) {
            throw new IllegalArgumentException("TestSet file not found: " + testSetFile.getAbsolutePath());
        }

        File baseDir = testSetFile.getParentFile();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        try (InputStream is = new FileInputStream(testSetFile)) {
            InputSource inputSource = new InputSource(is);
            inputSource.setSystemId(testSetFile.toURI().toASCIIString());
            doc = db.parse(inputSource);
        }

        Element root = doc.getDocumentElement();
        String contributor = root.getAttribute("contributor");
        String setName = root.getAttribute("name");
        if (contributor == null || contributor.trim().isEmpty()) {
            contributor = "W3C XML Schema TestSet";
        }
        if (setName == null || setName.trim().isEmpty()) {
            setName = testSetFile.getName();
        }

        SchemaTestSet testSet = new SchemaTestSet(contributor, setName);

        NodeList groupNodes = root.getElementsByTagName("testGroup");
        if (groupNodes.getLength() == 0) {
            groupNodes = root.getElementsByTagNameNS("*", "testGroup");
        }

        for (int i = 0; i < groupNodes.getLength(); i++) {
            Element groupElem = (Element) groupNodes.item(i);
            String groupName = groupElem.getAttribute("name");
            String description = getGroupDocumentation(groupElem);

            SchemaTestGroup group = new SchemaTestGroup(groupName, description);

            File currentSchemaFile = null;

            // 1. Schema tests
            NodeList schemaTests = groupElem.getElementsByTagName("schemaTest");
            if (schemaTests.getLength() == 0) {
                schemaTests = groupElem.getElementsByTagNameNS("*", "schemaTest");
            }
            for (int s = 0; s < schemaTests.getLength(); s++) {
                Element schemaElem = (Element) schemaTests.item(s);
                String testName = schemaElem.getAttribute("name");
                File schemaFile = resolveDocument(schemaElem, "schemaDocument", baseDir);
                if (schemaFile != null) {
                    currentSchemaFile = schemaFile;
                }
                SchemaTestCase.Validity validity = parseValidity(schemaElem);
                SchemaTestCase testCase = new SchemaTestCase(
                        groupName,
                        testName,
                        SchemaTestCase.Kind.SCHEMA,
                        schemaFile,
                        schemaFile,
                        validity,
                        description
                );
                group.addTestCase(testCase);
            }

            // 2. Instance tests
            NodeList instanceTests = groupElem.getElementsByTagName("instanceTest");
            if (instanceTests.getLength() == 0) {
                instanceTests = groupElem.getElementsByTagNameNS("*", "instanceTest");
            }
            for (int it = 0; it < instanceTests.getLength(); it++) {
                Element instElem = (Element) instanceTests.item(it);
                String testName = instElem.getAttribute("name");
                File instDocFile = resolveDocument(instElem, "instanceDocument", baseDir);
                SchemaTestCase.Validity validity = parseValidity(instElem);
                SchemaTestCase testCase = new SchemaTestCase(
                        groupName,
                        testName,
                        SchemaTestCase.Kind.INSTANCE,
                        instDocFile,
                        currentSchemaFile,
                        validity,
                        description
                );
                group.addTestCase(testCase);
            }

            if (!group.getTestCases().isEmpty()) {
                testSet.addGroup(group);
            }
        }

        return testSet;
    }

    private static String getGroupDocumentation(Element groupElem) {
        NodeList docList = groupElem.getElementsByTagName("documentation");
        if (docList.getLength() == 0) {
            docList = groupElem.getElementsByTagNameNS("*", "documentation");
        }
        if (docList.getLength() > 0) {
            return docList.item(0).getTextContent().trim();
        }
        return "";
    }

    private static File resolveDocument(Element parent, String docTagName, File baseDir) {
        NodeList list = parent.getElementsByTagName(docTagName);
        if (list.getLength() == 0) {
            list = parent.getElementsByTagNameNS("*", docTagName);
        }
        if (list.getLength() > 0) {
            Element docElem = (Element) list.item(0);
            String href = docElem.getAttribute("xlink:href");
            if (href == null || href.isEmpty()) {
                href = docElem.getAttributeNS("http://www.w3.org/1999/xlink", "href");
            }
            if (href != null && !href.isEmpty()) {
                // Remove leading ./ if present
                if (href.startsWith("./")) {
                    href = href.substring(2);
                }
                return new File(baseDir, href);
            }
        }
        return null;
    }

    private static SchemaTestCase.Validity parseValidity(Element testElem) {
        NodeList expList = testElem.getElementsByTagName("expected");
        if (expList.getLength() == 0) {
            expList = testElem.getElementsByTagNameNS("*", "expected");
        }
        if (expList.getLength() > 0) {
            Element expElem = (Element) expList.item(0);
            String validity = expElem.getAttribute("validity");
            if ("valid".equalsIgnoreCase(validity)) {
                return SchemaTestCase.Validity.VALID;
            } else if ("invalid".equalsIgnoreCase(validity)) {
                return SchemaTestCase.Validity.INVALID;
            }
        }
        return SchemaTestCase.Validity.NOT_KNOWN;
    }
}
