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
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.xerces.parsers.NonValidatingConfiguration;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDTDContentModelHandler;
import org.apache.xerces.xni.XMLDTDHandler;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLPullParserConfiguration;

/**
 * High-performance StAX XMLStreamReader implementation backed by Xerces XNI pull parsing.
 */
public class XMLStreamReaderImpl implements XMLStreamReader, XMLDocumentHandler, XMLDTDHandler, XMLDTDContentModelHandler {

    private final XMLPullParserConfiguration fConfiguration;
    private int fEventType = START_DOCUMENT;
    private boolean fHasEvent = false;
    private boolean fClosed = false;

    // Locator
    private XMLLocator fLocator;

    // Namespaces and symbol table
    private org.apache.xerces.xni.NamespaceContext fXniNamespaceContext;
    private final NamespaceContext fNamespaceContextWrapper;

    // Document information
    private String fVersion = "1.0";
    private String fEncodingScheme;
    private boolean fStandalone = false;
    private boolean fStandaloneSet = false;

    // Current element
    private final org.apache.xerces.xni.QName fCurrentElement = new org.apache.xerces.xni.QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();

    // Text buffer
    private final XMLStringBuffer fTextBuffer = new XMLStringBuffer();
    private String fTextString;

    // PI / Entity info
    private String fPITarget;
    private String fEntityName;
    private String fDTDName;
    private String fDTDPublicId;
    private String fDTDSystemId;

    // Coalescing and configuration flags
    private boolean fCoalescing = false;

    public XMLStreamReaderImpl(InputStream inputStream, String encoding) throws XMLStreamException {
        this(new XMLInputSource(null, null, null, inputStream, encoding));
    }

    public XMLStreamReaderImpl(Reader reader) throws XMLStreamException {
        this(new XMLInputSource(null, null, null, reader, null));
    }

    public XMLStreamReaderImpl(XMLInputSource inputSource) throws XMLStreamException {
        this(inputSource, new NonValidatingConfiguration());
    }

