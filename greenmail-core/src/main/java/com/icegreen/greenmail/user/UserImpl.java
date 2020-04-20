/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.user;

import java.util.Collection;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;



public class UserImpl implements GreenMailUser {
    String email;
    private final int cachedHashCode;
    private final String cachedHashCodeAsString;
    String login;
    String password;
    private ImapHostManager imapHostManager;

    public UserImpl(String email, String login, String password, ImapHostManager imapHostManager) {
        this.email = email;
        cachedHashCode = email.hashCode();
        cachedHashCodeAsString = String.valueOf(cachedHashCode);
        this.login = login;
        this.password = password;
        this.imapHostManager = imapHostManager;
    }

    @Override
    public void create() {
        try {
            imapHostManager.createPrivateMailAccount(this);
        } catch (FolderException e) {
            throw new IllegalStateException("Can not create user" + this, e);
        }
    }

    @Override
    public void delete() {
        try {
            Collection<MailFolder> mailfolders = imapHostManager.listMailboxes(this, "*");
            for(MailFolder mf : mailfolders) {
                imapHostManager.deleteMailbox(this, mf.getFullName());
            }
        } catch (FolderException | AuthorizationException e) {
            throw new IllegalStateException("Can not delete user mailboxes " + this, e);
        }
    }

    @Override
    public void deliver(MovingMessage msg) {
        try {
            imapHostManager.getInbox(this).store(msg);
        } catch (Exception e) {
            throw new IllegalStateException("Can not deliver " + msg + " for user " + this, e);
        }
    }

    @Override
    public void deliver(MimeMessage msg) {
        try {
            imapHostManager.getInbox(this).store(msg);
        } catch (Exception e) {
            throw new IllegalStateException("Can not deliver " + msg + " for user " + this, e);
        }
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getLogin() {
        if (null == login) {
            return email;
        }
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void authenticate(String pass) throws UserException {
        if (!password.equals(pass)) {
            throw new UserException("Invalid password");
        }
    }

    @Override
    public String getQualifiedMailboxName() {
        return cachedHashCodeAsString;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if ((null == o) || !(o instanceof UserImpl)) {
            return false;
        }
        UserImpl that = (UserImpl) o;
        return this.email.equals(that.email);
    }

    @Override
    public String toString() {
        return "UserImpl{" +
                "email='" + email + '\'' +
                ", login='" + login + '\'' +
                '}';
    }
}