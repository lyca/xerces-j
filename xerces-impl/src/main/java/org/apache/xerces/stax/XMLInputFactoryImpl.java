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

package org.apache.xerces.stax;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * Xerces StAX XMLInputFactory implementation.
 */
public class XMLInputFactoryImpl extends XMLInputFactory {

    private final Map<String, Object> fProperties = new HashMap<>();
    private XMLReporter fReporter;
    private XMLResolver fResolver;
    private XMLEventAllocator fAllocator;

    public XMLInputFactoryImpl() {
        fProperties.put(IS_NAMESPACE_AWARE, Boolean.TRUE);
        fProperties.put(IS_VALIDATING, Boolean.FALSE);
        fProperties.put(IS_COALESCING, Boolean.FALSE);
        fProperties.put(IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        fProperties.put(IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
        fProperties.put(SUPPORT_DTD, Boolean.TRUE);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return new XMLStreamReaderImpl(reader);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
        return new XMLStreamReaderImpl(stream, null);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(InputStream stream, String encoding) throws XMLStreamException {
        return new XMLStreamReaderImpl(stream, encoding);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(String systemId, InputStream stream) throws XMLStreamException {
        XMLInputSource is = new XMLInputSource(null, systemId, null, stream, null);
        return new XMLStreamReaderImpl(is);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException {
        XMLInputSource is = new XMLInputSource(null, systemId, null, reader, null);
        return new XMLStreamReaderImpl(is);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
        if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource) source;
            if (ss.getReader() != null) {
                return createXMLStreamReader(ss.getSystemId(), ss.getReader());
            } else if (ss.getInputStream() != null) {
                return createXMLStreamReader(ss.getSystemId(), ss.getInputStream());
            } else if (ss.getSystemId() != null) {
                XMLInputSource is = new XMLInputSource(ss.getPublicId(), ss.getSystemId(), null);
                return new XMLStreamReaderImpl(is);
            }
        }
        throw new UnsupportedOperationException("Unsupported Source type: " + (source != null ? source.getClass().getName() : "null"));
    }

    @Override
    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(reader));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, reader));
    }

    @Override
    public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {
        throw new UnsupportedOperationException("XMLEventReader not yet implemented");
    }

    @Override
    public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(source));
    }

    @Override
    public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, InputStream stream) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, stream));
    }

    @Override
    public XMLEventReader createXMLEventReader(InputStream stream, String encoding) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream, encoding));
    }

    @Override
    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) throws XMLStreamException {
        throw new UnsupportedOperationException("createFilteredReader not yet implemented");
    }

    @Override
    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException {
        throw new UnsupportedOperationException("createFilteredReader not yet implemented");
    }

    @Override
    public XMLResolver getXMLResolver() {
        return fResolver;
    }

    @Override
    public void setXMLResolver(XMLResolver resolver) {
        fResolver = resolver;
    }

    @Override
    public XMLReporter getXMLReporter() {
        return fReporter;
    }

    @Override
    public void setXMLReporter(XMLReporter reporter) {
        fReporter = reporter;
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        fProperties.put(name, value);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return fProperties.get(name);
    }

    @Override
    public boolean isPropertySupported(String name) {
        return name != null && fProperties.containsKey(name);
    }

    @Override
    public void setEventAllocator(XMLEventAllocator allocator) {
        fAllocator = allocator;
    }

    @Override
    public XMLEventAllocator getEventAllocator() {
        return fAllocator;
    }
}
