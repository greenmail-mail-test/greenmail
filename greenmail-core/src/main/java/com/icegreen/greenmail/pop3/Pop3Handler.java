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
import com.icegreen.greenmail.server.AbstractSocketProtocolHandler;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.user.UserManager;


public class Pop3Handler extends AbstractSocketProtocolHandler {
    Pop3CommandRegistry registry;
    Pop3Connection conn;
    UserManager manager;
    Pop3State state;
    String currentLine;

    public Pop3Handler(Pop3CommandRegistry registry,
                       UserManager manager, Socket socket) {
        super(socket);
        this.registry = registry;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            conn = new Pop3Connection(this, socket);
            state = new Pop3State(manager);

            sendGreetings();

            while (!isQuitting()) {
                handleCommand();
            }

            conn.close();
        } catch (SocketTimeoutException ste) {
            conn.println("421 Service shutting down and closing transmission channel " +
                "(socket timeout, SO_TIMEOUT: " + getSoTimeout() + "ms)");
            conn.quit();
        } catch (Exception e) {
            if (!isQuitting()) {
                log.error("Can not handle POP3 connection", e);
                throw new IllegalStateException("Can not handle POP3 connection", e);
            }
        } finally {
            close();
        }
    }

    void sendGreetings() {
        conn.println("+OK POP3 GreenMail Server v" + BuildInfo.INSTANCE.getProjectVersion() + " ready");
    }

    void handleCommand() throws IOException {
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
}
