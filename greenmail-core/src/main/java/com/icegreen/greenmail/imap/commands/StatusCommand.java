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
import com.icegreen.greenmail.imap.commands.parsers.StatusCommandParser;
import com.icegreen.greenmail.imap.commands.parsers.status.StatusDataItems;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder; // NOSONAR

/**
 * Handles processeing for the STATUS imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class StatusCommand extends AuthenticatedStateCommand {
    public static final String NAME = "STATUS";
    public static final String ARGS = "<mailbox> ( <status-data-item>+ )";

    public static final String MESSAGES = "MESSAGES";
    public static final String RECENT = "RECENT";
    public static final String UIDNEXT = "UIDNEXT";
    public static final String UIDVALIDITY = "UIDVALIDITY";
    public static final String UNSEEN = "UNSEEN";

    private StatusCommandParser statusParser = new StatusCommandParser();

    public StatusCommand() {
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
        String mailboxName = statusParser.mailbox(request);
        StatusDataItems statusDataItems = statusParser.statusDataItems(request);
        statusParser.endLine(request);

        MailFolder folder;
        try {
            folder = getMailbox(mailboxName, session, true);
        } catch (FolderException ex) {
            response.commandFailed(this, "No such mailbox");
            return;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append('\"').append(BASE64MailboxEncoder.encode(mailboxName)).append('\"');
        buffer.append(SP);
        buffer.append('(');

        if (statusDataItems.isMessages()) {
            buffer.append(MESSAGES);
            buffer.append(SP);
            buffer.append(folder.getMessageCount());
            buffer.append(SP);
        }

        if (statusDataItems.isRecent()) {
            buffer.append(RECENT);
            buffer.append(SP);
            buffer.append(folder.getRecentCount(false));
            buffer.append(SP);
        }

        if (statusDataItems.isUidNext()) {
            buffer.append(UIDNEXT);
            buffer.append(SP);
            buffer.append(folder.getUidNext());
            buffer.append(SP);
        }

        if (statusDataItems.isUidValidity()) {
            buffer.append(UIDVALIDITY);
            buffer.append(SP);
            buffer.append(folder.getUidValidity());
            buffer.append(SP);
        }

        if (statusDataItems.isUnseen()) {
            buffer.append(UNSEEN);
            buffer.append(SP);
            buffer.append(folder.getUnseenCount());
            buffer.append(SP);
        }
        if (buffer.charAt(buffer.length() - 1) == ' ') {
            buffer.setLength(buffer.length() - 1);
        }
        buffer.append(')');
        response.commandResponse(this, buffer.toString());

        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }
}

/*
6.3.10. STATUS Command

   Arguments:  mailbox name
               status data item names

   Responses:  untagged responses: STATUS

   Result:     OK - status completed
               NO - status failure: no status for that name
               BAD - command unknown or arguments invalid

      The STATUS command requests the status of the indicated mailbox.
      It does not change the currently selected mailbox, nor does it
      affect the state of any messages in the queried mailbox (in
      particular, STATUS MUST NOT cause messages to lose the \Recent
      flag).

      The STATUS command provides an alternative to opening a second
      IMAP4rev1 connection and doing an EXAMINE command on a mailbox to
      query that mailbox's status without deselecting the current
      mailbox in the first IMAP4rev1 connection.

      Unlike the LIST command, the STATUS command is not guaranteed to
      be fast in its response.  In some implementations, the server is
      obliged to open the mailbox read-only internally to obtain certain
      status information.  Also unlike the LIST command, the STATUS
      command does not accept wildcards.

      The currently defined status data items that can be requested are:

      MESSAGES       The number of messages in the mailbox.

      RECENT         The number of messages with the \Recent flag set.

      UIDNEXT        The next UID value that will be assigned to a new
                     message in the mailbox.  It is guaranteed that this
                     value will not change unless new messages are added
                     to the mailbox; and that it will change when new
                     messages are added even if those new messages are
                     subsequently expunged.



Crispin                     Standards Track                    [Page 33]

RFC 2060                       IMAP4rev1                   December 1996


      UIDVALIDITY    The unique identifier validity value of the
                     mailbox.

      UNSEEN         The number of messages which do not have the \Seen
                     flag set.


      Example:    C: A042 STATUS blurdybloop (UIDNEXT MESSAGES)
                  S: * STATUS blurdybloop (MESSAGES 231 UIDNEXT 44292)
                  S: A042 OK STATUS completed

*/
