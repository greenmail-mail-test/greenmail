/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class MailAddress {
    String host;
    String user;
    String email;
    String name;

    public MailAddress(String str)
            throws AddressException {
        InternetAddress address = new InternetAddress(str);
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

    public String toString() {
        return email;
    }

    public String getEmail() {
        return email;
    }

}