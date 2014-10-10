/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import java.util.HashMap;
import java.util.Map;


public class Pop3CommandRegistry {
    private final static Map<String, Pop3Command> commands = new HashMap<String, Pop3Command>();

    public void load() {
        commands.put("QUIT", new QuitCommand());
        commands.put("STAT", new StatCommand());
        commands.put("APOP", new ApopCommand());
        commands.put("USER", new UserCommand());
        commands.put("PASS", new PassCommand());
        commands.put("LIST", new ListCommand());
        commands.put("UIDL", new UidlCommand());
        commands.put("TOP", new TopCommand());
        commands.put("RETR", new RetrCommand());
        commands.put("DELE", new DeleCommand());
        commands.put("NOOP", new NoopCommand());
        commands.put("RSET", new RsetCommand());
    }

    public Pop3Command getCommand(String name) {
        if (commands.size() == 0) {
            load();
        }
        return commands.get(name);
    }
}