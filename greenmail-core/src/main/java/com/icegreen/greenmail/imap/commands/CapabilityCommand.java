/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import static com.icegreen.greenmail.imap.ImapConstants.*;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;

/**
 * Handles processing for the CAPABILITY imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 */
class CapabilityCommand extends CommandTemplate {
    public static final String NAME = "CAPABILITY";
    public static final String ARGS = null;

    public static final String CAPABILITIES = "LITERAL+" + SP + "UIDPLUS"
        + SP + SortCommand.CAPABILITY
        + SP + IdleCommand.CAPABILITY
        + SP + MoveCommand.CAPABILITY
        + SP + AuthenticateCommand.CAPABILITY;

    public static final String CAPABILITY_RESPONSE = NAME + SP + VERSION + SP + CAPABILITIES;

    CapabilityCommand() {
        super(NAME, ARGS);
    }

    /**
     * @see CommandTemplate#doProcess
     */
    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
        throws ProtocolException, FolderException {
        parser.endLine(request);

        if( session.getHost().getStore().isQuotaSupported()) {
            response.untaggedResponse(CAPABILITY_RESPONSE + SP + "QUOTA");
        }
        else {
            response.untaggedResponse(CAPABILITY_RESPONSE);
        }
        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }
}
