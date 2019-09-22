/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SmtpHandler implements ProtocolHandler {
    protected static final Logger log = LoggerFactory.getLogger(SmtpHandler.class);

    // protocol and configuration global stuff
    protected SmtpCommandRegistry registry;
    protected SmtpManager manager;

    // session stuff
    protected SmtpConnection conn;
    protected SmtpState state;

    // command parsing stuff
    protected boolean quitting;
    protected String currentLine;
    protected Socket socket;

    public SmtpHandler(SmtpCommandRegistry registry,
                       SmtpManager manager, Socket socket) {
        this.registry = registry;
        this.manager = manager;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            conn = new SmtpConnection(this, socket);
            state = new SmtpState();
            quitting = false;

            sendGreetings();

            while (!quitting) {
                handleCommand();
            }

        } catch (SocketTimeoutException ste) {
            conn.send("421 Service shutting down and closing transmission channel");

        } catch (Exception e) {
            // Closing socket on blocked read
            if(!quitting) {
                log.error("Unexpected error handling connection, quitting=", e);
                throw new IllegalStateException(e);
            }
        } finally {
            if (null != state) {
                state.clearMessage();
            }
        }
    }

    protected void sendGreetings() {
        conn.send("220 " + conn.getServerGreetingsName() +
                " GreenMail SMTP Service v" + BuildInfo.INSTANCE.getProjectVersion() + " ready");
    }

    protected void handleCommand()
            throws IOException {
        currentLine = conn.receiveLine();

        if (currentLine == null) {
            close();

            return;
        }

        // eliminate invalid line lengths before parsing
        if (!commandLegalSize()) {

            return;
        }

        String commandName = currentLine.substring(0, 4).toUpperCase();

        SmtpCommand command = registry.getCommand(commandName);

        if (command == null) {
            conn.send("500 Command not recognized");

            return;
        }

        command.execute(conn, state, manager, currentLine);
    }

    protected boolean commandLegalSize() {
        if (currentLine.length() < 4) {
            conn.send("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (currentLine.length() > 4 &&
                currentLine.charAt(4) != ' ') {
            conn.send("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (currentLine.length() > 1000) {
            conn.send("500 Command too long.  1000 character maximum.");

            return false;
        }

        return true;
    }

    @Override
    public void close() {
        if (log.isTraceEnabled()) {
            final StringBuilder msg = new StringBuilder("Closing SMTP(s) handler connection");
            if (null != socket) {
                msg.append(' ').append(socket.getInetAddress()).append(':')
                        .append(socket.getPort());
            }
            log.trace(msg.toString());
        }
        quitting = true;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch(IOException ignored) {
            //empty
        }
    }
}
