package com.icegreen.greenmail.configuration;

import java.util.Arrays;
import java.util.Properties;
import java.util.function.BinaryOperator;

/**
 * Creates GreenMailConfiguration from properties.
 * <p>
 * Example usage:
 * GreenMailConfiguration config = new PropertyBasedGreenMailConfigurationBuilder().build(System.getProperties());
 * <ul>
 * <li>greenmail.users :  List of comma separated users of format login:pwd[@domain][,login:pwd[@domain]]
 * <p>Example: user1:pwd1@localhost,user2:pwd2@0.0.0.0</p>
 * <p>Note: domain part must be DNS resolvable!</p>
 * </li>
 * <li>
 *  greenmail.users.login : Overrides the login for authentication
 *  <p>By default use the local-part of an email. Can be changed to full email.</p>
 * </li>
 * </ul>
 */
public class PropertiesBasedGreenMailConfigurationBuilder {
    /**
     * Property for list of users.
     */
    public static final String GREENMAIL_USERS = "greenmail.users";
    /**
     * Overrides the login for authentication for configured users (@see {@link #GREENMAIL_USERS}).
     * <p>
     * By default, use the local-part of an email and can be changed to full email (@see UsersLoginConfigurationType.EMAIL).
     */
    public static final String GREENMAIL_USERS_LOGIN = "greenmail.users.login";
    /**
     * Disables authentication check.
     *
     * @see GreenMailConfiguration#withDisabledAuthentication()
     */
    public static final String GREENMAIL_AUTH_DISABLED = "greenmail.auth.disabled";

    /**
     * Configures how user login should be extracted from user of pattern local-part:password@domain .
     */
    private enum UsersLoginConfigurationType {
        /**
         * Use local part of email (default)
         */
        LOCAL_PART,
        /**
         * Use email for login
         */
        EMAIL
    }

    /**
     * Builds a configuration object based on given properties.
     *
     * @param properties the properties.
     * @return a configuration and never null.
     */
    public GreenMailConfiguration build(Properties properties) {
        GreenMailConfiguration configuration = new GreenMailConfiguration();
        String usersParam = properties.getProperty(GREENMAIL_USERS);
        BinaryOperator<String> loginBuilder = configureLoginBuilder(properties);
        if (null != usersParam) {
            String[] usersArray = usersParam.split(",");
            for (String user : usersArray) {
                extractAndAddUser(configuration, loginBuilder, user);
            }
        }
        String disabledAuthentication = properties.getProperty(GREENMAIL_AUTH_DISABLED);
        if (null != disabledAuthentication) {
            configuration.withDisabledAuthentication();
        }
        return configuration;
    }

    private BinaryOperator<String> configureLoginBuilder(Properties properties) {
        String loginType = properties.getProperty(GREENMAIL_USERS_LOGIN);
        if (UsersLoginConfigurationType.EMAIL.name().equalsIgnoreCase(loginType)) {
            return this::buildEmailLogin;
        } else if (UsersLoginConfigurationType.LOCAL_PART.name().equalsIgnoreCase(loginType)) {
            return this::buildLocalPartLogin;
        }
        return this::buildLocalPartLogin;
    }

    protected String buildEmailLogin(String localPart, String domain) {
        return localPart + '@' + domain;
    }

    protected String buildLocalPartLogin(String localPart, String domain) {
        return localPart;
    }

    protected void extractAndAddUser(GreenMailConfiguration configuration, BinaryOperator<String> buildLogin, String user) {
        // login:pwd@domain
        String[] userParts = user.split(":|@");
        switch (userParts.length) {
            case 2:
                configuration.withUser(userParts[0], userParts[1]);
                break;
            case 3:
                configuration.withUser(userParts[0] + '@' + userParts[2],
                    buildLogin.apply(userParts[0], userParts[2]), userParts[1]);
                break;
            default:
                throw new IllegalArgumentException("Expected format login:pwd[@domain] but got " + user
                    + " parsed to " + Arrays.toString(userParts));
        }
    }
}
