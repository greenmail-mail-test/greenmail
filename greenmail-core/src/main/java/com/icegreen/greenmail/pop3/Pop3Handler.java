/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

import com.icegreen.greenmail.pop3.commands.Pop3Command;
import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pop3Handler implements ProtocolHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    Pop3CommandRegistry registry;
    Pop3Connection conn;
    UserManager manager;
    Pop3State state;
    boolean quitting;
    String currentLine;
    private Socket socket;

    public Pop3Handler(Pop3CommandRegistry registry,
                       UserManager manager, Socket socket) {
        this.registry = registry;
        this.manager = manager;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            conn = new Pop3Connection(this, socket);
            state = new Pop3State(manager);

            quitting = false;

            sendGreetings();

            while (!quitting) {
                handleCommand();
            }

            conn.close();
        } catch (SocketTimeoutException ste) {
            conn.println("421 Service shutting down and closing transmission channel");
        } catch (Exception e) {
            if (!quitting) {
                log.error("Can not handle POP3 connection", e);
                throw new IllegalStateException("Can not handle POP3 connection", e);
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioe) {
                // Nothing
            }
        }

    }

    void sendGreetings() {
        conn.println("+OK POP3 GreenMail Server v" + BuildInfo.INSTANCE.getProjectVersion() + " ready");
    }

    void handleCommand()
            throws IOException {
        currentLine = conn.readLine();

        if (currentLine == null) {
            close();

            return;
        }

        String commandName = new StringTokenizer(currentLine, " ").nextToken().toUpperCase();

        Pop3Command command = registry.getCommand(commandName);

        if (command == null) {
            conn.println("-ERR Command not recognized");
            return;
        }

        if (!command.isValidForState(state)) {
            conn.println("-ERR Command not valid for this state");
            return;
        }

        command.execute(conn, state, currentLine);
    }

    @Override
    public void close() {
        quitting = true;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
            //empty
        }
    }
}