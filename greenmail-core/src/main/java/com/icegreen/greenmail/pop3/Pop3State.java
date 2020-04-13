/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.NoSuchUserException;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;


public class Pop3State {
    UserManager manager;
    GreenMailUser user;
    MailFolder inbox;
    private ImapHostManager imapHostManager;

    public Pop3State(UserManager manager) {
        this.manager = manager;
        this.imapHostManager = manager.getImapHostManager();
    }

    public GreenMailUser getUser() {
        return user;
    }

    public GreenMailUser getUser(String username) throws UserException {
        GreenMailUser user = manager.getUser(username);
        if (null == user) {
            throw new NoSuchUserException("User <" + username + "> doesn't exist");
        }
        return user;
    }

    public void setUser(GreenMailUser user) {
        this.user = user;
    }

    public boolean isAuthenticated() {
        return inbox != null;
    }

    public void authenticate(String pass)
            throws UserException, FolderException {
        if (user == null)
            throw new UserException("No user selected");

        if (manager.isAuthRequired()) {
            user.authenticate(pass);
        }
        inbox = imapHostManager.getInbox(user);
    }

    public MailFolder getFolder() {
        return inbox;
    }

    public GreenMailUser findOrCreateUser(String username) throws UserException {
        if (manager.hasUser(username)) {
            return manager.getUser(username);
        }
        if (!manager.isAuthRequired()) {
            return manager.createUser(username, username, username);
        }
        throw new UserException("Unable to find or create user '" + username +"'");
    }

}
