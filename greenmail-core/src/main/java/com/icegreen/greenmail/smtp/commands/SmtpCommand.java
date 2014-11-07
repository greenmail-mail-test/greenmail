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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class SmtpCommand {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public abstract void execute(SmtpConnection conn,
                                 SmtpState state,
                                 SmtpManager manager,
                                 String commandLine)
            throws IOException;
}
