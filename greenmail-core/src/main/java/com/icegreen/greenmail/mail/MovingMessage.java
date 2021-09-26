/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import javax.mail.internet.MimeMessage;
import java.util.LinkedList;
import java.util.List;


/**
 * Contains information for delivering a mime email.
 */
public class MovingMessage {
    private MailAddress returnPath;
    private final List<MailAddress> toAddresses = new LinkedList<>();
    private MimeMessage message;

    public List<MailAddress> getToAddresses() {
        return toAddresses;
    }

    public MimeMessage getMessage() {
        return message;
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

    public void setMimeMessage(MimeMessage message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "MovingMessage{" +
            "toAddresses=" + toAddresses +
            ", returnPath=" + returnPath +
            ", message=" + message +
            '}';
    }
}
