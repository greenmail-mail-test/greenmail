package com.icegreen.greenmail.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Logs stream for debugging purpose on DEBUG level.
 */
public class LoggingOutputStream extends FilterOutputStream {
    protected final LineLoggingBuffer loggingBuffer;

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param out    the underlying output stream to be assigned to
     *               the field <tt>this.out</tt> for later use, or
     *               <code>null</code> if this instance is to be
     *               created without an underlying stream.
     * @param prefix a log message prefix, eg 's: ' for server side protocol trace output.
     */
    public LoggingOutputStream(OutputStream out, String prefix) {
        super(out);
        loggingBuffer = new LineLoggingBuffer(prefix);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        loggingBuffer.append(b);
        super.write(b);
    }


    @Override
    public synchronized void flush() throws IOException {
        loggingBuffer.logLine();
        super.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        loggingBuffer.logLine();
        super.close();
    }
}
