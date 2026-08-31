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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of tests within a Schema TestSet.
 */
public class SchemaTestGroup {

    private final String name;
    private final String description;
    private final List<SchemaTestCase> testCases = new ArrayList<SchemaTestCase>();

    public SchemaTestGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void addTestCase(SchemaTestCase testCase) {
        this.testCases.add(testCase);
    }

    public List<SchemaTestCase> getTestCases() {
        return Collections.unmodifiableList(testCases);
    }
}
