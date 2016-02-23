/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.user;

import com.icegreen.greenmail.imap.ImapHostManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UserManager {
    /**
     * User list by their trimmed, lowercased user names
     */
    private Map<String, GreenMailUser> loginToUser = Collections.synchronizedMap(new HashMap<String, GreenMailUser>());
    private Map<String, GreenMailUser> emailToUser = Collections.synchronizedMap(new HashMap<String, GreenMailUser>());
    private ImapHostManager imapHostManager;
    private boolean authRequired = true;
    private final Logger log = LoggerFactory.getLogger(UserManager.class);


    public UserManager(ImapHostManager imapHostManager) {
        this.imapHostManager = imapHostManager;
    }

    public GreenMailUser getUser(String login) {
        return loginToUser.get(normalizerUserName(login));
    }

    public GreenMailUser getUserByEmail(String email) {
        return emailToUser.get(normalizerUserName(email));
    }

    public GreenMailUser createUser(String email, String login, String password) throws UserException {
        GreenMailUser user = new UserImpl(email, login, password, imapHostManager);
        user.create();
        addUser(user);
        return user;
    }

    public void addUser(GreenMailUser user) {
        deleteUser(user);
        loginToUser.put(normalizerUserName(user.getLogin()), user);
        emailToUser.put(normalizerUserName(user.getEmail()), user);
    }

    public void deleteUser(GreenMailUser user) {
        GreenMailUser deletedUser = loginToUser.remove(normalizerUserName(user.getLogin()));
        if (deletedUser != null) {
            emailToUser.remove(normalizerUserName(deletedUser.getEmail()));
            deletedUser.delete();
        }
    }

    public Collection<GreenMailUser> listUser() {
        return Collections.unmodifiableCollection(loginToUser.values());
    }

    public boolean test(String userID, String password) {
        if(authRequired) {
            GreenMailUser u = getUser(userID);
            return null != u && u.getPassword().equals(password);
        }

        try {
            if(!userExists(userID)) {
                createUser(userID, userID, "");
            }
        } catch (UserException e) {
            log.error("Failed to create user with userid=" + userID,e);

        }
        return true;
    }

    public void setAuthRequired(boolean auth) {
        authRequired = auth;
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

    public boolean userExists(String userid) {
        return loginToUser.containsKey(userid) || emailToUser.containsKey(userid);
    }
}