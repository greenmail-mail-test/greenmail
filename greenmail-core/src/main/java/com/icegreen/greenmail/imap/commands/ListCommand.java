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
import org.eclipse.angus.mail.imap.protocol.BASE64MailboxDecoder; // NOSONAR
import org.eclipse.angus.mail.imap.protocol.BASE64MailboxEncoder; // NOSONAR

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles processeing for the LIST imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class ListCommand extends AuthenticatedStateCommand {
    public static final String NAME = "LIST";
    public static final String ARGS = "<reference-name> <mailbox-name-with-wildcards>";

    private ListCommandParser listParser = new ListCommandParser();

    ListCommand() {
        super(NAME, ARGS);
    }

    ListCommand(String name) {
        super(name, null);
    }

    /**
     * @see CommandTemplate#doProcess
     */
    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
        throws ProtocolException, FolderException {
        String referenceName = listParser.mailbox(request);
        String mailboxPattern = listParser.listMailbox(request);
        listParser.endLine(request);

        // Should the #user.userName section be removed from names returned?
        boolean removeUserPrefix;

        Collection<MailFolder> mailboxes;
        if (mailboxPattern.isEmpty()) {
            // An empty mailboxPattern signifies a request for the hierarchy delimiter
            // and root name of the referenceName argument

            String referenceRoot;
            if (referenceName.startsWith(NAMESPACE_PREFIX)) {
                // A qualified reference name - get the first element,
                // and don't remove the user prefix
                removeUserPrefix = false;
                int firstDelimiter = referenceName.indexOf(HIERARCHY_DELIMITER_CHAR);
                if (firstDelimiter == -1) {
                    referenceRoot = referenceName;
                } else {
                    referenceRoot = referenceName.substring(0, firstDelimiter);
                }
            } else {
                // A relative reference name - need to remove user prefix from results.
                referenceRoot = "";
                removeUserPrefix = true;
            }

            // Get the mailbox for the reference name.
            MailFolder referenceFolder = getMailbox(referenceRoot, session, false);

            // If it doesn't exist, act as though "" was passed for reference name.
            if (referenceFolder == null) {
                referenceFolder = getMailbox("", session, true);
                removeUserPrefix = true;
            }

            mailboxes = new ArrayList<>(1);
            mailboxes.add(referenceFolder);
        } else {
            String searchPattern;

            // If the mailboxPattern is fully qualified, ignore the
            // reference name.
            if (mailboxPattern.charAt(0) == NAMESPACE_PREFIX_CHAR) {
                searchPattern = mailboxPattern;
            } else {
                searchPattern = combineSearchTerms(referenceName, mailboxPattern);
            }

            // If the search pattern is relative, need to remove user prefix from results.
            removeUserPrefix = searchPattern.charAt(0) != NAMESPACE_PREFIX_CHAR;

            mailboxes = doList(session, searchPattern);
        }

        String personalNamespace = USER_NAMESPACE + HIERARCHY_DELIMITER_CHAR +
            session.getUser().getQualifiedMailboxName();
        int prefixLength = personalNamespace.length();

        for (final MailFolder folder : mailboxes) {
            StringBuilder message = new StringBuilder("(");
            if (!folder.isSelectable()) {
                message.append("\\Noselect");
            }
            message.append(") \"");
            message.append(HIERARCHY_DELIMITER_CHAR);
            message.append("\" ");

            String mailboxName = folder.getFullName();
            if (removeUserPrefix) {
                if (mailboxName.length() <= prefixLength) {
                    mailboxName = "";
                } else {
                    mailboxName = mailboxName.substring(prefixLength + 1);
                }
            }

            if (mailboxName.isEmpty()) {
                message.append("\"\"");
            } else {
                message.append('\"').append(CommandParser.escapeQuotedSpecials(BASE64MailboxEncoder.encode(mailboxName))).append('\"');
            }

            response.commandResponse(this, message.toString());
        }

        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }

    protected Collection<MailFolder> doList(ImapSession session, String searchPattern) throws FolderException {
        return session.getHost().listMailboxes(session.getUser(), searchPattern);
    }

    private String combineSearchTerms(String referenceName, String mailboxMatch) {

        // Otherwise, combine the referenceName and mailbox name.
        StringBuilder buffer = new StringBuilder(mailboxMatch);

        // Make sure the 2 strings are joined by only one HIERARCHY_DELIMITER_CHAR
        if (referenceName.endsWith(HIERARCHY_DELIMITER)) {
            if (buffer.charAt(0) == HIERARCHY_DELIMITER_CHAR) {
                buffer.deleteCharAt(0);
            }
        } else {
            if ((buffer.charAt(0) != HIERARCHY_DELIMITER_CHAR) && (!referenceName.isEmpty())) {
                buffer.insert(0, HIERARCHY_DELIMITER_CHAR);
            }
        }

        buffer.insert(0, referenceName);
        return buffer.toString();
    }

    private static class ListCommandParser extends CommandParser {
        /**
         * Reads an argument of type "list_mailbox" from the request, which is
         * the second argument for a LIST or LSUB command. Valid values are a "string"
         * argument, an "atom" with wildcard characters.
         *
         * @return An argument of type "list_mailbox"
         */
        public String listMailbox(ImapRequestLineReader request) throws ProtocolException {
            char next = request.nextWordChar();
            String name;
            switch (next) {
                case '"':
                    name = consumeQuoted(request);
                    break;
                case '{':
                    name = consumeLiteral(request);
                    break;
                default:
                    name = consumeWord(request, new ListCharValidator());
            }
            return BASE64MailboxDecoder.decode(name);
        }

        private class ListCharValidator extends AtomCharValidator {
            @Override
            public boolean isValid(char chr) {
                return isListWildcard(chr) || super.isValid(chr);
            }
        }
    }
}
