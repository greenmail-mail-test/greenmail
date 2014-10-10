/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

import java.io.IOException;

public abstract class SmtpCommand {

    public abstract void execute(SmtpConnection conn,
                                 SmtpState state,
                                 SmtpManager manager,
                                 String commandLine)
            throws IOException;
}
