/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import com.icegreen.greenmail.foedus.util.Resource;
import com.icegreen.greenmail.foedus.util.Workspace;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.InternetPrintWriter;

import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.LinkedList;
import java.util.List;


/**
 * Contains information for delivering a mime email.
 * <p/>
 * <p/>
 * Since a MovingMessage many be passed through many queues and
 * handlers before it can be safely deleted, destruction it handled
 * by reference counting. When an object first obtains a reference
 * to a MovingMessage, it should immediately call {@link #acquire()}.
 * As soon as it has finished processing, that object must call
 * {@link #releaseContent()}.
 * </p>
 */
public class MovingMessage {
    private MailAddress returnPath;
    private List<MailAddress> toAddresses = new LinkedList<>();
    private Workspace workspace;
    private Resource content;
    private MimeMessage message;
    private int references = 0;

    public MovingMessage(Workspace workspace) {
        this.workspace = workspace;
    }

    public List<MailAddress> getToAddresses() {
        return toAddresses;
    }

    public MimeMessage getMessage() {
        return message;
    }

    public Reader getContent()
            throws IOException {

        return content.getReader();
    }

    public void acquire() {
        references++;
    }

    public void releaseContent() {
        if (references > 0) {
            references--;
        } else if (content != null) {
            workspace.release(content);
            content = null;
        }
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
        content = workspace.getTmpFile();
        Writer data = content.getWriter();
        PrintWriter dataWriter = new InternetPrintWriter(data);

        while (true) {
            String line = in.readLine();
            if (line == null)
                throw new EOFException("Did not receive <CRLF>.<CRLF>");

            if (".".equals(line)) {
                dataWriter.close();

                break;
            } else if (line.startsWith(".")) {
                dataWriter.println(line.substring(1));
            } else {
                dataWriter.println(line);
            }
        }
        try {
            message = GreenMailUtil.newMimeMessage(content.getAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}