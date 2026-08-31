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

import org.openjdk.jmh.infra.Blackhole;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX DefaultHandler that feeds parsed XML events into a JMH Blackhole
 * to prevent dead-code elimination while avoiding unnecessary allocations.
 */
public class ConsumingDefaultHandler extends DefaultHandler {

    private Blackhole blackhole;

    public ConsumingDefaultHandler() {
    }

    public ConsumingDefaultHandler(Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    public void setBlackhole(Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (blackhole != null) {
            blackhole.consume(localName != null ? localName.length() : 0);
            blackhole.consume(qName != null ? qName.length() : 0);
            int len = attributes.getLength();
            blackhole.consume(len);
            for (int i = 0; i < len; i++) {
                blackhole.consume(attributes.getLocalName(i));
                blackhole.consume(attributes.getValue(i));
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (blackhole != null) {
            blackhole.consume(localName != null ? localName.length() : 0);
            blackhole.consume(qName != null ? qName.length() : 0);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (blackhole != null) {
            blackhole.consume(length);
            if (length > 0) {
                blackhole.consume(ch[start]);
            }
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (blackhole != null) {
            blackhole.consume(length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (blackhole != null) {
            blackhole.consume(target);
            blackhole.consume(data);
        }
    }
}
