/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License\"); you may not use this file except in compliance with
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
package conformance;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parser for the W3C XML Conformance Test Suite manifest (xmlconf.xml).
 */
public class XmlConfManifestParser {

    public static XmlConfGroup parse(File xmlconfFile) throws Exception {
        if (!xmlconfFile.exists()) {
            throw new IllegalArgumentException("Manifest file not found: " + xmlconfFile.getAbsolutePath());
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setExpandEntityReferences(true);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
        } catch (Exception ignored) {
        }

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        try (InputStream is = new FileInputStream(xmlconfFile)) {
            InputSource inputSource = new InputSource(is);
            inputSource.setSystemId(xmlconfFile.toURI().toASCIIString());
            doc = db.parse(inputSource);
        }

        Element root = doc.getDocumentElement();
        String rootProfile = root.getAttribute("PROFILE");
        if (rootProfile == null || rootProfile.trim().isEmpty()) {
            rootProfile = "XML Conformance Test Suite";
        }

        XmlConfGroup rootGroup = new XmlConfGroup(rootProfile);
        parseTestCases(root, rootGroup);
        return rootGroup;
    }

    private static void parseTestCases(Element parentElement, XmlConfGroup currentGroup) {
        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elem = (Element) node;
            String nodeName = elem.getNodeName();

            if ("TESTCASES".equalsIgnoreCase(nodeName)) {
                String profile = elem.getAttribute("PROFILE");
                if (profile == null || profile.trim().isEmpty()) {
                    profile = elem.getAttribute("xml:base");
                }
                if (profile == null || profile.trim().isEmpty()) {
                    profile = "Test Group";
                }

                XmlConfGroup subGroup = new XmlConfGroup(profile);
                parseTestCases(elem, subGroup);

                if (subGroup.getTotalTestCount() > 0) {
                    currentGroup.addSubGroup(subGroup);
                }
            } else if ("TEST".equalsIgnoreCase(nodeName)) {
                XmlConfTestItem testItem = parseTestElement(elem);
                if (testItem != null) {
                    currentGroup.addTest(testItem);
                }
            }
        }
    }

    private static XmlConfTestItem parseTestElement(Element elem) {
        String id = elem.getAttribute("ID");
        String uri = elem.getAttribute("URI");
        String typeStr = elem.getAttribute("TYPE");
        if (id == null || uri == null || typeStr == null) {
            return null;
        }

        String baseUriStr = elem.getBaseURI();
        File testFile;
        try {
            if (baseUriStr != null) {
                URI baseUri = new URI(baseUriStr);
                URI resolved = baseUri.resolve(uri.trim());
                testFile = new File(resolved);
            } else {
                testFile = new File(uri.trim());
            }
        } catch (Exception e) {
            testFile = new File(uri.trim());
        }

        if (!testFile.exists()) {
            String path = testFile.getPath();
            if (path.contains("eduni/namespaces/misc")) {
                File fixed = new File(path.replace("eduni/namespaces/misc", "eduni/misc"));
                if (fixed.exists()) {
                    testFile = fixed;
                }
            }
        }

        XmlConfTestItem.Type type;
        if ("valid".equalsIgnoreCase(typeStr)) {
            type = XmlConfTestItem.Type.VALID;
        } else if ("invalid".equalsIgnoreCase(typeStr)) {
            type = XmlConfTestItem.Type.INVALID;
        } else if ("not-wf".equalsIgnoreCase(typeStr)) {
            type = XmlConfTestItem.Type.NOT_WF;
        } else if ("error".equalsIgnoreCase(typeStr)) {
            type = XmlConfTestItem.Type.ERROR;
        } else {
            type = XmlConfTestItem.Type.ERROR;
        }

        String entities = elem.getAttribute("ENTITIES");
        String sections = elem.getAttribute("SECTIONS");
        String recommendation = elem.getAttribute("RECOMMENDATION");
        String version = elem.getAttribute("VERSION");
        String edition = elem.getAttribute("EDITION");
        String namespaceAttr = elem.getAttribute("NAMESPACE");
        boolean namespaceAware = !"no".equalsIgnoreCase(namespaceAttr);
        String output = elem.getAttribute("OUTPUT");
        String description = elem.getTextContent() != null ? elem.getTextContent().trim() : "";

        return new XmlConfTestItem(
                id,
                type,
                testFile,
                entities,
                sections,
                recommendation,
                version,
                edition,
                namespaceAware,
                output,
                description
        );
    }
}
