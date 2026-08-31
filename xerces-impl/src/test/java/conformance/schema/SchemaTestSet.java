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
 * Represents a complete XML Schema TestSet.
 */
public class SchemaTestSet {

    private final String contributor;
    private final String name;
    private final List<SchemaTestGroup> groups = new ArrayList<SchemaTestGroup>();

    public SchemaTestSet(String contributor, String name) {
        this.contributor = contributor;
        this.name = name;
    }

    public String getContributor() {
        return contributor;
    }

    public String getName() {
        return name;
    }

    public void addGroup(SchemaTestGroup group) {
        this.groups.add(group);
    }

    public List<SchemaTestGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public int getTotalTestCount() {
        int count = 0;
        for (SchemaTestGroup group : groups) {
            count += group.getTestCases().size();
        }
        return count;
    }
}
