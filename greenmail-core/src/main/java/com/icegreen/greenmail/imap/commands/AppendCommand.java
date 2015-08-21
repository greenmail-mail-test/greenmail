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
import com.icegreen.greenmail.util.GreenMailUtil;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
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

    private AppendCommandParser appendCommandParser = new AppendCommandParser();

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

        folder.appendMessage(message, flags, receivedDate);

        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }

    /**
     * @see ImapCommand#getName
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    @Override
    public String getArgSyntax() {
        return ARGS;
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

/*
6.3.11. APPEND Command

   Arguments:  mailbox name
               OPTIONAL flag parenthesized list
               OPTIONAL date/time string
               message literal

   Responses:  no specific responses for this command

   Result:     OK - append completed
               NO - append error: can't append to that mailbox, error
                    in flags or date/time or message text
               BAD - command unknown or arguments invalid

      The APPEND command appends the literal argument as a new message
      to the end of the specified destination mailbox.  This argument
      SHOULD be in the format of an [RFC-822] message.  8-bit characters
      are permitted in the message.  A server implementation that is
      unable to preserve 8-bit data properly MUST be able to reversibly
      convert 8-bit APPEND data to 7-bit using a [MIME-IMB] content
      transfer encoding.

      Note: There MAY be exceptions, e.g. draft messages, in which
      required [RFC-822] header lines are omitted in the message literal
      argument to APPEND.  The full implications of doing so MUST be
      understood and carefully weighed.

   If a flag parenthesized list is specified, the flags SHOULD be set in
   the resulting message; otherwise, the flag list of the resulting
   message is set empty by default.

   If a date_time is specified, the internal date SHOULD be set in the
   resulting message; otherwise, the internal date of the resulting
   message is set to the current date and time by default.

   If the append is unsuccessful for any reason, the mailbox MUST be
   restored to its state before the APPEND attempt; no partial appending
   is permitted.

   If the destination mailbox does not exist, a server MUST return an
   error, and MUST NOT automatically create the mailbox.  Unless it is
   certain that the destination mailbox can not be created, the server
   MUST send the response code "[TRYCREATE]" as the prefix of the text
   of the tagged NO response.  This gives a hint to the client that it
   can attempt a CREATE command and retry the APPEND if the CREATE is
   successful.

   If the mailbox is currently selected, the normal new mail actions
   SHOULD occur.  Specifically, the server SHOULD notify the client
   immediately via an untagged EXISTS response.  If the server does not
   do so, the client MAY issue a NOOP command (or failing that, a CHECK
   command) after one or more APPEND commands.

   Example:    C: A003 APPEND saved-messages (\Seen) {310}
               C: Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)
               C: From: Fred Foobar <foobar@Blurdybloop.COM>
               C: Subject: afternoon meeting
               C: To: mooch@owatagu.siam.edu
               C: Message-Id: <B27397-0100000@Blurdybloop.COM>
               C: MIME-Version: 1.0
               C: Content-Type: TEXT/PLAIN; CHARSET=US-ASCII
               C:
               C: Hello Joe, do you think we can meet at 3:30 tomorrow?
               C:
               S: A003 OK APPEND completed

      Note: the APPEND command is not used for message delivery, because
      it does not provide a mechanism to transfer [SMTP] envelope
      information.

*/
