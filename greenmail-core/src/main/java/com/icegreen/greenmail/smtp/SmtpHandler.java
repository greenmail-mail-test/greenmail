/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.server.AbstractSocketProtocolHandler;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SmtpHandler extends AbstractSocketProtocolHandler {
    // protocol and configuration global stuff
    protected SmtpCommandRegistry registry;
    protected SmtpManager manager;

    // session stuff
    protected SmtpConnection conn;
    protected SmtpState state;

    // command parsing stuff
    protected String currentLine;

    public SmtpHandler(SmtpCommandRegistry registry,
                       SmtpManager manager, Socket socket) {
        super(socket);
        this.registry = registry;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            conn = new SmtpConnection(this, socket);
            state = new SmtpState();

            sendGreetings();

            while (!isQuitting()) {
                handleCommand();
            }
        } catch (SocketTimeoutException ste) {
            conn.send("421 Service shutting down and closing transmission channel " +
                "(socket timeout, SO_TIMEOUT: " + getSoTimeout() + "ms)");
            conn.quit();
        } catch (Exception e) {
            // Closing socket on blocked read
            if (!isQuitting()) {
                throw new IllegalStateException("Unexpected error handling connection", e);
            }
        } finally {
            if (null != state) {
                state.clearMessage();
            }
            close();
        }
    }

    protected void sendGreetings() {
        conn.send("220 " + conn.getServerGreetingsName() +
            " GreenMail SMTP Service v" + BuildInfo.INSTANCE.getProjectVersion() + " ready");
    }

    protected void handleCommand()
        throws IOException {
        currentLine = conn.readLine();

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
}
