/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.foedus.util.Workspace;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

class SmtpHandler implements ProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(SmtpHandler.class);

    // protocol and configuration global stuff
    SmtpCommandRegistry _registry;
    SmtpManager _manager;
    Workspace _workspace;

    // session stuff
    SmtpConnection _conn;
    SmtpState _state;

    // command parsing stuff
    boolean _quitting;
    String _currentLine;
    private Socket _socket;

    public SmtpHandler(SmtpCommandRegistry registry,
                       SmtpManager manager, Workspace workspace, Socket socket) {
        _registry = registry;
        _manager = manager;
        _workspace = workspace;
        _socket = socket;
    }

    @Override
    public void run() {
        try {
            _conn = new SmtpConnection(this, _socket);
            _state = new SmtpState(_workspace);
            _quitting = false;

            sendGreetings();

            while (!_quitting) {
                handleCommand();
            }

        } catch (SocketTimeoutException ste) {
            _conn.send("421 Service shutting down and closing transmission channel");

        } catch (Exception e) {
            // Closing socket on blocked read
            if(!_quitting) {
                log.error("Unexpected error handling connection, quitting=", e);
                throw new IllegalStateException(e);
            }
        } finally {
            if (null != _state) {
                _state.clearMessage();
            }
        }
    }

    protected void sendGreetings() {
        _conn.send("220 " + _conn.getServerGreetingsName() +
                " GreenMail SMTP Service Ready at port " + _conn.sock.getLocalPort());
    }

    protected void handleCommand()
            throws IOException {
        _currentLine = _conn.receiveLine();

        if (_currentLine == null) {
            close();

            return;
        }

        // eliminate invalid line lengths before parsing
        if (!commandLegalSize()) {

            return;
        }

        String commandName = _currentLine.substring(0, 4).toUpperCase();

        SmtpCommand command = _registry.getCommand(commandName);

        if (command == null) {
            _conn.send("500 Command not recognized");

            return;
        }

        command.execute(_conn, _state, _manager, _currentLine);
    }

    private boolean commandLegalSize() {
        if (_currentLine.length() < 4) {
            _conn.send("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (_currentLine.length() > 4 &&
                _currentLine.charAt(4) != ' ') {
            _conn.send("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (_currentLine.length() > 1000) {
            _conn.send("500 Command too long.  1000 character maximum.");

            return false;
        }

        return true;
    }

    @Override
    public void close() {
        if (log.isTraceEnabled()) {
            final StringBuilder msg = new StringBuilder("Closing SMTP(s) handler connection");
            if (null != _socket) {
                msg.append(' ').append(_socket.getInetAddress()).append(':')
                        .append(_socket.getPort());
            }
            log.trace(msg.toString());
        }
        _quitting = true;
        try {
            if (_socket != null && !_socket.isClosed()) {
                _socket.close();
            }
        } catch(IOException ignored) {
            //empty
        }
    }
}