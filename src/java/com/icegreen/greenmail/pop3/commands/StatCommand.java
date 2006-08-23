/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.mail.MailException;
import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;

import javax.mail.MessagingException;
import java.util.Iterator;
import java.util.List;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;


public class StatCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            MailFolder inbox = state.getFolder();
            List messages = inbox.getNonDeletedMessages();
            long size = sumMessageSizes(messages);
            conn.println("+OK " + messages.size() + " " + size);
        } catch (Exception me) {
            conn.println("-ERR " + me);
        }
    }

    long sumMessageSizes(List messages)
            throws MailException {
        long total = 0;

        for (Iterator i = messages.iterator(); i.hasNext();) {
            SimpleStoredMessage msg = (SimpleStoredMessage) i.next();
            try {
                total += msg.getMimeMessage().getSize();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        return total;
    }
}