package com.icegreen.greenmail.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Buffers and logs when detecting CR-LF.
 * <p>
 * <ul>
 *     <li>Wraps long lines</li>
 *     <li>Escapes non-ASCII-printable chars</li>
 * </ul>
 */
public class LineLoggingBuffer {
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private final String linePrefix;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected static final byte[] WRAP = "[WRAP]".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates new buffer using given line prefix.
     *
     * @param linePrefix prefix for each line printed, eg "S:" or "C:"
     */
    public LineLoggingBuffer(String linePrefix) {
        this.linePrefix = linePrefix;
    }

    /**
     * Appends and escapes value.
     *
     * @param b value
     */
    public void append(int b) {
        if (b > 0) {
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
        }
        // Print when encountering negative (-1) value -or- new line -or- reaching max line length
        if (b < 0 || b == '\n' || isLineLengthExceeded()) {
            logLine();
        }
    }

    /**
     * Triggers log output of current buffer (if any) and resets back to empty buffer.
     */
    public void logLine() {
        if (buf.size() > 0) {
            try {
                if (isLineLengthExceeded()) { // Signal wrapping
                    buf.write(WRAP);
                }
                String line = buf.toString(StandardCharsets.UTF_8.name());
                log.debug(linePrefix + line);
                buf.reset();
            } catch (IOException e) {
                throw new IllegalStateException("Can not log output stream", e);
            }
        }
    }

    protected boolean isLineLengthExceeded() {
        return buf.size() > 128;
    }
}
