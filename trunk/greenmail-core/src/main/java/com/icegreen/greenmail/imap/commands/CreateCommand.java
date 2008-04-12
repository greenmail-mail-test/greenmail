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
 * Handles processeing for the CREATE imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class CreateCommand extends AuthenticatedStateCommand {
    public static final String NAME = "CREATE";
    public static final String ARGS = "<mailbox>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        session.getHost().createMailbox(session.getUser(), mailboxName);
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
6.3.3.  CREATE Command

   Arguments:  mailbox name

   Responses:  no specific responses for this command

   Result:     OK - create completed
               NO - create failure: can't create mailbox with that name
               BAD - command unknown or arguments invalid

      The CREATE command creates a mailbox with the given name.  An OK
      response is returned only if a new mailbox with that name has been
      created.  It is an error to attempt to create INBOX or a mailbox
      with a name that refers to an extant mailbox.  Any error in
      creation will return a tagged NO response.

      If the mailbox name is suffixed with the server's hierarchy
      separator character (as returned from the server by a LIST
      command), this is a declaration that the client intends to create
      mailbox names under this name in the hierarchy.  Server
      implementations that do not require this declaration MUST ignore
      it.

      If the server's hierarchy separator character appears elsewhere in
      the name, the server SHOULD create any superior hierarchical names
      that are needed for the CREATE command to complete successfully.
      In other words, an attempt to create "foo/bar/zap" on a server in
      which "/" is the hierarchy separator character SHOULD create foo/
      and foo/bar/ if they do not already exist.

      If a new mailbox is created with the same name as a mailbox which
      was deleted, its unique identifiers MUST be greater than any
      unique identifiers used in the previous incarnation of the mailbox
      UNLESS the new incarnation has a different unique identifier
      validity value.  See the description of the UID command for more
      detail.

   Example:    C: A003 CREATE owatagusiam/
               S: A003 OK CREATE completed
               C: A004 CREATE owatagusiam/blurdybloop
               S: A004 OK CREATE completed

      Note: the interpretation of this example depends on whether "/"
      was returned as the hierarchy separator from LIST.  If "/" is the
      hierarchy separator, a new level of hierarchy named "owatagusiam"
      with a member called "blurdybloop" is created.  Otherwise, two
      mailboxes at the same hierarchy level are created.
*/
