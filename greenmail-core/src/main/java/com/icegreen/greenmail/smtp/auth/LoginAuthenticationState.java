package com.icegreen.greenmail.smtp.auth;

import com.icegreen.greenmail.smtp.commands.AuthCommand.AuthMechanism;

/**
 * Details from the {@link AuthMechanism#LOGIN} authorization mechanism, when
 * that mechanism was used for authentication.
 */
public class LoginAuthenticationState implements AuthenticationState, UsernameAuthentication {
    private final String username;

    private final String password;
    
    /**
     * @param username The username from the AUTH command.
     * @param password The password from the AUTH command.
     */
    public LoginAuthenticationState(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getType() {
        return AuthMechanism.LOGIN.name();
    }

    /**
     * Retrieves the username that was used for {@code PLAIN} or {@code LOGIN} authentication.
     * Note that this will return {@code null} when no authentication was performed or needed. 
     * @return The username from the AUTH command.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the password that was used for {@code PLAIN} or {@code LOGIN} authentication.
     * Note that this will return {@code null} when no authentication was performed or needed. 
     * @return The password from the AUTH command.
     */
    public String getPassword() {
        return password;
    }
}
