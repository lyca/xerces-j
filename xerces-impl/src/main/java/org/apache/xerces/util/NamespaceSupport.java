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

package org.apache.xerces.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.apache.xerces.xni.NamespaceContext;

/**
 * Namespace support for XML document handlers. This class doesn't 
 * perform any error checking and assumes that all strings passed
 * as arguments to methods are unique symbols. The SymbolTable class
 * can be used for this purpose.
 *
 * @author Andy Clark, IBM
 *
 * @version $Id$
 */
public class NamespaceSupport implements NamespaceContext {

    //
    // Data
    //

    /** 
     * Namespace binding information. This array is composed of a
     * series of tuples containing the namespace binding information:
     * &lt;prefix, uri&gt;. The default size can be set to anything
     * as long as it is a power of 2 greater than 1.
     *
     * @see #fNamespaceSize
     * @see #fContext
     */
    protected String[] fNamespace = new String[16 * 2];

    /** The top of the namespace information array. */
    protected int fNamespaceSize;

    // NOTE: The constructor depends on the initial context size 
    //       being at least 1. -Ac

    /** 
     * Context indexes. This array contains indexes into the namespace
     * information array. The index at the current context is the start
     * index of declared namespace bindings and runs to the size of the
     * namespace information array.
     *
     * @see #fNamespaceSize
     */
    protected int[] fContext = new int[8];

    /** The current context. */
    protected int fCurrentContext;
    
    protected String[] fPrefixes = new String[16];
    
    //
    // Constructors
    //

    /** Default constructor. */
    public NamespaceSupport() {
    } // <init>()

    /** 
     * Constructs a namespace context object and initializes it with
     * the prefixes declared in the specified context.
     */
    public NamespaceSupport(NamespaceContext context) {
        pushContext();
        // copy declaration in the context
        Enumeration<String> prefixes = context.getAllPrefixes();
        while (prefixes.hasMoreElements()){
            String prefix = (String)prefixes.nextElement();
            String uri = context.getURI(prefix);
            declarePrefix(prefix, uri);
        }
      } // <init>(NamespaceContext)


    //
    // Public methods
    //
    
	/**
	 * @see org.apache.xerces.xni.NamespaceContext#reset()
	 */
    @Override
    public void reset() {

        // reset namespace and context info
        fNamespaceSize = 0;
        fCurrentContext = 0;
        fContext[fCurrentContext] = fNamespaceSize;

        // bind "xml" prefix to the XML uri
        fNamespace[fNamespaceSize++] = XMLSymbols.PREFIX_XML;
        fNamespace[fNamespaceSize++] = NamespaceContext.XML_URI;
        // bind "xmlns" prefix to the XMLNS uri
        fNamespace[fNamespaceSize++] = XMLSymbols.PREFIX_XMLNS;
        fNamespace[fNamespaceSize++] = NamespaceContext.XMLNS_URI;
        ++fCurrentContext;

    } // reset(SymbolTable)


	/**
	 * @see org.apache.xerces.xni.NamespaceContext#pushContext()
	 */
    @Override
    public void pushContext() {

        // extend the array, if necessary
        if (fCurrentContext + 1 == fContext.length) {
            int[] contextarray = new int[fContext.length * 2];
            System.arraycopy(fContext, 0, contextarray, 0, fContext.length);
            fContext = contextarray;
        }

        // push context
        fContext[++fCurrentContext] = fNamespaceSize;

    } // pushContext()


	/**
	 * @see org.apache.xerces.xni.NamespaceContext#popContext()
	 */
    @Override
    public void popContext() {
        fNamespaceSize = fContext[fCurrentContext--];
    } // popContext()

	/**
	 * @see org.apache.xerces.xni.NamespaceContext#declarePrefix(String, String)
	 */
    @Override
    public boolean declarePrefix(String prefix, String uri) {
        // ignore "xml" and "xmlns" prefixes
        if (prefix == XMLSymbols.PREFIX_XML || prefix == XMLSymbols.PREFIX_XMLNS) {
            return false;
        }

        final int currentContext = fContext[fCurrentContext];
        final String[] namespace = fNamespace;
        // see if prefix already exists in current context
        for (int i = fNamespaceSize; i > currentContext; i -= 2) {
            if (namespace[i - 2] == prefix) {
                // REVISIT: [Q] Should the new binding override the
                //          previously declared binding or should it
                //          it be ignored? -Ac
                // NOTE:    The SAX2 "NamespaceSupport" helper allows
                //          re-bindings with the new binding overwriting
                //          the previous binding. -Ac
                namespace[i - 1] = uri;
                return true;
            }
        }

        // resize array, if needed
        if (fNamespaceSize == namespace.length) {
            String[] namespacearray = new String[fNamespaceSize * 2];
            System.arraycopy(namespace, 0, namespacearray, 0, fNamespaceSize);
            fNamespace = namespacearray;
        }

        // bind prefix to uri in current context
        fNamespace[fNamespaceSize++] = prefix;
        fNamespace[fNamespaceSize++] = uri;

        return true;

    } // declarePrefix(String,String):boolean

