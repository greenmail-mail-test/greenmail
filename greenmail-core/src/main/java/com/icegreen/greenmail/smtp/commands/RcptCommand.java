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
 * RCPT command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="https://tools.ietf.org/html/rfc2821.html#section-4.1.1.3">
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.3</a>.
 * </p>
 * <p>
 * "RCPT TO:" ("<Postmaster@" domain ">" / "<Postmaster>" / Forward-Path)
 *            [SP Rcpt-parameters] CRLF
 * </p>
 */
public class RcptCommand
        extends SmtpCommand {
    static Pattern param = Pattern.compile("RCPT TO:\\s?<([^>]+)>",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        Matcher m = param.matcher(commandLine);

        if (m.matches()) {
            if (state.getMessage().getReturnPath() != null) {
                String to = m.group(1);

                MailAddress toAddr = new MailAddress(to);

                String err = manager.checkRecipient(state, toAddr);
                if (err != null) {
                    conn.send(err);

                    return;
                }

                state.getMessage().addRecipient(toAddr);
                conn.send("250 OK");
            } else {
                conn.send("503 MAIL must come before RCPT");
            }
        } else {
            conn.send("501 Required syntax: 'RCPT TO:<email@host>'");
        }
    }
}