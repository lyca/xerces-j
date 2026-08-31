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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class StaxParserBenchmark {

    @Param({"100", "1000"})
    public int itemCount;

    private byte[] xmlBytes;

    private XMLInputFactory xercesFactory;
    private XMLInputFactory woodstoxFactory;
    private XMLInputFactory aaltoFactory;
    private XMLInputFactory jdkFactory;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        xmlBytes = SampleXmlData.generateXml(itemCount);

        xercesFactory = (XMLInputFactory) Class.forName("org.apache.xerces.stax.XMLInputFactoryImpl").getDeclaredConstructor().newInstance();
        xercesFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

        woodstoxFactory = (XMLInputFactory) Class.forName("com.ctc.wstx.stax.WstxInputFactory").getDeclaredConstructor().newInstance();
        woodstoxFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

        aaltoFactory = (XMLInputFactory) Class.forName("com.fasterxml.aalto.stax.InputFactoryImpl").getDeclaredConstructor().newInstance();
        aaltoFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

        jdkFactory = XMLInputFactory.newDefaultFactory();
        jdkFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
    }

    private void iterateStax(XMLInputFactory factory, Blackhole bh) throws Exception {
        XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
        while (reader.hasNext()) {
            int event = reader.next();
            bh.consume(event);
            if (event == XMLStreamConstants.START_ELEMENT) {
                int attrCount = reader.getAttributeCount();
                bh.consume(attrCount);
                for (int i = 0; i < attrCount; i++) {
                    bh.consume(reader.getAttributeLocalName(i));
                    bh.consume(reader.getAttributeValue(i));
                }
            } else if (event == XMLStreamConstants.CHARACTERS) {
                bh.consume(reader.getTextLength());
            }
        }
        reader.close();
    }

    @Benchmark
    public void parseStaxXerces(Blackhole bh) throws Exception {
        iterateStax(xercesFactory, bh);
    }

    @Benchmark
    public void parseStaxWoodstox(Blackhole bh) throws Exception {
        iterateStax(woodstoxFactory, bh);
    }

    @Benchmark
    public void parseStaxAalto(Blackhole bh) throws Exception {
        iterateStax(aaltoFactory, bh);
    }

    @Benchmark
    public void parseStaxJdk(Blackhole bh) throws Exception {
        iterateStax(jdkFactory, bh);
    }
}
