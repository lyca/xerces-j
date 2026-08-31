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
package conformance.schema;

import java.io.File;

/**
 * Metadata model for an XML Schema test item (either a Schema test or an Instance test).
 */
public class SchemaTestCase {

    public enum Kind {
        SCHEMA,
        INSTANCE
    }

    public enum Validity {
        VALID,
        INVALID,
        NOT_KNOWN
    }

    private final String groupName;
    private final String testName;
    private final Kind kind;
    private final File documentFile;
    private final File schemaFile;
    private final Validity expectedValidity;
    private final String description;

    public SchemaTestCase(String groupName, String testName, Kind kind, File documentFile,
                          File schemaFile, Validity expectedValidity, String description) {
        this.groupName = groupName;
        this.testName = testName;
        this.kind = kind;
        this.documentFile = documentFile;
        this.schemaFile = schemaFile;
        this.expectedValidity = expectedValidity;
        this.description = description;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getTestName() {
        return testName;
    }

    public Kind getKind() {
        return kind;
    }

    public File getDocumentFile() {
        return documentFile;
    }

    public File getSchemaFile() {
        return schemaFile;
    }

    public Validity getExpectedValidity() {
        return expectedValidity;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "SchemaTestCase{" +
                "name='" + testName + '\'' +
                ", kind=" + kind +
                ", expectedValidity=" + expectedValidity +
                ", documentFile=" + documentFile +
                '}';
    }
}
