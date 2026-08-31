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

package xni;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.xerces.parsers.XMLDocumentParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.apache.xerces.xni.parser.XMLPullParserConfiguration;

/**
 * A sample XNI writer used for tests. This test helper illustrates how to
 * use the XNI XMLDocumentHandler callbacks in order to print a
 * document that is parsed.
 */
public class Writer
    extends XMLDocumentParser
    implements XMLErrorHandler {

    // feature ids

    protected static final String NAMESPACES_FEATURE_ID =
        "http://xml.org/sax/features/namespaces";

    protected static final String VALIDATION_FEATURE_ID =
        "http://xml.org/sax/features/validation";

    protected static final String SCHEMA_VALIDATION_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema";

    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID =
        "http://apache.org/xml/features/validation/schema-full-checking";
    
    protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = 
        "http://apache.org/xml/features/honour-all-schemaLocations";

    // default settings

    protected static final String DEFAULT_PARSER_CONFIG =
        "org.apache.xerces.parsers.XIncludeAwareParserConfiguration";

    protected static final boolean DEFAULT_NAMESPACES = true;

    protected static final boolean DEFAULT_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;
    
    protected static final boolean DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS = false;

    protected static final boolean DEFAULT_CANONICAL = false;

    protected static final boolean DEFAULT_INCREMENTAL = false;

    // Data

    protected PrintWriter fOut;

    protected boolean fCanonical;

    protected int fElementDepth;

    protected boolean fSeenRootElement;

    // Constructors

    public Writer(XMLParserConfiguration configuration) {
        super(configuration);
        fConfiguration.setErrorHandler(this);
    }

    // Public methods

    public void setCanonical(boolean canonical) {
        fCanonical = canonical;
    }

    public void setOutput(OutputStream stream, String encoding)
        throws UnsupportedEncodingException {

        if (encoding == null) {
            encoding = "UTF8";
        }

        java.io.Writer writer = new OutputStreamWriter(stream, encoding);
        fOut = new PrintWriter(writer);

    }

    public void setOutput(java.io.Writer writer) {

        fOut = writer instanceof PrintWriter
             ? (PrintWriter)writer : new PrintWriter(writer);

    }

    // XMLDocumentHandler methods

    public void startDocument(XMLLocator locator, String encoding,
            NamespaceContext namespaceContext, Augmentations augs)
        throws XNIException {

        fSeenRootElement = false;
        fElementDepth = 0;

        if (!fCanonical) {
            fOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            fOut.flush();
        }

    }

    public void startElement(QName element, XMLAttributes attrs, Augmentations augs)
        throws XNIException {

        fSeenRootElement = true;
        fElementDepth++;
        fOut.print('<');
        fOut.print(element.rawname);
        if (attrs != null) {
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                fOut.print(' ');
                fOut.print(attrs.getQName(i));
                fOut.print("=\"");
                normalizeAndPrint(attrs.getValue(i));
                fOut.print('"');
            }
        }
        fOut.print('>');
        fOut.flush();

    }

    public void emptyElement(QName element, XMLAttributes attrs, Augmentations augs)
        throws XNIException {

        fSeenRootElement = true;
        fElementDepth++;
        fOut.print('<');
        fOut.print(element.rawname);
        if (attrs != null) {
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                fOut.print(' ');
                fOut.print(attrs.getQName(i));
                fOut.print("=\"");
                normalizeAndPrint(attrs.getValue(i));
                fOut.print('"');
            }
        }
        fOut.print("/>");
        fOut.flush();

    }

    public void processingInstruction(String target, XMLString data, Augmentations augs)
        throws XNIException {

        if (fSeenRootElement) {
            fOut.print('\n');
        }
        fOut.print("<?");
        fOut.print(target);
        if (data != null && data.length > 0) {
            fOut.print(' ');
            fOut.print(data.toString());
        }
        fOut.print("?>");
        if (!fSeenRootElement) {
            fOut.print('\n');
        }
        fOut.flush();

    }

    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (!fCanonical) {
            if (fSeenRootElement && fElementDepth == 0) {
                fOut.print('\n');
            }
            fOut.print("<!--");
            fOut.print(text.toString());
            fOut.print("-->");
            if (!fSeenRootElement) {
                fOut.print('\n');
            }
            fOut.flush();
        }
    }

    public void characters(XMLString text, Augmentations augs) throws XNIException {

        normalizeAndPrint(text);
        fOut.flush();

    }

    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {

        characters(text, augs);
        fOut.flush();

    }

    public void endElement(QName element, Augmentations augs) throws XNIException {

        fElementDepth--;
        fOut.print("</");
        fOut.print(element.rawname);
        fOut.print('>');
        fOut.flush();

    }

    public void startCDATA(Augmentations augs) throws XNIException {
    }

    public void endCDATA(Augmentations augs) throws XNIException {
    }

    // XMLErrorHandler methods

    public void warning(String domain, String key, XMLParseException ex)
        throws XNIException {
        printError("Warning", ex);
    }

    public void error(String domain, String key, XMLParseException ex)
        throws XNIException {
        printError("Error", ex);
    }

    public void fatalError(String domain, String key, XMLParseException ex)
        throws XNIException {
        printError("Fatal Error", ex);
        throw ex;
    }

    // Protected methods

    protected void normalizeAndPrint(String s) {

        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            normalizeAndPrint(c);
        }

    }

    protected void normalizeAndPrint(XMLString text) {
        for (int i = 0; i < text.length; i++) {
            normalizeAndPrint(text.ch[text.offset + i]);
        }
    }

    protected void normalizeAndPrint(char c) {

        switch (c) {
            case '<': {
                fOut.print("&lt;");
                break;
            }
            case '>': {
                fOut.print("&gt;");
                break;
            }
            case '&': {
                fOut.print("&amp;");
                break;
            }
            case '"': {
                fOut.print("&quot;");
                break;
            }
            case '\r':
            case '\n': {
                if (fCanonical) {
                    fOut.print("&#");
                    fOut.print(Integer.toString(c));
                    fOut.print(';');
                    break;
                }
            }
            default: {
                fOut.print(c);
            }
        }

    }

    protected void printError(String type, XMLParseException ex) {

        System.err.print("[");
        System.err.print(type);
        System.err.print("] ");
        String systemId = ex.getExpandedSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            System.err.print(systemId);
        }
        System.err.print(':');
        System.err.print(ex.getLineNumber());
        System.err.print(':');
        System.err.print(ex.getColumnNumber());
        System.err.print(": ");
        System.err.print(ex.getMessage());
        System.err.println();
        System.err.flush();

    }

    // Main

    public static void main(String argv[]) {

        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }

        Writer writer = null;
        XMLParserConfiguration parserConfig = null;
        boolean namespaces = DEFAULT_NAMESPACES;
        boolean validation = DEFAULT_VALIDATION;
        boolean schemaValidation = DEFAULT_SCHEMA_VALIDATION;
        boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
        boolean honourAllSchemaLocations = DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS;
        boolean canonical = DEFAULT_CANONICAL;
        boolean incremental = DEFAULT_INCREMENTAL;

        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if (option.equals("p")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -p option.");
                    }
                    String parserName = argv[i];

                    try {
                        parserConfig = (XMLParserConfiguration) Class.forName(parserName).getDeclaredConstructor().newInstance();
                        writer = null;
                    }
                    catch (Exception e) {
                        parserConfig = null;
                        System.err.println("error: Unable to instantiate parser configuration ("+parserName+")");
                        e.printStackTrace(System.err);
                    }
                    continue;
                }
                if (option.equalsIgnoreCase("n")) {
                    namespaces = option.equals("n");
                    continue;
                }
                if (option.equalsIgnoreCase("v")) {
                    validation = option.equals("v");
                    continue;
                }
                if (option.equalsIgnoreCase("s")) {
                    schemaValidation = option.equals("s");
                    continue;
                }
                if (option.equalsIgnoreCase("f")) {
                    schemaFullChecking = option.equals("f");
                    continue;
                }
                if (option.equalsIgnoreCase("hs")) {
                    honourAllSchemaLocations = option.equals("hs");
                    continue;
                }
                if (option.equalsIgnoreCase("c")) {
                    canonical = option.equals("c");
                    continue;
                }
                if (option.equalsIgnoreCase("i")) {
                    incremental = option.equals("i");
                    continue;
                }
                if (option.equals("h")) {
                    printUsage();
                    continue;
                }
            }

            if (parserConfig == null) {
                try {
                    parserConfig = (XMLParserConfiguration) Class.forName(DEFAULT_PARSER_CONFIG).getDeclaredConstructor().newInstance();
                }
                catch (Exception e) {
                    System.err.println("error: Unable to instantiate parser configuration ("+DEFAULT_PARSER_CONFIG+")");
                    e.printStackTrace(System.err);
                    continue;
                }
            }

            if (writer == null) {
                writer = new Writer(parserConfig);
            }
            try {
                parserConfig.setFeature(NAMESPACES_FEATURE_ID, namespaces);
            }
            catch (XMLConfigurationException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
            }
            try {
                parserConfig.setFeature(VALIDATION_FEATURE_ID, validation);
            }
            catch (XMLConfigurationException e) {
                System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
            }
            try {
                parserConfig.setFeature(SCHEMA_VALIDATION_FEATURE_ID, schemaValidation);
            }
            catch (XMLConfigurationException e) {
                if (e.getType() == XMLConfigurationException.NOT_SUPPORTED) {
                    System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
                }
            }
            try {
                parserConfig.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
            }
            catch (XMLConfigurationException e) {
                if (e.getType() == XMLConfigurationException.NOT_SUPPORTED) {
                    System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
                }
            }
            try {
                parserConfig.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);
            }
            catch (XMLConfigurationException e) {
                if (e.getType() == XMLConfigurationException.NOT_SUPPORTED) {
                    System.err.println("warning: Parser does not support feature ("+HONOUR_ALL_SCHEMA_LOCATIONS_ID+")");
                }
            }

            try {
                writer.setOutput(System.out, "UTF8");
            }
            catch (UnsupportedEncodingException e) {
                System.err.println("error: Unable to set output. Exiting.");
                System.exit(1);
            }
            writer.setCanonical(canonical);
            try {
                if (incremental && parserConfig instanceof XMLPullParserConfiguration) {
                    XMLPullParserConfiguration pullParserConfig = (XMLPullParserConfiguration)parserConfig;
                    pullParserConfig.setInputSource(new XMLInputSource(null, arg, null));
                    int step = 1;
                    do {
                    } while (pullParserConfig.parse(false));
                }
                else {
                    writer.parse(new XMLInputSource(null, arg, null));
                }
            }
            catch (XMLParseException e) {
            }
            catch (Exception e) {
                System.err.println("error: Parse error occurred - "+e.getMessage());
                if (e instanceof XNIException) {
                    e = ((XNIException)e).getException();
                }
                e.printStackTrace(System.err);
            }
        }

    }

    private static void printUsage() {
        System.err.println("usage: java xni.Writer (options) uri ...");
    }

}
