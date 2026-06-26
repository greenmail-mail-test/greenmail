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
import org.eclipse.angus.mail.imap.protocol.BASE64MailboxEncoder; // NOSONAR

/**
 * Handles processeing for the STATUS imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class StatusCommand extends AuthenticatedStateCommand {
    public static final String NAME = "STATUS";
    public static final String ARGS = "<mailbox> ( <status-data-item>+ )";

    private static final String MESSAGES = "MESSAGES";
    private static final String RECENT = "RECENT";
    private static final String UIDNEXT = "UIDNEXT";
    private static final String UIDVALIDITY = "UIDVALIDITY";
    private static final String UNSEEN = "UNSEEN";

    private StatusCommandParser statusParser = new StatusCommandParser();

    StatusCommand() {
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
        buffer.append('\"').append(CommandParser.escapeQuotedSpecials(BASE64MailboxEncoder.encode(mailboxName))).append('\"');
        buffer.append(SP);
        buffer.append('(');

        if (statusDataItems.messages) {
            buffer.append(MESSAGES);
            buffer.append(SP);
            buffer.append(folder.getMessageCount());
            buffer.append(SP);
        }

        if (statusDataItems.recent) {
            buffer.append(RECENT);
            buffer.append(SP);
            buffer.append(folder.getRecentCount(false));
            buffer.append(SP);
        }

        if (statusDataItems.uidNext) {
            buffer.append(UIDNEXT);
            buffer.append(SP);
            buffer.append(folder.getUIDNext());
            buffer.append(SP);
        }

        if (statusDataItems.uidValidity) {
            buffer.append(UIDVALIDITY);
            buffer.append(SP);
            buffer.append(folder.getUidValidity());
            buffer.append(SP);
        }

        if (statusDataItems.unseen) {
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

    private static class StatusCommandParser extends CommandParser {
        StatusDataItems statusDataItems(ImapRequestLineReader request)
            throws ProtocolException {
            StatusDataItems items = new StatusDataItems();

            request.nextWordChar();
            consumeChar(request, '(');
            CharacterValidator validator = new NoopCharValidator();
            String nextWord = consumeWord(request, validator);
            while (!nextWord.endsWith(")")) {
                addItem(nextWord, items);
                nextWord = consumeWord(request, validator);
            }
            // Got the closing ")", may be attached to a word.
            if (nextWord.length() > 1) {
                addItem(nextWord.substring(0, nextWord.length() - 1), items);
            }

            return items;
        }

        private void addItem(String nextWord, StatusDataItems items)
            throws ProtocolException {
            if (nextWord.equals(MESSAGES)) {
                items.messages = true;
            } else if (nextWord.equals(RECENT)) {
                items.recent = true;
            } else if (nextWord.equals(UIDNEXT)) {
                items.uidNext = true;
            } else if (nextWord.equals(UIDVALIDITY)) {
                items.uidValidity = true;
            } else if (nextWord.equals(UNSEEN)) {
                items.unseen = true;
            } else {
                throw new ProtocolException("Unknown status item: '" + nextWord + '\'');
            }
        }
    }

    private static class StatusDataItems {
        boolean messages;
        boolean recent;
        boolean uidNext;
        boolean uidValidity;
        boolean unseen;
    }
}
