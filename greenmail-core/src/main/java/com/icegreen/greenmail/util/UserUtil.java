package com.icegreen.greenmail.util;

import javax.mail.internet.InternetAddress;

/**
 * Utility for managing users
 */
public final class UserUtil {
    /**
     * No instance
     */
    private UserUtil() {
    }

    /**
     * Create users for the given array of addresses. The passwords will be set to the email addresses.
     *
     * @param greenMail Greenmail instance to create users for
     * @param addresses Addresses
     */
    public static void createUsers(GreenMailOperations greenMail, InternetAddress... addresses) {
        for (InternetAddress address : addresses) {
            greenMail.setUser(address.getAddress(), address.getAddress());
        }
    }
}
