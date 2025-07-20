/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.SaslMessage;
import com.icegreen.greenmail.util.SaslXoauth2Message;

import java.io.IOException;
import java.util.Arrays;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc1734">AUTH</a> command for POP3.
 * SASL: PLAIN XOAUTH2
 * <p>
 * <a href="https://tools.ietf.org/html/rfc5034">...</a>
 * <p>
 * AUTH mechanism [initial-response]
 * <ul>
 *     <li>mechanism LOGIN: See <a href="https://tools.ietf.org/html/rfc4616">rfc4616</a></li>
 *     <li>mechanism XOAUTH@: See <a href="https://tools.ietf.org/html/rfc4616">rfc4616</a></li>
 * </ul>
 */
public class AuthCommand
        extends Pop3Command {

    public static final String CONTINUATION = "+ ";

    @Override
    public boolean isValidForState(Pop3State state) {
        return !state.isAuthenticated();
    }

    public enum Pop3SaslAuthMechanism {
        PLAIN,
        XOAUTH2;

        /**
         * Returns WS-separated list of the supported auth mechanism.
         *
         * @return a list of supported auth mechanism.
         */
        static String list() {
            StringBuilder buf = new StringBuilder();
            for (Pop3SaslAuthMechanism mechanism : values()) {
                if (buf.length() > 0) {
                    buf.append(' ');
                }
                buf.append(mechanism.name());
            }
            return buf.toString();
        }
    }

    @Override
    public void execute(Pop3Connection conn, Pop3State state, String cmd) {
        if (state.isAuthenticated()) {
            conn.println("-ERR Already authenticated");
            return;
        }

        String[] args = cmd.split(" ");
        if (args.length < 2) {
            conn.println("-ERR Required syntax: AUTH mechanism [initial-response] but received <" + cmd + ">");
            return;
        }

        String mechanism = args[1];
        if (Pop3SaslAuthMechanism.PLAIN.name().equalsIgnoreCase(mechanism)) {
            authPlain(conn, state, args);
        } else if (Pop3SaslAuthMechanism.XOAUTH2.name().equalsIgnoreCase(mechanism)) {
            authXoauth2(conn, state, args);
        } else {
            conn.println("-ERR Required syntax: AUTH mechanism <" + mechanism +
                "> not supported, expected one of " + Arrays.toString(Pop3SaslAuthMechanism.values()));
        }
    }

    private void authPlain(Pop3Connection conn, Pop3State state, String[] args) {
        // https://tools.ietf.org/html/rfc4616
        String initialResponse = initialResponse(conn, args);
        if (null == initialResponse) {
            conn.println("-ERR Invalid syntax, expected initial-response : AUTH PLAIN [initial-response]");
            return;
        }

        // authorization-id\0authentication-id\0passwd
        final SaslMessage saslMessage;
        try {
            saslMessage = SaslMessage.parse(EncodingUtil.decodeBase64(initialResponse));
        } catch(IllegalArgumentException ex) { // Invalid Base64
            log.error("Expected base64 encoding but got <{}>", initialResponse, ex); /* GreenMail is just a test server */
            conn.println("-ERR Authentication failed, expected base64 encoding : " + ex.getMessage() );
            return;
        }

        authenticateUser(conn, state, saslMessage.getAuthcid(), saslMessage.getPasswd());
    }

    private void authXoauth2(Pop3Connection conn, Pop3State state, String[] args) {
        // args: [AUTH, XOAUTH2, <xoauth2-auth-string>]
        String initialResponse = initialResponse(conn, args);
        if (null == initialResponse) {
            conn.println("-ERR Invalid syntax, expected initial-response : AUTH XOAUTH2 [initial-response]");
            return;
        }

        SaslXoauth2Message xoauth2Message;
        try {
            xoauth2Message = SaslXoauth2Message.parseBase64Encoded(initialResponse);
        } catch(IllegalArgumentException ex) { // Invalid encoding?
            log.error("Expected XOAUTH2 format but got <{}>", initialResponse, ex); /* GreenMail is just a test server */
            conn.println("-ERR Authentication failed, expected XOAUTH2 format : " + ex.getMessage() );
            return;
        }

        authenticateUser(conn, state, xoauth2Message.getUsername(), xoauth2Message.getAccessToken());
    }

    private String initialResponse(Pop3Connection conn, String[] args) {
        if (args.length == 2 || args.length == 3 && "=".equals(args[2])) { // Continuation?
            conn.println(CONTINUATION);
            try {
                return conn.readLine();
            } catch (IOException e) {
                conn.println("-ERR Invalid syntax, expected continuation with initial-response");
                return null;
            }
        } else if (args.length == 3) {
            return args[2];
        } else {
            return null;
        }
    }

    private void authenticateUser(Pop3Connection conn, Pop3State state, String username, String credentials) {
        GreenMailUser user;
        try {
            user = state.getUser(username);
            state.setUser(user);
        } catch (UserException e) {
            log.error("Can not get user <{}>",username , e);
            conn.println("-ERR Authentication failed: " + e.getMessage() /* GreenMail is just a test server */);
            return;
        }

        try {
            state.authenticate(credentials);
            conn.println("+OK");
        } catch (UserException e) {
            log.error("Can not authenticate using user <{}>", user.getLogin(), e);
            conn.println("-ERR Authentication failed: " + e.getMessage());
        } catch (FolderException e) {
            log.error("Can not authenticate using user {}, internal error", user, e);
            conn.println("-ERR Authentication failed, internal error: " + e.getMessage());
        }
    }
}
