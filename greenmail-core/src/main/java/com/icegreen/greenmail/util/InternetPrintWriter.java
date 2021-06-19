/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Writes to a wrapped Writer class, ensuring that all line separators are '\r\n', regardless
 * of platform.
 */
public class InternetPrintWriter
        extends PrintWriter {

    /**
     * The line separator to use.
     */
    private static final String LINE_SEPARATOR = "\r\n";

    /**
     * Whether the Writer autoflushes on line feeds
     */
    private final boolean autoFlush;

    /**
     * Constructor that takes a writer to wrap.
     *
     * @param out the wrapped Writer
     */
    public InternetPrintWriter(Writer out) {
        super(out);
        autoFlush = false;
    }

    /**
     * Constructor that takes a writer to wrap.
     *
     * @param out       the wrapped Writer
     * @param autoFlush whether to flush after each print call
     */
    public InternetPrintWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
        this.autoFlush = autoFlush;
    }

    /**
     * Constructor that takes a stream to wrap.
     *
     * @param out the wrapped OutputStream
     */
    public InternetPrintWriter(OutputStream out) {
        super(out);
        autoFlush = false;
    }

    /**
     * Constructor that takes a stream to wrap.
     *
     * @param out       the wrapped OutputStream
     * @param autoFlush whether to flush after each print call
     */
    public InternetPrintWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        this.autoFlush = autoFlush;
    }

    /**
     * Creates a new InternetPrintWriter for given charset encoding.
     *
     * @param outputStream the wrapped output stream.
     * @param charset      the charset.
     * @return a new InternetPrintWriter.
     */
    public static InternetPrintWriter createForEncoding(OutputStream outputStream, boolean autoFlush, Charset charset) {
        return new InternetPrintWriter(new OutputStreamWriter(outputStream, charset), autoFlush);
    }

    /**
     * Print a line separator.
     */
    @Override
    public void println() {
        synchronized (lock) {
            write(LINE_SEPARATOR);
            if (autoFlush) {
                flush();
            }
        }
    }

    /**
     * Print a boolean followed by a line separator.
     *
     * @param x the boolean to print
     */
    @Override
    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a char followed by a line separator.
     *
     * @param x the char to print
     */
    @Override
    public void println(char x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a int followed by a line separator.
     *
     * @param x the int to print
     */
    @Override
    public void println(int x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a long followed by a line separator.
     *
     * @param x the long to print
     */
    @Override
    public void println(long x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a float followed by a line separator.
     *
     * @param x the float to print
     */
    @Override
    public void println(float x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a double followed by a line separator.
     *
     * @param x the double to print
     */
    @Override
    public void println(double x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a character array followed by a line separator.
     *
     * @param x the character array to print
     */
    @Override
    public void println(char[] x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a String followed by a line separator.
     *
     * @param x the String to print
     */
    @Override
    public void println(String x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print an Object followed by a line separator.
     *
     * @param x the Object to print
     */
    @Override
    public void println(Object x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }
}
