/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.util.GreenMailUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


public class TopCommand
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
            String[] cmdLine = cmd.split(" ");
            if (cmdLine.length < 3)
                throw new IllegalArgumentException("range and line count required");

            String msgNumStr = cmdLine[1];
            List<StoredMessage> msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false));
            if (msgList.size() != 1) {
                conn.println("-ERR no such message");

                return;
            }

            StoredMessage msg = msgList.get(0);

            int numLines = Integer.parseInt(cmdLine[2]);

            try (BufferedReader in = new BufferedReader(
                    new StringReader(GreenMailUtil.getWholeMessage(msg.getMimeMessage())))) {
                conn.println("+OK");

                copyHeaders(in, conn);
                copyLines(in, conn, numLines);
            }
            conn.println(".");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }

    void copyHeaders(BufferedReader in, Pop3Connection conn)
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            conn.println(line);
            if (line.isEmpty())
                break;
        }
    }

    void copyLines(BufferedReader in, Pop3Connection conn,
                   int numLines)
            throws IOException {
        int count = 0;
        String line;
        while ((line = in.readLine()) != null && count < numLines) {
            conn.println(line);
            count++;
        }
    }
}
