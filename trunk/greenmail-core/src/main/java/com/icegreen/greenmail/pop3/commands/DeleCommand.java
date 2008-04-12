/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.foedus.util.MsgRangeFilter;

import java.util.List;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;

import javax.mail.Flags;


public class DeleCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            MailFolder inbox = state.getFolder();
            String[] cmdLine = cmd.split(" ");

            String msgNumStr = cmdLine[1];
            List msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false));
            if (msgList.size() != 1) {
                conn.println("-ERR no such message");

                return;
            }

            SimpleStoredMessage msg = (SimpleStoredMessage) msgList.get(0);
            Flags flags = msg.getFlags();

            if (flags.contains(Flags.Flag.DELETED)) {
                conn.println("-ERR message already deleted");

                return;
            }

            flags.add(Flags.Flag.DELETED);

            conn.println("+OK message scheduled for deletion");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}