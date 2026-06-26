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
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.util.GreenMailUtil;

import jakarta.mail.Flags;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.Date;

/**
 * Handles processing for the APPEND imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class AppendCommand extends AuthenticatedStateCommand {
    public static final String NAME = "APPEND";
    public static final String ARGS = "<mailbox> [<flag_list>] [<date_time>] literal";

    private final AppendCommandParser appendCommandParser = new AppendCommandParser();

    AppendCommand() {
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
        String mailboxName = appendCommandParser.mailbox(request);
        Flags flags = appendCommandParser.optionalAppendFlags(request);
        if (flags == null) {
            flags = new Flags();
        }
        Date receivedDate = appendCommandParser.optionalDateTime(request);
        if (receivedDate == null) {
            receivedDate = new Date();
        }
        MimeMessage message = appendCommandParser.mimeMessage(request);
        appendCommandParser.endLine(request);

        MailFolder folder;
        try {
            folder = getMailbox(mailboxName, session, true);
        } catch (FolderException e) {
            e.setResponseCode("TRYCREATE");
            throw e;
        }

        long uid = folder.appendMessage(message, flags, receivedDate);

        session.unsolicitedResponses(response);
        response.commandComplete(this, "APPENDUID" + SP + folder.getUidValidity() + SP + uid);
    }

    private static class AppendCommandParser extends CommandParser {
        /**
         * If the next character in the request is a '(', tries to read
         * a "flag_list" argument from the request. If not, returns a
         * MessageFlags with no flags set.
         */
        public Flags optionalAppendFlags(ImapRequestLineReader request)
            throws ProtocolException {
            char next = request.nextWordChar();
            if (next == '(') {
                return flagList(request);
            } else {
                return null;
            }
        }

        /**
         * If the next character in the request is a '"', tries to read
         * a DateTime argument. If not, returns null.
         */
        public Date optionalDateTime(ImapRequestLineReader request)
            throws ProtocolException {
            char next = request.nextWordChar();
            if (next == '"') {
                return dateTime(request);
            } else {
                return null;
            }
        }

        /**
         * Reads a MimeMessage encoded as a string literal from the request.
         * TODO shouldn't need to read as a string and write out bytes
         * use FixedLengthInputStream instead. Hopefully it can then be dynamic.
         *
         * @param request The Imap APPEND request
         * @return A MimeMessage read off the request.
         */
        public MimeMessage mimeMessage(ImapRequestLineReader request)
            throws ProtocolException {
            request.nextWordChar();
            byte[] mail = consumeLiteralAsBytes(request);

            try {
                return GreenMailUtil.newMimeMessage(new ByteArrayInputStream(mail));
            } catch (Exception e) {
                throw new ProtocolException("Can not create new mime message", e);
            }
        }

    }
}
