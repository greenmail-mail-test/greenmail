package com.icegreen.greenmail.smtp.auth;

import com.icegreen.greenmail.smtp.commands.AuthCommand.AuthMechanism;
import com.icegreen.greenmail.util.SaslMessage;

/**
 * Details from the {@link AuthMechanism#PLAIN} authorization mechanism, when
 * that mechanism was used for authentication.
 */
public class PlainAuthenticationState implements AuthenticationState, UsernameAuthentication {
    private final String authorizationId;
    private final String authenticationId;
    private final String password;

    /**
     * @param saslMessage The parsed message sent by the client with the {@code AUTH} command.
     */
    public PlainAuthenticationState(SaslMessage saslMessage) {
        this(saslMessage.getAuthzid(), saslMessage.getAuthcid(), saslMessage.getPasswd());
    }

    @Override
    public String getType() {
        return AuthMechanism.PLAIN.name();
    }

    /**
     * @param authorizationId The authorization ID sent by the client with the {@code AUTH} command.
     * @param authenticationId The authentication ID sent by the client with the {@code AUTH} command.
     * @param password The password sent by the client with the {@code AUTH} command.
     */
    public PlainAuthenticationState(String authorizationId, String authenticationId, String password) {
        this.authorizationId = authorizationId;
        this.authenticationId = authenticationId;
        this.password = password;
    }

    /**
     * @return The authorization ID sent by the client with the {@code AUTH} command.
     */
    public String getAuthorizationId() {
        return authorizationId;
    }

    /**
     * @return The authentication ID sent by the client with the {@code AUTH} command.
     */
    public String getAuthenticationId() {
        return authenticationId;
    }

    /**
     * @return password The password sent by the client with the {@code AUTH} command.
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return authenticationId;
    }
}
