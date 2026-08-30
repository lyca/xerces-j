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

package util;

import java.util.Iterator;
import java.util.Map;

import org.apache.xerces.util.AugmentationsImpl;
import org.apache.xerces.util.HTTPInputSource;
import org.apache.xerces.xni.Augmentations;
import org.apache.xml.serialize.HTMLdtd;
import org.junit.Assert;
import org.junit.Test;

/**
 * Regression unit tests verifying Java 8 modernized components (lambdas, try-with-resources, generics).
 */
public class Java8ModernizationTest {

    @Test
    public void testHTMLdtdInitializationAndEntityLookup() {
        // Verify entity character code and character lookup
        int ampCode = HTMLdtd.charFromName("amp");
        Assert.assertEquals('&', (char) ampCode);

        String ampName = HTMLdtd.fromChar('&');
        Assert.assertEquals("amp", ampName);

        Assert.assertTrue(HTMLdtd.isEmptyTag("BR") || HTMLdtd.isEmptyTag("br"));
        Assert.assertTrue(HTMLdtd.isElementContent("HEAD") || HTMLdtd.isElementContent("head"));
    }

    @Test
    public void testAugmentationsImplIteration() {
        Augmentations aug = new AugmentationsImpl();
        aug.putItem("key1", "val1");
        aug.putItem("key2", "val2");

        Assert.assertEquals("val1", aug.getItem("key1"));
        Assert.assertEquals("val2", aug.getItem("key2"));

        String str = aug.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("key1") || str.contains("SmallContainer"));

        // Put more than small container size limit to trigger LargeContainer
        for (int i = 0; i < 20; i++) {
            aug.putItem("extraKey" + i, "extraVal" + i);
        }

        String largeStr = aug.toString();
        Assert.assertNotNull(largeStr);
        Assert.assertTrue(largeStr.contains("LargeContainer"));
        Assert.assertTrue(largeStr.contains("extraKey10"));
    }

    @Test
    public void testHTTPInputSourcePropertiesIteration() {
        HTTPInputSource source = new HTTPInputSource("publicId", "systemId", "baseSystemId");
        source.setHTTPRequestProperty("Accept", "text/xml");
        source.setHTTPRequestProperty("User-Agent", "Xerces-Test");

        Assert.assertEquals("text/xml", source.getHTTPRequestProperty("Accept"));
        Assert.assertEquals("Xerces-Test", source.getHTTPRequestProperty("User-Agent"));

        Iterator<Map.Entry<String, String>> it = source.getHTTPRequestProperties();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            Assert.assertNotNull(entry.getKey());
            Assert.assertNotNull(entry.getValue());
            count++;
        }
        Assert.assertEquals(2, count);
    }
}
