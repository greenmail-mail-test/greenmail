/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

import javax.mail.internet.AddressException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MAIL command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.2">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.2</a>.
 * </p>
 */
public class MailCommand
        extends SmtpCommand {
    static final Pattern param = Pattern.compile("MAIL FROM:\\s?<(.*)>",
            Pattern.CASE_INSENSITIVE);

    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        Matcher m = param.matcher(commandLine);
        try {
            if (m.matches()) {
                String from = m.group(1);

                MailAddress fromAddr = new MailAddress(from);

                String err = manager.checkSender(state, fromAddr);
                if (err != null) {
                    conn.println(err);

                    return;
                }

                state.clearMessage();
                state.getMessage().setReturnPath(fromAddr);
                conn.println("250 OK");
            } else {
                conn.println("501 Required syntax: 'MAIL FROM:<email@host>'");
            }
        } catch (AddressException e) {
            conn.println("501 Malformed email address. Use form email@host");
        }
    }
}