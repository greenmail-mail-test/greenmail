package com.icegreen.greenmail.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Parses and holds the details of a SASL XOAUTH2 message.
 */
public class SaslXoauth2Message {
    private final String username;
    private final String accessToken;

    /**
     * Constructs a new SASL XOAUTH2 message.
     * @param username the username
     * @param accessToken the XOAUTH2 access token
     */
    public SaslXoauth2Message(String username, String accessToken) {
        if (username == null || accessToken == null) {
            throw new IllegalArgumentException("Username and access token must not be null");
        }
        this.username = username;
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Parses a bas64-encoded SASL XOAUTH@ mechanism message.
     * See {@link #parse(String)} for details.
     *
     * @param message the base64-encoded SASL message
     * @return the parsed SASL XOAUTH2 message
     * @throws IllegalArgumentException on invalid format or Base64 decoding errors
     */
    public static SaslXoauth2Message parseBase64Encoded(String message) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(message);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            return parse(decodedString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 or malformed input: " + e.getMessage());
        }
    }

    /**
     * Parses a SASL XOAUTH@ mechanism message.
     * See <a href="https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth#sasl-xoauth2">RFC 7628</a> for details.
     * <p>
     *     Message format: base64("user=" + userName + "^Aauth=Bearer " + accessToken + "^A^A")
     *     ^A represents a Control + A (%x01).
     * </p>
     *
     * @param message the SASL message
     * @return the parsed SASL XOAUTH2 message
     * @throws IllegalArgumentException on invalid format or Base64 decoding errors
     */
    public static SaslXoauth2Message parse(String message) {
        try {
            String[] parts = message.split("\\u0001");
            if (parts.length > 2 || !parts[0].startsWith("user=") || !parts[1].startsWith("auth=Bearer ")) {
                throw new IllegalArgumentException("Invalid authentication string format");
            }

            return new SaslXoauth2Message(parts[0].substring(5), parts[1].substring(12));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 or malformed input: " + e.getMessage());
        }
    }

}
