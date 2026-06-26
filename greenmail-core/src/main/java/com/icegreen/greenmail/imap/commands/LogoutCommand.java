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

/**
 * Handles processeing for the LOGOUT imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class LogoutCommand extends CommandTemplate {
    public static final String NAME = "LOGOUT";
    public static final String BYE_MESSAGE = VERSION + SP + "Server logging out";

    LogoutCommand() {
        super(NAME, null);
    }

    /**
     * @see CommandTemplate#doProcess
     */
    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException {
        parser.endLine(request);

        response.byeResponse(BYE_MESSAGE);
        response.commandComplete(this);
        session.closeConnection();
    }
}
