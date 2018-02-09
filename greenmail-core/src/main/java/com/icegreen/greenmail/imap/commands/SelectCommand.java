/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

/**
 * Handles processeing for the SELECT imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class SelectCommand extends AuthenticatedStateCommand {
    public static final String NAME = "SELECT";
    public static final String ARGS = "mailbox";

    SelectCommand() {
        super(NAME, ARGS);
    }

    SelectCommand(String name) {
        super(name, null);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        session.deselect();

        final boolean isExamine = this instanceof ExamineCommand;
        try {
            selectMailbox(mailboxName, session, isExamine);
        } catch (FolderException ex) {
            response.commandFailed(this, "No such mailbox");
            return;
        }

        ImapSessionFolder mailbox = session.getSelected();
        response.flagsResponse(mailbox.getPermanentFlags());
        response.existsResponse(mailbox.getMessageCount());
        final boolean resetRecent = !isExamine;
        response.recentResponse(mailbox.getRecentCount(resetRecent));
        response.okResponse("UIDVALIDITY " + mailbox.getUidValidity(), null);
        response.okResponse("UIDNEXT " + mailbox.getUidNext(), null);

        int firstUnseen = mailbox.getFirstUnseen();
        if (firstUnseen > 0) {
            response.okResponse("UNSEEN " + firstUnseen,
                    "Message " + firstUnseen + " is the first unseen");
        } else {
            response.okResponse(null, "No messages unseen");
        }

        response.permanentFlagsResponse(mailbox.getPermanentFlags());

        if (mailbox.isReadonly()) {
            response.commandComplete(this, "READ-ONLY");
        } else {
            response.commandComplete(this, "READ-WRITE");
        }
    }

    private boolean selectMailbox(String mailboxName, ImapSession session, boolean readOnly) throws FolderException {
        MailFolder folder = getMailbox(mailboxName, session, true);

        if (!folder.isSelectable()) {
            throw new FolderException("Non selectable mailbox " + mailboxName);
        }

        session.setSelected(folder, readOnly);
        return readOnly;
    }
}

/*
6.3.1.  SELECT Command

   Arguments:  mailbox name

   Responses:  REQUIRED untagged responses: FLAGS, EXISTS, RECENT
               OPTIONAL OK untagged responses: UNSEEN, PERMANENTFLAGS

   Result:     OK - select completed, now in selected state
               NO - select failure, now in authenticated state: no
                    such mailbox, can't access mailbox
               BAD - command unknown or arguments invalid

   The SELECT command selects a mailbox so that messages in the
   mailbox can be accessed.  Before returning an OK to the client,
   the server MUST send the following untagged data to the client:

      FLAGS       Defined flags in the mailbox.  See the description
                  of the FLAGS response for more detail.

      <n> EXISTS  The number of messages in the mailbox.  See the
                  description of the EXISTS response for more detail.

      <n> RECENT  The number of messages with the \Recent flag set.
                  See the description of the RECENT response for more
                  detail.

      OK [UIDVALIDITY <n>]
                  The unique identifier validity value.  See the
                  description of the UID command for more detail.

   to define the initial state of the mailbox at the client.

   The server SHOULD also send an UNSEEN response code in an OK
   untagged response, indicating the message sequence number of the
   first unseen message in the mailbox.

   If the client can not change the permanent state of one or more of
   the flags listed in the FLAGS untagged response, the server SHOULD
   send a PERMANENTFLAGS response code in an OK untagged response,
   listing the flags that the client can change permanently.

   Only one mailbox can be selected at a time in a connection;
   simultaneous access to multiple mailboxes requires multiple
   connections.  The SELECT command automatically deselects any
   currently selected mailbox before attempting the new selection.
   Consequently, if a mailbox is selected and a SELECT command that
   fails is attempted, no mailbox is selected.




Crispin                     Standards Track                    [Page 23]

RFC 2060                       IMAP4rev1                   December 1996


   If the client is permitted to modify the mailbox, the server
   SHOULD prefix the text of the tagged OK response with the
         "[READ-WRITE]" response code.

      If the client is not permitted to modify the mailbox but is
      permitted read access, the mailbox is selected as read-only, and
      the server MUST prefix the text of the tagged OK response to
      SELECT with the "[READ-ONLY]" response code.  Read-only access
      through SELECT differs from the EXAMINE command in that certain
      read-only mailboxes MAY permit the change of permanent state on a
      per-user (as opposed to global) basis.  Netnews messages marked in
      a server-based .newsrc file are an example of such per-user
      permanent state that can be modified with read-only mailboxes.

   Example:    C: A142 SELECT INBOX
               S: * 172 EXISTS
               S: * 1 RECENT
               S: * OK [UNSEEN 12] Message 12 is first unseen
               S: * OK [UIDVALIDITY 3857529045] UIDs valid
               S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
               S: * OK [PERMANENTFLAGS (\Deleted \Seen \*)] Limited
               S: A142 OK [READ-WRITE] SELECT completed
*/
