/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPStore; // NOSONAR

import javax.mail.NoSuchProviderException;
import java.net.Socket;

public final class ImapServer extends AbstractServer {

    public ImapServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(Socket clientSocket) {
        if (null != setup.getImapHandler()) {
            return setup.getImapHandler().init(managers.getUserManager(), managers.getImapHostManager(), clientSocket);
        } else {
            return new ImapHandlerImpl().init(managers.getUserManager(), managers.getImapHostManager(), clientSocket);
        }
    }

    @Override
    public IMAPStore createStore() throws NoSuchProviderException {
        return (IMAPStore) super.createStore();
    }
}