/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.server.AbstractSocketProtocolHandler;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.LoggingInputStream;
import com.icegreen.greenmail.util.LoggingOutputStream;

import java.io.*;
import java.net.Socket;

/**
 * The handler class for IMAP connections.
 *
 * @author Federico Barbieri &lt;scoobie@systemy.it&gt;
 * @author Peter M. Goldstein &lt;farsight@alum.mit.edu&gt;
 */
public class ImapHandler extends AbstractSocketProtocolHandler implements ImapConstants {
    private final ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;

    private ImapResponse response;

    UserManager userManager;
    private final ImapHostManager imapHost;

    public ImapHandler(UserManager userManager, ImapHostManager imapHost, Socket socket) {
        super(socket);
        this.userManager = userManager;
        this.imapHost = imapHost;
    }

    public void forceConnectionClose(final String message) {
        response.byeResponse(message);
        close();
    }

    @Override
    public void run() {
        // Closed automatically when socket is closed via #close()
        try (InputStream ins = prepareInputStream();
             OutputStream outs = prepareOutputStream()
        ) {
            response = new ImapResponse(outs);

            // Write welcome message
            String responseBuffer = VERSION + " Server GreenMail v" +
                    BuildInfo.INSTANCE.getProjectVersion() + " ready";
            response.okResponse(null, responseBuffer);

            session = new ImapSessionImpl(imapHost,
                    userManager,
                    this,
                    socket.getInetAddress().getHostAddress());

            while (requestHandler.handleRequest(ins, outs, session)) {
                // Loop ...
            }
        } catch (Exception e) {
            throw new IllegalStateException("Can not handle IMAP connection", e);
        } finally {
            close();
        }
    }

    private InputStream prepareInputStream() throws IOException {
        InputStream is = new BufferedInputStream(socket.getInputStream(), 512);
        if (log.isDebugEnabled()) {
            is = new LoggingInputStream(is, "C: ");
        }
        return is;
    }

    private OutputStream prepareOutputStream() throws IOException {
        OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024);
        if (log.isDebugEnabled()) {
            outputStream = new LoggingOutputStream(outputStream, "S: ");
        }
        return outputStream;
    }

    /**
     * Resets the handler data to a basic state.
     */
    @Override
    public void close() {
        super.close();

        // Clear user data
        session = null;
        response = null;
    }
}
