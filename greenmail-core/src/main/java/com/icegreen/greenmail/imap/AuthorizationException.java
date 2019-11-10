/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

/**
 * Thrown when a user attempts to do something (e.g. alter mailbox) for which
 * they do not have appropriate rights.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class AuthorizationException extends Exception {
    static final long serialVersionUID = -4448741163197075584L;

    /**
     * Construct a new <code>AuthorizationException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public AuthorizationException(final String message) {
        super(message);
    }
}
