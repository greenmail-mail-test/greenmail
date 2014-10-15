/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import com.icegreen.greenmail.AbstractServer;

import javax.mail.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Apr 16, 2005
 */
public class Retriever {
    public static final String PROTOCOL_POP3 = "pop3";
    public static final String PROTOCOL_POP3S = "pop3s";
    public static final String PROTOCOL_IMAP = "imap";
    public static final String PROTOCOL_IMAPS = "imaps";

    private String protocol;
    private int port;
    private String host;
    private Store store = null;

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
     * @param server
     */
    public Retriever(AbstractServer server) {
        this.protocol = server.getProtocol();
        port = server.getPort();
        host = server.getBindTo();
    }

    public Message[] getMessages(String account) throws Exception {
        return getMessages(account, account);
    }

    public Message[] getMessages(String account, String password) throws Exception {
        Properties props = new Properties();
        if (protocol.endsWith("s")) {
            props.put("mail.pop3.starttls.enable", Boolean.TRUE);
            props.put("mail.imap.starttls.enable", Boolean.TRUE);
        }
        props.setProperty("mail.imaps.socketFactory.class", DummySSLSocketFactory.class.getName());
        props.setProperty("mail.pop3s.socketFactory.class", DummySSLSocketFactory.class.getName());
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.imaps.socketFactory.fallback", "false");
        props.setProperty("mail.pop3s.socketFactory.fallback", "false");

        final String timeout = "15000";
//        final String timeout = "150000";
        props.setProperty("mail.imap.connectiontimeout", timeout);
        props.setProperty("mail.imaps.connectiontimeout", timeout);
        props.setProperty("mail.pop3.connectiontimeout", timeout);
        props.setProperty("mail.pop3s.connectiontimeout", timeout);
        props.setProperty("mail.imap.timeout", timeout);
        props.setProperty("mail.imaps.timeout", timeout);
        props.setProperty("mail.pop3.timeout", timeout);
        props.setProperty("mail.pop3s.timeout", timeout);

        Session session = Session.getInstance(props, null);
//        session.setDebug(true);
        store = session.getStore(protocol);
        store.connect(host, port, account, password);
        Folder rootFolder = store.getFolder("INBOX");
        final List<Message> messages = getMessages(rootFolder);
        return messages.toArray(new Message[messages.size()]);
    }

    public void logout() {
        try {
            store.close();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Message> getMessages(Folder folder) throws MessagingException {
        List<Message> ret = new ArrayList<Message>();
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
