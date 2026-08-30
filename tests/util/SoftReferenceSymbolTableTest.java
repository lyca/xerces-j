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

package util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.xerces.dom.CoreDOMImplementationImpl;
import org.apache.xerces.impl.xpath.regex.RegularExpression;
import org.apache.xerces.util.SoftReferenceSymbolTable;
import org.apache.xerces.util.SynchronizedSymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

/**
 * Tests verifying the modernized concurrency, caching and soft reference primitives.
 */
public class SoftReferenceSymbolTableTest {

    @Test
    public void testSoftReferenceSymbolTableBasic() {
        SoftReferenceSymbolTable table = new SoftReferenceSymbolTable(10);
        String s1 = table.addSymbol("testSymbol");
        String s2 = table.addSymbol("testSymbol");
        assertSame(s1, s2);
        assertTrue(table.containsSymbol("testSymbol"));

        char[] chars = "testSymbol".toCharArray();
        assertTrue(table.containsSymbol(chars, 0, chars.length));
        String s3 = table.addSymbol(chars, 0, chars.length);
        assertSame(s1, s3);
    }

    @Test
    public void testSynchronizedSymbolTable() {
        SynchronizedSymbolTable syncTable = new SynchronizedSymbolTable();
        String s1 = syncTable.addSymbol("syncSymbol");
        String s2 = syncTable.addSymbol("syncSymbol");
        assertSame(s1, s2);
        assertTrue(syncTable.containsSymbol("syncSymbol"));
    }

    @Test
    public void testGrammarPoolLockingAndClear() {
        XMLGrammarPoolImpl pool = new XMLGrammarPoolImpl();
        pool.lockPool();
        pool.clear();
        pool.unlockPool();
        pool.clear();
    }

    @Test
    public void testDOMImplementationDocumentCreation() {
        DOMImplementation domImpl = CoreDOMImplementationImpl.getDOMImplementation();
        Document doc1 = domImpl.createDocument("http://example.com", "root1", null);
        Document doc2 = domImpl.createDocument("http://example.com", "root2", null);
        assertNotNull(doc1);
        assertNotNull(doc2);

        DocumentType dt1 = domImpl.createDocumentType("root1", null, null);
        DocumentType dt2 = domImpl.createDocumentType("root2", null, null);
        assertNotNull(dt1);
        assertNotNull(dt2);
    }

    @Test
    public void testRegularExpressionMatching() {
        RegularExpression regex = new RegularExpression("[a-zA-Z_][a-zA-Z0-9_]*");
        assertTrue(regex.matches("validIdentifier_123"));
    }
}
