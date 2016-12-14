/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public class MailAddress {
    String host;
    String user;
    String email;
    String name;

    static final Pattern RFC6531_VALIDATION_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    public MailAddress(String str)
            throws AddressException {

        // Decoding the mail address in
        // case it contains non us-ascii characters
        String decoded = decodeStr(str);

        assertValidEmailAddress(decoded);
        InternetAddress address = new InternetAddress();
        address.setAddress(decoded);
        email = address.getAddress();
        name = address.getPersonal();

        String[] strs = email.split("@");
        user = strs[0];
        if (strs.length>1) {
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

    private void assertValidEmailAddress(String str) {

        if (str == null || !RFC6531_VALIDATION_PATTERN.matcher(str).matches()) {
            new AddressException("Invalid email address!");
        }
    }

    /**
     * Returns the decoded string, in case it contains non us-ascii characters.
     * Returns the same string if it doesn't or the passed value in case
     * of an UnsupportedEncodingException.
     *
     * @param str string to be decoded
     * @return the decoded string, in case it contains non us-ascii characters;
     *  or the same string if it doesn't or the passed value in case
     *  of an UnsupportedEncodingException.
     */
    private String decodeStr(String str) {

        try {
            return MimeUtility.decodeText(str);
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }
}