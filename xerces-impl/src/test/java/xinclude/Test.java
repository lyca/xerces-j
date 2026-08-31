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
package xinclude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import org.junit.jupiter.api.BeforeEach;
import xni.Writer;

import org.apache.xerces.parsers.XIncludeParserConfiguration;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

/**
 * Tests for XInclude implementation converted to JUnit 4.
 */
public class Test implements XMLErrorHandler {

    protected static final String NAMESPACES_FEATURE_ID =
        "http://xml.org/sax/features/namespaces";

    protected static final String VALIDATION_FEATURE_ID =
        "http://xml.org/sax/features/validation";

    protected static final String SCHEMA_VALIDATION_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema";

    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema-full-checking";

    protected static final String ERROR_HANDLER =
        "http://apache.org/xml/properties/internal/error-handler";

    private static final int NUM_TESTS = 41;
    private static final boolean[] TEST_RESULTS = new boolean[] {
        true, true, true, true, true, true, false, true, false, true,   // 1-10
        false, false, false, false, true, true, true, false, true, true, // 11-20
        true, false, true, false, false, false, true, true, false, true, // 21-30
        true, false, true, true, true, true, true, true, false, false,   // 31-40
        true                                                            // 41
    };

    private static final String XML_EXTENSION = ".xml";
    private static final String TXT_EXTENSION = ".txt";

    private Writer fWriter;
    private PrintWriter fOutputWriter;

