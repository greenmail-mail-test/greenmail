/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class StreamUtils {
    public static String toString(Reader in)
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        char[] buffer = new char[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            sbuf.append(buffer, 0, len);
        }

        return sbuf.toString();
    }

    public static void copy(Reader in, Writer out)
            throws IOException {
        char[] buffer = new char[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        in.close();
    }

    /**
     * Creates a reader that will return -1 after <code>len</code>
     * chars have been read.
     */
    public static Reader limit(Reader in, long len) {

        return new LimitedReader(in, len);
    }

    public static Reader splice(Reader one, Reader two) {

        return new SpliceReader(one, two);
    }

    private static class SpliceReader
            extends Reader {
        Reader _one;
        Reader _two;
        boolean oneFinished;

        public SpliceReader(Reader one, Reader two) {
            _one = one;
            _two = two;
        }

        public void close()
                throws IOException {
            _one.close();

            _two.close();
        }

        public int read()
                throws IOException {
            if (oneFinished) {

                return _two.read();
            } else {
                int value = _one.read();
                if (value == -1) {
                    oneFinished = true;

                    return read();
                } else

                    return value;

            }
        }

        public int read(char[] buf, int start, int len)
                throws IOException {
            if (oneFinished) {

                return _two.read(buf, start, len);
            } else {
                int value = _one.read(buf, start, len);
                if (value == -1) {
                    oneFinished = true;

                    return read(buf, start, len);
                } else

                    return value;

            }
        }

        public int read(char[] buf)
                throws IOException {
            if (oneFinished) {

                return _two.read(buf);
            } else {
                int value = _one.read(buf);
                if (value == -1) {
                    oneFinished = true;

                    return read(buf);
                } else

                    return value;
            }
        }
    }

    private static class LimitedReader
            extends Reader {
        Reader _in;
        long _maxLen;
        long _lenRead;

        public LimitedReader(Reader in, long len) {
            _in = in;
            _maxLen = len;
        }

        public void close() {

            // don't close the original stream
        }

        public int read()
                throws IOException {
            if (_lenRead < _maxLen) {
                _lenRead++;

                return _in.read();
            } else {

                return -1;
            }
        }

        public int read(char[] buf, int start, int len)
                throws IOException {
            if (_lenRead < _maxLen) {
                int numAllowedToRead = (int) Math.min(_maxLen - _lenRead,
                        len);
                int count = _in.read(buf, start, numAllowedToRead);
                _lenRead += count;

                return count;
            } else {

                return -1;
            }
        }

        public int read(char[] buf)
                throws IOException {

            return read(buf, 0, buf.length);
        }
    }
}