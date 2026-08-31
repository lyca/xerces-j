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

package io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.xerces.impl.io.UTF16Reader;
import org.apache.xerces.util.XMLChar;

/**
 * Tests custom UTF-16 reader against Java standard UTF-16 reader (BE and LE).
 */
public class UTF16 {

    private static final int BLOCK_READ_SIZE = 2048;

    

    @Test
    public void testJavaUTF16BECharByChar() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF16Producer(true), "UnicodeBig")) {
            testCharByChar(reader);
        }
    }

    @Test
    public void testJavaUTF16BECharArray() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF16Producer(true), "UnicodeBig")) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    @Test
    public void testCustomUTF16BECharByChar() throws Exception {
        try (Reader reader = new UTF16Reader(new UTF16Producer(true), true)) {
            testCharByChar(reader);
        }
    }

    @Test
    public void testCustomUTF16BECharArray() throws Exception {
        try (Reader reader = new UTF16Reader(new UTF16Producer(true), true)) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    @Test
    public void testJavaUTF16LECharByChar() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF16Producer(false), "UnicodeLittle")) {
            testCharByChar(reader);
        }
    }

    @Test
    public void testJavaUTF16LECharArray() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF16Producer(false), "UnicodeLittle")) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    @Test
    public void testCustomUTF16LECharByChar() throws Exception {
        try (Reader reader = new UTF16Reader(new UTF16Producer(false), false)) {
            testCharByChar(reader);
        }
    }

    @Test
    public void testCustomUTF16LECharArray() throws Exception {
        try (Reader reader = new UTF16Reader(new UTF16Producer(false), false)) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    public static long testCharByChar(Reader reader) throws Exception {
        long before = System.currentTimeMillis();

        for (int i = 0; i < 0xD800; i++) {
            int c = reader.read();
            if (c != i) {
                UTF8.expectedChar(null, i, c);
            }
        }
        for (int i = 0xE000; i < 0xFFFE; i++) {
            int c = reader.read();
            if (c != i) {
                UTF8.expectedChar(null, i, c);
            }
        }
        for (int i = 0x10000; i < 0x110000; i++) {
            int uuuuu = (i >> 16) & 0x001F;
            int wwww = uuuuu - 1;
            int zzzz = (i >> 12) & 0x000F;
            int yyyyyy = (i >> 6) & 0x003F;
            int xxxxxx = i & 0x003F;
            int hs = 0xD800 | (wwww << 6) | (zzzz << 2) | (yyyyyy >> 4);
            int ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx;

            int c = reader.read();
            if (c != hs) {
                UTF8.expectedChar("high surrogate", hs, c);
            }
            c = reader.read();
            if (c != ls) {
                UTF8.expectedChar("low surrogate", ls, c);
            }
        }
        int c = reader.read();
        if (c != -1) {
            UTF8.extraChar(c);
        }
        return System.currentTimeMillis() - before;
    }

    public static long testCharArray(Reader reader, int size) throws Exception {
        long before = System.currentTimeMillis();
        char[] ch = new char[size];
        int count = 0;
        int position = 0;

        for (int i = 0; i < 0xD800; i++) {
            if (position == count) {
                count = UTF8.load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                UTF8.expectedChar(null, i, c);
            }
        }
        for (int i = 0xE000; i < 0xFFFE; i++) {
            if (position == count) {
                count = UTF8.load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                UTF8.expectedChar(null, i, c);
            }
        }
        for (int i = 0x10000; i < 0x110000; i++) {
            int uuuuu = (i >> 16) & 0x001F;
            int wwww = uuuuu - 1;
            int zzzz = (i >> 12) & 0x000F;
            int yyyyyy = (i >> 6) & 0x003F;
            int xxxxxx = i & 0x003F;
            int hs = 0xD800 | (wwww << 6) | (zzzz << 2) | (yyyyyy >> 4);
            int ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx;

            if (position == count) {
                count = UTF8.load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != hs) {
                UTF8.expectedChar("high surrogate", hs, c);
            }
            if (position == count) {
                count = UTF8.load(reader, ch);
                position = 0;
            }
            c = ch[position++];
            if (c != ls) {
                UTF8.expectedChar("low surrogate", ls, c);
            }
        }
        if (position == count) {
            count = UTF8.load(reader, ch);
            position = 0;
        }
        if (count != -1) {
            UTF8.extraChar(ch[position]);
        }
        return System.currentTimeMillis() - before;
    }

    public static class UTF16Producer extends InputStream {
        private int fCodePoint;
        private int fByte;
        private final boolean fIsBigEndian;

        public UTF16Producer(boolean isBigEndian) {
            fIsBigEndian = isBigEndian;
        }

        public int read() throws IOException {
            if (fCodePoint < 0xFFFE) {
                if (fCodePoint == 0xD800) {
                    fCodePoint = 0xE000;
                }
                switch (fByte) {
                    case 0:
                        final int b0 = fIsBigEndian ? fCodePoint >> 8 : fCodePoint & 0xff;
                        fByte++;
                        return b0;
                    case 1:
                        final int b1 = fIsBigEndian ? fCodePoint & 0xff : fCodePoint >> 8;
                        fCodePoint++;
                        fByte = 0;
                        return b1;
                    default:
                        throw new RuntimeException("byte " + fByte + " of 2 byte UTF-16 sequence");
                }
            }
            if (fCodePoint == 0xFFFE) {
                fCodePoint = 0x10000;
            }
            if (fCodePoint < 0x110000) {
                switch (fByte) {
                    case 0:
                        final int b0 = fIsBigEndian ? XMLChar.highSurrogate(fCodePoint) >> 8 : XMLChar.highSurrogate(fCodePoint) & 0xff;
                        fByte++;
                        return b0;
                    case 1:
                        final int b1 = fIsBigEndian ? XMLChar.highSurrogate(fCodePoint) & 0xff : XMLChar.highSurrogate(fCodePoint) >> 8;
                        fByte++;
                        return b1;
                    case 2:
                        final int b2 = fIsBigEndian ? XMLChar.lowSurrogate(fCodePoint) >> 8 : XMLChar.lowSurrogate(fCodePoint) & 0xff;
                        fByte++;
                        return b2;
                    case 3:
                        final int b3 = fIsBigEndian ? XMLChar.lowSurrogate(fCodePoint) & 0xff : XMLChar.lowSurrogate(fCodePoint) >> 8;
                        fCodePoint++;
                        fByte = 0;
                        return b3;
                    default:
                        throw new RuntimeException("byte " + fByte + " of 4 byte UTF-16 sequence");
                }
            }
            return -1;
        }
    }
}
