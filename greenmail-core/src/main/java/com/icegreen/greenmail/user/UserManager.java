/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.user;

import com.icegreen.greenmail.imap.ImapHostManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserManager {
    /**
     * User list by their trimmed, lowercased user names
     */
    private Map<String, GreenMailUser> _users = Collections.synchronizedMap(new HashMap<String, GreenMailUser>());
    private ImapHostManager imapHostManager;

    public UserManager(ImapHostManager imapHostManager) {
        this.imapHostManager = imapHostManager;
    }

    public GreenMailUser getUser(String login) {
        return _users.get(normalizerUserName(login));
    }

    public GreenMailUser getUserByEmail(String email) {
        return getUser(email);
    }

    public GreenMailUser createUser(String email, String login, String password) throws UserException {
        GreenMailUser user = new UserImpl(email, login, password, imapHostManager);
        user.create();
        addUser(user);
        return user;
    }

    private void addUser(GreenMailUser user) {
        deleteUser(user);
        _users.put(normalizerUserName(user.getLogin()), user);
    }

    public void deleteUser(GreenMailUser user) {
        user = _users.remove(normalizerUserName(user.getLogin()));
        if (user != null) {
            user.delete();
        }
    }

    public boolean test(String userid, String password) {
        GreenMailUser u = getUser(userid);
        return null != u && u.getPassword().equals(password);

    }

    public ImapHostManager getImapHostManager() {
        return imapHostManager;
    }

    /**
     * Normalize the user name (to lowercase, trim)
     *
     * @param login Login name
     * @return Normalized name
     */
    private static String normalizerUserName(String login) {
        return login.trim().toLowerCase(Locale.ENGLISH);
    }
}