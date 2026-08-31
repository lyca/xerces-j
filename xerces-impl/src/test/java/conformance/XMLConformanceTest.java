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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xerces.parsers.SAXParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-driven JUnit 5 test suite executing the W3C XML Conformance Test Suite.
 */
public class XMLConformanceTest {

    private static final Set<String> KNOWN_EXCLUSIONS = new HashSet<String>();

    static {
        // Namespaces 1.0 Erratum NE13 tests: In Namespaces 1.0 1st edition, binding
        // http://www.w3.org/XML/1998/namespace as default xmlns was permitted.
        KNOWN_EXCLUSIONS.add("rmt-ns-e1.0-13a");
        KNOWN_EXCLUSIONS.add("rmt-ns-e1.0-13b");
        KNOWN_EXCLUSIONS.add("rmt-ns-e1.0-13c");

        // UTF-8 BOM with ISO-8859-1 encoding declaration (Bjoern Hoehrmann errata test)
        KNOWN_EXCLUSIONS.add("hst-lhs-007");
    }

    private static File findXmlConfDir() {
        String sysProp = System.getProperty("xmlconf.dir");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (new File(f, "xmlconf.xml").exists()) {
                return f;
            }
        }

        String[] candidates = {
                "build/xmlconf-suite/xmlconf",
                "build/test-tmp/xmlconf",
                "../build/xmlconf-suite/xmlconf",
                "../build/test-tmp/xmlconf",
                "tests/conformance/xmlconf",
                "xmlconf"
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (new File(f, "xmlconf.xml").exists()) {
                return f;
            }
        }
        return null;
    }

    private static boolean isEditionSupported(String edition) {
        if (edition == null || edition.trim().isEmpty()) {
            return true;
        }
        // Filter out tests that specifically target XML 1.0 5th edition only
        String[] parts = edition.trim().split("\\s+");
        for (String part : parts) {
            if ("1".equals(part) || "2".equals(part) || "3".equals(part) || "4".equals(part) || "all".equalsIgnoreCase(part)) {
                return true;
            }
        }
        return false;
    }

    @TestFactory
    @DisplayName("W3C XML Conformance - Non-Validating SAX")
    public Stream<DynamicNode> nonValidatingSaxSuite() throws Exception {
        return buildSuite(false);
    }

    @TestFactory
    @DisplayName("W3C XML Conformance - Validating SAX")
    public Stream<DynamicNode> validatingSaxSuite() throws Exception {
        return buildSuite(true);
    }

    private Stream<DynamicNode> buildSuite(boolean validating) throws Exception {
        File confDir = findXmlConfDir();
        Assumptions.assumeTrue(confDir != null && confDir.exists(),
                "XML Conformance Suite directory not found. Please run Gradle download task or set -Dxmlconf.dir");

        File manifestFile = new File(confDir, "xmlconf.xml");
        XmlConfGroup rootGroup = XmlConfManifestParser.parse(manifestFile);

        return Stream.of(createDynamicNode(rootGroup, validating));
    }

    private DynamicNode createDynamicNode(XmlConfGroup group, boolean validating) {
        List<DynamicNode> children = new ArrayList<DynamicNode>();

        for (XmlConfGroup subGroup : group.getSubGroups()) {
            DynamicNode childContainer = createDynamicNode(subGroup, validating);
            if (childContainer != null) {
                children.add(childContainer);
            }
        }

        for (XmlConfTestItem test : group.getTests()) {
            children.add(DynamicTest.dynamicTest(
                    test.getId() + " [" + test.getType() + "]",
                    test.getTestFile().toURI(),
                    () -> runSaxTest(test, validating)
            ));
        }

        if (children.isEmpty()) {
            return null;
        }

        return DynamicContainer.dynamicContainer(group.getName() + " (" + group.getTotalTestCount() + " tests)", children);
    }

    private void runSaxTest(XmlConfTestItem testItem, boolean validating) throws Exception {
        if (KNOWN_EXCLUSIONS.contains(testItem.getId())) {
            Assumptions.assumeTrue(false, "Skipping known excluded test: " + testItem.getId());
        }

        if (!isEditionSupported(testItem.getEdition())) {
            Assumptions.assumeTrue(false, "Skipping test for unsupported edition: " + testItem.getEdition());
        }

        File testFile = testItem.getTestFile();
        assertTrue(testFile.exists(), () -> "Test file does not exist: " + testFile.getAbsolutePath());

        SAXParser parser = new SAXParser();
        try {
            parser.setFeature("http://xml.org/sax/features/namespaces", testItem.isNamespaceAware());
            parser.setFeature("http://xml.org/sax/features/validation", validating);
            parser.setFeature("http://xml.org/sax/features/external-general-entities", true);
            parser.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SAXParser: " + e.getMessage(), e);
        }

        TestErrorHandler errorHandler = new TestErrorHandler();
        parser.setErrorHandler(errorHandler);

        try (FileInputStream fis = new FileInputStream(testFile)) {
            InputSource is = new InputSource(fis);
            is.setSystemId(testFile.toURI().toASCIIString());
            try {
                parser.parse(is);
            } catch (SAXParseException spe) {
                errorHandler.fatalErrors.add(spe);
            } catch (SAXException se) {
                if (se.getException() instanceof SAXParseException) {
                    errorHandler.fatalErrors.add((SAXParseException) se.getException());
                } else {
                    errorHandler.fatalErrors.add(new SAXParseException(se.getMessage(), null, se));
                }
            } catch (Exception e) {
                errorHandler.fatalErrors.add(new SAXParseException(e.getMessage(), null, e));
            }
        }

        switch (testItem.getType()) {
            case VALID:
                assertTrue(errorHandler.fatalErrors.isEmpty(),
                        () -> "Document marked as VALID failed with fatal error: " + formatErrors(errorHandler.fatalErrors));
                assertTrue(errorHandler.errors.isEmpty(),
                        () -> "Document marked as VALID reported validity error: " + formatErrors(errorHandler.errors));
                break;

            case INVALID:
                assertTrue(errorHandler.fatalErrors.isEmpty(),
                        () -> "Document marked as INVALID failed with fatal well-formedness error: " + formatErrors(errorHandler.fatalErrors));
                if (validating) {
                    assertFalse(errorHandler.errors.isEmpty(),
                            () -> "Document marked as INVALID must report validity error when validating, but had none.");
                }
                break;

            case NOT_WF:
                assertFalse(errorHandler.fatalErrors.isEmpty(),
                        () -> "Document marked as NOT-WF should have failed with fatal error, but parsed successfully without fatal error.");
                break;

            case ERROR:
                // Optional error condition in the specification; no crash should occur
                break;
        }
    }

    private static String formatErrors(List<SAXParseException> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        for (SAXParseException e : list) {
            sb.append("\n  - [Line ").append(e.getLineNumber()).append(", Col ").append(e.getColumnNumber())
                    .append("] ").append(e.getMessage());
        }
        return sb.toString();
    }

    private static class TestErrorHandler implements ErrorHandler {
        final List<SAXParseException> warnings = Collections.synchronizedList(new ArrayList<SAXParseException>());
        final List<SAXParseException> errors = Collections.synchronizedList(new ArrayList<SAXParseException>());
        final List<SAXParseException> fatalErrors = Collections.synchronizedList(new ArrayList<SAXParseException>());

        @Override
        public void warning(SAXParseException exception) {
            warnings.add(exception);
        }

        @Override
        public void error(SAXParseException exception) {
            errors.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            fatalErrors.add(exception);
        }
    }
}
