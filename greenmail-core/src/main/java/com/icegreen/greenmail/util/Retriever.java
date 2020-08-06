/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.mail.*;

import com.icegreen.greenmail.server.AbstractServer;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Apr 16, 2005
 */
public class Retriever implements AutoCloseable {
    private final AbstractServer server;
    private Store store;

    /**
     * Creates a retriever object for a particular server<br>
     * Example:<br>
     * <i>
     * GreenMail greenMail = new GreenMail();<br>
     * ...<br>
     * Retriever r = new Retriever(greenMail.getPop3())<br>;
     * r.getMessages("bill@microsoft.com");<br>
     * </i>
     * This will fetch all available messages for Billy using POP3.
     *
     * @param server the POP3 or IMAP server
     */
    public Retriever(AbstractServer server) {
        if (null == server) {
            throw new IllegalArgumentException("Expected non null server argument");
        }
        if (!(server.getProtocol().startsWith(ServerSetup.PROTOCOL_IMAP)
                || server.getProtocol().startsWith(ServerSetup.PROTOCOL_POP3))) {
            throw new IllegalArgumentException("Requires a " + ServerSetup.PROTOCOL_POP3 + " or " +
                    ServerSetup.PROTOCOL_IMAP + " server but got " + server.getProtocol());
        }
        this.server = server;
    }

    public Message[] getMessages(String account) {
        return getMessages(account, account);
    }

    public Message[] getMessages(String account, String password) {
        try {
            store = server.createStore();

            store.connect(server.getBindTo(), server.getPort(), account, password);
            Folder rootFolder = store.getFolder("INBOX");
            final List<Message> messages = getMessages(rootFolder);

            // Fetch all UIDs
            FetchProfile fp = new FetchProfile();
            fp.add(UIDFolder.FetchProfileItem.UID);
            rootFolder.fetch(rootFolder.getMessages(), fp);

            return messages.toArray(new Message[0]);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the underlying store.
     * Make sure you finished processing any fetched messages before closing!
     *
     * @since 1.5
     */
    @Override
    public void close() {
        if (null != store)
            try {
                store.close();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
    }

    private List<Message> getMessages(Folder folder) throws MessagingException {
        List<Message> ret = new ArrayList<>();
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            Message[] messages = folder.getMessages();
            Collections.addAll(ret, messages);
        }
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            Folder[] f = folder.list();
            for (Folder aF : f) {
                ret.addAll(getMessages(aF));
            }
        }
        return ret;
    }
}
