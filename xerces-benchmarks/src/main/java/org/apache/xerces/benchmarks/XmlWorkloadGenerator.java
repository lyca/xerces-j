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

import java.nio.charset.StandardCharsets;

/**
 * Generates synthetic XML data configured by profile and item count / depth.
 */
public final class XmlWorkloadGenerator {

    private XmlWorkloadGenerator() {}

    public static byte[] generate(WorkloadProfile profile, int sizeParam) {
        StringBuilder sb = new StringBuilder(Math.max(1024, sizeParam * 250));
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        switch (profile) {
            case DEEP_NESTING:
                int depth = Math.max(10, sizeParam);
                for (int i = 0; i < depth; i++) {
                    sb.append("<node_").append(i).append(" level=\"").append(i).append("\">\n");
                }
                sb.append("<leaf>Deepest node payload content</leaf>\n");
                for (int i = depth - 1; i >= 0; i--) {
                    sb.append("</node_").append(i).append(">\n");
                }
                break;

            case ATTRIBUTE_HEAVY:
                sb.append("<root xmlns=\"http://example.com/attrs\">\n");
                for (int i = 0; i < sizeParam; i++) {
                    sb.append("  <record id=\"rec-").append(i).append("\" ");
                    for (int a = 0; a < 20; a++) {
                        sb.append("attr_").append(a).append("=\"val_").append(i).append("_").append(a).append("\" ");
                    }
                    sb.append("status=\"ok\" enabled=\"true\" type=\"benchmark\"/>\n");
                }
                sb.append("</root>\n");
                break;

            case TEXT_HEAVY:
                sb.append("<document xmlns=\"http://example.com/text\">\n");
                for (int i = 0; i < sizeParam; i++) {
                    sb.append("  <section id=\"sec-").append(i).append("\">\n");
                    sb.append("    <title>Section Title ").append(i).append("</title>\n");
                    sb.append("    <content>\n");
                    sb.append("      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n");
                    sb.append("    </content>\n");
                    sb.append("    <![CDATA[Unparsed text content <block> with special characters & entities for section ").append(i).append("]]>\n");
                    sb.append("  </section>\n");
                }
                sb.append("</document>\n");
                break;

            case NAMESPACE_HEAVY:
                sb.append("<ns0:root xmlns:ns0=\"http://example.com/ns0\"\n");
                for (int n = 1; n <= 10; n++) {
                    sb.append("          xmlns:ns").append(n).append("=\"http://example.com/ns").append(n).append("\"\n");
                }
                sb.append(">\n");
                for (int i = 0; i < sizeParam; i++) {
                    int nsIdx = (i % 10) + 1;
                    sb.append("  <ns").append(nsIdx).append(":item id=\"item-").append(i).append("\">\n");
                    sb.append("    <ns0:name>Namespace item ").append(i).append("</ns0:name>\n");
                    sb.append("    <ns").append(nsIdx).append(":data value=\"val-").append(i).append("\"/>\n");
                    sb.append("  </ns").append(nsIdx).append(":item>\n");
                }
                sb.append("</ns0:root>\n");
                break;

            case STANDARD:
            default:
                sb.append("<catalog xmlns:ns=\"http://example.com/ns\" id=\"cat-01\">\n");
                for (int i = 0; i < sizeParam; i++) {
                    sb.append("  <ns:item id=\"item-").append(i).append("\" status=\"active\" category=\"electronic\">\n");
                    sb.append("    <ns:name>Item Number ").append(i).append("</ns:name>\n");
                    sb.append("    <ns:price currency=\"EUR\">").append(10.0 + (i % 100)).append("</ns:price>\n");
                    sb.append("    <ns:description>High performance XML parsing benchmark payload item with description index ").append(i).append(".</ns:description>\n");
                    sb.append("    <ns:specs weight=\"1.2\" height=\"10\" width=\"20\" depth=\"5\"/>\n");
                    sb.append("  </ns:item>\n");
                }
                sb.append("</catalog>\n");
                break;
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
