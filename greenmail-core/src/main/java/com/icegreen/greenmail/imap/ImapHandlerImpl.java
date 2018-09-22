/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * The handler class for IMAP connections.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class ImapHandlerImpl implements ImapConstants, ProtocolHandler, ImapHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;
    private final Object closeMonitor = new Object();

    /**
     * The TCP/IP socket over which the IMAP interaction
     * is occurring
     */
    private Socket socket;

    private ImapResponse response;

    UserManager userManager;
    private ImapHostManager imapHost;
    
    @Override
    public ImapHandler init(UserManager userManager, ImapHostManager imapHost, Socket socket) {
        this.userManager = userManager;
        this.imapHost = imapHost;
        this.socket = socket;
        return this;
    }

    @Override
    public void forceConnectionClose(final String message) {
        response.byeResponse(message);
        close();
    }

    @Override
    public void run() {
        // Closed automatically when socket is closed via #close()
        try (InputStream ins = new BufferedInputStream(socket.getInputStream(), 512);
             OutputStream outs = new BufferedOutputStream(socket.getOutputStream(), 1024)
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
            log.error("Can not handle IMAP connection", e);
            throw new IllegalStateException("Can not handle IMAP connection", e);
        } finally {
            close();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    @Override
    public void close() {
        // Use monitor to avoid race between external close and handler thread run()
        synchronized (closeMonitor) {
            // Close and clear streams, sockets etc.
            if (socket != null) {
                try {
                    // Terminates thread blocking on socket read
                    // and automatically closed depending streams
                    socket.close();
                } catch (IOException e) {
                    log.warn("Can not close socket", e);
                } finally {
                    socket = null;
                }
            }

            // Clear user data
            session = null;
            response = null;
        }
    }

    public ImapRequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void setRequestHandler(ImapRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }
}

