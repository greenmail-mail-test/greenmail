/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapSessionState;

/**
 * A base class for ImapCommands only valid in the SELECTED state.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
abstract class SelectedStateCommand extends CommandTemplate {
    /**
     * Subclasses of this command are only valid in the
     * {@link ImapSessionState#SELECTED} state.
     */
    public boolean validForState(ImapSessionState state) {
        return (state == ImapSessionState.SELECTED);
    }

    protected boolean includes(IdRange[] idSet, long id) {
        for (int i = 0; i < idSet.length; i++) {
            IdRange idRange = idSet[i];
            if (idRange.includes(id)) {
                return true;
            }
        }
        return false;
    }
}
