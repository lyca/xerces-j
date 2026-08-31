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

package thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.parsers.SAXParser;
import static org.junit.jupiter.api.Assertions.*;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Multithreaded parser stress test and benchmark for DOM and SAX parsers.
 * Supports JUnit 4 execution as well as standalone CLI execution.
 *
 * @author Andy Heninger, IBM (C++ version)
 * @author Arnaud Le Hors, IBM
 * @version $Id$
 */
public class Test {

    

    @org.junit.jupiter.api.Test
    public void testDOMThreaded() throws Exception {
        Test test = new Test();
        test.run(new String[]{"-dom", "-quiet", "-threads", "4", "-time", "1", resolveXmlPath("data/personal-schema.xml")});
    }

    @org.junit.jupiter.api.Test
    public void testSAXThreaded() throws Exception {
        Test test = new Test();
        test.run(new String[]{"-quiet", "-threads", "4", "-time", "1", resolveXmlPath("data/personal-schema.xml")});
    }

    @org.junit.jupiter.api.Test
    public void testDOMThreadedInMemory() throws Exception {
        Test test = new Test();
        test.run(new String[]{"-dom", "-mem", "-quiet", "-threads", "4", "-time", "1", resolveXmlPath("data/personal-schema.xml")});
    }

    @org.junit.jupiter.api.Test
    public void testSAXThreadedInMemory() throws Exception {
        Test test = new Test();
        test.run(new String[]{"-mem", "-quiet", "-threads", "4", "-time", "1", resolveXmlPath("data/personal-schema.xml")});
    }

