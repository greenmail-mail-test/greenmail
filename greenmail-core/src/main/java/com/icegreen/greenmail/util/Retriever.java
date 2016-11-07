/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import com.icegreen.greenmail.server.AbstractServer;

import javax.mail.*;
import java.util.*;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Apr 16, 2005
 */
public class Retriever implements AutoCloseable {
    private AbstractServer server;
    private Store store;

    /**
     * Creates a retriever object for a particular server<br>
     * Example:<br>
     * <i>
     * GreenMail greenMail = new GreenMail();<br>
     * ...<br>
     * Retriever r = new Retriever(greenMail.getPop3())<br>;
     * r.getMessageEntries("bill@microsoft.com");<br>
     * </i>
     * This will fetch all available messages for Billy using POP3.
     *
     * @param server the POP3 or IMAP server
     */
    public Retriever(AbstractServer server) {
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

            return messages.toArray(new Message[messages.size()]);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the underlying store.
     * Make sure you finished processing any fetched messages before closing!
     *
     * @deprecated use {@link #close()} instead.
     * @since 1.5
     */
    @Deprecated
    public void logout() {
        close();
    }

    /**
     * Closes the underlying store.
     * Make sure you finished processing any fetched messages before closing!
     *
     * @since 1.5
     */
    @Override
    public void close() {
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
