/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class Pop3CommandRegistry {
    public static final Map<String, Pop3Command> DEFAULT_COMMANDS;
    private final Map<String, Pop3Command> commands;

    static {
    	Map<String, Pop3Command> defaultCommands = new HashMap<>();
        defaultCommands.put("QUIT", new QuitCommand());
        defaultCommands.put("STAT", new StatCommand());
        defaultCommands.put("APOP", new ApopCommand());
        defaultCommands.put("USER", new UserCommand());
        defaultCommands.put("PASS", new PassCommand());
        defaultCommands.put("LIST", new ListCommand());
        defaultCommands.put("UIDL", new UidlCommand());
        defaultCommands.put("TOP", new TopCommand());
        defaultCommands.put("RETR", new RetrCommand());
        defaultCommands.put("DELE", new DeleCommand());
        defaultCommands.put("NOOP", new NoopCommand());
        defaultCommands.put("RSET", new RsetCommand());
        defaultCommands.put("CAPA", new CapaCommand());
        DEFAULT_COMMANDS = Collections.unmodifiableMap(new HashMap<>(defaultCommands));
    }
    
    public Pop3CommandRegistry() {
    	commands = DEFAULT_COMMANDS;
	}
    
    public Pop3CommandRegistry(Map<String, Pop3Command> commands) {
    	this.commands = commands;
    }

    public Pop3Command getCommand(String name) {
        return commands.get(name);
    }
}