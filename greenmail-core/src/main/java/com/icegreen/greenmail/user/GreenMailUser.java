/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.user;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.mail.MovingMessage;


public interface GreenMailUser {
    String getEmail();
    String getLogin();

    void deliver(MovingMessage msg);
    void deliver(MimeMessage msg);

    void create();

    void delete();

    String getPassword();

    void setPassword(String password);

    void authenticate(String password) throws UserException;

    String getQualifiedMailboxName();

    /**
     * Returns a textual representation of this user in a single line (without line-sep)
     *
     * @return - a single line representation of this user
     */
    String toSingleLine();

}