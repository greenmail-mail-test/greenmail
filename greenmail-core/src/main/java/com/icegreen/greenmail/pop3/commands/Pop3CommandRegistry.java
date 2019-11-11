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
	public enum Command {
		QUIT, STAT, APOP, USER, PASS, LIST, UIDL, TOP, RETR, DELE, NOOP, RSET, CAPA
	}

	public static final Map<Command, Pop3Command> DEFAULT_COMMANDS;

	static {
		Map<Command, Pop3Command> defaultCommands = new HashMap<>();
		defaultCommands.put(Command.QUIT, new QuitCommand());
		defaultCommands.put(Command.STAT, new StatCommand());
		defaultCommands.put(Command.APOP, new ApopCommand());
		defaultCommands.put(Command.USER, new UserCommand());
		defaultCommands.put(Command.PASS, new PassCommand());
		defaultCommands.put(Command.LIST, new ListCommand());
		defaultCommands.put(Command.UIDL, new UidlCommand());
		defaultCommands.put(Command.TOP, new TopCommand());
		defaultCommands.put(Command.RETR, new RetrCommand());
		defaultCommands.put(Command.DELE, new DeleCommand());
		defaultCommands.put(Command.NOOP, new NoopCommand());
		defaultCommands.put(Command.RSET, new RsetCommand());
		defaultCommands.put(Command.CAPA, new CapaCommand());
		DEFAULT_COMMANDS = Collections.unmodifiableMap(new HashMap<>(defaultCommands));
	}

	private final Map<Command, Pop3Command> commands;

	public Pop3CommandRegistry() {
		commands = DEFAULT_COMMANDS;
	}

	public Pop3CommandRegistry(Map<Command, Pop3Command> commands) {
		this.commands = Collections.unmodifiableMap(commands);
	}

	public Pop3Command getCommand(String name) {
		Command value;
		try {
			value = Command.valueOf(name);
		} catch (IllegalArgumentException iae) {
			return null;
		}
		return commands.get(value);
	}

	public Pop3Command getCommand(Command name) {
		return commands.get(name);
	}
}