/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;
import com.icegreen.greenmail.util.GreenMailUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

        String initialContent = "Return-Path: <" + msg.getReturnPath() +
            ">\r\n" + "Received: from " +
            conn.getClientAddress() + " (HELO " +
            conn.getHeloName() + "); " +
            new java.util.Date() + "\r\n";

        try (final InputStream messageIs = conn.dotLimitedInputStream(initialContent.getBytes(StandardCharsets.UTF_8))) {
            msg.setMimeMessage(GreenMailUtil.newMimeMessage(messageIs));
        }

        String err = manager.checkData(state);
        if (err != null) {
            conn.send(err);
            return;
        }

        try {
            manager.send(state);
            conn.send("250 OK");
        } catch (Exception je) {
            log.error("Can not send state '250 OK', aborted.", je);
            conn.send("451 Requested action aborted: local error in processing");
        }

        state.clearMessage();
    }
}
