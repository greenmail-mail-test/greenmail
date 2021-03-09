/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.InternetPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SmtpConnection {


    private static final Logger log = LoggerFactory.getLogger(SmtpConnection.class);

    // networking/io stuff
    Socket sock;
    InetAddress clientAddress;
    InternetPrintWriter out;
    BufferedInputStream in;
    SmtpHandler handler;
    String heloName;
    boolean authenticated; // Was there a successful authentication?

    public SmtpConnection(SmtpHandler handler, Socket sock)
        throws IOException {
        this.sock = sock;
        clientAddress = sock.getInetAddress();
        OutputStream o = sock.getOutputStream();
        in = new BufferedInputStream(sock.getInputStream());
        out = InternetPrintWriter.createForEncoding(o, true, EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);

        this.handler = handler;
    }

    public void send(String line) {
        log.trace("S: {}", line);
        out.println(line);
    }

    public String readLine() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        try {
            while (true) {
                int b = in.read();
                if (b < 0) { // End
                    if(log.isDebugEnabled()) {
                        log.debug("Unexpected end of stream, read {0} bytes: {1}",bos.size(), bos.toString());
                    }
                    if(bos.size()>0) {
                        // Best effort?
                        return bos.toString(StandardCharsets.US_ASCII.name());
                    } else {
                        return null; // No input received
                    }
                }
                if (b == '\r') { // CRLF ?
                    b = in.read();
                    if (b == '\n') {
                        String line = bos.toString(StandardCharsets.US_ASCII.name());
                        log.trace("C: {}", line);
                        return line;
                    } else {
                        bos.write('\r');
                    }
                }
                bos.write(b);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Can not read line, read " + bos.size() + " bytes: " + bos.toString(), ex);
        }
    }

    private static final int CR_LF_DOT = '\r' << 16 | '\n' << 8 | '.';
    private static final int CR_LF_DOT_CR = '\r' << 24 | '\n' << 16 | '.' << 8 | '\r';

    /**
     * Reads the contents of the stream until
     * &lt;CRLF&gt;.&lt;CRLF&gt; is encountered.
     *
     * @param initialContent initial content
     * @return a limited input stream.
     */
    public InputStream dotLimitedInputStream(byte[] initialContent) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        try {
            bos.write(initialContent); // Insert initial prefix content

            int cbuf = 0; // Caches current last 4 bytes
            while (true) {
                int b = in.read();
                if (b < 0) {
                    throw new IllegalStateException("Unexpected end of stream, read " + bos.size() + " bytes: " + bos.toString());
                }

                if (cbuf == CR_LF_DOT_CR && b == '\n') { // CRLF-DOT-CRLF
                    final byte[] buf = bos.toByteArray();
                    int maxLen = Math.min(bos.size(), bos.size() - 2 /* DOT + CR */);
                    return new ByteArrayInputStream(buf, 0, maxLen);
                } else if ((cbuf & 0xffffff) == CR_LF_DOT && b == '.') { // CR_LF_DOT and DOT => Skip dot once
                    // https://tools.ietf.org/html/rfc5321#section-4.5.2 :
                    // When a line of mail text is received by the SMTP server, it checks
                    // the line.  If the line is composed of a single period, it is
                    // treated as the end of mail indicator.  If the first character is a
                    // period and there are other characters on the line, the first
                    // character is deleted.
                } else {
                    bos.write(b);
                }
                cbuf = (cbuf << 8) | b;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Can not read line, read " + bos.size() + " bytes: " + bos.toString(), ex);
        }
    }

    public String getClientAddress() {
        return clientAddress.getHostAddress();
    }

    public InetAddress getServerAddress() {
        return sock.getLocalAddress();
    }

    public String getServerGreetingsName() {
        InetAddress address = getServerAddress();
        if (address != null)
            return address.toString();
        else
            return System.getProperty("user.name");
    }

    public String getHeloName() {
        return heloName;
    }

    public void setHeloName(String n) {
        heloName = n;
    }

    public void quit() {
        handler.close();
    }

    /**
     * Checks if there was a successful authentication for this connection.
     *
     * @return true, if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Sets the authentication state of this connection.
     *
     * @param authenticated true,
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
