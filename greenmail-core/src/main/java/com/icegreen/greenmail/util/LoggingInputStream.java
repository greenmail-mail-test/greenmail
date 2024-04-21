package com.icegreen.greenmail.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Logs stream for debugging purpose on DEBUG level.
 */
public class LoggingInputStream extends FilterInputStream {
    protected final LineLoggingBuffer loggingBuffer;

    /**
     * Creates an input stream filter built on top of the specified
     * underlying input stream.
     *
     * @param is     the underlying input stream to be assigned to
     *               the field {@link #in} for later use, or
     *               <code>null</code> if this instance is to be
     *               created without an underlying stream.
     * @param prefix a log message prefix, eg 'c: ' for server side protocol trace input.
     */
    public LoggingInputStream(InputStream is, String prefix) {
        super(is);
        loggingBuffer = new LineLoggingBuffer(prefix);
    }

    @Override
    public synchronized int read() throws IOException {
        int b = super.read();
        loggingBuffer.append(b);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        for (int i = off; i < off + bytesRead; i++) {
            loggingBuffer.append(b[i]);
        }
        return bytesRead;
    }

    @Override
    public synchronized void close() throws IOException {
        loggingBuffer.logLine();
        super.close();
    }
}
