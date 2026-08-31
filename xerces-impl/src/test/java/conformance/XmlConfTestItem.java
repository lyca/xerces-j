/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License\"); you may not use this file except in compliance with
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
package conformance;

import java.io.File;

/**
 * Metadata model for an individual XML Conformance test case.
 */
public class XmlConfTestItem {

    public enum Type {
        VALID,
        INVALID,
        NOT_WF,
        ERROR
    }

    private final String id;
    private final Type type;
    private final File testFile;
    private final String entities;
    private final String sections;
    private final String recommendation;
    private final String version;
    private final String edition;
    private final boolean namespaceAware;
    private final String output;
    private final String description;

    public XmlConfTestItem(String id, Type type, File testFile, String entities,
                           String sections, String recommendation, String version,
                           String edition, boolean namespaceAware, String output,
                           String description) {
        this.id = id;
        this.type = type;
        this.testFile = testFile;
        this.entities = entities;
        this.sections = sections;
        this.recommendation = recommendation;
        this.version = version;
        this.edition = edition;
        this.namespaceAware = namespaceAware;
        this.output = output;
        this.description = description;
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

    public String getEntities() {
        return entities;
    }

    public String getSections() {
        return sections;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getVersion() {
        return version;
    }

    public String getEdition() {
        return edition;
    }

    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    public String getOutput() {
        return output;
    }

    public String getDescription() {
        return description;
    }

    public boolean isXml11() {
        return "1.1".equals(version) || "XML1.1".equalsIgnoreCase(recommendation) || "NS1.1".equalsIgnoreCase(recommendation);
    }

    @Override
    public String toString() {
        return id + " (" + type + (recommendation != null ? ", " + recommendation : "") + ")";
    }
}
