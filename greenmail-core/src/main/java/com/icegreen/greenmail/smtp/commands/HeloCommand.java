/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;


/**
 * EHLO/HELO command.
 * <p/>
 * TODO: What does HELO do if it's already been called before?
 * <p/>
 * <a https://tools.ietf.org/html/rfc2821#section-4.1.1.1">RFC2821</a>
 * <a href="https://tools.ietf.org/html/rfc4954">RFC4954</a>
 * <a href="https://tools.ietf.org/html/rfc2554">RFC2554</a>
 */
public class HeloCommand
        extends SmtpCommand {
    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        extractHeloName(conn, commandLine);
        state.clearMessage();
        conn.send("250-" + conn.getServerGreetingsName());
        conn.send("250 AUTH "+AuthCommand.SUPPORTED_AUTH_MECHANISM);
    }

    private void extractHeloName(SmtpConnection conn,
                                 String commandLine) {
        String heloName;

        if (commandLine.length() > 5)
            heloName = commandLine.substring(5);
        else
            heloName = null;

        conn.setHeloName(heloName);
    }
}