package com.icegreen.greenmail.smtp.auth;

/**
 * Base interface for the state used by various authentication methods. The data contained
 * in the state depends on the authentication method that was used. The state can be
 * mutable, such as when the authentication method requires multiple exchanges between the
 * client and the server.
 */
public interface AuthenticationState {
    /**
     * @return The type of the used authentication mechanism, e.g. {@code PLAIN} or {@code LOGIN}.
     */
    String getType();
}
