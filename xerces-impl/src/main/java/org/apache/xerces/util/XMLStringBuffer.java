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

import org.apache.xerces.xni.XMLString;

/**
 * XMLString is a structure used to pass character arrays. However,
 * XMLStringBuffer is a buffer in which characters can be appended
 * and extends XMLString so that it can be passed to methods
 * expecting an XMLString object. This is a safe operation because
 * it is assumed that any callee will <strong>not</strong> modify
 * the contents of the XMLString structure.
 * <p> 
 * The contents of the string are managed by the string buffer. As
 * characters are appended, the string buffer will grow as needed.
 * <p>
 * <strong>Note:</strong> Never set the <code>ch</code>, 
 * <code>offset</code>, and <code>length</code> fields directly.
 * These fields are managed by the string buffer. In order to reset
 * the buffer, call <code>clear()</code>.
 * 
 * @author Andy Clark, IBM
 * @author Eric Ye, IBM
 *
 * @version $Id$
 */
public class XMLStringBuffer
    extends XMLString {

    //
    // Constants
    //

    /** Default buffer size (32). */
    public static final int DEFAULT_SIZE = 32;

    //
    // Constructors
    //

    /** Default constructor. */
    public XMLStringBuffer() {
        this(DEFAULT_SIZE);
    } // <init>()

    /**
     * Constructs a string buffer with a specified size.
     * 
     * @param size The initial size of the buffer.
     */
    public XMLStringBuffer(int size) {
        ch = new char[size > 0 ? size : DEFAULT_SIZE];
    } // <init>(int)

    /** Constructs a string buffer from a char. */
    public XMLStringBuffer(char c) {
        this(DEFAULT_SIZE);
        append(c);
    } // <init>(char)

    /** Constructs a string buffer from a String. */
    public XMLStringBuffer(String s) {
        this(s != null ? s.length() : DEFAULT_SIZE);
        if (s != null) {
            append(s);
        }
    } // <init>(String)

    /** Constructs a string buffer from the specified character array. */
    public XMLStringBuffer(char[] ch, int offset, int length) {
        this(length > 0 ? length : DEFAULT_SIZE);
        append(ch, offset, length);
    } // <init>(char[],int,int)

    /** Constructs a string buffer from the specified XMLString. */
    public XMLStringBuffer(XMLString s) {
        this(s != null && s.length > 0 ? s.length : DEFAULT_SIZE);
        if (s != null) {
            append(s);
        }
    } // <init>(XMLString)

    //
    // Public methods
    //

    /** Clears the string buffer. */
    public void clear() {
        offset = 0;
        length = 0;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > this.ch.length) {
            int newLength = this.ch.length * 2;
            if (newLength < minCapacity + DEFAULT_SIZE) {
                newLength = minCapacity + DEFAULT_SIZE;
            }
            char[] newch = new char[newLength];
            System.arraycopy(this.ch, 0, newch, 0, this.length);
            this.ch = newch;
        }
    }

    /**
     * Appends a single character to the buffer.
     * 
     * @param c The character to append.
     */
    public void append(char c) {
        if (this.length >= this.ch.length) {
            ensureCapacity(this.length + 1);
        }
        this.ch[this.length++] = c;
    } // append(char)

    /**
     * Appends a string to the buffer.
     * 
     * @param s The string to append.
     */
    public void append(String s) {
        if (s == null) {
            return;
        }
        int len = s.length();
        if (len == 0) {
            return;
        }
        int minCapacity = this.length + len;
        if (minCapacity > this.ch.length) {
            ensureCapacity(minCapacity);
        }
        s.getChars(0, len, this.ch, this.length);
        this.length = minCapacity;
    } // append(String)

    /**
     * Appends a range of characters to the buffer.
     * 
     * @param ch The character array.
     * @param offset The offset into the character array.
     * @param length The number of characters to append.
     */
    public void append(char[] ch, int offset, int length) {
        if (length <= 0 || ch == null) {
            return;
        }
        int minCapacity = this.length + length;
        if (minCapacity > this.ch.length) {
            ensureCapacity(minCapacity);
        }
        System.arraycopy(ch, offset, this.ch, this.length, length);
        this.length = minCapacity;
    } // append(char[],int,int)

    /**
     * Appends the contents of an XMLString to the buffer.
     * 
     * @param s The XMLString to append.
     */
    public void append(XMLString s) {
        if (s != null && s.length > 0) {
            append(s.ch, s.offset, s.length);
        }
    } // append(XMLString)

} // class XMLStringBuffer
