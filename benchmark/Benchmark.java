import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java Benchmark <parser-type> <xml-file> <iterations> <warmup-iterations> [mode:parse|traverse]");
            System.exit(1);
        }

        String parserType = args[0];
        File xmlFile = new File(args[1]);
        int iterations = Integer.parseInt(args[2]);
        int warmup = Integer.parseInt(args[3]);
        String mode = (args.length >= 5) ? args[4] : "parse";

        byte[] xmlBytes = Files.readAllBytes(xmlFile.toPath());
        long fileSize = xmlBytes.length;

        ParserRunner runner = createRunner(parserType, mode);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            runner.parse(xmlBytes);
        }

        // Measure
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            System.gc();
            Thread.sleep(10);
            long start = System.nanoTime();
            runner.parse(xmlBytes);
            long end = System.nanoTime();
            times[i] = end - start;
        }

        Arrays.sort(times);
        long min = times[0];
        long max = times[iterations - 1];
        long sum = 0;
        for (long t : times) sum += t;
        double avg = (double) sum / iterations;

        double avgMs = avg / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        double throughputMBs = (fileSize / (1024.0 * 1024.0)) / (avg / 1_000_000_000.0);

        System.out.printf(Locale.US, "RESULT|%s|%.2f|%.2f|%.2f|%d%n", parserType, avgMs, minMs, throughputMBs, fileSize);
    }

    interface ParserRunner {
        void parse(byte[] xml) throws Exception;
    }

    static void traverseDOM(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            traverseDOM(child);
            child = child.getNextSibling();
        }
    }

    static ParserRunner createRunner(String type, String mode) throws Exception {
        final boolean fullTraverse = "traverse".equalsIgnoreCase(mode);
        final DefaultHandler nullHandler = new DefaultHandler();

        switch (type) {
            case "jdk-sax": {
                final SAXParserFactory spf = SAXParserFactory.newDefaultInstance();
                spf.setNamespaceAware(true);
                return xml -> {
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(new ByteArrayInputStream(xml), nullHandler);
                };
            }

            case "repo-sax":
            case "xerces212-sax": {
                final SAXParserFactory spf = (SAXParserFactory) Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
                spf.setNamespaceAware(true);
                return xml -> {
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(new ByteArrayInputStream(xml), nullHandler);
                };
            }

            case "jdk-dom-defer":
            case "jdk-dom-nodefer": {
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
                dbf.setNamespaceAware(true);
                boolean defer = type.contains("-defer") && !type.contains("-nodefer");
                dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", defer);

                return xml -> {
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    Document doc = builder.parse(new ByteArrayInputStream(xml));
                    if (fullTraverse) {
                        traverseDOM(doc);
                    }
                };
            }

            case "repo-dom-defer":
            case "repo-dom-nodefer":
            case "xerces212-dom-defer":
            case "xerces212-dom-nodefer": {
                final DocumentBuilderFactory dbf = (DocumentBuilderFactory) Class.forName("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl").getDeclaredConstructor().newInstance();
                dbf.setNamespaceAware(true);
                boolean defer = type.contains("-defer") && !type.contains("-nodefer");
                dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", defer);

                return xml -> {
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    Document doc = builder.parse(new ByteArrayInputStream(xml));
                    if (fullTraverse) {
                        traverseDOM(doc);
                    }
                };
            }

            case "woodstox-sax": {
                final SAXParserFactory spf = (SAXParserFactory) Class.forName("com.ctc.wstx.sax.WstxSAXParserFactory").getDeclaredConstructor().newInstance();
                spf.setNamespaceAware(true);
                return xml -> {
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(new ByteArrayInputStream(xml), nullHandler);
                };
            }

            case "woodstox-stax": {
                final XMLInputFactory xif = (XMLInputFactory) Class.forName("com.ctc.wstx.stax.WstxInputFactory").getDeclaredConstructor().newInstance();
                xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
                return xml -> {
                    XMLStreamReader reader = xif.createXMLStreamReader(new ByteArrayInputStream(xml));
                    while (reader.hasNext()) {
                        reader.next();
                    }
                    reader.close();
                };
            }

            case "aalto-sax": {
                final SAXParserFactory spf = (SAXParserFactory) Class.forName("com.fasterxml.aalto.sax.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
                spf.setNamespaceAware(true);
                return xml -> {
                    SAXParser parser = spf.newSAXParser();
                    parser.parse(new ByteArrayInputStream(xml), nullHandler);
                };
            }

            case "repo-stax": {
                final XMLInputFactory xif = (XMLInputFactory) Class.forName("org.apache.xerces.stax.XMLInputFactoryImpl").getDeclaredConstructor().newInstance();
                xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
                return xml -> {
                    XMLStreamReader reader = xif.createXMLStreamReader(new ByteArrayInputStream(xml));
                    while (reader.hasNext()) {
                        reader.next();
                    }
                    reader.close();
                };
            }

            case "aalto-stax": {
                final XMLInputFactory xif = (XMLInputFactory) Class.forName("com.fasterxml.aalto.stax.InputFactoryImpl").getDeclaredConstructor().newInstance();
                xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
                return xml -> {
                    XMLStreamReader reader = xif.createXMLStreamReader(new ByteArrayInputStream(xml));
                    while (reader.hasNext()) {
                        reader.next();
                    }
                    reader.close();
                };
            }

            default:
                throw new IllegalArgumentException("Unknown parser type: " + type);
        }
    }
}
