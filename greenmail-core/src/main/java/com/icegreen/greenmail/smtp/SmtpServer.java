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
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.util.ServerSetup;

import java.net.Socket;
import java.util.Properties;

public class SmtpServer extends AbstractServer {
    public SmtpServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(final Socket clientSocket) {
        return new SmtpHandler(new SmtpCommandRegistry(), managers.getSmtpManager(),
                new InMemoryWorkspace(), clientSocket);
    }

    /**
     * Creates SMTP specific JavaMail session properties.
     *
     * See https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html for valid properties.
     *
     * @return the properties.
     */
    @Override
    protected Properties createProtocolSpecificSessionProperties() {
        Properties props = createDefaultSessionProperties();

        // On Mac, somehow we need to set the smtp host for smtps.
        // Otherwise, JavaMail uses a default host 'localhost'
        if (setup.isSecure()) {
            props.setProperty("mail.smtp.host", String.valueOf(setup.getBindAddress()));
        }

        props.setProperty("mail.transport.protocol", setup.getProtocol());

        return props;
    }
}