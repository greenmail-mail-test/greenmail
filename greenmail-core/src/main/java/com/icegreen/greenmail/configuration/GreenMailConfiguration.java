package com.icegreen.greenmail.configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Properties that can be defined to configure a GreenMail instance or GreenMailRule.
 */
public class GreenMailConfiguration {
    private final List<UserBean> usersToCreate = new ArrayList<>();
    private boolean disableAuthenticationCheck = false;
    private boolean sieveIgnoreDetail = false;
    private String preloadDir;


    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start.
     *
     * @param login User id and email address
     * @param password Password of user that belongs to login name
     * @return Modified configuration
     */
    public GreenMailConfiguration withUser(final String login, final String password) {
        return withUser(login, login, password);
    }

    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start.
     *
     * @param email Email address
     * @param login Login name of user
     * @param password Password of user that belongs to login name
     * @return Modified configuration
     */
    public GreenMailConfiguration withUser(final String email, final String login, final String password) {
        this.usersToCreate.add(new UserBean(email, login, password));
        return this;
    }

    /**
     * @return New GreenMail configuration
     */
    public static GreenMailConfiguration aConfig() {
        return new GreenMailConfiguration();
    }

    /**
     * @return Users that should be created on server startup
     */
    public List<UserBean> getUsersToCreate() {
        return usersToCreate;
    }

    /**
     * Disables authentication.
     * <p>
     * Useful if you want to avoid setting up users up front.
     *
     * @return Modified configuration.
     */
    public GreenMailConfiguration withDisabledAuthentication() {
        disableAuthenticationCheck = true;
        return this;
    }

    /**
     * @return true, if authentication is disabled.
     *
     * @see GreenMailConfiguration#withDisabledAuthentication()
     */
    public boolean isAuthenticationDisabled() {
        return disableAuthenticationCheck;
    }

    /**
     * Enables Sieve detail handling, also known as RFC 5233 sub-address extension.
     *
     * @return Modified configuration.
     */
    public GreenMailConfiguration withSieveIgnoreDetail() {
        sieveIgnoreDetail = true;
        return this;
    }

    /**
     * @return true, if Sieve detail handling is enabled.
     *
     * @see GreenMailConfiguration#withSieveIgnoreDetail() ()
     */
    public boolean isSieveIgnoreDetailEnabled() {
        return sieveIgnoreDetail;
    }

    /**
     * Configures directory path for preloading emails from filesystem.
     * @param preloadDir directory containing emails
     * @see com.icegreen.greenmail.base.GreenMailOperations#loadEmails(Path)
     * @return Modified configuration.
     */
    public GreenMailConfiguration withPreloadDir(String preloadDir) {
        this.preloadDir = preloadDir;
           return this;
    }

    /**
     * Gets preload directory value or null if not set.
     * @return the directory
     */
    public String getPreloadDir() {
        return preloadDir;
    }

    /**
     * Checks if preload directory value exists.
     * @return true if available
     */
    public boolean hasPreloadDir() {
        return null != preloadDir;
    }
}
