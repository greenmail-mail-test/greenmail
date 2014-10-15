/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;


public class ApopCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        conn.println("-ERR APOP not supported");

        /*
                try
                {
                    String[] arguments = cmd.split(" ");
                    String username = arguments[1];
                    state.setUser(state.getUser(username));
                    conn.send("+OK");
                }
                catch (MailboxException me)
                {
                    getLogger().warn("APOP exception", me);
                    conn.send("-ERR " + me);
                }
                catch (UserException nsue)
                {
                    getLogger().warn("APOP exception", nsue);
                    conn.send("-ERR " + nsue);
                }
        */
    }
}