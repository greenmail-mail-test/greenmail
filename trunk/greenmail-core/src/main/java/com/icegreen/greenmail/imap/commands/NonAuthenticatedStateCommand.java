/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapSessionState;

/**
 * A base class for ImapCommands only valid in the NON_AUTHENTICATED state.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
abstract class NonAuthenticatedStateCommand extends CommandTemplate {

    /**
     * Ensure that state is {@link ImapSessionState#NON_AUTHENTICATED}.
     */
    public boolean validForState(ImapSessionState state) {
        return (state == ImapSessionState.NON_AUTHENTICATED);
    }
}
