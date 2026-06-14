/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MAIL command.
 * <p>
 * <a href="https://tools.ietf.org/html/rfc2821.html#section-4.1.1.2">
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.2</a>
 */
public class MailCommand
        extends SmtpCommand {
    // "MAIL FROM:" ("<>" / Reverse-Path)
    //                      [SP Mail-parameters] CRLF
    static final Pattern PARAM = Pattern.compile("MAIL FROM:\\s?<([^>]*)>(.*)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        Matcher m = PARAM.matcher(commandLine);
        if (m.matches()) {
            String from = m.group(1);
            String parameters = m.group(2);

            MailAddress fromAddr = null;
            if (!from.isEmpty()) {
                fromAddr = new MailAddress(from);
                String err = manager.checkSender(state, fromAddr);
                if (err != null) {
                    conn.send(err);
                    return;
                }
            }

            state.clearMessagePreservingAuthenticationState();
            MovingMessage msg = state.getMessage();
            if (fromAddr != null) {
                msg.setReturnPath(fromAddr);
            }

            if (parameters != null && parameters.toUpperCase().contains("SMTPUTF8")) {
                msg.setSmtpUtf8(true);
            }

            conn.send("250 OK");
        } else {
            conn.send("501 Required syntax: 'MAIL FROM:<email@host>'");
        }
    }
}
