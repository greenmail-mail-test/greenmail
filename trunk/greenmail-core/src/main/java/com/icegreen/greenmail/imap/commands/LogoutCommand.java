/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

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
    public static final String ARGS = null;
    public static final String BYE_MESSAGE = VERSION + SP + "Server logging out";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException {
        parser.endLine(request);

        response.byeResponse(BYE_MESSAGE);
        response.commandComplete(this);
        session.closeConnection();
    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    public String getArgSyntax() {
        return ARGS;
    }
}

/*
6.1.3.  LOGOUT Command

   Arguments:  none

   Responses:  REQUIRED untagged response: BYE

   Result:     OK - logout completed
               BAD - command unknown or arguments invalid

      The LOGOUT command informs the server that the client is done with
      the connection.  The server MUST send a BYE untagged response
      before the (tagged) OK response, and then close the network
      connection.

   Example:    C: A023 LOGOUT
               S: * BYE IMAP4rev1 Server logging out
               S: A023 OK LOGOUT completed
               (Server and client then close the connection)
*/
