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
 * Handles processeing for the AUTHENTICATE imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class AuthenticateCommand extends NonAuthenticatedStateCommand {
    public static final String NAME = "AUTHENTICATE";
    public static final String ARGS = "<auth_type> *(CRLF base64)";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException {
        String authType = parser.astring(request);
        parser.endLine(request);

        response.commandFailed(this, "Unsupported authentication mechanism '" +
                authType + "'");
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
6.2.1.  AUTHENTICATE Command

   Arguments:  authentication mechanism name

   Responses:  continuation data can be requested

   Result:     OK - authenticate completed, now in authenticated state
               NO - authenticate failure: unsupported authentication
                    mechanism, credentials rejected
              BAD - command unknown or arguments invalid,
                    authentication exchange cancelled

      The AUTHENTICATE command indicates an authentication mechanism,
      such as described in [IMAP-AUTH], to the server.  If the server
      supports the requested authentication mechanism, it performs an
      authentication protocol exchange to authenticate and identify the
      client.  It MAY also negotiate an OPTIONAL protection mechanism
      for subsequent protocol interactions.  If the requested
      authentication mechanism is not supported, the server SHOULD
      reject the AUTHENTICATE command by sending a tagged NO response.

      The authentication protocol exchange consists of a series of
      server challenges and client answers that are specific to the
      authentication mechanism.  A server challenge consists of a
      command continuation request response with the "+" token followed
      by a BASE64 encoded string.  The client answer consists of a line
      consisting of a BASE64 encoded string.  If the client wishes to
      cancel an authentication exchange, it issues a line with a single
      "*".  If the server receives such an answer, it MUST reject the
      AUTHENTICATE command by sending a tagged BAD response.

      A protection mechanism provides integrity and privacy protection
      to the connection.  If a protection mechanism is negotiated, it is
      applied to all subsequent data sent over the connection.  The
      protection mechanism takes effect immediately following the CRLF
      that concludes the authentication exchange for the client, and the
      CRLF of the tagged OK response for the server.  Once the
      protection mechanism is in effect, the stream of command and
      response octets is processed into buffers of ciphertext.  Each
      buffer is transferred over the connection as a stream of octets
      prepended with a four octet field in network byte order that
      represents the length of the following data.  The maximum
      ciphertext buffer length is defined by the protection mechanism.

      Authentication mechanisms are OPTIONAL.  Protection mechanisms are
      also OPTIONAL; an authentication mechanism MAY be implemented
      without any protection mechanism.  If an AUTHENTICATE command
      fails with a NO response, the client MAY try another
      authentication mechanism by issuing another AUTHENTICATE command,
      or MAY attempt to authenticate by using the LOGIN command.  In
      other words, the client MAY request authentication types in
      decreasing order of preference, with the LOGIN command as a last
      resort.

   Example:    S: * OK KerberosV4 IMAP4rev1 Server
               C: A001 AUTHENTICATE KERBEROS_V4
               S: + AmFYig==
               C: BAcAQU5EUkVXLkNNVS5FRFUAOCAsho84kLN3/IJmrMG+25a4DT
                  +nZImJjnTNHJUtxAA+o0KPKfHEcAFs9a3CL5Oebe/ydHJUwYFd
                  WwuQ1MWiy6IesKvjL5rL9WjXUb9MwT9bpObYLGOKi1Qh
               S: + or//EoAADZI=
               C: DiAF5A4gA+oOIALuBkAAmw==
               S: A001 OK Kerberos V4 authentication successful

      Note: the line breaks in the first client answer are for editorial
      clarity and are not in real authenticators.
*/
