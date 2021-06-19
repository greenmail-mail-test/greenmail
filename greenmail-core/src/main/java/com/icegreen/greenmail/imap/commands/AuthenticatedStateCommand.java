/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapSessionState;

/**
 * A base class for ImapCommands only valid in AUTHENTICATED and SELECTED states.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
abstract class AuthenticatedStateCommand extends CommandTemplate {

    AuthenticatedStateCommand(String name, String argSyntax) {
        super(name, argSyntax);
    }

    /**
     * Check that the state is {@link ImapSessionState#AUTHENTICATED } or
     * {@link ImapSessionState#SELECTED}
     */
    @Override
    public boolean validForState(ImapSessionState state) {
        return state == ImapSessionState.AUTHENTICATED
                || state == ImapSessionState.SELECTED;
    }
}
