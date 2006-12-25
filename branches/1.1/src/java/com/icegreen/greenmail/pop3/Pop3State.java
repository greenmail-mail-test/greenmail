/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.user.NoSuchUserException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.imap.ImapHostManager;


public class Pop3State {
    UserManager _manager;
    GreenMailUser _user;
    Store store;
    MailFolder _inbox;
    private ImapHostManager imapHostManager;

    public Pop3State(UserManager manager) {
        _manager = manager;
        this.imapHostManager = manager.getImapHostManager();

    }

    public GreenMailUser getUser() {

        return _user;
    }

    public GreenMailUser getUser(String username) throws UserException {
        GreenMailUser user = _manager.getUser(username);
        if (null == user) {
            throw new NoSuchUserException(username + " doesn't exist");
        }
        return user;
    }

    public void setUser(GreenMailUser user)
            throws UserException {
        _user = user;
    }

    public boolean isAuthenticated() {
        return _inbox != null;
    }

    public void authenticate(String pass)
            throws UserException, FolderException {
        if (_user == null)
            throw new UserException("No user selected");

        _user.authenticate(pass);
        _inbox = imapHostManager.getInbox(_user);
    }

    public MailFolder getFolder() {

        return _inbox;
    }
}