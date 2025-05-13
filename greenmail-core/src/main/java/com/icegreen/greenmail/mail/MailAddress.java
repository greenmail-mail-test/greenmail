/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;

public class MailAddress {
    final String host;
    final String user;
    final String email;
    final String name;

    public MailAddress(String str) {
        // Decoding the mail address in
        // case it contains non us-ascii characters
        String decoded = decodeStr(str);

        InternetAddress address = new InternetAddress();
        address.setAddress(decoded);
        email = address.getAddress();
        name = address.getPersonal();

        String[] strs = email.split("@");
        user = strs[0];
        if (strs.length > 1) {
            host = strs[1];
        } else {
            host = "localhost";
        }
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Returns the decoded string, in case it contains non us-ascii characters.
     * Returns the same string if it doesn't or the passed value in case
     * of an UnsupportedEncodingException.
     *
     * @param str string to be decoded
     * @return the decoded string, in case it contains non us-ascii characters;
     * or the same string if it doesn't or the passed value in case
     * of an UnsupportedEncodingException.
     */
    private String decodeStr(String str) {
        try {
            return MimeUtility.decodeText(str);
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * Compares this MailAddress object to another for equality.
     * Two MailAddress objects are considered equal if their email, host, and user parts
     * are equal, ignoring case.
     * <p>
     * The name field was not considered in the equality check.
     *
     * @param object The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {

        if (object instanceof MailAddress) {
            MailAddress otherMailAddress = (MailAddress) object;

            boolean emailsAreEqual = email.equalsIgnoreCase(otherMailAddress.getEmail());
            boolean hostsAreEqual = host.equalsIgnoreCase(otherMailAddress.getHost());
            boolean usersAreEqual = user.equalsIgnoreCase(otherMailAddress.getUser());

            return emailsAreEqual && hostsAreEqual && usersAreEqual;
        }

        return false;
    }

    /**
     * Computes the hash code for this MailAddress object.
     * The hash code is based on the name, host, user, and email fields.
     *
     * @return The hash code for this MailAddress object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            name != null ? name : "",
            host != null ? host : "",
            user != null ? user : "",
            email != null ? email : ""
        );
    }
}
