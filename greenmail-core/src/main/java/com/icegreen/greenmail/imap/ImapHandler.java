/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.InternetPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * The handler class for IMAP connections.
 * TODO: This is a quick cut-and-paste hack from POP3Handler. This, and the ImapServer
 * should probably be rewritten from scratch.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class ImapHandler implements ImapConstants, ProtocolHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;

    /**
     * The TCP/IP socket over which the IMAP interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader in;

    /**
     * The socket's input stream.
     */
    private InputStream ins;

    /**
     * The writer to which outgoing messages are written.
     */
    private InternetPrintWriter out;

    /**
     * The socket's output stream
     */
    private OutputStream outs;

    UserManager userManager;
    private ImapHostManager imapHost;

    public ImapHandler(UserManager userManager, ImapHostManager imapHost, Socket socket) {
        this.userManager = userManager;
        this.imapHost = imapHost;
        this.socket = socket;
    }

    public void forceConnectionClose(final String message) {
        ImapResponse response = new ImapResponse(outs);
        response.byeResponse(message);
        close();
    }

    public void run() {
        try {
            ins = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"), 512);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            outs = new BufferedOutputStream(socket.getOutputStream(), 1024);
            out = new InternetPrintWriter(outs, true);
            ImapResponse response = new ImapResponse(outs);

            // Write welcome message
            String responseBuffer = VERSION +" Server GreenMail ready";
            response.okResponse(null, responseBuffer);

            session = new ImapSessionImpl(imapHost,
                    userManager,
                    this,
                    socket.getInetAddress().getHostAddress());

            while (requestHandler.handleRequest(ins, outs, session)) {
            }

        } catch (Exception e) {
            log.error("Can not handle IMAP connection", e);
        } finally {
           close();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void close() {
        // Close and clear streams, sockets etc.
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Can not close socket", e);
        } finally {
            socket = null;
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            log.warn("Can not close input stream", e);
        } finally {
            in = null;
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            // Ignored
            log.warn("Can not close writer", e);
        } finally {
            out = null;
        }

        try {
            if (outs != null) {
                outs.close();
            }
        } catch (Exception e) {
            log.warn("Can not close output stream", e);
        } finally {
            outs = null;
        }

        // Clear user data
        session = null;
    }
}

