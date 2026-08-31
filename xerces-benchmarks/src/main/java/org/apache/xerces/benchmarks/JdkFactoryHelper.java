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

package org.apache.xerces.benchmarks;

import java.lang.reflect.Method;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

/**
 * Utility for instantiating JDK built-in XML parser factories across Java 8 - Java 25+.
 */
public final class JdkFactoryHelper {

    private JdkFactoryHelper() {}

    public static SAXParserFactory newJdkSaxParserFactory() {
        try {
            Method m = SAXParserFactory.class.getMethod("newDefaultInstance");
            return (SAXParserFactory) m.invoke(null);
        } catch (Throwable t) {
            try {
                return (SAXParserFactory) Class.forName("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
            } catch (Throwable t2) {
                return SAXParserFactory.newInstance();
            }
        }
    }

    public static XMLInputFactory newJdkXmlInputFactory() {
        try {
            Method m = XMLInputFactory.class.getMethod("newDefaultFactory");
            return (XMLInputFactory) m.invoke(null);
        } catch (Throwable t) {
            try {
                return (XMLInputFactory) Class.forName("com.sun.xml.internal.stream.XMLInputFactoryImpl").getDeclaredConstructor().newInstance();
            } catch (Throwable t2) {
                return XMLInputFactory.newInstance();
            }
        }
    }

    public static DocumentBuilderFactory newJdkDocumentBuilderFactory() {
        try {
            Method m = DocumentBuilderFactory.class.getMethod("newDefaultInstance");
            return (DocumentBuilderFactory) m.invoke(null);
        } catch (Throwable t) {
            try {
                return (DocumentBuilderFactory) Class.forName("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl").getDeclaredConstructor().newInstance();
            } catch (Throwable t2) {
                return DocumentBuilderFactory.newInstance();
            }
        }
    }
}
