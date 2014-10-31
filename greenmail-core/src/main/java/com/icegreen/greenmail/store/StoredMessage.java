/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * A mail message with all of the extra stuff that IMAP requires.
 * This is just a placeholder object, while I work out what's really required. A common
 * way of handling *all* messages needs to be available for James (maybe MovingMessage?)
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class StoredMessage {
    private MimeMessage mimeMessage;
    private Date receivedDate;
    private long uid;
    private SimpleMessageAttributes attributes;

    StoredMessage(MimeMessage mimeMessage,
                  Date receivedDate, long uid) {
        this.mimeMessage = mimeMessage;
        this.receivedDate = receivedDate;
        this.uid = uid;
        try {
            this.attributes = new SimpleMessageAttributes(mimeMessage, receivedDate);
        } catch (MessagingException e) {
            throw new RuntimeException("Could not parse mime message." + e.getMessage());
        }
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public Flags getFlags() {
        try {
            return getMimeMessage().getFlags();
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not access flags", e);
        }
    }

    public boolean isSet(Flags.Flag flag) {
        try {
            return getMimeMessage().isSet(flag);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not access flag " + flag, e);
        }
    }

    public void setFlag(Flags.Flag flag, boolean value) {
        try {
            getMimeMessage().setFlag(flag, value);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not set flag " + flag + " to " + value, e);
        }
    }

    public void setFlags(Flags flags, boolean value) {
        try {
            getMimeMessage().setFlags(flags, value);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not set flags " + flags + " to " + value, e);
        }
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public long getUid() {
        return uid;
    }

    public MailMessageAttributes getAttributes() throws FolderException {
        return attributes;
    }
}
