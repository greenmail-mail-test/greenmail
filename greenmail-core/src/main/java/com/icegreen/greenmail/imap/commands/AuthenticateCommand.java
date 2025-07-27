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
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.SaslXoauth2Message;

/**
 * Handles processing for the AUTHENTICATE imap command.
 * <a href="https://datatracker.ietf.org/doc/html/rfc3501#section-6.2.2">RC 3501 / Section 6.2.2</a>
 * <a href="https://datatracker.ietf.org/doc/html/rfc4959">RFC 4959</a>
 * <p>
 * Syntax RFC 4959:
 * <code>
 * authenticate  = "AUTHENTICATE" SP auth-type [SP (base64 / "=")]
 * *(CRLF base64)
 * ;;redefine AUTHENTICATE from [RFC3501]
 * </code>
 *
 * @author Darrell DeBoer <darrell@apache.org>
 */
class AuthenticateCommand extends NonAuthenticatedStateCommand {
    public static final String NAME = "AUTHENTICATE";
    public static final String ARGS = "<auth_type> *(CRLF base64)";
    public static final String CAPABILITY = "SASL-IR" + SP + "AUTH=XOAUTH2";

    AuthenticateCommand() {
        super(NAME, ARGS);
    }

    /**
     * @see CommandTemplate#doProcess
     */
    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException {
        String authType = parser.astring(request);

        if ("XOAUTH2".equalsIgnoreCase(authType)) {
            // https://developers.google.com/workspace/gmail/imap/xoauth2-protocol#imap_protocol_exchange
            String base64 = parser.astring(request);
            if ("=".equals(base64)) {
                parser.endLine(request);
                base64 = parser.astring(request);
            }
            parser.endLine(request);
            // Create a new SASL message for XOAUTH2
            SaslXoauth2Message xoauth2Message = SaslXoauth2Message.parseBase64Encoded(base64);
            if (session.getUserManager().test(xoauth2Message.getUsername(), xoauth2Message.getAccessToken())) {
                GreenMailUser user = session.getUserManager().getUser(xoauth2Message.getUsername());
                session.setAuthenticated(user);
                response.commandComplete(this);
            } else {
                response.commandFailed(this, "Invalid login/password for user id " + xoauth2Message.getUsername());
            }
        } else {
            parser.endLine(request);
            response.commandFailed(this, "Unsupported authentication mechanism '" +
                authType + "'");
        }
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
                    authentication exchange canceled

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
