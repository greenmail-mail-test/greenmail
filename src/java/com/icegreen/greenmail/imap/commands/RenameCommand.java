/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;

/**
 * Handles processeing for the RENAME imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class RenameCommand extends AuthenticatedStateCommand {
    public static final String NAME = "RENAME";
    public static final String ARGS = "existing-mailbox-name SPACE new-mailbox-name";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        String existingName = parser.mailbox(request);
        String newName = parser.mailbox(request);
        parser.endLine(request);

        session.getHost().renameMailbox(session.getUser(), existingName, newName);

        session.unsolicitedResponses(response);
        response.commandComplete(this);
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
6.3.5.  RENAME Command

   Arguments:  existing mailbox name
               new mailbox name

   Responses:  no specific responses for this command

   Result:     OK - rename completed
               NO - rename failure: can't rename mailbox with that name,
                    can't rename to mailbox with that name
               BAD - command unknown or arguments invalid

      The RENAME command changes the name of a mailbox.  A tagged OK
      response is returned only if the mailbox has been renamed.  It is
      an error to attempt to rename from a mailbox name that does not
      exist or to a mailbox name that already exists.  Any error in
      renaming will return a tagged NO response.

      If the name has inferior hierarchical names, then the inferior
      hierarchical names MUST also be renamed.  For example, a rename of
      "foo" to "zap" will rename "foo/bar" (assuming "/" is the
      hierarchy delimiter character) to "zap/bar".

      The value of the highest-used unique identifier of the old mailbox
      name MUST be preserved so that a new mailbox created with the same
      name will not reuse the identifiers of the former incarnation,
      UNLESS the new incarnation has a different unique identifier
      validity value.  See the description of the UID command for more
      detail.

      Renaming INBOX is permitted, and has special behavior.  It moves
      all messages in INBOX to a new mailbox with the given name,
      leaving INBOX empty.  If the server implementation supports
      inferior hierarchical names of INBOX, these are unaffected by a
      rename of INBOX.

   Examples:   C: A682 LIST "" *
               S: * LIST () "/" blurdybloop
               S: * LIST (\Noselect) "/" foo
               S: * LIST () "/" foo/bar
               S: A682 OK LIST completed
               C: A683 RENAME blurdybloop sarasoop
               S: A683 OK RENAME completed
               C: A684 RENAME foo zowie
               S: A684 OK RENAME Completed
               C: A685 LIST "" *
               S: * LIST () "/" sarasoop
               S: * LIST (\Noselect) "/" zowie
               S: * LIST () "/" zowie/bar
               S: A685 OK LIST completed

               C: Z432 LIST "" *
               S: * LIST () "." INBOX
               S: * LIST () "." INBOX.bar
               S: Z432 OK LIST completed
               C: Z433 RENAME INBOX old-mail
               S: Z433 OK RENAME completed
               C: Z434 LIST "" *
               S: * LIST () "." INBOX
               S: * LIST () "." INBOX.bar
               S: * LIST () "." old-mail
               S: Z434 OK LIST completed
*/
