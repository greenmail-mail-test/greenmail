/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.user;

import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.imap.ImapHostManager;

import javax.mail.internet.MimeMessage;
import java.io.Serializable;


public class UserImpl implements GreenMailUser, Serializable {
    String email;
    String login;
    String password;
    private ImapHostManager imapHostManager;

    public UserImpl(String email, String login, String password, ImapHostManager imapHostManager) {
        this.email = email;
        this.login = login;
        this.password = password;
        this.imapHostManager = imapHostManager;
    }

    public void create()
            throws UserException {
        try {

            imapHostManager.createPrivateMailAccount(this);
        } catch (Exception me) {
            throw new UserException(me);
        }
    }

    public void delete()
            throws UserException {
//        try {
//            imapHostManager.destroyMailbox(this);
//        } catch (MailboxException me) {
//            throw new UserException(me);
//        }
    }

    public void deliver(MovingMessage msg)
            throws UserException {
        try {
            imapHostManager.getInbox(this).store(msg);
        } catch (Exception me) {
            throw new UserException(me);
        }
    }

    public void deliver(MimeMessage msg)
            throws UserException {
        try {
            imapHostManager.getInbox(this).store(msg);
        } catch (Exception me) {
            throw new UserException(me);
        }
    }

    public String getEmail() {
        return email;
    }

    public String getLogin() {
        if (null == login) {
            return email;
        }
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void authenticate(String pass)
            throws UserException {
        if (!password.equals(pass))
            throw new UserException("Invalid password");
    }

    public String getQualifiedMailboxName() {
        return String.valueOf(email.hashCode());
    }

    public int hashCode() {
        return email.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof UserImpl) || (null == o)) {
            return false;
        }
        UserImpl that = (UserImpl) o;
        return this.email.equals(that.email);
    }
}