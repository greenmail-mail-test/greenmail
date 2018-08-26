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
import com.icegreen.greenmail.store.MailFolder;

/**
 * Handles processing for the EXPUNGE imap command.
 * <p>
 * Supports also <a href="https://tools.ietf.org/html/rfc4315#section-2.1">UID EXPUNGE</a> of UIDPLUS extension.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class ExpungeCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "EXPUNGE";

    ExpungeCommand() {
        super(NAME, "<message-set>");
    }

    /**
     * @see CommandTemplate#doProcess
     */
    @Override
    protected void doProcess(ImapRequestLineReader request,
                             final ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        doProcess(request, response, session, false);
    }

    /**
     * @see UidEnabledCommand#doProcess(ImapRequestLineReader, ImapResponse, ImapSession, boolean)
     */
    @Override
    public void doProcess(ImapRequestLineReader request, ImapResponse response, ImapSession session, boolean useUids)
            throws ProtocolException, FolderException {
        IdRange[] idSet = null;
        if (useUids) {
            idSet = parser.parseIdRange(request);
        }
        parser.endLine(request);

        if (session.getSelected().isReadonly()) {
            response.commandFailed(this, "Mailbox selected read only.");
        }

        MailFolder folder = session.getSelected();
        if (log.isDebugEnabled() && useUids) {
            log.debug("Expunging messages matching uids {} from {}", IdRange.idRangesToString(idSet) ,folder.getFullName());
        }

        if (useUids) {
            folder.expunge(idSet);
        } else {
            folder.expunge();
        }

        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }
}

/*
http://tools.ietf.org/html/rfc3501#page-49 :
6.4.3.  EXPUNGE Command

   Arguments:  none

   Responses:  untagged responses: EXPUNGE

   Result:     OK - expunge completed
               NO - expunge failure: can't expunge (e.g. permission
                    denied)
               BAD - command unknown or arguments invalid

      The EXPUNGE command permanently removes from the currently
      selected mailbox all messages that have the \Deleted flag set.
      Before returning an OK to the client, an untagged EXPUNGE response
      is sent for each message that is removed.

   Example:    C: A202 EXPUNGE
               S: * 3 EXPUNGE
               S: * 3 EXPUNGE
               S: * 5 EXPUNGE
               S: * 8 EXPUNGE
               S: A202 OK EXPUNGE completed

      Note: in this example, messages 3, 4, 7, and 11 had the
      \Deleted flag set.  See the description of the EXPUNGE
      response for further explanation.
*/
/*
https://tools.ietf.org/html/rfc4315#section-2.1 :

2.1.  UID EXPUNGE Command

   Arguments:  sequence set

   Data:       untagged responses: EXPUNGE

   Result:     OK - expunge completed
               NO - expunge failure (e.g., permission denied)
               BAD - command unknown or arguments invalid

      The UID EXPUNGE command permanently removes all messages that both
      have the \Deleted flag set and have a UID that is included in the
      specified sequence set from the currently selected mailbox.  If a
      message either does not have the \Deleted flag set or has a UID
      that is not included in the specified sequence set, it is not
      affected.

      This command is particularly useful for disconnected use clients.
      By using UID EXPUNGE instead of EXPUNGE when resynchronizing with
      the server, the client can ensure that it does not inadvertantly
      remove any messages that have been marked as \Deleted by other
      clients between the time that the client was last connected and
      the time the client resynchronizes.

      If the server does not support the UIDPLUS capability, the client
      should fall back to using the STORE command to temporarily remove
      the \Deleted flag from messages it does not want to remove, then
      issuing the EXPUNGE command.  Finally, the client should use the
      STORE command to restore the \Deleted flag on the messages in
      which it was temporarily removed.

      Alternatively, the client may fall back to using just the EXPUNGE
      command, risking the unintended removal of some messages.






Crispin                     Standards Track                     [Page 2]

RFC 4315                IMAP - UIDPLUS Extension           December 2005


   Example:    C: A003 UID EXPUNGE 3000:3002
               S: * 3 EXPUNGE
               S: * 3 EXPUNGE
               S: * 3 EXPUNGE
               S: A003 OK UID EXPUNGE completed
 */

