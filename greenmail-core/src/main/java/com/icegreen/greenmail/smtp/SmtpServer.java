/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.foedus.util.InMemoryWorkspace;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.smtp.commands.SSLSmtpCommandRegistry;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.util.ServerSetup;

import java.net.Socket;

public class SmtpServer extends AbstractServer {
    public SmtpServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(final Socket clientSocket) {
    	// If we start with a plain SMTP connection we can allow STARTTLS as a command
    	SmtpCommandRegistry registry = this.setup.equals(ServerSetup.SMTPS) ? new SSLSmtpCommandRegistry() : new SmtpCommandRegistry();
        return new SmtpHandler(registry, managers.getSmtpManager(),
                new InMemoryWorkspace(), clientSocket);
    }
}