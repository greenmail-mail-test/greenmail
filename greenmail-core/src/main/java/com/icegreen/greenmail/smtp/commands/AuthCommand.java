/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp.commands;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;
import com.icegreen.greenmail.smtp.auth.AuthenticationState;
import com.icegreen.greenmail.smtp.auth.LoginAuthenticationState;
import com.icegreen.greenmail.smtp.auth.PlainAuthenticationState;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.SaslMessage;

import java.io.IOException;
import java.util.Arrays;

/**
 * AUTH command.
 * <p>
 * <pre>
 * Supported: PLAIN, LOGIN
 * AUTH mechanism [initial-response]
 * or
 * AUTH LOGIN [initial-response]
 * </pre>
 * <a href="https://tools.ietf.org/html/rfc4954">RFC4954</a>
 * <a href="https://tools.ietf.org/html/rfc2554">RFC2554</a>
 * <a href="http://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml">SASL mechanisms</a>
 * <a href="https://datatracker.ietf.org/doc/draft-murchison-sasl-login/">SASL LOGIN</a>
 */
public class AuthCommand extends SmtpCommand {
    public static final String AUTH_SUCCEDED = "235 2.7.0  Authentication Succeeded";
    public static final String AUTH_CREDENTIALS_INVALID = "535 5.7.8  Authentication credentials invalid";
    public static final String AUTH_ALREADY_AUTHENTICATED = "503 already authenticated";
    public static final String SMTP_SYNTAX_ERROR = "500 syntax error";
    public static final String SMTP_SERVER_CONTINUATION = "334 "; /* with space !!! */

    public enum AuthMechanism {
        PLAIN,
        LOGIN
    }

    public static final String SUPPORTED_AUTH_MECHANISM = getValuesWsSeparated();

    @Override
    public void execute(SmtpConnection conn, SmtpState state,
                        SmtpManager manager, String commandLine) throws IOException {
        if (conn.isAuthenticated()) {
            conn.send(AUTH_ALREADY_AUTHENTICATED);
            return;
        }

        //  AUTH mechanism [initial-response]
        final String[] commandParts = commandLine.split(" ");

        if (commandParts.length < 2) {
            conn.send(SMTP_SYNTAX_ERROR + " : expected mechanism but received <" + commandLine + ">");
            return;
        }

        // Check auth mechanism
        final String authMechanismValue = commandParts[1];
        if (AuthMechanism.LOGIN.name().equalsIgnoreCase(authMechanismValue)) {
            authLogin(conn, state, manager, commandLine, commandParts, authMechanismValue);
        } else if (AuthMechanism.PLAIN.name().equalsIgnoreCase(authMechanismValue)) {
            authPlain(conn, state, manager, commandParts);
        } else {
            conn.send(SMTP_SYNTAX_ERROR + " : Unsupported auth mechanism " + authMechanismValue +
                ". Only auth mechanism <" + Arrays.toString(AuthMechanism.values()) + "> supported.");
        }
    }

    private void authPlain(SmtpConnection conn, SmtpState state, SmtpManager manager, String[] commandParts) throws IOException {
        // Continuation?
        String initialResponse;
        if (commandParts.length == 2) {
            conn.send(SMTP_SERVER_CONTINUATION);
            initialResponse = conn.readLine();
        } else {
            initialResponse = commandParts[2];
        }

        SaslMessage saslMessage = parseInitialResponse(initialResponse);
        AuthenticationState authenticationContext = new PlainAuthenticationState(saslMessage);
        state.getMessage().setAuthenticationState(authenticationContext);

        if (authenticate(manager.getUserManager(), saslMessage)) {
            conn.setAuthenticated(true);
            conn.send(AUTH_SUCCEDED);
        } else {
            conn.send(AUTH_CREDENTIALS_INVALID);
        }
    }

    private void authLogin(SmtpConnection conn, SmtpState state, SmtpManager manager, String commandLine,
            String[] commandParts, String authMechanismValue) throws IOException {
        // https://www.samlogic.net/articles/smtp-commands-reference-auth.htm
        if (commandParts.length != 2) {
            conn.send(SMTP_SYNTAX_ERROR + " : Unsupported auth mechanism " + authMechanismValue +
                " with unexpected values. Line is: <" + commandLine + ">");
        } else {
            conn.send(SMTP_SERVER_CONTINUATION + "VXNlcm5hbWU6"); // "Username:"
            String username = conn.readLine();
            conn.send(SMTP_SERVER_CONTINUATION + "UGFzc3dvcmQ6"); // "Password:"
            String pwd = conn.readLine();
            String plainUsername = EncodingUtil.decodeBase64(username);
            String plainPwd = EncodingUtil.decodeBase64(pwd);
            AuthenticationState authenticationContext = new LoginAuthenticationState(plainUsername, plainPwd);
            state.getMessage().setAuthenticationState(authenticationContext);

            if (manager.getUserManager().test(plainUsername, plainPwd)) {
                conn.setAuthenticated(true);
                conn.send(AUTH_SUCCEDED);
            } else {
                conn.send(AUTH_CREDENTIALS_INVALID);
            }
        }
    }

    private boolean authenticate(UserManager userManager, SaslMessage saslMessage) {
        return userManager.test(saslMessage.getAuthcid(), saslMessage.getPasswd());
    }

    private SaslMessage parseInitialResponse(String initialReponse) {
        String value = EncodingUtil.decodeBase64(initialReponse);
        // authorization-id\0authentication-id\0passwd
        return SaslMessage.parse(value);
    }

    private static String getValuesWsSeparated() {
        StringBuilder buf = new StringBuilder();
        for (AuthMechanism mechanism : AuthMechanism.values()) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(mechanism);
        }
        return buf.toString();
    }
}
