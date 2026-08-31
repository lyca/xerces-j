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

package dom.range;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ranges.Range;
import org.xml.sax.InputSource;

/** 
 * RangeTest tests cases delineated as examples in the DOM Level 2 Range specification.
 */
public class Test {

    private static final String[] TESTS = {
        "<FOO>AB<MOO>CD</MOO>CD</FOO>",
        "<FOO>A<MOO>BC</MOO>DE</FOO>",
        "<FOO>XY<BAR>ZW</BAR>Q</FOO>",
        "<FOO><BAR1>AB</BAR1><BAR2/><BAR3>CD</BAR3></FOO>",
        "<A><B><M/><C><D/><E/><F/><HELLO/></C><N/><O/></B>" +
        "<Z><X/><Y/></Z>" +
        "<G/><Q><V/><W/></Q></A>"
    };

    private static final String[] DELETE_RESULT = {
        "<FOO>ACD</FOO>",
        "<FOO>A<MOO>B</MOO>E</FOO>",
        "<FOO>X<BAR>W</BAR>Q</FOO>",
        "<FOO><BAR1>A</BAR1><BAR3>D</BAR3></FOO>",
        "<A><B><M></M><C><D></D></C></B><Q><W></W></Q></A>"
    };

    private static final String[] EXTRACT_RESULT = {
        "B<MOO>CD</MOO>",
        "<MOO>C</MOO>D",
        "Y<BAR>Z</BAR>",
        "<BAR1>B</BAR1><BAR2></BAR2><BAR3>C</BAR3>",
        "<B><C><E></E><F></F><HELLO></HELLO></C>" +
        "<N></N><O></O></B><Z><X></X><Y></Y></Z><G></G><Q><V></V></Q>"
    };

    private static final String INSERT = "***";
    private static final String[] INSERT_RESULT = {
        "<FOO>A" + INSERT + "B<MOO>CD</MOO>CD</FOO>",
        "<FOO>A<MOO>B" + INSERT + "C</MOO>DE</FOO>",
        "<FOO>X" + INSERT + "Y<BAR>ZW</BAR>Q</FOO>",
        "<FOO><BAR1>A" + INSERT + "B</BAR1><BAR2></BAR2><BAR3>CD</BAR3></FOO>",
        "<A><B><M></M><C><D></D>" + INSERT + "<E></E><F></F><HELLO></HELLO></C>" +
        "<N></N><O></O></B><Z><X></X><Y></Y></Z><G></G><Q><V></V><W></W></Q></A>"
    };

    private static final String SURROUND = "SURROUND";
    private static final String[] SURROUND_RESULT = {
        "<FOO>A<" + SURROUND + ">B<MOO>CD</MOO>C</" + SURROUND + ">D</FOO>",
        "<FOO>A<MOO>B<" + SURROUND + ">C</" + SURROUND + "></MOO>DE</FOO>",
        "<FOO>X<" + SURROUND + ">Y<BAR>ZW</BAR></" + SURROUND + ">Q</FOO>",
        "<FOO><BAR1>AB</BAR1><" + SURROUND + "><BAR2></BAR2></" + SURROUND + "><BAR3>CD</BAR3></FOO>",
        "<A><B><M></M><C><D></D><E></E><F></F><HELLO></HELLO></C>" +
        "<N></N><O></O></B><Z><" + SURROUND + "><X></X><Y></Y></" + SURROUND + "></Z>" +
        "<G></G><Q><V></V><W></W></Q></A>"
    };

    private static final String[] RANGE_DELETE = {
        "<P>Abcd efgh The Range ijkl</P>",
        "<p>Abcd efgh The Range ijkl</p>",
        "<P>ABCD efgh The <EM>Range</EM> ijkl</P>",
        "<P>Abcd efgh The Range ijkl</P>",
        "<P>Abcd <EM>efgh The Range ij</EM>kl</P>"
    };

    private static final String[] RANGE_DELETE_RESULT = {
        "<P>Abcd ^Range ijkl</P>",
        "<p>Abcd ^kl</p>",
        "<P>ABCD ^<EM>ange</EM> ijkl</P>",
        "<P>Abcd ^he Range ijkl</P>",
        "<P>Abcd ^kl</P>"
    };

    private static final String INSERT2 = "<P>Abcd efgh XY blah ijkl</P>";
    private static final String INSERTED_TEXT = "INSERTED TEXT";

    private static final String[] RANGE_INSERT_RESULT = {
        "<P>Abcd efgh INSERTED TEXTXY blah ijkl</P>",
        "<P>Abcd efgh XINSERTED TEXTY blah ijkl</P>",
        "<P>Abcd efgh XYINSERTED TEXT blah ijkl</P>",
        "<P>Abcd efgh XY blahINSERTED TEXT ijkl</P>"
    };

