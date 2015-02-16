package com.icegreen.greenmail.configuration;

/**
 * A user to create when servers start
 */
public class UserBean {
    private final String email;
    private final String login;
    private final String password;

    /**
     * Initialize user
     */
    public UserBean(final String email, final String login, final String password) {
        this.email = email;
        this.login = login;
        this.password = password;
    }

    /**
     * @return Email address of mail box
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * @return Login name of user
     */
    public String getLogin() {
        return this.login;
    }

    /**
     * @return Password of user that belongs to login name
     */
    public String getPassword() {
        return this.password;
    }
}
