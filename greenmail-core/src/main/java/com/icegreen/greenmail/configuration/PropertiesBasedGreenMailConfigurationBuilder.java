package com.icegreen.greenmail.configuration;

import java.util.Arrays;
import java.util.Properties;

/**
 * Creates GreenMailConfiguration from properties.
 * <p/>
 * Example usage:
 * GreenMailConfiguration config = new PropertyBasedGreenMailConfigurationBuilder().build(System.getProperties());
 * <ul>
 * <li>greenmail.users :  List of comma separated users of format login:pwd[@domain][,login:pwd[@domain]]
 * <p>Example: user1:pwd1@localhost,user2:pwd2@0.0.0.0</p>
 * <p>Note: domain part must be DNS resolvable!</p>
 * </li>
 * </ul>
 */
public class PropertiesBasedGreenMailConfigurationBuilder {
    /**
     * Property for list of users.
     */
    public static final String GREENMAIL_USERS = "greenmail.users";

    /**
     * Builds a configuration object based on given properties.
     *
     * @param properties the properties.
     * @return a configuration and never null.
     */
    public GreenMailConfiguration build(Properties properties) {
        GreenMailConfiguration configuration = new GreenMailConfiguration();
        String usersParam = properties.getProperty(GREENMAIL_USERS);
        if (null != usersParam) {
            String[] usersArray = usersParam.split(",");
            for (String user : usersArray) {
                extractAndAddUser(configuration, user);
            }
        }
        return configuration;
    }

    protected void extractAndAddUser(GreenMailConfiguration configuration, String user) {
        // login:pwd@domain
        String[] userParts = user.split(":|@");
        switch (userParts.length) {
            case 2:
                configuration.withUser(userParts[0], userParts[1]);
                break;
            case 3:
                configuration.withUser(userParts[0] + '@' + userParts[2], userParts[0], userParts[1]);
                break;
            default:
                throw new IllegalArgumentException("Expected format login:pwd[@domain] but got " + user
                        + " parsed to " + Arrays.toString(userParts));
        }
    }
}
