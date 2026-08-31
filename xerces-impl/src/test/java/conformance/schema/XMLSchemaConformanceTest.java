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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

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
 * Data-driven JUnit 5 test suite executing the W3C XML Schema Test Suite (XSTS).
 */
public class XMLSchemaConformanceTest {

    private static final Set<String> KNOWN_EXCLUSIONS = new HashSet<String>();

    static {
        loadExclusions();
    }

    private static void loadExclusions() {
        try (InputStream is = XMLSchemaConformanceTest.class.getResourceAsStream("/conformance/schema/ms-exclusions.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            KNOWN_EXCLUSIONS.add(line);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static File findXmlSchemaSuiteDir() {
        String sysProp = System.getProperty("xmlschema.dir");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (new File(f, "SunXMLSchema1-0-20020116.testSet").exists() ||
                new File(f, "NISTXMLSchema1-0-20020116.testSet").exists()) {
                return f;
            }
        }

        String[] candidates = {
                "build/xsts-suite/xmlschema2002-01-16",
                "build/test-tmp/xsts/xmlschema2002-01-16",
                "../build/xsts-suite/xmlschema2002-01-16",
                "../build/test-tmp/xsts/xmlschema2002-01-16",
                "tests/conformance/xsts/xmlschema2002-01-16",
                "xsts-suite"
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (new File(f, "SunXMLSchema1-0-20020116.testSet").exists()) {
                return f;
            }
        }
        return null;
    }

    @TestFactory
    @DisplayName("W3C XML Schema Conformance - Sun TestSet")
    public Stream<DynamicNode> sunSchemaSuite() throws Exception {
        return buildSuite("SunXMLSchema1-0-20020116.testSet");
    }

    @TestFactory
    @DisplayName("W3C XML Schema Conformance - NIST Datatypes TestSet")
    public Stream<DynamicNode> nistSchemaSuite() throws Exception {
        return buildSuite("NISTXMLSchema1-0-20020116.testSet");
    }

    @TestFactory
    @DisplayName("W3C XML Schema Conformance - Microsoft TestSet")
    public Stream<DynamicNode> microsoftSchemaSuite() throws Exception {
        return buildSuite("MSXMLSchema1-0-20020116.testSet");
    }

    private Stream<DynamicNode> buildSuite(String testSetName) throws Exception {
        File suiteDir = findXmlSchemaSuiteDir();
        Assumptions.assumeTrue(suiteDir != null && suiteDir.exists(),
                "XML Schema Test Suite directory not found. Please run Gradle download task or set -Dxmlschema.dir");

        File testSetFile = new File(suiteDir, testSetName);
        Assumptions.assumeTrue(testSetFile.exists(), "TestSet file not found: " + testSetFile.getAbsolutePath());

        SchemaTestSet testSet = SchemaTestSetParser.parse(testSetFile);
        return Stream.of(createDynamicNode(testSet));
    }

    private DynamicNode createDynamicNode(SchemaTestSet testSet) {
        List<DynamicNode> groupContainers = new ArrayList<DynamicNode>();

        for (SchemaTestGroup group : testSet.getGroups()) {
            List<DynamicNode> tests = new ArrayList<DynamicNode>();
            for (SchemaTestCase testCase : group.getTestCases()) {
                tests.add(DynamicTest.dynamicTest(
                        testCase.getTestName() + " [" + testCase.getKind() + ": " + testCase.getExpectedValidity() + "]",
                        testCase.getDocumentFile() != null ? testCase.getDocumentFile().toURI() : null,
                        () -> runTestCase(testCase)
                ));
            }
            groupContainers.add(DynamicContainer.dynamicContainer(
                    group.getName() + " (" + group.getTestCases().size() + " tests)",
                    tests
            ));
        }

        return DynamicContainer.dynamicContainer(
                testSet.getContributor() + " (" + testSet.getTotalTestCount() + " tests)",
                groupContainers
        );
    }

    private void runTestCase(SchemaTestCase testCase) throws Exception {
        if (KNOWN_EXCLUSIONS.contains(testCase.getTestName()) ||
            KNOWN_EXCLUSIONS.contains(testCase.getGroupName())) {
            Assumptions.assumeTrue(false, "Skipping known excluded test: " + testCase.getTestName());
        }

        if (testCase.getExpectedValidity() == SchemaTestCase.Validity.NOT_KNOWN) {
            Assumptions.assumeTrue(false, "Skipping test with unspecified expected validity");
        }

        // Skip known NIST 2002 QName tests that rely on undeclared prefix strings
        if (testCase.getGroupName().startsWith("NISTSchema-QName")) {
            Assumptions.assumeTrue(false, "Skipping NIST 2002 QName test with undeclared prefix errata");
        }

        // Skip NIST 2002 float/double precision boundary tests corrected in later editions
        if (testCase.getGroupName().startsWith("NISTSchema-double-minExclusive") ||
            testCase.getGroupName().startsWith("NISTSchema-double-maxExclusive") ||
            testCase.getGroupName().startsWith("NISTSchema-float-minExclusive") ||
            testCase.getGroupName().startsWith("NISTSchema-float-maxExclusive")) {
            Assumptions.assumeTrue(false, "Skipping NIST 2002 float/double precision boundary test");
        }

        File docFile = testCase.getDocumentFile();
        Assumptions.assumeTrue(docFile != null && docFile.exists(),
                "Document file does not exist: " + (docFile != null ? docFile.getAbsolutePath() : "null"));

        TestErrorHandler errorHandler = new TestErrorHandler();

        if (testCase.getKind() == SchemaTestCase.Kind.SCHEMA) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
            } catch (Exception ignored) {
            }
            factory.setErrorHandler(errorHandler);
            Schema schema = null;
            try {
                schema = factory.newSchema(new StreamSource(docFile));
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

            if (testCase.getExpectedValidity() == SchemaTestCase.Validity.VALID) {
                assertTrue(errorHandler.fatalErrors.isEmpty() && errorHandler.errors.isEmpty(),
                        () -> "Schema expected to be VALID reported errors: " + formatErrors(errorHandler.allErrors()));
                assertTrue(schema != null, "Schema was expected to compile successfully but was null");
            } else if (testCase.getExpectedValidity() == SchemaTestCase.Validity.INVALID) {
                boolean hasErrors = !errorHandler.fatalErrors.isEmpty() || !errorHandler.errors.isEmpty() || schema == null;
                assertTrue(hasErrors, "Schema was expected to be INVALID but compiled without errors");
            }
        } else {
            // Instance test
            File schemaFile = testCase.getSchemaFile();
            if (schemaFile != null && schemaFile.exists()) {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                try {
                    factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
                } catch (Exception ignored) {
                }
                Schema schema = null;
                try {
                    schema = factory.newSchema(new StreamSource(schemaFile));
                } catch (Exception e) {
                    if (testCase.getExpectedValidity() == SchemaTestCase.Validity.INVALID) {
                        // Schema failed to compile, so instance is also invalid
                        return;
                    }
                    throw e;
                }

                Validator validator = schema.newValidator();
                validator.setErrorHandler(errorHandler);
                try {
                    validator.validate(new StreamSource(docFile));
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
            } else {
                SAXParser parser = new SAXParser();
                parser.setFeature("http://xml.org/sax/features/namespaces", true);
                parser.setFeature("http://xml.org/sax/features/validation", true);
                parser.setFeature("http://apache.org/xml/features/validation/schema", true);
                parser.setErrorHandler(errorHandler);

                try (FileInputStream fis = new FileInputStream(docFile)) {
                    InputSource is = new InputSource(fis);
                    is.setSystemId(docFile.toURI().toASCIIString());
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

            if (testCase.getExpectedValidity() == SchemaTestCase.Validity.VALID) {
                assertTrue(errorHandler.fatalErrors.isEmpty() && errorHandler.errors.isEmpty(),
                        () -> "Instance expected to be VALID reported errors: " + formatErrors(errorHandler.allErrors()));
            } else if (testCase.getExpectedValidity() == SchemaTestCase.Validity.INVALID) {
                boolean hasErrors = !errorHandler.fatalErrors.isEmpty() || !errorHandler.errors.isEmpty();
                assertTrue(hasErrors, "Instance was expected to be INVALID but validated without errors");
            }
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

        public List<SAXParseException> allErrors() {
            List<SAXParseException> all = new ArrayList<SAXParseException>(fatalErrors);
            all.addAll(errors);
            return all;
        }

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
