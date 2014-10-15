/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
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
        for (IdRange idRange : idSet) {
            if (idRange.includes(id)) {
                return true;
            }
        }
        return false;
    }
}
