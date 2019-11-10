/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * A mail message with all of the extra stuff that IMAP requires.
 * This is just a placeholder object, while I work out what's really required. A common
 * way of handling *all* messages needs to be available for James (maybe MovingMessage?)
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class StoredMessage {
    private UidAwareMimeMessage mimeMessage;
    private Date receivedDate;
    private long uid;
    private SimpleMessageAttributes attributes;

    /**
     * Wraps a mime message and provides support for uid.
     * Required for searching.
     *
     * @see com.icegreen.greenmail.imap.commands.SearchTermBuilder.UidSearchTerm
     */
    public static class UidAwareMimeMessage extends MimeMessage {
        private long uid;
        private Date receivedDate;

        public UidAwareMimeMessage(MimeMessage source, long uid, Date receivedDate) throws MessagingException {
            super(source);
            this.uid = uid;
            this.receivedDate = receivedDate;
        }

        @Override
        public Date getReceivedDate() {
            return receivedDate;
        }

        /**
         * @return the UID.
         */
        public long getUid() {
            return uid;
        }

        /**
         * Updates the MSN.
         *
         * @param messageNumber the MSN.
         */
        public void updateMessageNumber(int messageNumber) {
            setMessageNumber(messageNumber);
        }
    }

    StoredMessage(MimeMessage mimeMessage,
            Date receivedDate, long uid) {
        this.receivedDate = receivedDate;
        this.uid = uid;
        try {
            this.mimeMessage = new UidAwareMimeMessage(mimeMessage, uid, receivedDate);
            this.attributes = new SimpleMessageAttributes(mimeMessage, receivedDate);
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not parse mime message " + mimeMessage + " with uid " + uid, e);
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

    public MailMessageAttributes getAttributes() {
        return attributes;
    }

    /**
     * Updates the MSN.
     *
     * @param messageNumber the MSN.
     */
    public void updateMessageNumber(int messageNumber) {
        mimeMessage.updateMessageNumber(messageNumber);
    }
}
