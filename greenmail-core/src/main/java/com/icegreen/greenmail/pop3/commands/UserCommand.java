/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;

public class UserCommand
        extends Pop3Command {
    @Override
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    @Override
    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            String[] args = cmd.split(" ");
            if (args.length < 2) {
                conn.println("-ERR Required syntax: USER <username>");
                return;
            }

            String username = args[1];
            GreenMailUser user = state.findOrCreateUser(username);
            if (null == user) {
                conn.println("-ERR User '" + username + "' not found");
                return;
            }
            state.setUser(user);
            conn.println("+OK");
        } catch (UserException nsue) {
            conn.println("-ERR " + nsue);
        }
    }
}