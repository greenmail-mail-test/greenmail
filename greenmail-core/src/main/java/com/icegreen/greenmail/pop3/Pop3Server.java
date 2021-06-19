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

    public Pop3Server(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(final Socket clientSocket) {
        return new Pop3Handler(new Pop3CommandRegistry(), managers.getUserManager(), clientSocket);
    }

    @Override
    public POP3Store createStore() throws NoSuchProviderException {
        return (POP3Store) super.createStore();
    }
}