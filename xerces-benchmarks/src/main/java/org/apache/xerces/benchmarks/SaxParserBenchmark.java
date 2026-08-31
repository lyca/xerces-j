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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SaxParserBenchmark {

    @Param({"100", "1000"})
    public int itemCount;

    private byte[] xmlBytes;

    private SAXParserFactory xercesFactory;
    private SAXParserFactory woodstoxFactory;
    private SAXParserFactory aaltoFactory;
    private SAXParserFactory jdkFactory;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        xmlBytes = SampleXmlData.generateXml(itemCount);

        xercesFactory = (SAXParserFactory) Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
        xercesFactory.setNamespaceAware(true);

        woodstoxFactory = (SAXParserFactory) Class.forName("com.ctc.wstx.sax.WstxSAXParserFactory").getDeclaredConstructor().newInstance();
        woodstoxFactory.setNamespaceAware(true);

        aaltoFactory = (SAXParserFactory) Class.forName("com.fasterxml.aalto.sax.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
        aaltoFactory.setNamespaceAware(true);

        jdkFactory = SAXParserFactory.newDefaultInstance();
        jdkFactory.setNamespaceAware(true);
    }

    @Benchmark
    public void parseSaxXerces(Blackhole bh) throws Exception {
        SAXParser parser = xercesFactory.newSAXParser();
        parser.parse(new ByteArrayInputStream(xmlBytes), new ConsumingDefaultHandler(bh));
    }

    @Benchmark
    public void parseSaxWoodstox(Blackhole bh) throws Exception {
        SAXParser parser = woodstoxFactory.newSAXParser();
        parser.parse(new ByteArrayInputStream(xmlBytes), new ConsumingDefaultHandler(bh));
    }

    @Benchmark
    public void parseSaxAalto(Blackhole bh) throws Exception {
        SAXParser parser = aaltoFactory.newSAXParser();
        parser.parse(new ByteArrayInputStream(xmlBytes), new ConsumingDefaultHandler(bh));
    }

    @Benchmark
    public void parseSaxJdk(Blackhole bh) throws Exception {
        SAXParser parser = jdkFactory.newSAXParser();
        parser.parse(new ByteArrayInputStream(xmlBytes), new ConsumingDefaultHandler(bh));
    }
}
