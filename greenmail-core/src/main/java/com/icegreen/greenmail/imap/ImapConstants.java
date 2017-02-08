/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

public interface ImapConstants {
    // Basic response types
    String OK = "OK";
    String NO = "NO";
    String BAD = "BAD";
    String BYE = "BYE";
    String UNTAGGED = "*";

    String SP = " ";
    String VERSION = "IMAP4rev1";
    String CAPABILITIES = "LITERAL+" + SP + "SORT" + SP + "UIDPLUS";

    String USER_NAMESPACE = "#mail";

    char HIERARCHY_DELIMITER_CHAR = '.';
    char NAMESPACE_PREFIX_CHAR = '#';
    String HIERARCHY_DELIMITER = String.valueOf(HIERARCHY_DELIMITER_CHAR);
    String NAMESPACE_PREFIX = String.valueOf(NAMESPACE_PREFIX_CHAR);
    String ALL = "*";

    String INBOX_NAME = "INBOX";
    String STORAGE = "STORAGE";
    String MESSAGES = "MESSAGES";
}
