/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import java.util.HashMap;
import java.util.Map;


public class SmtpCommandRegistry {
    private static Map commands = new HashMap();
    private static Object[][] COMMANDS = new Object[][]
    {
        {"HELO", new HeloCommand()}, {"EHLO", new HeloCommand()},
        {"NOOP", new NoopCommand()}, {"RSET", new RsetCommand()},
        {"QUIT", new QuitCommand()}, {"MAIL", new MailCommand()},
        {"RCPT", new RcptCommand()}, {"DATA", new DataCommand()},
        {"VRFY", new VrfyCommand()}
    };

    public void load()
            throws Exception {
        for (Object[] COMMAND : COMMANDS) {
            String name = COMMAND[0].toString();

            if (commands.containsKey(name)) {
                continue;
            }

            try {
                SmtpCommand command = (SmtpCommand) COMMAND[1];
                registerCommand(name, command);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    private void registerCommand(String name, SmtpCommand command) {
        commands.put(name, command);
    }

    public SmtpCommand getCommand(String name) {
        if (commands.size() == 0) {
            try {
                load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (SmtpCommand) commands.get(name);
    }
}