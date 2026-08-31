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

package org.apache.xerces.impl.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import org.apache.xerces.impl.msg.XMLMessageFormatter;
import org.apache.xerces.util.MessageFormatter;

/**
 * A simple ASCII byte reader. This reader processes bytes 
 * compiled into an ASCII format.
 * 
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 *
 * @version $Id$
 */
public final class ASCIIReader
    extends Reader {

    //
    // Constants
    //

    /** Default byte buffer size (8192). */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    //
    // Data
    //

    /** Input stream. */
    protected final InputStream fInputStream;

    /** Byte buffer. */
    protected final byte[] fBuffer;

    // message formatter; used to produce localized
    // exception messages
    private final MessageFormatter fFormatter;

    //Locale to use for messages
    private final Locale fLocale;

    //
    // Constructors
    //

    /** 
     * Constructs an ASCII reader from the specified input stream 
     * using the default byte buffer size.
     *
     * @param inputStream The input stream.
     * @param messageFormatter  the MessageFormatter to use to message reporting.
     * @param locale    the Locale for which messages are to be reported
     */
    public ASCIIReader(InputStream inputStream, MessageFormatter messageFormatter,
            Locale locale) {
        this(inputStream, DEFAULT_BUFFER_SIZE, messageFormatter, locale);
    } // <init>(InputStream, MessageFormatter, Locale)

    /** 
     * Constructs an ASCII reader from the specified input stream 
     * and buffer size and MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param size        The initial buffer size.
     * @param messageFormatter  the MessageFormatter to use to message reporting.
     * @param locale    the Locale for which messages are to be reported
     */
    public ASCIIReader(InputStream inputStream, int size,
            MessageFormatter messageFormatter, Locale locale) {
        this(inputStream, new byte[size], messageFormatter, locale);
    } // <init>(InputStream, int, MessageFormatter, Locale)

    /** 
     * Constructs an ASCII reader from the specified input stream 
     * and byte array.
     *
     * @param inputStream The input stream.
     * @param buffer      The byte buffer.
     * @param messageFormatter  the MessageFormatter to use to message reporting.
     * @param locale    the Locale for which messages are to be reported
     */
    public ASCIIReader(InputStream inputStream, byte [] buffer,
            MessageFormatter messageFormatter, Locale locale) {
        fInputStream = inputStream;
        fBuffer = buffer;
        fFormatter = messageFormatter;
        fLocale = locale;
    } // <init>(InputStream, byte[], MessageFormatter, Locale)

    //
    // Reader methods
    //

    /**
     * Read a single character.  This method will block until a character is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p> Subclasses that intend to support efficient single-character input
     * should override this method.
     *
     * @return     The character read, as an integer in the range 0 to 127
     *             (<code>0x00-0x7f</code>), or -1 if the end of the stream has
     *             been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        int b0 = fInputStream.read();
        if (b0 >= 0x80) {
            throw new MalformedByteSequenceException(fFormatter, 
                fLocale, XMLMessageFormatter.XML_DOMAIN, 
                "InvalidASCII", new Object [] {Integer.toString(b0)});
        }
        return b0;
    } // read():int

    /**
     * Read characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param      ch     Destination buffer
     * @param      offset Offset at which to start storing characters
     * @param      length Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char ch[], int offset, int length) throws IOException {
        if (length > fBuffer.length) {
            length = fBuffer.length;
        }
        int count = fInputStream.read(fBuffer, 0, length);
        if (count == -1) {
            return -1;
        }
        final byte[] buffer = fBuffer;
        int i = 0;
        while (i + 4 <= count) {
            byte b0 = buffer[i];
            byte b1 = buffer[i + 1];
            byte b2 = buffer[i + 2];
            byte b3 = buffer[i + 3];
            if ((b0 | b1 | b2 | b3) < 0) {
                break;
            }
            ch[offset + i] = (char) b0;
            ch[offset + i + 1] = (char) b1;
            ch[offset + i + 2] = (char) b2;
            ch[offset + i + 3] = (char) b3;
            i += 4;
        }
        for (; i < count; i++) {
            int b0 = buffer[i];
            if (b0 < 0) {
                throw new MalformedByteSequenceException(fFormatter,
                    fLocale, XMLMessageFormatter.XML_DOMAIN,
                    "InvalidASCII", new Object [] {Integer.toString(b0 & 0x0FF)});
            }
            ch[offset + i] = (char)b0;
        }
        return count;
    } // read(char[],int,int)

    /**
     * Skip characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param  n  The number of characters to skip
     *
     * @return    The number of characters actually skipped
     *
     * @exception  IOException  If an I/O error occurs
     */
    public long skip(long n) throws IOException {
        long remaining = n;
        final char[] ch = new char[fBuffer.length];
        do {
            int len = ch.length < remaining ? ch.length : (int)remaining;
            int count = read(ch, 0, len);
            if (count > 0) {
                remaining -= count;
            }
            else {
                break;
            }
        } while (remaining > 0);

        long skipped = n - remaining;
        return skipped;
    } // skip(long):long

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise.  Note that returning false does not guarantee that the
     * next read will block.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
	    return false;
    } // ready():boolean

    /**
     * Tell whether this stream supports the mark() operation.
     */
    public boolean markSupported() {
	    return false;
    } // markSupported():boolean

    /**
     * Mark the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.  Not all
     * character-input streams support the mark() operation.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     *                         read while still preserving the mark.  After
     *                         reading this many characters, attempting to
     *                         reset the stream may fail.
     *
     * @exception  IOException  If the stream does not support the mark()
     *                          operation, or if some other I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
	    throw new IOException(fFormatter.formatMessage(fLocale, "OperationNotSupported", new Object[]{"mark()", "US-ASCII"}));
    } // mark(int)

    /**
     * Reset the stream.  If the stream has been marked, then attempt to
     * reposition it at the mark.  If the stream has not been marked, then
     * attempt to reset it in some way appropriate to the particular stream,
     * for example by repositioning it to its starting point.  Not all
     * character-input streams support the reset() operation, and some support
     * reset() without supporting mark().
     *
     * @exception  IOException  If the stream has not been marked,
     *                          or if the mark has been invalidated,
     *                          or if the stream does not support the reset()
     *                          operation, or if some other I/O error occurs
     */
    public void reset() throws IOException {
    } // reset()

    /**
     * Close the stream.  Once a stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        fInputStream.close();
    } // close()

} // class ASCIIReader
