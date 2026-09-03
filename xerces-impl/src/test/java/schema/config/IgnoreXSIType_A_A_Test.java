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

package schema.config;

import org.junit.jupiter.api.Test;

import org.apache.xerces.dom.PSVIElementNSImpl;
import org.apache.xerces.xs.ItemPSVI;

/**
 * @author Peter McCracken, IBM
 * @version $Id$
 */
public class IgnoreXSIType_A_A_Test extends BaseTest {
    protected String getXMLDocument() {
        return "xsitype_A_A.xml";
    }
    
    protected String getSchemaFile() {
        return "base.xsd";
    }
    
    public IgnoreXSIType_A_A_Test() {}
    
    @Test
    public void testDefaultDocument() throws Exception {
        validateDocument();
        
        // default value of the feature is false
        checkFalseResult();
    }
    
    @Test
    public void testDefaultFragment() throws Exception {
        validateFragment();
        
        // default value of the feature is false
        checkFalseResult();
    }
    
    @Test
    public void testSetFalseDocument() throws Exception {
        fValidator.setFeature(IGNORE_XSI_TYPE, false);
        
        validateDocument();
        
        checkFalseResult();
    }
    
    @Test
    public void testSetFalseFragment() throws Exception {
        fValidator.setFeature(IGNORE_XSI_TYPE, false);
        
        validateFragment();
        
        checkFalseResult();
    }
    
    @Test
    public void testSetTrueDocument() throws Exception {
        fValidator.setFeature(IGNORE_XSI_TYPE, true);
        
        validateDocument();
        
        checkTrueResult();
    }
    
    @Test
    public void testSetTrueFragment() throws Exception {
        fValidator.setFeature(IGNORE_XSI_TYPE, true);
        
        validateFragment();
        
        checkTrueResult();
    }
    
    private void checkTrueResult() {
        checkResult();
    }
    
    private void checkFalseResult() {
        checkResult();
    }
    
    private void checkResult() {
        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("Y", fRootNode.getTypeDefinition().getName());
        assertTypeNamespaceNull(fRootNode.getTypeDefinition().getNamespace());
        
        PSVIElementNSImpl child = super.getChild(1);
        assertValidity(ItemPSVI.VALIDITY_VALID, child.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, child
                .getValidationAttempted());
        assertElementName("A", child.getElementDeclaration().getName());
        assertTypeName("Y", child.getTypeDefinition().getName());
        assertTypeNamespaceNull(child.getTypeDefinition().getNamespace());
    }
}