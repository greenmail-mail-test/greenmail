/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.EncodingUtil;
import com.sun.mail.util.BASE64DecoderStream;

/**
 * SASL : PLAIN
 * <p>
 * https://tools.ietf.org/html/rfc5034
 * <p>
 * AUTH mechanism [initial-response]
 * mechanism PLAIN : See https://tools.ietf.org/html/rfc4616
 */
public class AuthCommand
        extends Pop3Command {

    public static final String CONTINUATION = "+ ";

    @Override
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    public enum Pop3SaslAuthMechanism {
        PLAIN;

        /**
         * WS separated list of supported auth mechanism.
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
            conn.println("-ERR Required syntax: AUTH mechanism [initial-response]");
            return;
        }

        String mechanism = args[1];
        if (Pop3SaslAuthMechanism.PLAIN.name().equalsIgnoreCase(mechanism)) {
            authPlain(conn, state, args);
        } else {
            conn.println("-ERR Required syntax: AUTH mechanism <" + mechanism +
                    "> not supported, expected one of " + Arrays.toString(Pop3SaslAuthMechanism.values()));
        }
    }

    private void authPlain(Pop3Connection conn, Pop3State state, String[] args) {
        // https://tools.ietf.org/html/rfc4616
        String initialResponse;
        if (args.length == 2 || args.length == 3 && "=".equals(args[2])) { // Continuation?
            conn.println(CONTINUATION);
            try {
                initialResponse = conn.readLine();
            } catch (IOException e) {
                conn.println("-ERR Invalid syntax, expected continuation with iniital-response");
                return;
            }
        } else if (args.length == 3) {
            initialResponse = args[2];
        } else {
            conn.println("-ERR Invalid syntax, expected initial-response : AUTH PLAIN [initial-response]");
            return;
        }

        // authorization-id\0authentication-id\0passwd
        final BASE64DecoderStream stream = new BASE64DecoderStream(
                new ByteArrayInputStream(initialResponse.getBytes(StandardCharsets.UTF_8)));
        readTillNullChar(stream); // authorizationId Not used
        String authenticationId = readTillNullChar(stream);

        GreenMailUser user;
        try {
            user = state.getUser(authenticationId);
            state.setUser(user);
        } catch (UserException e) {
            log.error("Can not get user <" + authenticationId + ">", e);
            conn.println("-ERR Authentication failed: " + e.getMessage() /* GreenMail is just a test server */);
            return;
        }

        try {
            state.authenticate(readTillNullChar(stream));
            conn.println("+OK");
        } catch (UserException e) {
            log.error("Can not authenticate using user <" + user.getLogin() + ">", e);
            conn.println("-ERR Authentication failed: " + e.getMessage());
        } catch (FolderException e) {
            log.error("Can not authenticate using user " + user + ", internal error", e);
            conn.println("-ERR Authentication failed, internal error: " + e.getMessage());
        }
    }


    @Deprecated // Remove once JDK baseline is 1.8
    private String readTillNullChar(BASE64DecoderStream stream) {
        try {
            return EncodingUtil.readTillNullChar(stream);
        } catch (IOException e) {
            log.error("Can not decode", e);
            return null;
        }
    }
}