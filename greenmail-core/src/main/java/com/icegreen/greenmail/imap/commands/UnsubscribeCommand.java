/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;

/**
 * Handles processeing for the UNSUBSCRIBE imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class UnsubscribeCommand extends AuthenticatedStateCommand {
    public static final String NAME = "UNSUBSCRIBE";

    public UnsubscribeCommand() {
        super(NAME, "<mailbox>");
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        session.getHost().unsubscribe(session.getUser(), mailboxName);
        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }
}
