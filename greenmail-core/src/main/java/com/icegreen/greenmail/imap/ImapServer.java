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
import com.sun.mail.imap.IMAPStore;

import javax.mail.NoSuchProviderException;
import java.net.Socket;
import java.util.Properties;

public final class ImapServer extends AbstractServer {

    public ImapServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(Socket clientSocket) {
        return new ImapHandler(managers.getUserManager(), managers.getImapHostManager(), clientSocket);
    }

    /**
     * Creates IMAP specific JavaMail session properties.
     *
     * See https://javamail.java.net/nonav/docs/api/com/sun/mail/imap/package-summary.html for valid properties.
     *
     * @return the properties.
     */
    @Override
    protected Properties createProtocolSpecificSessionProperties() {
        return createDefaultSessionProperties();
    }

    @Override
    public IMAPStore createStore() throws NoSuchProviderException {
        return (IMAPStore) super.createStore();
    }
}