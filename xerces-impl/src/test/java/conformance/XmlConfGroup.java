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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container model for a test group or profile in the XML Conformance Test Suite.
 */
public class XmlConfGroup {

    private final String name;
    private final List<XmlConfGroup> subGroups = new ArrayList<XmlConfGroup>();
    private final List<XmlConfTestItem> tests = new ArrayList<XmlConfTestItem>();

    public XmlConfGroup(String name) {
        this.name = name != null ? name.trim() : "Test Group";
    }

    public String getName() {
        return name;
    }

    public void addSubGroup(XmlConfGroup group) {
        subGroups.add(group);
    }

    public void addTest(XmlConfTestItem test) {
        tests.add(test);
    }

    public List<XmlConfGroup> getSubGroups() {
        return Collections.unmodifiableList(subGroups);
    }

    public List<XmlConfTestItem> getTests() {
        return Collections.unmodifiableList(tests);
    }

    public int getTotalTestCount() {
        int count = tests.size();
        for (XmlConfGroup sub : subGroups) {
            count += sub.getTotalTestCount();
        }
        return count;
    }
}
