/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;


/**
 * MAIL command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="https://tools.ietf.org/html/rfc2821.html#section-4.1.1.2">
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.2</a>
 * </p>
 */
public class MailCommand
        extends SmtpCommand {
    // "MAIL FROM:" ("<>" / Reverse-Path)
    //                      [SP Mail-parameters] CRLF
    static final Pattern PARAM = Pattern.compile("MAIL FROM:\\s?<([^>]*)>.*",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        Matcher m = PARAM.matcher(commandLine);
        if (m.matches()) {
            String from = m.group(1);

            if (!from.isEmpty()) {
                MailAddress fromAddr = new MailAddress(from);
                String err = manager.checkSender(state, fromAddr);
                if (err != null) {
                    conn.send(err);
                    return;
                }
                state.clearMessage();
                state.getMessage().setReturnPath(fromAddr);
                conn.send("250 OK");
            } else {
                state.clearMessage();
                state.getMessage();
                conn.send("250 OK");
            }

        } else {
            conn.send("501 Required syntax: 'MAIL FROM:<email@host>'");
        }
    }
}