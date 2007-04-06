/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
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
        for (int i = 0; i < COMMANDS.length; i++) {
            String name = COMMANDS[i][0].toString();

            if (commands.containsKey(name))

                continue;

            try {
                SmtpCommand command = (SmtpCommand) COMMANDS[i][1];
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