/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.util.GreenMailUtil;

import javax.mail.Flags;
import java.io.StringReader;
import java.util.List;


public class RetrCommand
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

            StoredMessage msg = (StoredMessage) msgList.get(0);
            String email = GreenMailUtil.getWholeMessage(msg.getMimeMessage());
            conn.println("+OK");
            conn.print(new StringReader(email));
            conn.println();
            conn.println(".");
            msg.setFlag(Flags.Flag.SEEN, true);
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}