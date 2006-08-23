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
import com.icegreen.greenmail.imap.ImapSessionState;

/**
 * Represents a processor for a particular Imap command. Implementations of this
 * interface should encpasulate all command specific processing.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface ImapCommand {
    /**
     * @return the name of the command, as specified in rfc2060.
     */
    String getName();

    /**
     * Specifies if this command is valid for the given session state.
     *
     * @param state The current {@link com.icegreen.greenmail.imap.ImapSessionState state} of the {@link com.icegreen.greenmail.imap.ImapSession}
     * @return <code>true</code> if the command is valid in this state.
     */
    boolean validForState(ImapSessionState state);

    /**
     * Performs all processing of the current Imap request. Reads command
     * arguments from the request, performs processing, and writes responses
     * back to the request object, which are sent to the client.
     *
     * @param request  The current client request
     * @param response The current server response
     * @param session  The current session
     */
    void process(ImapRequestLineReader request,
                 ImapResponse response,
                 ImapSession session);
}
