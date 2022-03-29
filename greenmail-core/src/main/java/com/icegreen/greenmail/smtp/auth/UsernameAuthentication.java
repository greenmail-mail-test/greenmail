package com.icegreen.greenmail.smtp.auth;

/**
 * Base interface for authentication methods that provide or make use of
 * a plain text username to identify a user, such as the {@code PLAIN} or
 * {@code LOGIN} authentication mechanisms.
 */
public interface UsernameAuthentication {
    String getUsername();
}
