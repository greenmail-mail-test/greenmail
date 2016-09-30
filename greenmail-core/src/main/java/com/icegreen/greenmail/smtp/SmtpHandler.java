/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icegreen.greenmail.foedus.util.InMemoryWorkspace;
import com.icegreen.greenmail.foedus.util.Workspace;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.smtp.commands.SSLSmtpCommandRegistry;
import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.util.DummySSLSocketFactory;

class SmtpHandler implements ProtocolHandler {
    private static final String STARTTLS = "STARTTLS";

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

    public SmtpHandler(SmtpCommandRegistry registry, SmtpManager manager, InMemoryWorkspace workspace, Socket socket) {
        _registry = registry;
        _manager = manager;
        _workspace = workspace;
        _socket = socket;
    }

    private void initConnection() throws IOException {

        _conn = new SmtpConnection(this, _socket);
        _state = new SmtpState(_workspace);
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
            if (!_quitting) {
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
        if (this._currentLine.length() < 4) {
        	_conn.send("500 Invalid command. Must be at least 4 characters");
        	return;
        }
        String commandName = _currentLine.toUpperCase().startsWith(STARTTLS) ? STARTTLS : _currentLine.substring(0, 4).toUpperCase();

        SmtpCommand command = _registry.getCommand(commandName);

        if (command == null) {
            _conn.send("500 Command not recognized");

            return;
        }

        command.execute(_conn, _state, _manager, _currentLine);
        if (_currentLine.equalsIgnoreCase(STARTTLS)) {
            _socket = createSslExchangeSocket();
            initConnection();
            _registry = new SSLSmtpCommandRegistry(); // This registry doesn't contain an STARTTLS command
        }
    }

    private SSLSocket createSslExchangeSocket() throws IOException {
        final SSLSocket sslSocket = (SSLSocket) 
        		((SSLSocketFactory) DummySSLSocketFactory.getDefault())
        			.createSocket(_socket, _socket.getLocalAddress().getHostName(), _socket.getPort(), true);
        sslSocket.setUseClientMode(false);
        sslSocket.startHandshake();
        return sslSocket;
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