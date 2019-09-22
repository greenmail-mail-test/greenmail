/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import com.icegreen.greenmail.util.GreenMailUtil;

import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.LinkedList;
import java.util.List;


/**
 * Contains information for delivering a mime email.
 */
public class MovingMessage {
    private MailAddress returnPath;
    private List<MailAddress> toAddresses = new LinkedList<>();
    private MimeMessage message;
    private String content;

    public List<MailAddress> getToAddresses() {
        return toAddresses;
    }

    public MimeMessage getMessage() {
        return message;
    }

    public Reader getContent() {
        return new StringReader(content);
    }

    public void releaseContent() {
        content = "";
    }

    public MailAddress getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(MailAddress fromAddress) {
        this.returnPath = fromAddress;
    }

    public void addRecipient(MailAddress s) {
        toAddresses.add(s);
    }

    public void removeRecipient(MailAddress s) {
        toAddresses.remove(s);
    }

    /**
     * Reads the contents of the stream until
     * &lt;CRLF&gt;.&lt;CRLF&gt; is encountered.
     * <p/>
     * <p/>
     * It would be possible and perhaps desirable to prevent the
     * adding of an unnecessary CRLF at the end of the message, but
     * it hardly seems worth 30 seconds of effort.
     * </p>
     */
    public void readDotTerminatedContent(BufferedReader in)
            throws IOException {
        StringBuilder buf = new StringBuilder();

        while (true) {
            String line = in.readLine();
            if (line == null)
                throw new EOFException("Did not receive <CRLF>.<CRLF>");

            if (".".equals(line)) {
                break;
            } else if (line.startsWith(".")) {
                println(buf, line.substring(1));
            } else {
                println(buf, line);
            }
        }
        content = buf.toString();
        try {
            message = GreenMailUtil.newMimeMessage(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void println(StringBuilder buf, String line) {
        buf.append(line).append("\r\n");
    }
}