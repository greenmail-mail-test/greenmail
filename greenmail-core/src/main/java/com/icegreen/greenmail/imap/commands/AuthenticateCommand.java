/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import static com.icegreen.greenmail.imap.ImapConstants.*;

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
