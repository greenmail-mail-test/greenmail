/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

import java.util.Collection;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class LsubCommand extends ListCommand {
    public static final String NAME = "LSUB";

    LsubCommand() {
        super(NAME);
    }

    @Override
    protected Collection<MailFolder> doList(ImapSession session, String searchPattern)
            throws FolderException {
        return session.getHost().listSubscribedMailboxes(session.getUser(), searchPattern);
    }
}
