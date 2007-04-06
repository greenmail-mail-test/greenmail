/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap;

/**
 * Enumerated type representing an IMAP session state.
 */
public class ImapSessionState {
    public static final ImapSessionState NON_AUTHENTICATED = new ImapSessionState("NON_AUTHENTICATED");
    public static final ImapSessionState AUTHENTICATED = new ImapSessionState("AUTHENTICATED");
    public static final ImapSessionState SELECTED = new ImapSessionState("SELECTED");
    public static final ImapSessionState LOGOUT = new ImapSessionState("LOGOUT");

    private final String myName; // for debug only

    private ImapSessionState(String name) {
        myName = name;
    }

    public String toString() {
        return myName;
    }
}
