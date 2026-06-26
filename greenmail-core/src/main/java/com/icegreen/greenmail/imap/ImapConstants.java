/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

/**
 * Utility class containing IMAP protocol constants.
 */
public final class ImapConstants {

    private ImapConstants() {
        // Prevent instantiation
    }

    // Basic response types
    public static final String OK = "OK";
    public static final String NO = "NO";
    public static final String BAD = "BAD";
    public static final String BYE = "BYE";
    public static final String UNTAGGED = "*";
    public static final String SP = " ";

    public static final String VERSION = "IMAP4rev1";

    public static final String USER_NAMESPACE = "#mail";

    public static final char HIERARCHY_DELIMITER_CHAR = '.';
    public static final char NAMESPACE_PREFIX_CHAR = '#';

    public static final String HIERARCHY_DELIMITER =
        String.valueOf(HIERARCHY_DELIMITER_CHAR);

    public static final String NAMESPACE_PREFIX =
        String.valueOf(NAMESPACE_PREFIX_CHAR);

    public static final String INBOX_NAME = "INBOX";

    public static final String STORAGE = "STORAGE";

    public static final String MESSAGES = "MESSAGES";
}
