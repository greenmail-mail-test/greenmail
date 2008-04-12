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
 * QUIT command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.10">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.10</a>.
 * </p>
 */
public class QuitCommand
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        state.clearMessage();
        conn.println("221 " + conn.getServerGreetingsName() +
                " Service closing transmission channel");
        conn.quit();
    }
}