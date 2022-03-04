package com.icegreen.greenmail.configuration;

import java.util.Objects;

/**
 * A user to create when servers start
 */
public class UserBean {
    private final String email;
    private final String login;
    private final String password;

    /**
     * Initialize user
     *
     * @param email the user email.
     * @param login the user login.
     * @param password the user password.
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

    @Override
    public String toString() {
        return "UserBean{" +
                "email='" + email + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' + // NOSONAR
                '}';
    }

    @Override
    public boolean equals(Object o) { // nosonar
        if (this == o) return true;
        if (!(o instanceof UserBean)) return false;

        UserBean userBean = (UserBean) o;

        if (!Objects.equals(email, userBean.email)) return false;
        if (!Objects.equals(login, userBean.login)) return false;
        return Objects.equals(password, userBean.password);

    }

    @Override
    public int hashCode() { // nosonar
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
