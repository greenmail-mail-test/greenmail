package com.icegreen.greenmail.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Logs stream for debugging purpose on DEBUG level.
 */
public class LoggingOutputStream extends FilterOutputStream {
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final String prefix;

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
        this.prefix = prefix;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        appendToBuffer(b);
        super.write(b);
    }


    @Override
    public synchronized void flush() throws IOException {
        logLine();
        super.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        logLine();
        super.close();
    }

    private void appendToBuffer(int b) {
        if (b == '\n') {
            buf.write('\\');
            buf.write('n');
        } else if (b == '\r') {
            buf.write('\\');
            buf.write('r');
        } else if (b == '\t') {
            buf.write('\\');
            buf.write('t');
        } else if (b < ' ') { // Non-printable ASCII
            buf.write('^');
            buf.write('@' + b);
        } else {
            buf.write(b);
        }
        // Print when encountering new line or reaching max line length
        if (b == '\n' || isLineLengthExceeded()) {
            logLine();
        }
    }

    protected void logLine() {
        if (buf.size() > 0) {
            try {
                if (isLineLengthExceeded()) { // Signal wrapping
                    buf.write("[WRAP]".getBytes(StandardCharsets.US_ASCII));
                }
                String line = buf.toString(StandardCharsets.UTF_8.name());
                log.debug(prefix + line);
                buf.reset();
            } catch (IOException e) {
                throw new IllegalStateException("Can not log output stream", e);
            }
        }
    }

    private boolean isLineLengthExceeded() {
        return buf.size() > 128;
    }
}
