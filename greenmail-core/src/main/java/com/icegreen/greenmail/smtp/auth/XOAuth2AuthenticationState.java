package com.icegreen.greenmail.smtp.auth;

import com.icegreen.greenmail.smtp.commands.AuthCommand;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class XOAuth2AuthenticationState implements AuthenticationState, UsernameAuthentication{

    private final String username;
    private final String accessToken;

    /**
     * @param authenticationString base64("user=" {User} "^Aauth=Bearer " {Access Token} "^A^A")
     */
    public XOAuth2AuthenticationState(String authenticationString) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(authenticationString);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            String[] parts = decodedString.split("\\u0001");
            if (parts.length > 2 || !parts[0].startsWith("user=") || !parts[1].startsWith("auth=Bearer ")) {
                throw new IllegalArgumentException("Invalid authentication string format");
            }

            this.username = parts[0].substring(5);
            this.accessToken = parts[1].substring(12);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 or malformed input: " + e.getMessage());
        }
    }


    @Override
    public String getType() {
        return AuthCommand.AuthMechanism.XOAUTH2.name();
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public String getAccessToken() {
        return this.accessToken;
    }
}
