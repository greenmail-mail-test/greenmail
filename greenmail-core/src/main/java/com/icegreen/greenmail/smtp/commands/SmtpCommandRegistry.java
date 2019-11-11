/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry.Command;


public class SmtpCommandRegistry {
    public enum Command { AUTH, HELO, EHLO, NOOP, RSET, QUIT, MAIL, RCPT, DATA, VRFY };

    public static final Map<Command, SmtpCommand> DEFAULT_COMMANDS;

    static {
    	Map<Command, SmtpCommand> defaultCommands = new HashMap<>();
		defaultCommands.put(Command.AUTH, new AuthCommand());
		defaultCommands.put(Command.HELO, new HeloCommand());
		defaultCommands.put(Command.EHLO, new HeloCommand());
		defaultCommands.put(Command.NOOP, new NoopCommand());
		defaultCommands.put(Command.RSET, new RsetCommand());
		defaultCommands.put(Command.QUIT, new QuitCommand());
		defaultCommands.put(Command.MAIL, new MailCommand());
		defaultCommands.put(Command.RCPT, new RcptCommand());
		defaultCommands.put(Command.DATA, new DataCommand());
		defaultCommands.put(Command.VRFY, new VrfyCommand());
		DEFAULT_COMMANDS = Collections.unmodifiableMap(new HashMap<>(defaultCommands));
    }

    private final Map<Command, SmtpCommand> commands;
    
    public SmtpCommandRegistry() {
    	commands = DEFAULT_COMMANDS;
	}
    
    public SmtpCommandRegistry(Map<Command, SmtpCommand> commands) {
    	this.commands = Collections.unmodifiableMap(commands);
    }
    
    public SmtpCommand getCommand(String name) {
		Command value;
		try {
			value = Command.valueOf(name);
		} catch (IllegalArgumentException iae) {
			return null;
		}
        return commands.get(value);
    }
    
    public SmtpCommand getCommand(Command command) {
        return commands.get(command);
    }
}