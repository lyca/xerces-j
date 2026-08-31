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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xerces.parsers.DOMParser;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-driven JUnit 5 test suite executing the W3C XInclude 1.0 Conformance Test Suite.
 */
public class XIncludeConformanceTest {

    private static final Set<String> KNOWN_EXCLUSIONS = new HashSet<String>();

    static {
        // Harold testcases that rely on the optional full xpointer() scheme (XPath)
        KNOWN_EXCLUSIONS.add("harold-05");
        KNOWN_EXCLUSIONS.add("harold-06");
        KNOWN_EXCLUSIONS.add("harold-10");
        KNOWN_EXCLUSIONS.add("harold-11");
        KNOWN_EXCLUSIONS.add("harold-12");
        KNOWN_EXCLUSIONS.add("harold-21");
        KNOWN_EXCLUSIONS.add("harold-34");
        KNOWN_EXCLUSIONS.add("harold-63");

        // Harold testcases that rely on external HTTP network connections (content negotiation to ibiblio / cafeconleche.org)
        KNOWN_EXCLUSIONS.add("harold-87");
        KNOWN_EXCLUSIONS.add("harold-88");
        KNOWN_EXCLUSIONS.add("harold-89");
        KNOWN_EXCLUSIONS.add("harold-90");

        // NIST testcases testing intra-document xpointer references without href or unparsed entities
        KNOWN_EXCLUSIONS.add("Nist-include-17");
        KNOWN_EXCLUSIONS.add("Nist-include-21");
    }

    private static File findXIncludeSuiteDir() {
        String sysProp = System.getProperty("xinclude.dir");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (new File(f, "testdescr.xml").exists()) {
                return f;
            }
        }

        String[] candidates = {
                "build/xinclude-suite/XIncl20060927",
                "build/test-tmp/xinclude/XIncl20060927",
                "../build/xinclude-suite/XIncl20060927",
                "../build/test-tmp/xinclude/XIncl20060927",
                "tests/conformance/xinclude/XIncl20060927",
                "xinclude-suite"
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (new File(f, "testdescr.xml").exists()) {
                return f;
            }
        }
        return null;
    }

    @TestFactory
    @DisplayName("W3C XInclude 1.0 Conformance - SAX Parser")
    public Stream<DynamicNode> saxXIncludeSuite() throws Exception {
        return buildSuite(false);
    }

    @TestFactory
    @DisplayName("W3C XInclude 1.0 Conformance - DOM Parser")
    public Stream<DynamicNode> domXIncludeSuite() throws Exception {
        return buildSuite(true);
    }

    private Stream<DynamicNode> buildSuite(boolean useDom) throws Exception {
        File suiteDir = findXIncludeSuiteDir();
        Assumptions.assumeTrue(suiteDir != null && suiteDir.exists(),
                "XInclude Conformance Suite directory not found. Please run Gradle download task or set -Dxinclude.dir");

        File manifestFile = new File(suiteDir, "testdescr.xml");
        XIncludeGroup rootGroup = XIncludeManifestParser.parse(manifestFile);

        return Stream.of(createDynamicNode(rootGroup, useDom));
    }

    private DynamicNode createDynamicNode(XIncludeGroup group, boolean useDom) {
        List<DynamicNode> children = new ArrayList<DynamicNode>();

        for (XIncludeGroup subGroup : group.getSubGroups()) {
            DynamicNode childContainer = createDynamicNode(subGroup, useDom);
            if (childContainer != null) {
                children.add(childContainer);
            }
        }

        for (XIncludeTestCase test : group.getTests()) {
            children.add(DynamicTest.dynamicTest(
                    test.getId() + " [" + test.getType() + "]",
                    test.getTestFile().toURI(),
                    () -> runTest(test, useDom)
            ));
        }

        if (children.isEmpty()) {
            return null;
        }

        return DynamicContainer.dynamicContainer(group.getName() + " (" + group.getTotalTestCount() + " tests)", children);
    }

    private void runTest(XIncludeTestCase testCase, boolean useDom) throws Exception {
        if (KNOWN_EXCLUSIONS.contains(testCase.getId())) {
            Assumptions.assumeTrue(false, "Skipping known excluded test: " + testCase.getId());
        }

        // Check for optional features (e.g. general full xpointer schemes or unparsed-entities)
        String features = testCase.getFeatures();
        if (features != null) {
            if (features.contains("xpointer-scheme")) {
                Assumptions.assumeTrue(false, "Optional XPointer scheme not implemented in Xerces: " + testCase.getId());
            }
            if (features.contains("unparsed-entities")) {
                Assumptions.assumeTrue(false, "Optional unparsed entities feature skipped: " + testCase.getId());
            }
        }

        File testFile = testCase.getTestFile();
        assertTrue(testFile.exists(), () -> "Test file does not exist: " + testFile.getAbsolutePath());

        TestErrorHandler errorHandler = new TestErrorHandler();

        if (useDom) {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/xinclude", true);
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
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
        } else {
            SAXParser parser = new SAXParser();
            parser.setFeature("http://apache.org/xml/features/xinclude", true);
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
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
        }

        if (testCase.getType() == XIncludeTestCase.Type.ERROR) {
            assertTrue(errorHandler.hasErrors(),
                    () -> "Test expected to fail with error, but passed cleanly: " + testCase.getId());
        } else {
            assertTrue(!errorHandler.hasErrors(),
                    () -> "Test marked as SUCCESS failed with fatal error: \n  - " + errorHandler.formatErrors());
        }
    }

    private static class TestErrorHandler implements ErrorHandler {
        final List<SAXParseException> warnings = Collections.synchronizedList(new ArrayList<>());
        final List<SAXParseException> errors = Collections.synchronizedList(new ArrayList<>());
        final List<SAXParseException> fatalErrors = Collections.synchronizedList(new ArrayList<>());

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

        boolean hasErrors() {
            return !fatalErrors.isEmpty();
        }

        String formatErrors() {
            StringBuilder sb = new StringBuilder();
            for (SAXParseException e : fatalErrors) {
                if (sb.length() > 0) sb.append("\n  - ");
                sb.append("[Line ").append(e.getLineNumber())
                  .append(", Col ").append(e.getColumnNumber()).append("] ")
                  .append(e.getMessage());
            }
            return sb.toString();
        }
    }
}