    public XMLStreamReaderImpl(XMLInputSource inputSource, XMLPullParserConfiguration config) throws XMLStreamException {
        fConfiguration = config;
        fConfiguration.setDocumentHandler(this);
        fConfiguration.setDTDHandler(this);
        fConfiguration.setDTDContentModelHandler(this);

        fNamespaceContextWrapper = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("Prefix cannot be null.");
                }
                if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
                    return XMLConstants.XML_NS_URI;
                }
                if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
                    return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                }
                if (fXniNamespaceContext != null) {
                    String uri = fXniNamespaceContext.getURI(prefix);
                    return uri != null ? uri : XMLConstants.NULL_NS_URI;
                }
                return XMLConstants.NULL_NS_URI;
            }

            public String getPrefix(String namespaceURI) {
                if (namespaceURI == null) {
                    throw new IllegalArgumentException("Namespace URI cannot be null.");
                }
                if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XML_NS_PREFIX;
                }
                if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XMLNS_ATTRIBUTE;
                }
                if (fXniNamespaceContext != null) {
                    return fXniNamespaceContext.getPrefix(namespaceURI);
                }
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                if (namespaceURI == null) {
                    throw new IllegalArgumentException("Namespace URI cannot be null.");
                }
                String prefix = getPrefix(namespaceURI);
                if (prefix == null) {
                    return Collections.emptyIterator();
                }
                return Collections.singleton(prefix).iterator();
            }
        };

        try {
            fConfiguration.setInputSource(inputSource);
            // Advance to parse XMLDecl and start document metadata
            fConfiguration.parse(false);
            fEventType = START_DOCUMENT;
        } catch (Exception e) {
            throw new XMLStreamException(e.getMessage(), e);
        }
    }

    public void setCoalescing(boolean coalescing) {
        fCoalescing = coalescing;
    }

    @Override
    public int next() throws XMLStreamException {
        if (!hasNext()) {
            throw new NoSuchElementException("End of XML stream has been reached.");
        }
        if (fEventType == END_DOCUMENT) {
            throw new NoSuchElementException("End of document reached.");
        }

        fHasEvent = false;
        fTextString = null;
        fTextBuffer.clear();

        try {
            while (!fHasEvent) {
                if (!fConfiguration.parse(false)) {
                    if (!fHasEvent) {
                        fEventType = END_DOCUMENT;
                        fHasEvent = true;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new XMLStreamException(e.getMessage(), getLocation(), e);
        }

        return fEventType;
    }

    @Override
    public int getEventType() {
        return fEventType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return fEventType != END_DOCUMENT && !fClosed;
    }

    @Override
    public void close() throws XMLStreamException {
        if (!fClosed) {
            fClosed = true;
            fConfiguration.cleanup();
        }
    }

    @Override
    public boolean isStartElement() {
        return fEventType == START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
        return fEventType == END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
        return fEventType == CHARACTERS || fEventType == CDATA || fEventType == SPACE;
    }

    @Override
    public boolean isWhiteSpace() {
        if (fEventType == SPACE) {
            return true;
        }
        if (fEventType == CHARACTERS) {
            char[] ch = fTextBuffer.ch;
            int offset = fTextBuffer.offset;
            int len = fTextBuffer.length;
            for (int i = 0; i < len; i++) {
                if (!XMLChar.isSpace(ch[offset + i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        if (type != fEventType) {
            throw new XMLStreamException("Expected event type " + type + ", got " + fEventType, getLocation());
        }
        if (localName != null && !localName.equals(getLocalName())) {
            throw new XMLStreamException("Expected local name " + localName + ", got " + getLocalName(), getLocation());
        }
        if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) {
            throw new XMLStreamException("Expected namespace URI " + namespaceURI + ", got " + getNamespaceURI(), getLocation());
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (fEventType != START_ELEMENT) {
            throw new XMLStreamException("getElementText() only valid on START_ELEMENT", getLocation());
        }
        StringBuilder buf = new StringBuilder();
        int event = next();
        while (event != END_ELEMENT) {
            if (event == CHARACTERS || event == CDATA || event == SPACE || event == ENTITY_REFERENCE) {
                buf.append(getText());
            } else if (event == PROCESSING_INSTRUCTION || event == COMMENT) {
                // skip
            } else if (event == START_ELEMENT) {
                throw new XMLStreamException("Unexpected nested START_ELEMENT in getElementText()", getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type " + event + " in getElementText()", getLocation());
            }
            event = next();
        }
        return buf.toString();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int event = next();
        while ((event == CHARACTERS && isWhiteSpace()) || event == CDATA || event == SPACE ||
               event == PROCESSING_INSTRUCTION || event == COMMENT) {
            event = next();
        }
        if (event != START_ELEMENT && event != END_ELEMENT) {
            throw new XMLStreamException("Expected START_ELEMENT or END_ELEMENT, got " + event, getLocation());
        }
        return event;
    }

    @Override
    public QName getName() {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getName() only valid on START_ELEMENT or END_ELEMENT");
        }
        String uri = fCurrentElement.uri != null ? fCurrentElement.uri : XMLConstants.NULL_NS_URI;
        String prefix = fCurrentElement.prefix != null ? fCurrentElement.prefix : XMLConstants.DEFAULT_NS_PREFIX;
        return new QName(uri, fCurrentElement.localpart, prefix);
    }

    @Override
    public String getLocalName() {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getLocalName() not valid on event type " + fEventType);
        }
        if (fEventType == ENTITY_REFERENCE) {
            return fEntityName;
        }
        return fCurrentElement.localpart;
    }

    @Override
    public boolean hasName() {
        return fEventType == START_ELEMENT || fEventType == END_ELEMENT;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        if (fXniNamespaceContext != null) {
            return fXniNamespaceContext.getURI(prefix);
        }
        return null;
    }

    @Override
    public String getNamespaceURI() {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getNamespaceURI() only valid on START_ELEMENT or END_ELEMENT");
        }
        return fCurrentElement.uri;
    }

    @Override
    public String getPrefix() {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getPrefix() only valid on START_ELEMENT or END_ELEMENT");
        }
        return fCurrentElement.prefix;
    }

    @Override
    public int getAttributeCount() {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeCount() only valid on START_ELEMENT");
        }
        return fAttributes.getLength();
    }

    @Override
    public QName getAttributeName(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeName() only valid on START_ELEMENT");
        }
        String uri = fAttributes.getURI(index);
        String localName = fAttributes.getLocalName(index);
        String prefix = fAttributes.getPrefix(index);
        return new QName(uri != null ? uri : XMLConstants.NULL_NS_URI, localName, prefix != null ? prefix : XMLConstants.DEFAULT_NS_PREFIX);
    }

    @Override
    public String getAttributeNamespace(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeNamespace() only valid on START_ELEMENT");
        }
        return fAttributes.getURI(index);
    }

    @Override
    public String getAttributeLocalName(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeLocalName() only valid on START_ELEMENT");
        }
        return fAttributes.getLocalName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributePrefix() only valid on START_ELEMENT");
        }
        return fAttributes.getPrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeType() only valid on START_ELEMENT");
        }
        return fAttributes.getType(index);
    }

    @Override
    public String getAttributeValue(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeValue() only valid on START_ELEMENT");
        }
        return fAttributes.getValue(index);
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("getAttributeValue() only valid on START_ELEMENT");
        }
        if (namespaceURI == null || namespaceURI.isEmpty()) {
            return fAttributes.getValue(localName);
        }
        return fAttributes.getValue(namespaceURI, localName);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        if (fEventType != START_ELEMENT) {
            throw new IllegalStateException("isAttributeSpecified() only valid on START_ELEMENT");
        }
        return fAttributes.isSpecified(index);
    }

    @Override
    public int getNamespaceCount() {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getNamespaceCount() only valid on START_ELEMENT or END_ELEMENT");
        }
        return fXniNamespaceContext != null ? fXniNamespaceContext.getDeclaredPrefixCount() : 0;
    }

    @Override
    public String getNamespacePrefix(int index) {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getNamespacePrefix() only valid on START_ELEMENT or END_ELEMENT");
        }
        if (fXniNamespaceContext != null) {
            String prefix = fXniNamespaceContext.getDeclaredPrefixAt(index);
            return (prefix != null && !prefix.isEmpty()) ? prefix : null;
        }
        return null;
    }

    @Override
    public String getNamespaceURI(int index) {
        if (fEventType != START_ELEMENT && fEventType != END_ELEMENT) {
            throw new IllegalStateException("getNamespaceURI() only valid on START_ELEMENT or END_ELEMENT");
        }
        if (fXniNamespaceContext != null) {
            String prefix = fXniNamespaceContext.getDeclaredPrefixAt(index);
            return fXniNamespaceContext.getURI(prefix != null ? prefix : "");
        }
        return null;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return fNamespaceContextWrapper;
    }

    @Override
    public String getText() {
        if (fEventType != CHARACTERS && fEventType != CDATA && fEventType != SPACE &&
            fEventType != COMMENT && fEventType != DTD && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getText() not valid on event type " + fEventType);
        }
        if (fTextString == null) {
            fTextString = fTextBuffer.toString();
        }
        return fTextString;
    }

    @Override
    public char[] getTextCharacters() {
        if (fEventType != CHARACTERS && fEventType != CDATA && fEventType != SPACE &&
            fEventType != COMMENT && fEventType != DTD && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getTextCharacters() not valid on event type " + fEventType);
        }
        return fTextBuffer.ch;
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        if (fEventType != CHARACTERS && fEventType != CDATA && fEventType != SPACE &&
            fEventType != COMMENT && fEventType != DTD && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getTextCharacters() not valid on event type " + fEventType);
        }
        if (target == null) {
            throw new NullPointerException("target array cannot be null");
        }
        if (sourceStart < 0 || sourceStart > fTextBuffer.length) {
            throw new IndexOutOfBoundsException("sourceStart out of bounds: " + sourceStart);
        }
        int count = Math.min(length, fTextBuffer.length - sourceStart);
        if (count > 0) {
            System.arraycopy(fTextBuffer.ch, fTextBuffer.offset + sourceStart, target, targetStart, count);
        }
        return count;
    }

    @Override
    public int getTextStart() {
        if (fEventType != CHARACTERS && fEventType != CDATA && fEventType != SPACE &&
            fEventType != COMMENT && fEventType != DTD && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getTextStart() not valid on event type " + fEventType);
        }
        return fTextBuffer.offset;
    }

    @Override
    public int getTextLength() {
        if (fEventType != CHARACTERS && fEventType != CDATA && fEventType != SPACE &&
            fEventType != COMMENT && fEventType != DTD && fEventType != ENTITY_REFERENCE) {
            throw new IllegalStateException("getTextLength() not valid on event type " + fEventType);
        }
        return fTextBuffer.length;
    }

    @Override
    public boolean hasText() {
        return fEventType == CHARACTERS || fEventType == CDATA || fEventType == SPACE ||
               fEventType == COMMENT || fEventType == DTD || fEventType == ENTITY_REFERENCE;
    }

    @Override
    public Location getLocation() {
        if (fLocator != null) {
            return new ImmutableLocation(
                fLocator.getCharacterOffset(),
                fLocator.getColumnNumber(),
                fLocator.getLineNumber(),
                fLocator.getPublicId(),
                fLocator.getExpandedSystemId()
            );
        }
        return EmptyLocation.getInstance();
    }

    @Override
    public String getPITarget() {
        if (fEventType != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("getPITarget() only valid on PROCESSING_INSTRUCTION");
        }
        return fPITarget;
    }

    @Override
    public String getPIData() {
        if (fEventType != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("getPIData() only valid on PROCESSING_INSTRUCTION");
        }
        return getText();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return fEncodingScheme;
    }

    @Override
    public String getEncoding() {
        return fEncodingScheme;
    }

    @Override
    public String getVersion() {
        return fVersion;
    }

    @Override
    public boolean isStandalone() {
        return fStandalone;
    }

    @Override
    public boolean standaloneSet() {
        return fStandaloneSet;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return null;
    }

    //
    // XMLDocumentHandler callbacks
    //

    @Override
    public void startDocument(XMLLocator locator, String encoding, org.apache.xerces.xni.NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        fLocator = locator;
        fEncodingScheme = encoding;
        fXniNamespaceContext = namespaceContext;
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        if (version != null) fVersion = version;
        if (encoding != null) fEncodingScheme = encoding;
        if (standalone != null) {
            fStandalone = "yes".equals(standalone);
            fStandaloneSet = true;
        }
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
        fDTDName = rootElement;
        fDTDPublicId = publicId;
        fDTDSystemId = systemId;
        fEventType = DTD;
        fHasEvent = true;
    }

    @Override
    public void startElement(org.apache.xerces.xni.QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        fCurrentElement.setValues(element);
        fAttributes.removeAllAttributes();
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            String prefix = attributes.getPrefix(i);
            String rawname = attributes.getQName(i);
            // In StAX, namespace declarations are not exposed as attributes
            if ("xmlns".equals(prefix) || "xmlns".equals(rawname) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attributes.getURI(i))) {
                continue;
            }
            org.apache.xerces.xni.QName attrName = new org.apache.xerces.xni.QName();
            attributes.getName(i, attrName);
            fAttributes.addAttribute(
                attrName,
                attributes.getType(i),
                attributes.getValue(i)
            );
            fAttributes.setSpecified(fAttributes.getLength() - 1, attributes.isSpecified(i));
        }
        fEventType = START_ELEMENT;
        fHasEvent = true;
    }

    @Override
    public void emptyElement(org.apache.xerces.xni.QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (fEventType == CHARACTERS && fHasEvent && fCoalescing) {
            fTextBuffer.append(text);
        } else {
            fTextBuffer.clear();
            fTextBuffer.append(text);
            fEventType = CHARACTERS;
            fHasEvent = true;
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        fTextBuffer.clear();
        fTextBuffer.append(text);
        fEventType = SPACE;
        fHasEvent = true;
    }

    @Override
    public void endElement(org.apache.xerces.xni.QName element, Augmentations augs) throws XNIException {
        fCurrentElement.setValues(element);
        fEventType = END_ELEMENT;
        fHasEvent = true;
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        // CDATA start
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        // CDATA end
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        fTextBuffer.clear();
        fTextBuffer.append(text);
        fEventType = COMMENT;
        fHasEvent = true;
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        fPITarget = target;
        fTextBuffer.clear();
        fTextBuffer.append(data);
        fEventType = PROCESSING_INSTRUCTION;
        fHasEvent = true;
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        fEventType = END_DOCUMENT;
        fHasEvent = true;
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        fEntityName = name;
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {}

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {}

    @Override
    public void setDocumentSource(org.apache.xerces.xni.parser.XMLDocumentSource source) {}

    @Override
    public org.apache.xerces.xni.parser.XMLDocumentSource getDocumentSource() {
        return null;
    }

    //
    // XMLDTDHandler & XMLDTDContentModelHandler empty callbacks
    //

    @Override
    public void startDTD(XMLLocator locator, Augmentations augmentations) throws XNIException {}
    @Override
    public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augmentations) throws XNIException {}
    @Override
    public void endParameterEntity(String name, Augmentations augmentations) throws XNIException {}
    @Override
    public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {}
    @Override
    public void endExternalSubset(Augmentations augmentations) throws XNIException {}
    @Override
    public void elementDecl(String name, String model, Augmentations augmentations) throws XNIException {}
    @Override
    public void startAttlist(String elementName, Augmentations augmentations) throws XNIException {}
    @Override
    public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augmentations) throws XNIException {}
    @Override
    public void endAttlist(Augmentations augmentations) throws XNIException {}
    @Override
    public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augmentations) throws XNIException {}
    @Override
    public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {}
    @Override
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augmentations) throws XNIException {}
    @Override
    public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {}
    @Override
    public void startConditional(short type, Augmentations augmentations) throws XNIException {}
    @Override
    public void ignoredCharacters(XMLString text, Augmentations augmentations) throws XNIException {}
    @Override
    public void endConditional(Augmentations augmentations) throws XNIException {}
    @Override
    public void endDTD(Augmentations augmentations) throws XNIException {}
    @Override
    public void setDTDSource(org.apache.xerces.xni.parser.XMLDTDSource source) {}
    @Override
    public org.apache.xerces.xni.parser.XMLDTDSource getDTDSource() { return null; }
    @Override
    public void startContentModel(String elementName, Augmentations augmentations) throws XNIException {}
    @Override
    public void any(Augmentations augmentations) throws XNIException {}
    @Override
    public void empty(Augmentations augmentations) throws XNIException {}
    @Override
    public void startGroup(Augmentations augmentations) throws XNIException {}
    @Override
    public void pcdata(Augmentations augmentations) throws XNIException {}
    @Override
    public void element(String elementName, Augmentations augmentations) throws XNIException {}
    @Override
    public void separator(short separator, Augmentations augmentations) throws XNIException {}
    @Override
    public void occurrence(short occurrence, Augmentations augmentations) throws XNIException {}
    @Override
    public void endGroup(Augmentations augmentations) throws XNIException {}
    @Override
    public void endContentModel(Augmentations augmentations) throws XNIException {}
    @Override
    public void setDTDContentModelSource(org.apache.xerces.xni.parser.XMLDTDContentModelSource source) {}
    @Override
    public org.apache.xerces.xni.parser.XMLDTDContentModelSource getDTDContentModelSource() { return null; }
}
