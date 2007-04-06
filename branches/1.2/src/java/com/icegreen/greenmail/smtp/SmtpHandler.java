/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.foedus.util.Workspace;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

class SmtpHandler extends Thread {

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
            _conn.println("421 Service shutting down and closing transmission channel");

        } catch (Exception e) {
        } finally {
            _state.clearMessage();
        }
    }

    protected void sendGreetings() {
        _conn.println("220 " + _conn.getServerGreetingsName() +
                " GreenMail SMTP Service Ready");
    }

    protected void handleCommand()
            throws IOException {
        _currentLine = _conn.readLine();

        if (_currentLine == null) {
            quit();

            return;
        }

        // eliminate invalid line lengths before parsing
        if (!commandLegalSize()) {

            return;
        }

        String commandName = _currentLine.substring(0, 4).toUpperCase();

        SmtpCommand command = _registry.getCommand(commandName);

        if (command == null) {
            _conn.println("500 Command not recognized");

            return;
        }

        command.execute(_conn, _state, _manager, _currentLine);
    }

    private boolean commandLegalSize() {
        if (_currentLine.length() < 4) {
            _conn.println("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (_currentLine.length() > 4 &&
                _currentLine.charAt(4) != ' ') {
            _conn.println("500 Invalid command. Must be 4 characters");

            return false;
        }

        if (_currentLine.length() > 1000) {
            _conn.println("500 Command too long.  1000 character maximum.");

            return false;
        }

        return true;
    }

    public void quit() {
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