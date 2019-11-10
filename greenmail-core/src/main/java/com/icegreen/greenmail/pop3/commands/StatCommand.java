/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

import javax.mail.MessagingException;
import java.util.List;


public class StatCommand
        extends Pop3Command {
    @Override
    public boolean isValidForState(Pop3State state) {
        return state.isAuthenticated();
    }

    @Override
    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            MailFolder inbox = state.getFolder();
            List<StoredMessage> messages = inbox.getNonDeletedMessages();
            long size = sumMessageSizes(messages);
            conn.println("+OK " + messages.size() + " " + size);
        } catch (Exception me) {
            conn.println("-ERR " + me);
        }
    }

    long sumMessageSizes(List<StoredMessage> messages) {
        long total = 0;

        for (StoredMessage msg : messages) {
            try {
                total += msg.getMimeMessage().getSize();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        return total;
    }
}