	/**
	 * @see org.apache.xerces.xni.NamespaceContext#getURI(String)
	 */
    @Override
    public String getURI(String prefix) {
        final int size = fNamespaceSize;
        if (size == 4) {
            if (prefix == XMLSymbols.PREFIX_XML) {
                return NamespaceContext.XML_URI;
            }
            if (prefix == XMLSymbols.PREFIX_XMLNS) {
                return NamespaceContext.XMLNS_URI;
            }
            return null;
        }

        final String[] namespace = fNamespace;
        // find prefix in current context
        for (int i = size; i > 0; i -= 2) {
            if (namespace[i - 2] == prefix) {
                return namespace[i - 1];
            }
        }

        // prefix not found
        return null;

    } // getURI(String):String


	/**
	 * @see org.apache.xerces.xni.NamespaceContext#getPrefix(String)
	 */
    @Override
    public String getPrefix(String uri) {
        final int size = fNamespaceSize;
        final String[] namespace = fNamespace;
        // find uri in current context
        for (int i = size; i > 0; i -= 2) {
            if (namespace[i - 1] == uri) {
                if (getURI(namespace[i - 2]) == uri)
                    return namespace[i - 2];
            }
        }

        // uri not found
        return null;

    } // getPrefix(String):String


	/**
	 * @see org.apache.xerces.xni.NamespaceContext#getDeclaredPrefixCount()
	 */
    @Override
    public int getDeclaredPrefixCount() {
        return (fNamespaceSize - fContext[fCurrentContext]) / 2;
    } // getDeclaredPrefixCount():int

	/**
	 * @see org.apache.xerces.xni.NamespaceContext#getDeclaredPrefixAt(int)
	 */
    @Override
    public String getDeclaredPrefixAt(int index) {
        return fNamespace[fContext[fCurrentContext] + index * 2];
    } // getDeclaredPrefixAt(int):String

	/**
	 * @see org.apache.xerces.xni.NamespaceContext#getAllPrefixes()
	 */
	@Override
    public Enumeration<String> getAllPrefixes() {
        int count = 0;
        if (fPrefixes.length < (fNamespace.length/2)) {
            // resize prefix array          
            String[] prefixes = new String[fNamespaceSize];
            fPrefixes = prefixes;
        }
        String prefix = null;
        boolean unique = true;
        final String[] namespace = fNamespace;
        final String[] prefixes = fPrefixes;
        for (int i = 2; i < (fNamespaceSize-2); i += 2) {
            prefix = namespace[i + 2];            
            for (int k=0;k<count;k++){
                if (prefixes[k]==prefix){
                    unique = false;
                    break;
                }               
            }
            if (unique){
                prefixes[count++] = prefix;
            }
            unique = true;
        }
		return new Prefixes(prefixes, count);
	}
    
    /*
     * non-NamespaceContext methods
     */
    
    /** 
     * Checks whether a binding or unbinding for
     * the given prefix exists in the context.
     * 
     * @param prefix The prefix to look up. 
     * 
     * @return true if the given prefix exists in the context
     */
    public boolean containsPrefix(String prefix) {
        final int size = fNamespaceSize;
        final String[] namespace = fNamespace;
        // find prefix in current context
        for (int i = size; i > 0; i -= 2) {
            if (namespace[i - 2] == prefix) {
                return true;
            }
        }
        
        // prefix not found
        return false;
    }
    
    protected static final class Prefixes implements Enumeration<String> {
        private final String[] prefixes;
        private int counter = 0;
        private final int size;
               
		/**
		 * Constructor for Prefixes.
		 */
		public Prefixes(String[] prefixes, int size) {
			this.prefixes = prefixes;
            this.size = size;
		}

       /**
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		@Override
        public boolean hasMoreElements() {           
			return (counter < size);
		}

		/**
		 * @see java.util.Enumeration#nextElement()
		 */
		@Override
        public String nextElement() {
            if (counter < size){
                return prefixes[counter++];
            }
			throw new NoSuchElementException("Illegal access to Namespace prefixes enumeration.");
		}
        
        @Override
        public String toString(){
            StringBuilder buf = new StringBuilder();
            for (int i=0;i<size;i++){
                buf.append(prefixes[i]);
                buf.append(' ');
            }
                
            return buf.toString(); 
        }

}

} // class NamespaceSupport
