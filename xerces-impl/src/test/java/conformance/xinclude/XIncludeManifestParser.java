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
package conformance.xinclude;

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
 * Parser for the W3C XInclude Conformance Test Suite manifest (testdescr.xml).
 */
public class XIncludeManifestParser {

    public static XIncludeGroup parse(File manifestFile) throws Exception {
        if (!manifestFile.exists()) {
            throw new IllegalArgumentException("Manifest file not found: " + manifestFile.getAbsolutePath());
        }

        File suiteBaseDir = manifestFile.getParentFile();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        try (InputStream is = new FileInputStream(manifestFile)) {
            InputSource inputSource = new InputSource(is);
            inputSource.setSystemId(manifestFile.toURI().toASCIIString());
            doc = db.parse(inputSource);
        }

        Element root = doc.getDocumentElement();
        XIncludeGroup rootGroup = new XIncludeGroup("W3C XInclude Conformance Test Suite");

        NodeList testcasesList = root.getElementsByTagName("testcases");
        for (int i = 0; i < testcasesList.getLength(); i++) {
            Element testcasesElem = (Element) testcasesList.item(i);
            String creator = testcasesElem.getAttribute("creator");
            String basedirStr = testcasesElem.getAttribute("basedir");
            if (creator == null || creator.trim().isEmpty()) {
                creator = "TestCases Group " + (i + 1);
            }

            File groupBaseDir = new File(suiteBaseDir, basedirStr);
            XIncludeGroup group = new XIncludeGroup(creator + " (" + basedirStr + ")");

            NodeList testcaseList = testcasesElem.getElementsByTagName("testcase");
            for (int j = 0; j < testcaseList.getLength(); j++) {
                Element testcaseElem = (Element) testcaseList.item(j);
                XIncludeTestCase testCase = parseTestCase(testcaseElem, groupBaseDir);
                if (testCase != null) {
                    group.addTest(testCase);
                }
            }

            if (group.getTotalTestCount() > 0) {
                rootGroup.addSubGroup(group);
            }
        }

        return rootGroup;
    }

    private static XIncludeTestCase parseTestCase(Element elem, File groupBaseDir) {
        String id = elem.getAttribute("id");
        String href = elem.getAttribute("href");
        String typeStr = elem.getAttribute("type");
        String features = elem.getAttribute("features");

        if (id == null || href == null || typeStr == null) {
            return null;
        }

        XIncludeTestCase.Type type = "error".equalsIgnoreCase(typeStr) ?
                XIncludeTestCase.Type.ERROR : XIncludeTestCase.Type.SUCCESS;

        File testFile = new File(groupBaseDir, href);

        String description = getChildElementText(elem, "description");
        String section = getChildElementText(elem, "section");
        String outputHref = getChildElementText(elem, "output");

        File outputFile = null;
        if (outputHref != null && !outputHref.trim().isEmpty()) {
            outputFile = new File(groupBaseDir, outputHref.trim());
        }

        return new XIncludeTestCase(
                id,
                type,
                testFile,
                outputFile,
                features,
                description,
                section
        );
    }

    private static String getChildElementText(Element parent, String tagName) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equalsIgnoreCase(node.getNodeName())) {
                return node.getTextContent() != null ? node.getTextContent().trim() : "";
            }
        }
        return null;
    }
}