    @BeforeEach
    public void setUp() {
        XMLParserConfiguration parserConfig = new XIncludeParserConfiguration();
        parserConfig.setFeature(NAMESPACES_FEATURE_ID, true);
        parserConfig.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        parserConfig.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, true);
        fWriter = new Writer(parserConfig);
        parserConfig.setProperty(ERROR_HANDLER, this);
    }

    private void runTest(int testnum) throws Exception {
        String numStr = (testnum < 10 ? "0" : "") + testnum;
        URL testUrl = getClass().getClassLoader().getResource("xinclude/tests/test" + numStr + XML_EXTENSION);
        assertNotNull(testUrl, "Test resource not found: test" + numStr);

        boolean expectedPass = TEST_RESULTS[testnum - 1];
        String ext = expectedPass ? XML_EXTENSION : TXT_EXTENSION;
        String expectedResource = "xinclude/output/test" + numStr + ext;

        boolean passed = true;
        StringBuffer buffer = new StringBuffer();
        try {
            java.io.Writer myWriter = new StringWriter();
            buffer = ((StringWriter) myWriter).getBuffer();
            fOutputWriter = new PrintWriter(myWriter);
            fWriter.setOutput(myWriter);
            fWriter.parse(new XMLInputSource(null, testUrl.toExternalForm(), null));
        }
        catch (XNIException e) {
            passed = false;
        }

        String results = stripUserDir(buffer);

        if (passed != expectedPass) {
            fail("Test " + testnum + " expected " + (expectedPass ? "pass" : "fail") + " but got " + (passed ? "pass" : "fail") + "\nOutput: " + results);
        }

        String expectedContent = normalizeLineEndings(readResource(expectedResource));
        String actualContent = normalizeLineEndings(results);
        assertEquals(expectedContent, actualContent, "Test " + testnum + " output mismatch");
    }

    private String readResource(String resourcePath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private String normalizeLineEndings(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    public void error(String domain, String key, XMLParseException exception) throws XNIException {
        printError("Error", exception);
    }

    public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
        printError("Fatal Error", exception);
    }

    public void warning(String domain, String key, XMLParseException exception) throws XNIException {
        printError("Warning", exception);
    }

    protected void printError(String type, XMLParseException ex) {
        fOutputWriter.print("[");
        fOutputWriter.print(type);
        fOutputWriter.print("] ");
        String systemId = ex.getExpandedSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            fOutputWriter.print(systemId);
        }
        fOutputWriter.print(':');
        fOutputWriter.print(ex.getLineNumber());
        fOutputWriter.print(':');
        fOutputWriter.print(ex.getColumnNumber());
        fOutputWriter.println();
        fOutputWriter.flush();
    }

    private String stripUserDir(StringBuffer buf) {
        String userDir = System.getProperty("user.dir");
        String userURI = "file://";
        if (userDir.charAt(0) != '/') {
            userURI += "/";
        }
        userURI += userDir.replace('\\', '/');
        String str = getPathWithoutEscapes(buf.toString());

        int start = 0, end = 0;
        while ((start = str.indexOf(userURI, start)) != -1) {
            end = start + userURI.length();
            str = str.substring(0, start) + str.substring(end + 1);
        }

        while ((start = str.indexOf(userDir, start)) != -1) {
            end = start + userDir.length();
            str = str.substring(0, start) + str.substring(end + 1);
        }
        return str;
    }

    private static String getPathWithoutEscapes(String origPath) {
        if (origPath != null && origPath.length() != 0 && origPath.indexOf('%') != -1) {
            StringTokenizer tokenizer = new StringTokenizer(origPath, "%");
            StringBuffer result = new StringBuffer(origPath.length());
            int size = tokenizer.countTokens();
            result.append(tokenizer.nextToken());
            for (int i = 1; i < size; ++i) {
                String token = tokenizer.nextToken();
                result.append((char) Integer.valueOf(token.substring(0, 2), 16).intValue());
                result.append(token.substring(2));
            }
            return result.toString();
        }
        return origPath;
    }

    @org.junit.jupiter.api.Test public void test01() throws Exception { runTest(1); }
    @org.junit.jupiter.api.Test public void test02() throws Exception { runTest(2); }
    @org.junit.jupiter.api.Test public void test03() throws Exception { runTest(3); }
    @org.junit.jupiter.api.Test public void test04() throws Exception { runTest(4); }
    @org.junit.jupiter.api.Test public void test05() throws Exception { runTest(5); }
    @org.junit.jupiter.api.Test public void test06() throws Exception { runTest(6); }
    @org.junit.jupiter.api.Test public void test07() throws Exception { runTest(7); }
    @org.junit.jupiter.api.Test public void test08() throws Exception { runTest(8); }
    @org.junit.jupiter.api.Test public void test09() throws Exception { runTest(9); }
    @org.junit.jupiter.api.Test public void test10() throws Exception { runTest(10); }
    @org.junit.jupiter.api.Test public void test11() throws Exception { runTest(11); }
    @org.junit.jupiter.api.Test public void test12() throws Exception { runTest(12); }
    @org.junit.jupiter.api.Test public void test13() throws Exception { runTest(13); }
    @org.junit.jupiter.api.Test public void test14() throws Exception { runTest(14); }
    @org.junit.jupiter.api.Test public void test15() throws Exception { runTest(15); }
    @org.junit.jupiter.api.Test public void test16() throws Exception { runTest(16); }
    @org.junit.jupiter.api.Test public void test17() throws Exception { runTest(17); }
    @org.junit.jupiter.api.Test public void test18() throws Exception { runTest(18); }
    @org.junit.jupiter.api.Test public void test19() throws Exception { runTest(19); }
    @org.junit.jupiter.api.Test public void test20() throws Exception { runTest(20); }
    @org.junit.jupiter.api.Test public void test21() throws Exception { runTest(21); }
    @org.junit.jupiter.api.Test public void test22() throws Exception { runTest(22); }
    @org.junit.jupiter.api.Test public void test23() throws Exception { runTest(23); }
    @org.junit.jupiter.api.Test public void test24() throws Exception { runTest(24); }
    @org.junit.jupiter.api.Test public void test25() throws Exception { runTest(25); }
    @org.junit.jupiter.api.Test public void test26() throws Exception { runTest(26); }
    @org.junit.jupiter.api.Test public void test27() throws Exception { runTest(27); }
    @org.junit.jupiter.api.Test public void test28() throws Exception { runTest(28); }
    @org.junit.jupiter.api.Test public void test29() throws Exception { runTest(29); }
    @org.junit.jupiter.api.Test public void test30() throws Exception { runTest(30); }
    @org.junit.jupiter.api.Test public void test31() throws Exception { runTest(31); }
    @org.junit.jupiter.api.Test public void test32() throws Exception { runTest(32); }
    @org.junit.jupiter.api.Test public void test33() throws Exception { runTest(33); }
    @org.junit.jupiter.api.Test public void test34() throws Exception { runTest(34); }
    @org.junit.jupiter.api.Test public void test35() throws Exception { runTest(35); }
    @org.junit.jupiter.api.Test public void test36() throws Exception { runTest(36); }
    @org.junit.jupiter.api.Test public void test37() throws Exception { runTest(37); }
    @org.junit.jupiter.api.Test public void test38() throws Exception { runTest(38); }
    @org.junit.jupiter.api.Test public void test39() throws Exception { runTest(39); }
    @org.junit.jupiter.api.Test public void test40() throws Exception { runTest(40); }
    @org.junit.jupiter.api.Test public void test41() throws Exception { runTest(41); }
}
