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
 * VRFY command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.6">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.6</a>.
 * </p>
 */
public class VrfyCommand
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        conn.println("252 Cannot VRFY user, but will accept message and attempt delivery");
    }
}