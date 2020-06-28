/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing lookahead=1
 * on the underlying character stream.
 * TODO need to look at encoding, and whether we should be wrapping an InputStream instead.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ImapRequestLineReader {
    private static final Logger log = LoggerFactory.getLogger(ImapRequestLineReader.class);
    private final InputStream input;
    private final OutputStream output;

    private boolean nextSeen = false;
    private char nextChar; // unknown
    private final StringBuilder buf = new StringBuilder();
    private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r\n");

    public ImapRequestLineReader(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Reads the next regular, non-space character in the current line. Spaces are skipped
     * over, but end-of-line characters will cause a {@link ProtocolException} to be thrown.
     * This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next non-space character.
     * @throws ProtocolException If the end-of-line or end-of-stream is reached.
     */
    public char nextWordChar() throws ProtocolException {
        char next = nextChar();
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        if (next == '\r' || next == '\n') {
            throw new ProtocolException("Missing argument.");
        }

        return next;
    }

    /**
     * Reads the next character in the current line. This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next character.
     * @throws ProtocolException If the end-of-stream is reached.
     */
    public char nextChar() throws ProtocolException {
        if (!nextSeen) {
            try {
                final int read = input.read();
                final char c = (char) read;
                buf.append(c);
                if (read == -1) {
                    dumpLine();
                    throw new ProtocolException("End of stream");
                }
                nextChar = c;
                nextSeen = true;
            } catch (IOException e) {
                throw new ProtocolException("Error reading from stream.", e);
            }
        }
        return nextChar;
    }

    public void dumpLine() {
        if (log.isDebugEnabled()) {
            // Replace carriage return to avoid confusing multiline log output
            log.debug("IMAP Line received : <" + CARRIAGE_RETURN.matcher(buf).replaceAll("\\\\r\\\\n") + '>');
        }
        buf.delete(0, buf.length());
    }

    /**
     * Moves the request line reader to end of the line, checking that no non-space
     * character are found.
     *
     * @throws ProtocolException If more non-space tokens are found in this line,
     *                           or the end-of-file is reached.
     */
    public void eol() throws ProtocolException {
        char next = nextChar();

        // Ignore trailing spaces.
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        // handle DOS and unix end-of-lines
        if (next == '\r') {
            consume();
            next = nextChar();
        }

        // Check if we found extra characters.
        if (next != '\n') {
            throw new ProtocolException("Expected end-of-line, found more character(s): " + next);
        }
        dumpLine();
    }

    /**
     * Consumes the current character in the reader, so that subsequent calls to the request will
     * provide a new character. This method does *not* read the new character, or check if
     * such a character exists. If no current character has been seen, the method moves to
     * the next character, consumes it, and moves on to the subsequent one.
     *
     * @throws ProtocolException if a the current character can't be obtained (eg we're at
     *                           end-of-file).
     */
    public char consume() throws ProtocolException {
        char current = nextChar();
        nextSeen = false;
        nextChar = 0;
        return current;
    }


    /**
     * Reads and consumes a number of characters from the underlying reader,
     * filling the byte array provided.
     *
     * @param holder A byte array which will be filled with bytes read from the underlying reader.
     * @throws ProtocolException If a char can't be read into each array element.
     */
    public void read(byte[] holder) throws ProtocolException {
        int readTotal = 0;
        try {
            while (readTotal < holder.length) {
                int count = input.read(holder, readTotal, holder.length - readTotal);
                if (count == -1) {
                    throw new ProtocolException("Unexpected end of stream.");
                }
                readTotal += count;
            }
            // Unset the next char.
            nextSeen = false;
            nextChar = 0;
        } catch (IOException e) {
            throw new ProtocolException("Error reading from stream.", e);
        }
    }

    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
    public void commandContinuationRequest()
            throws ProtocolException {
        try {
            output.write('+');
            output.write(' ');
            output.write('O');
            output.write('K');
            output.write('\r');
            output.write('\n');
            output.flush();
        } catch (IOException e) {
            throw new ProtocolException("Unexpected exception in sending command continuation request.", e);
        }
    }

    /**
     * Consumes whole line till newline.
     *
     * @throws ProtocolException on error.
     */
    public void consumeLine()
            throws ProtocolException {
        char next = nextChar();
        while (next != '\n') {
            consume();
            next = nextChar();
        }
        consume();
    }

    /**
     * Consumes all given chars.
     *
     * @throws ProtocolException on error.
     * @return next char
     */
    public char consumeAll(char c)
            throws ProtocolException {
        char next = nextChar();
        while(next == c) {
            consume();
            next = nextChar();
        }
        return next;
    }
}
