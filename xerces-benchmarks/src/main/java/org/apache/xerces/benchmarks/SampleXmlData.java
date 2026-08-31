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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Standard synthetic XML data generator and sample resource loader for microbenchmarks.
 */
public final class SampleXmlData {

    private SampleXmlData() {}

    public static byte[] generateXml(int itemCount) {
        return XmlWorkloadGenerator.generate(WorkloadProfile.STANDARD, itemCount);
    }

    public static byte[] generate(WorkloadProfile profile, int sizeParam) {
        return XmlWorkloadGenerator.generate(profile, sizeParam);
    }

    public static byte[] loadSample(String resourceName) throws IOException {
        String path = resourceName.startsWith("/") ? resourceName : "/samples/" + resourceName;
        try (InputStream in = SampleXmlData.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Sample resource not found: " + path);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }
}