    private DocumentImpl parseDoc(String xml) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(xml)));
        return (DocumentImpl) parser.getDocument();
    }

    private void setupRange(Range range, Node root, int index, boolean surround) {
        if (index == 0) {
            range.setStart(root.getFirstChild(), 1);
            range.setEndBefore(root.getLastChild());
            if (surround) {
                range.setEnd(root.getLastChild(), 1);
            }
        } else if (index == 1) {
            Node n1 = root.getFirstChild().getNextSibling().getFirstChild();
            range.setStart(n1, 1);
            range.setEnd(root.getLastChild(), 1);
            if (surround) {
                range.setEnd(n1, 2);
            }
        } else if (index == 2) {
            range.setStart(root.getFirstChild(), 1);
            Node n2 = root.getFirstChild().getNextSibling().getFirstChild();
            range.setEnd(n2, 1);
            if (surround) {
                range.setEndBefore(root.getLastChild());
            }
        } else if (index == 3) {
            Node n3 = root.getFirstChild().getFirstChild();
            range.setStart(n3, 1);
            range.setEnd(root.getLastChild().getFirstChild(), 1);
            if (surround) {
                range.selectNode(root.getFirstChild().getNextSibling());
            }
        } else if (index == 4) {
            Node n4 = root.getFirstChild().getFirstChild().getNextSibling().getFirstChild();
            range.setStartAfter(n4);
            range.setEndAfter(root.getLastChild().getFirstChild());
            if (surround) {
                range.selectNodeContents(root.getFirstChild().getNextSibling());
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void testDelete() throws Exception {
        for (int i = 0; i < TESTS.length; i++) {
            DocumentImpl document = parseDoc(TESTS[i]);
            Range range = document.createRange();
            setupRange(range, document.getDocumentElement(), i, false);
            range.deleteContents();
            assertEquals("Delete test failed at index " + i, DELETE_RESULT[i], toString(document));
        }
    }

    @org.junit.jupiter.api.Test
    public void testExtract() throws Exception {
        for (int i = 0; i < TESTS.length; i++) {
            DocumentImpl document = parseDoc(TESTS[i]);
            Range range = document.createRange();
            setupRange(range, document.getDocumentElement(), i, false);
            DocumentFragment frag = range.extractContents();
            assertEquals("Extract doc failed at index " + i, DELETE_RESULT[i], toString(document));
            assertEquals("Extract fragment failed at index " + i, EXTRACT_RESULT[i], toString(frag));
        }
    }

    @org.junit.jupiter.api.Test
    public void testClone() throws Exception {
        for (int i = 0; i < TESTS.length; i++) {
            DocumentImpl document = parseDoc(TESTS[i]);
            Range range = document.createRange();
            setupRange(range, document.getDocumentElement(), i, false);
            DocumentFragment frag = range.cloneContents();
            assertEquals("Clone fragment failed at index " + i, EXTRACT_RESULT[i], toString(frag));
        }
    }

    @org.junit.jupiter.api.Test
    public void testInsert() throws Exception {
        for (int i = 0; i < TESTS.length; i++) {
            DocumentImpl document = parseDoc(TESTS[i]);
            Range range = document.createRange();
            setupRange(range, document.getDocumentElement(), i, false);
            range.insertNode(document.createTextNode(INSERT));
            assertEquals("Insert test failed at index " + i, INSERT_RESULT[i], toString(document));
        }
    }

    @org.junit.jupiter.api.Test
    public void testSurround() throws Exception {
        for (int i = 0; i < TESTS.length; i++) {
            DocumentImpl document = parseDoc(TESTS[i]);
            Range range = document.createRange();
            setupRange(range, document.getDocumentElement(), i, true);
            Node surroundNode = document.createElement(SURROUND);
            range.surroundContents(surroundNode);
            assertEquals("Surround test failed at index " + i, SURROUND_RESULT[i], toString(document));
        }
    }

    @org.junit.jupiter.api.Test
    public void testInsertMutation() throws Exception {
        for (int i = 0; i < 4; i++) {
            DocumentImpl document = parseDoc(INSERT2);
            Node root = document.getDocumentElement();
            Range rangei = document.createRange();
            if (i == 0) {
                rangei.setStart(root.getFirstChild(), 10);
                rangei.setEnd(root.getFirstChild(), 10);
            } else if (i == 1) {
                rangei.setStart(root.getFirstChild(), 11);
                rangei.setEnd(root.getFirstChild(), 11);
            } else if (i == 2) {
                rangei.setStart(root.getFirstChild(), 12);
                rangei.setEnd(root.getFirstChild(), 12);
            } else if (i == 3) {
                rangei.setStart(root.getFirstChild(), 17);
                rangei.setEnd(root.getFirstChild(), 17);
            }
            rangei.insertNode(document.createTextNode(INSERTED_TEXT));
            assertEquals("Insert mutation failed at index " + i, RANGE_INSERT_RESULT[i], toString(document));
        }
    }

    @org.junit.jupiter.api.Test
    public void testDeleteMutation() throws Exception {
        for (int i = 0; i < RANGE_DELETE.length; i++) {
            DocumentImpl document = parseDoc(RANGE_DELETE[i]);
            Range ranged = document.createRange();
            Node root = document.getDocumentElement();

            if (i == 0) {
                ranged.setStart(root.getFirstChild(), 5);
                ranged.setEnd(root.getFirstChild(), 14);
            } else if (i == 1) {
                ranged.setStart(root.getFirstChild(), 5);
                ranged.setEnd(root.getFirstChild(), 22);
            } else if (i == 2) {
                ranged.setStart(root.getFirstChild(), 5);
                ranged.setEnd(root.getFirstChild().getNextSibling().getFirstChild(), 1);
            } else if (i == 3) {
                ranged.setStart(root.getFirstChild(), 5);
                ranged.setEnd(root.getFirstChild(), 11);
            } else if (i == 4) {
                ranged.selectNode(root.getFirstChild().getNextSibling());
            }

            ranged.deleteContents();
            ranged.insertNode(document.createTextNode("^"));
            assertEquals("Delete mutation failed at index " + i, RANGE_DELETE_RESULT[i], toString(document));
        }
    }

    private String toString(Node node) {
        StringBuffer sb = new StringBuffer();
        return print(node, sb);
    }

    private String print(Node node, StringBuffer sb) {
        if (node == null) {
            return sb.toString();
        }

        int type = node.getNodeType();
        switch (type) {
            case Node.DOCUMENT_NODE:
                return print(((Document) node).getDocumentElement(), sb);
            case Node.ELEMENT_NODE:
                sb.append('<');
                sb.append(node.getNodeName());
                Attr[] attrs = sortAttributes(node.getAttributes());
                for (int i = 0; i < attrs.length; i++) {
                    Attr attr = attrs[i];
                    sb.append(' ');
                    sb.append(attr.getNodeName());
                    sb.append("=\"");
                    sb.append(normalize(attr.getNodeValue()));
                    sb.append('\"');
                }
                sb.append('>');
                NodeList children = node.getChildNodes();
                if (children != null) {
                    int len = children.getLength();
                    for (int i = 0; i < len; i++) {
                        print(children.item(i), sb);
                    }
                }
                sb.append("</");
                sb.append(node.getNodeName());
                sb.append('>');
                break;
            case Node.ENTITY_REFERENCE_NODE:
                NodeList entChildren = node.getChildNodes();
                if (entChildren != null) {
                    int len = entChildren.getLength();
                    for (int i = 0; i < len; i++) {
                        print(entChildren.item(i), sb);
                    }
                }
                break;
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                sb.append(normalize(node.getNodeValue()));
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                sb.append("<?");
                sb.append(node.getNodeName());
                String data = node.getNodeValue();
                if (data != null && data.length() > 0) {
                    sb.append(' ');
                    sb.append(data);
                }
                sb.append("?>");
                break;
            case Node.DOCUMENT_FRAGMENT_NODE:
                NodeList fragChildren = node.getChildNodes();
                if (fragChildren != null) {
                    int len = fragChildren.getLength();
                    for (int i = 0; i < len; i++) {
                        print(fragChildren.item(i), sb);
                    }
                }
                break;
            default:
                break;
        }

        return sb.toString();
    }

    private Attr[] sortAttributes(NamedNodeMap attrs) {
        int len = (attrs != null) ? attrs.getLength() : 0;
        Attr[] array = new Attr[len];
        for (int i = 0; i < len; i++) {
            array[i] = (Attr) attrs.item(i);
        }
        for (int i = 0; i < len - 1; i++) {
            String name = array[i].getNodeName();
            int index = i;
            for (int j = i + 1; j < len; j++) {
                String curName = array[j].getNodeName();
                if (curName.compareTo(name) < 0) {
                    name = curName;
                    index = j;
                }
            }
            if (index != i) {
                Attr temp = array[i];
                array[i] = array[index];
                array[index] = temp;
            }
        }
        return array;
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        StringBuffer str = new StringBuffer();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '<':
                    str.append("&lt;");
                    break;
                case '>':
                    str.append("&gt;");
                    break;
                case '&':
                    str.append("&amp;");
                    break;
                case '"':
                    str.append("&quot;");
                    break;
                case '\r':
                case '\n':
                    str.append("&#");
                    str.append(Integer.toString(ch));
                    str.append(';');
                    break;
                default:
                    str.append(ch);
            }
        }
        return str.toString();
    }

    

    public static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
    public static void assertTrue(String message, boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }
    public static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
    public static void assertFalse(String message, boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }
    public static void assertNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object);
    }
    public static void assertNull(String message, Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object, message);
    }
    public static void assertNotNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object);
    }
    public static void assertNotNull(String message, Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object, message);
    }
    public static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertEquals(double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    public static void assertEquals(String message, double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    public static void assertSame(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }
    public static void assertSame(String message, Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual, message);
    }
    public static void fail(String message) {
        org.junit.jupiter.api.Assertions.fail(message);
    }

}