    private static String resolveXmlPath(String defaultPath) {
        File file = new File(defaultPath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        URL resource = Test.class.getClassLoader().getResource(defaultPath);
        if (resource != null) {
            return resource.getPath();
        }
        return defaultPath;
    }

    //------------------------------------------------------------------------------
    //
    //  struct InFileInfo   One of these structs will be set up for each file listed
    //                      on the command line.  Once set, the data is unchanging
    //                      and can safely be referenced by the test threads without
    //                      use of synchronization.
    //
    //------------------------------------------------------------------------------
    static class InFileInfo {
        public String fileName;
        public String systemId;
        public String fileContent; // If doing an in-memory parse, this field points
                                   // to an allocated string containing the entire file
                                   // contents. Otherwise it's null.
        int checkSum;              // The XML checksum. Set up by the main thread for
                                   // each file before the worker threads are started.
    }

    //------------------------------------------------------------------------------
    //
    //  struct RunInfo     Holds the info extracted from the command line.
    //
    //------------------------------------------------------------------------------
    static final int MAXINFILES = 25;

    static class RunInfo {
        boolean quiet;
        boolean verbose;
        int numThreads;
        boolean validating;
        boolean dom;
        boolean reuseParser;
        boolean inMemory;
        boolean dumpOnErr;
        int totalTime;
        int numInputFiles;
        volatile boolean stopRequested;
        InFileInfo files[] = new InFileInfo[MAXINFILES];
    }

    //------------------------------------------------------------------------------
    //
    //  struct ThreadInfo  Holds information specific to an individual thread.
    //
    //------------------------------------------------------------------------------
    static class ThreadInfo {
        volatile boolean fHeartBeat; // Set true by the thread each time it finishes
                                    // parsing a file.
        int fParses;                // Number of parses completed.
        int fThreadNum;             // Identifying number for this thread.
        volatile String fErrorMessage; // Error message if a failure occurred.

        ThreadInfo() {
            fHeartBeat = false;
            fParses = 0;
            fThreadNum = -1;
            fErrorMessage = null;
        }
    }

    //
    // Global Data
    //
    RunInfo gRunInfo = new RunInfo();
    ThreadInfo gThreadInfo[];

    //------------------------------------------------------------------------------
    //
    //  class ThreadParser   Bundles together a SAX/DOM parser and handlers.
    //
    //------------------------------------------------------------------------------
    class ThreadParser extends HandlerBase {
        private int fCheckSum;
        private SAXParser fSAXParser;
        private DOMParser fDOMParser;

        public void characters(char chars[], int start, int length) {
            addToCheckSum(chars, start, length);
        }

        public void ignorableWhitespace(char chars[], int start, int length) {
            addToCheckSum(chars, start, length);
        }

        public void warning(SAXParseException ex) {
            if (gRunInfo.verbose) {
                System.err.print("*** Warning " + ex.getMessage());
            }
        }

        public void error(SAXParseException ex) {
            if (gRunInfo.verbose) {
                System.err.print("*** Error " + ex.getMessage());
            }
        }

        public void fatalError(SAXParseException ex) {
            if (gRunInfo.verbose) {
                System.err.print("***** Fatal error " + ex.getMessage());
            }
        }

        ThreadParser() {
            if (gRunInfo.dom) {
                fDOMParser = new DOMParser();
                try {
                    fDOMParser.setFeature("http://xml.org/sax/features/validation",
                            gRunInfo.validating);
                } catch (Exception e) {}
                fDOMParser.setErrorHandler(this);
            } else {
                fSAXParser = new SAXParser();
                try {
                    fSAXParser.setFeature("http://xml.org/sax/features/validation",
                            gRunInfo.validating);
                } catch (Exception e) {}
                fSAXParser.setDocumentHandler(this);
                fSAXParser.setErrorHandler(this);
            }
        }

        int parse(int fileNum) {
            InputSource mbis = null;
            InFileInfo fInfo = gRunInfo.files[fileNum];

            fCheckSum = 0;

            if (gRunInfo.inMemory) {
                mbis = new InputSource(new StringReader(fInfo.fileContent));
                mbis.setSystemId(fInfo.systemId);
            }

            try {
                if (gRunInfo.dom) {
                    if (gRunInfo.inMemory) {
                        fDOMParser.parse(mbis);
                    } else {
                        fDOMParser.parse(fInfo.systemId);
                    }
                    Document doc = fDOMParser.getDocument();
                    domCheckSum(doc);
                    if (doc instanceof CoreDocumentImpl) {
                        CoreDocumentImpl core = (CoreDocumentImpl) doc;
                        DOMConfiguration config = core.getDomConfig();
                        config.setParameter("validate", Boolean.TRUE);
                        config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
                        core.normalizeDocument();
                    }
                } else {
                    if (gRunInfo.inMemory) {
                        fSAXParser.parse(mbis);
                    } else {
                        fSAXParser.parse(fInfo.systemId);
                    }
                }
            } catch (SAXException | IOException e) {
                if (gRunInfo.verbose) {
                    System.err.println(" during parsing: " + fInfo.fileName +
                            " Exception message is: " + e.getMessage());
                }
            }

            return fCheckSum;
        }

        private void addToCheckSum(char chars[], int start, int len) {
            for (int i = start; i < (start + len); i++) {
                fCheckSum = fCheckSum * 5 + chars[i];
            }
        }

        private void addToCheckSum(String chars) {
            int len = chars.length();
            for (int i = 0; i < len; i++) {
                fCheckSum = fCheckSum * 5 + chars.charAt(i);
            }
        }

        public void startElement(String name, AttributeList attributes) {
            addToCheckSum(name);

            int n = attributes.getLength();
            for (int i = 0; i < n; i++) {
                String attNam = attributes.getName(i);
                addToCheckSum(attNam);
                String attVal = attributes.getValue(i);
                addToCheckSum(attVal);
            }
        }

        public void domCheckSum(Node node) {
            if (node == null) return;
            String s;
            Node child;
            NamedNodeMap attributes;

            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE: {
                    s = node.getNodeName();
                    attributes = node.getAttributes();
                    int numAttributes = attributes.getLength();
                    for (int i = 0; i < numAttributes; i++) {
                        domCheckSum(attributes.item(i));
                    }
                    addToCheckSum(s);
                    for (child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                        domCheckSum(child);
                    }
                    break;
                }
                case Node.ATTRIBUTE_NODE: {
                    s = node.getNodeName();
                    addToCheckSum(s);
                    s = node.getNodeValue();
                    if (s != null) {
                        addToCheckSum(s);
                    }
                    break;
                }
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE: {
                    s = node.getNodeValue();
                    if (s != null) {
                        addToCheckSum(s);
                    }
                    break;
                }
                case Node.ENTITY_REFERENCE_NODE:
                case Node.DOCUMENT_NODE: {
                    for (child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                        domCheckSum(child);
                    }
                    break;
                }
            }
        }

        public int reCheck() {
            if (gRunInfo.dom && fDOMParser != null) {
                fCheckSum = 0;
                Document doc = fDOMParser.getDocument();
                domCheckSum(doc);
            }
            return fCheckSum;
        }

        public void domPrint() {
            System.out.println("Begin DOMPrint ...");
            if (gRunInfo.dom && fDOMParser != null) {
                domPrint(fDOMParser.getDocument());
            }
            System.out.println("End DOMPrint");
        }

        void domPrint(Node node) {
            if (node == null) return;
            String s;
            Node child;
            NamedNodeMap attributes;

            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE: {
                    System.out.print("<" + node.getNodeName());
                    attributes = node.getAttributes();
                    int numAttributes = attributes.getLength();
                    for (int i = 0; i < numAttributes; i++) {
                        domPrint(attributes.item(i));
                    }
                    System.out.print(">");
                    for (child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                        domPrint(child);
                    }
                    System.out.print("</" + node.getNodeName() + ">");
                    break;
                }
                case Node.ATTRIBUTE_NODE: {
                    System.out.print(" " + node.getNodeName() + "=\"" + node.getNodeValue() + "\"");
                    break;
                }
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE: {
                    System.out.print(node.getNodeValue());
                    break;
                }
                case Node.ENTITY_REFERENCE_NODE:
                case Node.DOCUMENT_NODE: {
                    for (child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                        domPrint(child);
                    }
                    break;
                }
            }
        }
    }

    void parseCommandLine(String argv[]) {
        gRunInfo.quiet = false;
        gRunInfo.verbose = false;
        gRunInfo.numThreads = 2;
        gRunInfo.validating = false;
        gRunInfo.dom = false;
        gRunInfo.reuseParser = false;
        gRunInfo.inMemory = false;
        gRunInfo.dumpOnErr = false;
        gRunInfo.totalTime = 0;
        gRunInfo.numInputFiles = 0;
        gRunInfo.stopRequested = false;

        try {
            int argnum = 0;
            int argc = argv.length;
            while (argnum < argc) {
                if (argv[argnum].equals("-quiet"))
                    gRunInfo.quiet = true;
                else if (argv[argnum].equals("-verbose"))
                    gRunInfo.verbose = true;
                else if (argv[argnum].equals("-v"))
                    gRunInfo.validating = true;
                else if (argv[argnum].equals("-dom"))
                    gRunInfo.dom = true;
                else if (argv[argnum].equals("-reuse"))
                    gRunInfo.reuseParser = true;
                else if (argv[argnum].equals("-dump"))
                    gRunInfo.dumpOnErr = true;
                else if (argv[argnum].equals("-mem"))
                    gRunInfo.inMemory = true;
                else if (argv[argnum].equals("-threads")) {
                    ++argnum;
                    if (argnum >= argc) throw new IllegalArgumentException();
                    gRunInfo.numThreads = Integer.parseInt(argv[argnum]);
                    if (gRunInfo.numThreads < 0) throw new IllegalArgumentException();
                } else if (argv[argnum].equals("-time")) {
                    ++argnum;
                    if (argnum >= argc) throw new IllegalArgumentException();
                    gRunInfo.totalTime = Integer.parseInt(argv[argnum]);
                    if (gRunInfo.totalTime < 1) throw new IllegalArgumentException();
                } else if (argv[argnum].startsWith("-")) {
                    System.err.println("Unrecognized command line option: " + argv[argnum]);
                    throw new IllegalArgumentException();
                } else {
                    gRunInfo.numInputFiles++;
                    if (gRunInfo.numInputFiles >= MAXINFILES) {
                        System.err.println("Too many input files. Limit is " + MAXINFILES);
                        throw new IllegalArgumentException();
                    }
                    gRunInfo.files[gRunInfo.numInputFiles - 1] = new InFileInfo();
                    gRunInfo.files[gRunInfo.numInputFiles - 1].fileName = argv[argnum];
                    gRunInfo.files[gRunInfo.numInputFiles - 1].systemId = new File(argv[argnum]).toURI().toString();
                }
                argnum++;
            }

            if (gRunInfo.numInputFiles == 0) {
                System.err.println("No input XML file specified on command line.");
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            String usage = "usage: java thread.Test [-v] [-threads nnn] [-time nnn] [-quiet] [-verbose] xmlfile...\n" +
                    "     -v             Use validating parser. Non-validating is default.\n" +
                    "     -dom           Use a DOM parser. Default is SAX.\n" +
                    "     -quiet         Suppress periodic status display.\n" +
                    "     -verbose       Display extra messages.\n" +
                    "     -reuse         Retain and reuse parser. Default creates new for each parse.\n" +
                    "     -threads nnn   Number of threads. Default is 2.\n" +
                    "     -time nnn      Total time to run, in seconds. Default is forever.\n" +
                    "     -dump          Dump DOM tree on error.\n" +
                    "     -mem           Read files into memory once only, and parse them from there.\n";
            throw new IllegalArgumentException(usage, e);
        }
    }

    void readFilesIntoMemory() throws IOException {
        char chars[] = new char[1024];
        StringBuilder buf = new StringBuilder();

        if (gRunInfo.inMemory) {
            for (int fileNum = 0; fileNum < gRunInfo.numInputFiles; fileNum++) {
                InFileInfo fInfo = gRunInfo.files[fileNum];
                buf.setLength(0);
                try (InputStream in = new FileInputStream(fInfo.fileName);
                     InputStreamReader fileF = new InputStreamReader(in)) {
                    int len;
                    while ((len = fileF.read(chars, 0, chars.length)) > 0) {
                        buf.append(chars, 0, len);
                    }
                    fInfo.fileContent = buf.toString();
                }
            }
        }
    }

    class WorkerThread extends Thread {
        ThreadInfo thInfo;

        WorkerThread(ThreadInfo param) {
            super("WorkerThread-" + param.fThreadNum);
            setDaemon(true);
            thInfo = param;
        }

        public void run() {
            ThreadParser thParser = null;
            if (gRunInfo.verbose) {
                System.out.println("Thread " + thInfo.fThreadNum + ": starting");
            }

            int docNum = gRunInfo.numInputFiles;

            while (!gRunInfo.stopRequested) {
                if (thParser == null) {
                    thParser = new ThreadParser();
                }

                docNum++;
                if (docNum >= gRunInfo.numInputFiles) {
                    docNum = 0;
                }

                InFileInfo fInfo = gRunInfo.files[docNum];
                if (gRunInfo.verbose) {
                    System.out.println("Thread " + thInfo.fThreadNum + ": starting file " + fInfo.fileName);
                }

                int checkSum = thParser.parse(docNum);

                if (checkSum != fInfo.checkSum) {
                    String err = "Thread " + thInfo.fThreadNum + ": Parse checksum error on file \"" +
                            fInfo.fileName + "\". Expected " + fInfo.checkSum + ", got " + checkSum;
                    System.err.println(err);
                    thInfo.fErrorMessage = err;
                    gRunInfo.stopRequested = true;
                    if (gRunInfo.dumpOnErr) {
                        thParser.domPrint();
                    }
                    break;
                }

                if (!gRunInfo.reuseParser) {
                    thParser = null;
                }

                thInfo.fHeartBeat = true;
                thInfo.fParses++;
            }
        }
    }

    public double run(String argv[]) throws Exception {
        parseCommandLine(argv);
        readFilesIntoMemory();

        ThreadParser mainParser = new ThreadParser();
        for (int n = 0; n < gRunInfo.numInputFiles; n++) {
            String fileName = gRunInfo.files[n].fileName;
            if (gRunInfo.verbose) {
                System.out.print(fileName + " checksum is ");
            }

            int cksum = mainParser.parse(n);
            if (cksum == 0) {
                throw new AssertionError("An error occurred while initially parsing " + fileName);
            }

            gRunInfo.files[n].checkSum = cksum;
            if (gRunInfo.verbose) {
                System.out.println(cksum);
            }
        }

        if (gRunInfo.numThreads == 0) {
            return 0.0;
        }

        gThreadInfo = new ThreadInfo[gRunInfo.numThreads];
        WorkerThread[] workerThreads = new WorkerThread[gRunInfo.numThreads];

        for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
            gThreadInfo[threadNum] = new ThreadInfo();
            gThreadInfo[threadNum].fThreadNum = threadNum;
            workerThreads[threadNum] = new WorkerThread(gThreadInfo[threadNum]);
            workerThreads[threadNum].start();
        }

        long startTime = System.currentTimeMillis();
        long elapsedSeconds = 0;
        while (!gRunInfo.stopRequested && (gRunInfo.totalTime == 0 || gRunInfo.totalTime > elapsedSeconds)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
            if (!gRunInfo.quiet && !gRunInfo.verbose) {
                char c = '+';
                for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
                    if (!gThreadInfo[threadNum].fHeartBeat) {
                        c = '.';
                        break;
                    }
                }
                System.out.print(c);
                System.out.flush();
                if (c == '+') {
                    for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
                        gThreadInfo[threadNum].fHeartBeat = false;
                    }
                }
            }
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        }

        gRunInfo.stopRequested = true;

        for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
            try {
                workerThreads[threadNum].join(2000);
            } catch (InterruptedException e) {}
        }

        for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
            if (gThreadInfo[threadNum].fErrorMessage != null) {
                org.junit.jupiter.api.Assertions.fail("Worker thread failed: " + gThreadInfo[threadNum].fErrorMessage);
            }
        }

        double totalParsesCompleted = 0;
        for (int threadNum = 0; threadNum < gRunInfo.numThreads; threadNum++) {
            totalParsesCompleted += gThreadInfo[threadNum].fParses;
        }

        double effectiveSeconds = Math.max(1, elapsedSeconds);
        double parsesPerMinute = totalParsesCompleted / (effectiveSeconds / 60.0);
        if (!gRunInfo.quiet) {
            System.out.println("\n" + parsesPerMinute + " parses per minute.");
        }

        return parsesPerMinute;
    }

    public static void main(String argv[]) {
        try {
            Test test = new Test();
            test.run(argv);
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
