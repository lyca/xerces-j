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
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class DomParserBenchmark {

    private static final String FEATURE_DEFER_EXPANSION = "http://apache.org/xml/features/dom/defer-node-expansion";

    @Param({"STANDARD"})
    public WorkloadProfile profile = WorkloadProfile.STANDARD;

    @Param({"100", "1000"})
    public int itemCount = 100;

    private byte[] xmlBytes;

    private DocumentBuilderFactory xercesDeferredFactory;
    private DocumentBuilderFactory xercesEagerFactory;
    private DocumentBuilderFactory jdkDeferredFactory;
    private DocumentBuilderFactory jdkEagerFactory;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        xmlBytes = SampleXmlData.generate(profile, itemCount);

        xercesDeferredFactory = (DocumentBuilderFactory) Class.forName("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl").getDeclaredConstructor().newInstance();
        xercesDeferredFactory.setNamespaceAware(true);
        xercesDeferredFactory.setFeature(FEATURE_DEFER_EXPANSION, true);

        xercesEagerFactory = (DocumentBuilderFactory) Class.forName("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl").getDeclaredConstructor().newInstance();
        xercesEagerFactory.setNamespaceAware(true);
        xercesEagerFactory.setFeature(FEATURE_DEFER_EXPANSION, false);

        jdkDeferredFactory = DocumentBuilderFactory.newDefaultInstance();
        jdkDeferredFactory.setNamespaceAware(true);
        try {
            jdkDeferredFactory.setFeature(FEATURE_DEFER_EXPANSION, true);
        } catch (Exception ignored) {}

        jdkEagerFactory = DocumentBuilderFactory.newDefaultInstance();
        jdkEagerFactory.setNamespaceAware(true);
        try {
            jdkEagerFactory.setFeature(FEATURE_DEFER_EXPANSION, false);
        } catch (Exception ignored) {}
    }

    @Benchmark
    public void parseDomXercesDeferred(Blackhole bh) throws Exception {
        DocumentBuilder builder = xercesDeferredFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        bh.consume(doc);
    }

    @Benchmark
    public void parseDomXercesEager(Blackhole bh) throws Exception {
        DocumentBuilder builder = xercesEagerFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        bh.consume(doc);
    }

    @Benchmark
    public void parseDomJdkDeferred(Blackhole bh) throws Exception {
        DocumentBuilder builder = jdkDeferredFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        bh.consume(doc);
    }

    @Benchmark
    public void parseDomJdkEager(Blackhole bh) throws Exception {
        DocumentBuilder builder = jdkEagerFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        bh.consume(doc);
    }
}
