/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.regex.Pattern;

public class MailAddress {
    String host;
    String user;
    String email;
    String name;

    static final Pattern RFC6531_VALIDATION_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    public MailAddress(String str)
            throws AddressException {
        assertValidEmailAddress(str);
        InternetAddress address = new InternetAddress();
        address.setAddress(str);
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

}