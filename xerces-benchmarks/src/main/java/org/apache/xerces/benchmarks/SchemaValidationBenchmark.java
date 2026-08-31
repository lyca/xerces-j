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

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SchemaValidationBenchmark {

    private static final String SCHEMA_XSD =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
        "           targetNamespace=\"http://example.com/catalog\"\n" +
        "           xmlns=\"http://example.com/catalog\"\n" +
        "           elementFormDefault=\"qualified\">\n" +
        "  <xs:element name=\"catalog\">\n" +
        "    <xs:complexType>\n" +
        "      <xs:sequence>\n" +
        "        <xs:element name=\"item\" maxOccurs=\"unbounded\">\n" +
        "          <xs:complexType>\n" +
        "            <xs:sequence>\n" +
        "              <xs:element name=\"name\" type=\"xs:string\"/>\n" +
        "              <xs:element name=\"price\">\n" +
        "                <xs:complexType>\n" +
        "                  <xs:simpleContent>\n" +
        "                    <xs:extension base=\"xs:decimal\">\n" +
        "                      <xs:attribute name=\"currency\" type=\"xs:string\" use=\"required\"/>\n" +
        "                    </xs:extension>\n" +
        "                  </xs:simpleContent>\n" +
        "                </xs:complexType>\n" +
        "              </xs:element>\n" +
        "              <xs:element name=\"description\" type=\"xs:string\"/>\n" +
        "            </xs:sequence>\n" +
        "            <xs:attribute name=\"id\" type=\"xs:string\" use=\"required\"/>\n" +
        "            <xs:attribute name=\"status\" type=\"xs:string\" use=\"required\"/>\n" +
        "            <xs:attribute name=\"category\" type=\"xs:string\" use=\"optional\"/>\n" +
        "          </xs:complexType>\n" +
        "        </xs:element>\n" +
        "      </xs:sequence>\n" +
        "      <xs:attribute name=\"id\" type=\"xs:string\" use=\"required\"/>\n" +
        "    </xs:complexType>\n" +
        "  </xs:element>\n" +
        "</xs:schema>";

    @Param({"50", "200"})
    public int itemCount;

    private byte[] xsdBytes;
    private byte[] xmlBytes;

    private SchemaFactory xercesSchemaFactory;
    private SchemaFactory jdkSchemaFactory;

    private Schema xercesPrecompiledSchema;
    private Schema jdkPrecompiledSchema;

    private SAXParserFactory xercesGrammarPoolSaxFactory;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        xsdBytes = SCHEMA_XSD.getBytes(StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder(itemCount * 300 + 500);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<catalog xmlns=\"http://example.com/catalog\" id=\"cat-01\">\n");
        for (int i = 0; i < itemCount; i++) {
            sb.append("  <item id=\"item-").append(i).append("\" status=\"active\" category=\"electronic\">\n");
            sb.append("    <name>Item Number ").append(i).append("</name>\n");
            sb.append("    <price currency=\"EUR\">").append(10.0 + (i % 100)).append("</price>\n");
            sb.append("    <description>Validation benchmark item ").append(i).append(".</description>\n");
            sb.append("  </item>\n");
        }
        sb.append("</catalog>\n");
        xmlBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        xercesSchemaFactory = (SchemaFactory) Class.forName("org.apache.xerces.jaxp.validation.XMLSchemaFactory").getDeclaredConstructor().newInstance();
        jdkSchemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        xercesPrecompiledSchema = xercesSchemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(xsdBytes)));
        jdkPrecompiledSchema = jdkSchemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(xsdBytes)));

        // Configure SAX parser with XMLGrammarPool
        xercesGrammarPoolSaxFactory = (SAXParserFactory) Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl").getDeclaredConstructor().newInstance();
        xercesGrammarPoolSaxFactory.setNamespaceAware(true);
        xercesGrammarPoolSaxFactory.setValidating(true);
        xercesGrammarPoolSaxFactory.setFeature("http://apache.org/xml/features/validation/schema", true);
    }

    @Benchmark
    public void validatePrecompiledXerces(Blackhole bh) throws Exception {
        Validator validator = xercesPrecompiledSchema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
        bh.consume(validator);
    }

    @Benchmark
    public void validatePrecompiledJdk(Blackhole bh) throws Exception {
        Validator validator = jdkPrecompiledSchema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
        bh.consume(validator);
    }

    @Benchmark
    public void compileSchemaXerces(Blackhole bh) throws Exception {
        Schema schema = xercesSchemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(xsdBytes)));
        bh.consume(schema);
    }

    @Benchmark
    public void compileSchemaJdk(Blackhole bh) throws Exception {
        Schema schema = jdkSchemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(xsdBytes)));
        bh.consume(schema);
    }
}
