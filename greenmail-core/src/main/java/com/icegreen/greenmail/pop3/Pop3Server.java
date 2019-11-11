/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.pop3.POP3Store; // NOSONAR

import javax.mail.NoSuchProviderException;
import java.net.Socket;

public class Pop3Server extends AbstractServer {

	private final Pop3CommandRegistry pop3CommandRegistry;
	
    public Pop3Server(ServerSetup setup, Managers managers) {
        super(setup, managers);
        this.pop3CommandRegistry = new Pop3CommandRegistry();
    }

    public Pop3Server(ServerSetup setup, Managers managers, Pop3CommandRegistry pop3CommandRegistry) {
        super(setup, managers);
        this.pop3CommandRegistry = pop3CommandRegistry;
    }

    @Override
    protected ProtocolHandler createProtocolHandler(final Socket clientSocket) {
        return new Pop3Handler(pop3CommandRegistry, managers.getUserManager(), clientSocket);
    }

    @Override
    public POP3Store createStore() throws NoSuchProviderException {
        return (POP3Store) super.createStore();
    }
}