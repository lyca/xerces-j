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

package schema.annotations;

import java.io.File;
import java.net.URL;

/**
 * @author Neil Delima, IBM
 * @version $Id$
 */
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

public class TestCase {

    public TestCase() {
    }

    public TestCase(String test) {}

    
    public void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }
    public void assertTrue(String message, boolean condition) {
        Assertions.assertTrue(condition, message);
    }
    public void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }
    public void assertFalse(String message, boolean condition) {
        Assertions.assertFalse(condition, message);
    }
    public void assertNull(Object object) {
        Assertions.assertNull(object);
    }
    public void assertNull(String message, Object object) {
        Assertions.assertNull(object, message);
    }
    public void assertNotNull(Object object) {
        Assertions.assertNotNull(object);
    }
    public void assertNotNull(String message, Object object) {
        Assertions.assertNotNull(object, message);
    }
    public void assertEquals(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual);
    }
    public void assertEquals(String message, Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, message);
    }
    public void assertEquals(int expected, int actual) {
        Assertions.assertEquals(expected, actual);
    }
    public void assertEquals(String message, int expected, int actual) {
        Assertions.assertEquals(expected, actual, message);
    }
    public void assertSame(Object expected, Object actual) {
        Assertions.assertSame(expected, actual);
    }
    public void assertSame(String message, Object expected, Object actual) {
        Assertions.assertSame(expected, actual, message);
    }
    public void fail(String message) {
        Assertions.fail(message);
    }

    protected String getResourceURL(String path) {
        // build the location URL of the document
        String packageDir = this.getClass().getPackage().getName().replace('.',
                File.separatorChar);
        String documentPath = packageDir + "/" + path;
        URL url = this.getClass().getClassLoader().getResource(documentPath);
        if (url == null) {
            String message = "Couldn't find xml file for test: " + documentPath;
            fail (message);
        }
        return url.toExternalForm();
    }

    protected String trim(String toTrim) {
        String replaced = toTrim.replace('\t', ' ');
        replaced =  replaced.replace('\n', ' ');
        replaced =  replaced.trim();

        int i = 0, j = 0;
        char[] src = replaced.toCharArray();
        char[] dest = new char[src.length]; 
        
        while (i < src.length) {
            if (src [i] != ' ' ) {
                dest[j] = src [i];
                j++;
            } 
            i++;
        }
        return String.copyValueOf(dest,0,j-1);
    }

}
