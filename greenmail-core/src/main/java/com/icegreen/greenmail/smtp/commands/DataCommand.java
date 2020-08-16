/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.foedus.util.StreamUtils;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


/**
 * DATA command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="https://tools.ietf.org/html/rfc2821.html#section-4.1.1.4">
 * https://tools.ietf.org/html/rfc2821.html#section-4.1.1.4</a>.
 * </p>
 */
public class DataCommand extends SmtpCommand {
    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine)
            throws IOException {
        MovingMessage msg = state.getMessage();

        if (msg.getReturnPath() == null) {
            conn.send("503 MAIL command required");
            return;
        }

        if (msg.getToAddresses().isEmpty()) {
            conn.send("503 RCPT command(s) required");
            return;
        }

        conn.send("354 Start mail input; end with <CRLF>.<CRLF>");

        String value = "Return-Path: <" + msg.getReturnPath() +
                ">\r\n" + "Received: from " +
                conn.getClientAddress() + " (HELO " +
                conn.getHeloName() + "); " +
                new java.util.Date() + "\r\n";

        msg.readDotTerminatedContent(new BufferedReader(StreamUtils.splice(new StringReader(value),
                conn.getReader())));

        String err = manager.checkData(state);
        if (err != null) {
            conn.send(err);

            return;
        }

        try {
            conn.send("250 OK");
            manager.send(state);
        } catch (Exception je) {
            log.error("Can not send state '250 OK', aborted.", je);
            conn.send("451 Requested action aborted: local error in processing");
        }

        state.clearMessage();
    }
}