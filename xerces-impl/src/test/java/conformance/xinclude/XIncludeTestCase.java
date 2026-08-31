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
package conformance.xinclude;

import java.io.File;

/**
 * Represents a single test case from the W3C XInclude Conformance Test Suite.
 */
public class XIncludeTestCase {

    public enum Type {
        SUCCESS,
        ERROR
    }

    private final String id;
    private final Type type;
    private final File testFile;
    private final File outputFile;
    private final String features;
    private final String description;
    private final String section;

    public XIncludeTestCase(String id, Type type, File testFile, File outputFile,
                             String features, String description, String section) {
        this.id = id;
        this.type = type;
        this.testFile = testFile;
        this.outputFile = outputFile;
        this.features = features;
        this.description = description;
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public File getTestFile() {
        return testFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getFeatures() {
        return features;
    }

    public String getDescription() {
        return description;
    }

    public String getSection() {
        return section;
    }

    @Override
    public String toString() {
        return "XIncludeTestCase{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", testFile=" + testFile +
                '}';
    }
}
