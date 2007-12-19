/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.user.GreenMailUser;


public class PassCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        GreenMailUser user = state.getUser();
        if (user == null) {
            conn.println("-ERR USER required");

            return;
        }

        String[] args = cmd.split(" ");
        if (args.length < 2) {
            conn.println("-ERR Required syntax: PASS <username>");

            return;
        }

        try {
            String pass = args[1];
            state.authenticate(pass);
            conn.println("+OK");
        } catch (Exception e) {
            conn.println("-ERR Authentication failed: " + e);
        }
    }
}