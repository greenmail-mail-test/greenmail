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
 * QUIT command.
 * <p/>
 * <p/>
 * The spec is at <a
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.10">
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.10</a>.
 * </p>
 * <p>"QUIT" CRLF</p>
 */
public class QuitCommand
        extends SmtpCommand {
    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) {
        state.clearMessage();
        conn.send("221 " + conn.getServerGreetingsName() +
                " Service closing transmission channel");
        conn.quit();
    }
}