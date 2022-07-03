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

import com.icegreen.greenmail.smtp.auth.AuthenticationState;
import com.icegreen.greenmail.smtp.auth.LoginAuthenticationState;
import com.icegreen.greenmail.smtp.auth.PlainAuthenticationState;


/**
 * Contains information for delivering a mime email.
 */
public class MovingMessage {
    private AuthenticationState authenticationState;
    private MailAddress returnPath;
    private final List<MailAddress> toAddresses = new LinkedList<>();
    private MimeMessage message;

    /**
     * Retrieves the state object with the data used for authentication. Currently
     * {@link PlainAuthenticationState PLAIN} and {@link LoginAuthenticationState LOGIN}
     * authentication is supported. You can use this, for example, to retrieve the username
     * or password that was sent by the client.
     *
     * Note that this will return {@code null} when no authentication was performed or needed.
     *
     * @return The state used by the AUTH command, if any.
     */
    public AuthenticationState getAuthenticationState() {
        return authenticationState;
    }

    /**
     * Retrieves the state object with the data used for authentication. Currently
     * {@link PlainAuthenticationState PLAIN} and {@link LoginAuthenticationState LOGIN}
     * authentication is supported. You can use this, for example, to retrieve the username
     * or password that was sent by the client.
     *
     * Note that this will return {@code null} when no authentication was performed or needed.
     */
    public void setAuthenticationState(AuthenticationState authenticationState) {
        this.authenticationState = authenticationState;
    }

    /**
     * Retrieves the addresses from which the email was sent. Note that these are
     * the {@code RCPT TO} addresses from the SMTP envelope, not the {@code TO}
     * addresses from the mail header.
     * @return The address to which the mail is directed.
     */
    public List<MailAddress> getToAddresses() {
        return toAddresses;
    }

    /**
     * Retrieves the contents of the mail message, including all mail headers and the body.
     * @return The message that was sent.
     */
    public MimeMessage getMessage() {
        return message;
    }

    /**
     * Retrieves the address from which the email was sent. Note that this is the
     * {@code MAIL FROM} address from the SMTP envelope, not the {@code FROM}
     * address(es) from the mail header.
     * @return The address from which the email was sent.
     */
    public MailAddress getReturnPath() {
        return returnPath;
    }

    /**
     * Sets or overwrites the address from which the email was sent. Note that this is
     * the {@code MAIL FROM} address from the SMTP envelope, not the {@code FROM}
     * address(es) from the mail header.
     * @param fromAddress The address from which the email was sent.
     */
    public void setReturnPath(MailAddress fromAddress) {
        this.returnPath = fromAddress;
    }

    /**
     * Adds an address from which the email was sent. Note that these are the {@code RCPT TO}
     * addresses from the SMTP envelope, not the {@code TO} addresses from the mail header.
     */
    public void addRecipient(MailAddress s) {
        toAddresses.add(s);
    }

    /**
     * Removes an address from the list of addresses from which the email was sent. Note
     * that these are the {@code RCPT TO} addresses from the SMTP envelope, not the {@code TO}
     * addresses from the mail header.
     */
    public void removeRecipient(MailAddress s) {
        toAddresses.remove(s);
    }

    /**
     * Sets or overwrites the contents of the mail message, including all mail headers
     * and the body.
     * @param message The message that was sent.
     */
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
