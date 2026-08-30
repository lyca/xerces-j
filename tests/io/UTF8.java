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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.xerces.impl.io.UTF8Reader;

/**
 * Tests the customized UTF-8 reader comparing with Java standard UTF-8 reader.
 */
public class UTF8 {

    private static final int BLOCK_READ_SIZE = 2048;

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(UTF8.class);
    }

    @org.junit.Test
    public void testJavaUTF8CharByChar() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF8Producer(), "UTF8")) {
            testCharByChar(reader);
        }
    }

    @org.junit.Test
    public void testJavaUTF8CharArray() throws Exception {
        try (Reader reader = new InputStreamReader(new UTF8Producer(), "UTF8")) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    @org.junit.Test
    public void testCustomUTF8CharByChar() throws Exception {
        try (Reader reader = new UTF8Reader(new UTF8Producer())) {
            testCharByChar(reader);
        }
    }

    @org.junit.Test
    public void testCustomUTF8CharArray() throws Exception {
        try (Reader reader = new UTF8Reader(new UTF8Producer())) {
            testCharArray(reader, BLOCK_READ_SIZE);
        }
    }

    public static long testCharByChar(Reader reader) throws Exception {
        long before = System.currentTimeMillis();
        for (int i = 0; i < 0x0080; i++) {
            int c = reader.read();
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0x0080; i < 0x0800; i++) {
            int c = reader.read();
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0x0800; i < 0xD800; i++) {
            int c = reader.read();
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0xE000; i < 0x010000; i++) {
            int c = reader.read();
            if (c != i) {
                expectedChar(null, i, c);
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
                expectedChar("high surrogate", hs, c);
            }
            c = reader.read();
            if (c != ls) {
                expectedChar("low surrogate", ls, c);
            }
        }
        int c = reader.read();
        if (c != -1) {
            extraChar(c);
        }
        return System.currentTimeMillis() - before;
    }

    public static long testCharArray(Reader reader, int size) throws Exception {
        long before = System.currentTimeMillis();
        char[] ch = new char[size];
        int count = 0;
        int position = 0;

        for (int i = 0; i < 0x0080; i++) {
            if (position == count) {
                count = load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0x0080; i < 0x0800; i++) {
            if (position == count) {
                count = load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0x0800; i < 0xD800; i++) {
            if (position == count) {
                count = load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                expectedChar(null, i, c);
            }
        }
        for (int i = 0xE000; i < 0x010000; i++) {
            if (position == count) {
                count = load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != i) {
                expectedChar(null, i, c);
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
                count = load(reader, ch);
                position = 0;
            }
            int c = ch[position++];
            if (c != hs) {
                expectedChar("high surrogate", hs, c);
            }
            if (position == count) {
                count = load(reader, ch);
                position = 0;
            }
            c = ch[position++];
            if (c != ls) {
                expectedChar("low surrogate", ls, c);
            }
        }
        if (position == count) {
            count = load(reader, ch);
            position = 0;
        }
        if (count != -1) {
            extraChar(ch[position]);
        }
        return System.currentTimeMillis() - before;
    }

    static int load(Reader reader, char[] ch) throws IOException {
        return reader.read(ch, 0, ch.length);
    }

    static void expectedChar(String prefix, int ec, int fc) throws IOException {
        StringBuilder str = new StringBuilder();
        str.append("expected ");
        if (prefix != null) {
            str.append(prefix).append(' ');
        }
        str.append("0x").append(Integer.toHexString(ec)).append(" but found 0x");
        if (fc != -1) {
            str.append(Integer.toHexString(fc));
        }
        else {
            str.append("EOF");
        }
        throw new IOException(str.toString());
    }

    static void extraChar(int c) throws IOException {
        throw new IOException("found extra character 0x" + Integer.toHexString(c));
    }

    public static class UTF8Producer extends InputStream {
        private int fCodePoint;
        private int fByte;

        public int read() throws IOException {
            if (fCodePoint < 0x0080) {
                int b = fCodePoint++;
                fByte = 0;
                return b;
            }
            if (fCodePoint < 0x0800) {
                switch (fByte) {
                    case 0:
                        fByte++;
                        return 0x00C0 | ((fCodePoint >> 6) & 0x001F);
                    case 1:
                        fCodePoint++;
                        fByte = 0;
                        return 0x0080 | (fCodePoint - 1 & 0x003F);
                    default:
                        throw new RuntimeException("byte " + fByte + " of 2 byte UTF-8 sequence");
                }
            }
            if (fCodePoint < 0x10000) {
                switch (fByte) {
                    case 0:
                        fByte++;
                        return 0x00E0 | ((fCodePoint >> 12) & 0x000F);
                    case 1:
                        fByte++;
                        return 0x0080 | ((fCodePoint >> 6) & 0x003F);
                    case 2:
                        int b = 0x0080 | (fCodePoint & 0x003F);
                        fCodePoint++;
                        if (fCodePoint == 0xD800) {
                            fCodePoint = 0xE000;
                        }
                        fByte = 0;
                        return b;
                    default:
                        throw new RuntimeException("byte " + fByte + " of 3 byte UTF-8 sequence");
                }
            }
            if (fCodePoint < 0x110000) {
                switch (fByte) {
                    case 0:
                        fByte++;
                        return 0x00F0 | (((fCodePoint >> 16) & 0x001F) >> 2);
                    case 1:
                        fByte++;
                        return 0x0080 | ((((fCodePoint >> 16) & 0x001F) << 4) & 0x0030) | ((fCodePoint >> 12) & 0x000F);
                    case 2:
                        fByte++;
                        return 0x0080 | ((fCodePoint >> 6) & 0x003F);
                    case 3:
                        int b = 0x0080 | (fCodePoint & 0x003F);
                        fCodePoint++;
                        fByte = 0;
                        return b;
                    default:
                        throw new RuntimeException("byte " + fByte + " of 4 byte UTF-8 sequence");
                }
            }
            return -1;
        }
    }
}
