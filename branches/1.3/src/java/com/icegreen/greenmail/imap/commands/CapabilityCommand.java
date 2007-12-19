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
import com.icegreen.greenmail.store.FolderException;

/**
 * Handles processeing for the CAPABILITY imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class CapabilityCommand extends CommandTemplate {
    public static final String NAME = "CAPABILITY";
    public static final String ARGS = null;

    public static final String CAPABILITY_RESPONSE = NAME + SP + VERSION + SP + CAPABILITIES;

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        parser.endLine(request);
        response.untaggedResponse(CAPABILITY_RESPONSE);
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
6.1.1.  CAPABILITY Command

   Arguments:  none

   Responses:  REQUIRED untagged response: CAPABILITY

   Result:     OK - capability completed
               BAD - command unknown or arguments invalid

      The CAPABILITY command requests a listing of capabilities that the
      server supports.  The server MUST send a single untagged
      CAPABILITY response with "IMAP4rev1" as one of the listed
      capabilities before the (tagged) OK response.  This listing of
      capabilities is not dependent upon connection state or user.  It
      is therefore not necessary to issue a CAPABILITY command more than
      once in a connection.

      A capability name which begins with "AUTH=" indicates that the
      server supports that particular authentication mechanism.  All
      such names are, by definition, part of this specification.  For
      example, the authorization capability for an experimental
      "blurdybloop" authenticator would be "AUTH=XBLURDYBLOOP" and not
      "XAUTH=BLURDYBLOOP" or "XAUTH=XBLURDYBLOOP".

      Other capability names refer to extensions, revisions, or
      amendments to this specification.  See the documentation of the
      CAPABILITY response for additional information.  No capabilities,
      beyond the base IMAP4rev1 set defined in this specification, are
      enabled without explicit client action to invoke the capability.

      See the section entitled "Client Commands -
      Experimental/Expansion" for information about the form of site or
      implementation-specific capabilities.

   Example:    C: abcd CAPABILITY
               S: * CAPABILITY IMAP4rev1 AUTH=KERBEROS_V4
               S: abcd OK CAPABILITY completed


7.2.1.  CAPABILITY Response

   Contents:   capability listing

      The CAPABILITY response occurs as a result of a CAPABILITY
      command.  The capability listing contains a space-separated
      listing of capability names that the server supports.  The
      capability listing MUST include the atom "IMAP4rev1".

      A capability name which begins with "AUTH=" indicates that the
      server supports that particular authentication mechanism.
      Other capability names indicate that the server supports an
      extension, revision, or amendment to the IMAP4rev1 protocol.
      Server responses MUST conform to this document until the client
      issues a command that uses the associated capability.

      Capability names MUST either begin with "X" or be standard or
      standards-track IMAP4rev1 extensions, revisions, or amendments
      registered with IANA.  A server MUST NOT offer unregistered or
      non-standard capability names, unless such names are prefixed with
      an "X".

      Client implementations SHOULD NOT require any capability name
      other than "IMAP4rev1", and MUST ignore any unknown capability
      names.

   Example:    S: * CAPABILITY IMAP4rev1 AUTH=KERBEROS_V4 XPIG-LATIN

*/
