/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import java.util.HashMap;
import java.util.Map;


public class SmtpCommandRegistry {
    private static final Map<String, SmtpCommand> commands = new HashMap<>();

    static {
        commands.put("AUTH", new AuthCommand());
        commands.put("HELO", new HeloCommand());
        commands.put("EHLO", new HeloCommand());
        commands.put("NOOP", new NoopCommand());
        commands.put("RSET", new RsetCommand());
        commands.put("QUIT", new QuitCommand());
        commands.put("MAIL", new MailCommand());
        commands.put("RCPT", new RcptCommand());
        commands.put("DATA", new DataCommand());
        commands.put("VRFY", new VrfyCommand());
    }

    public SmtpCommand getCommand(String name) {
        return commands.get(name);
    }
}