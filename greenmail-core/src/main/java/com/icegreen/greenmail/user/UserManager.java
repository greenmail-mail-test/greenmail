/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.user;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icegreen.greenmail.imap.ImapHostManager;

public class UserManager {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);
    /**
     * User list by their trimmed, lower-cased user names
     */
    private Map<String, GreenMailUser> loginToUser = new ConcurrentHashMap<>();
    private Map<String, GreenMailUser> emailToUser = new ConcurrentHashMap<>();
    private ImapHostManager imapHostManager;
    private boolean authRequired = true;

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

    public boolean test(String userId, String password) {
        log.debug("Authenticating user {}", userId);
        GreenMailUser u = getUser(userId);

        if (!authRequired) {
            if (null == u) { // Auto create user
                try {
                    createUser(userId, userId, password);
                } catch (UserException e) {
                    throw new IllegalStateException("Failed to create user with userid=" + userId, e);
                }
            }

            return true; // always authenticate successfully, if no auth required
        }

        return null != u && checkPassword(u.getPassword(), password);
    }

    private boolean checkPassword(String expectedPassword, String password) {
        return (null != expectedPassword && expectedPassword.equals(password))
                || (null == password && expectedPassword == null);
    }

    public void setAuthRequired(boolean auth) {
        authRequired = auth;
    }

    public boolean isAuthRequired() {
        return authRequired;
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

    /**
     * Checks if user exists.
     *
     * @param userId the user id, which can be an email or the login.
     * @return true, if user exists.
     */
    public boolean hasUser(String userId) {
        String normalized = normalizerUserName(userId);
        return loginToUser.containsKey(normalized) || emailToUser.containsKey(normalized);
    }
}