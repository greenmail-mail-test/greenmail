/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;


/**
 * EHLO/HELO command.
 * <p/>
 * <p/>
 * TODO: What does HELO do if it's already been called before?
 * </p>
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.1">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.1 </a>.
 * </p>
 */
public class HeloCommand
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        extractHeloName(conn, commandLine);
        state.clearMessage();
        conn.println("250 " + conn.getServerGreetingsName());
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