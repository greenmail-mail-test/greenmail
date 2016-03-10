package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortCommand extends SelectedStateCommand implements UidEnabledCommand {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    public static final String NAME = "SORT";
    public static final String ARGS = "(<sort criteria>) <charset specification> <search term>";

    private SearchCommandParser searchCommandParser = new SearchCommandParser();
    private SortCommandParser parser = new SortCommandParser();

    SortCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        doProcess(request, response, session, false);
    }

    @Override
    public void doProcess(final ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids) throws ProtocolException, FolderException {
        final SortTerm sortTerm = parser.sortTerm(request);

        final MailFolder folder = session.getSelected();

        long[] uids = folder.search(sortTerm.searchTerm);
        List<StoredMessage> messages = new ArrayList<>();
        for (long uid : uids) {
            messages.add(folder.getMessage(uid));
        }

        Collections.sort(messages, new StoredMessageComparator(sortTerm));

        StringBuilder idList = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                idList.append(SP);
            }
            StoredMessage message = messages.get(i);
            if (useUids) {
                idList.append(message.getUid());
            } else {
                int msn = folder.getMsn(message.getUid());
                idList.append(msn);
            }
        }

        response.commandResponse(this, idList.toString());

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

    private class StoredMessageComparator implements Comparator<StoredMessage> {

        private SortTerm sortTerm;

        private final AtomicBoolean reverse = new AtomicBoolean();

        StoredMessageComparator(SortTerm sortTerm) {
            this.sortTerm = sortTerm;
        }

        @Override
        public int compare(StoredMessage m1, StoredMessage m2) {
            try {
                int compareResult = 0;
                for (SortKey sortKey : sortTerm.sortCriteria) {
                    switch (sortKey) {
                        case REVERSE:
                            reverse.set(true);
                            break;
                        case ARRIVAL:
                            compareResult = doCompare(m1.getReceivedDate(), m2.getReceivedDate());
                            break;
                        case CC:
                            compareResult = doCompare(getCc(m1), getCc(m2));
                            break;
                        case DATE:
                            compareResult = doCompare(m1.getMimeMessage().getSentDate(), m2.getMimeMessage().getSentDate());
                            break;
                        case FROM:
                            compareResult = doCompare(getFrom(m1), getFrom(m2));
                            break;
                        case SIZE:
                            compareResult = doCompare(m1.getMimeMessage().getSize(), m2.getMimeMessage().getSize());
                            break;
                        case SUBJECT:
                            compareResult = doCompare(m1.getMimeMessage().getSubject(), m2.getMimeMessage().getSubject());
                            break;
                        case TO:
                            compareResult = doCompare(getTo(m1), getTo(m2));
                            break;
                        default:
                            break;
                    }
                    if (compareResult != 0) {
                        break;
                    }
                }
                return compareResult;
            } catch (MessagingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        private String getFrom(StoredMessage message) throws MessagingException {
            Address[] addresses = message.getMimeMessage().getFrom();
            if (addresses == null || addresses.length == 0) {
                return null;
            }
            return String.valueOf(addresses[0]);
        }

        private String getTo(StoredMessage message) throws MessagingException {
            Address[] addresses = message.getMimeMessage().getRecipients(Message.RecipientType.TO);
            if (addresses == null || addresses.length == 0) {
                return null;
            }
            return String.valueOf(addresses[0]);
        }

        private String getCc(StoredMessage message) throws MessagingException {
            Address[] addresses = message.getMimeMessage().getRecipients(Message.RecipientType.CC);
            if (addresses == null || addresses.length == 0) {
                return null;
            }
            return String.valueOf(addresses[0]);
        }

        private int doCompare(Comparable c1, Comparable c2) {
            int multiplier = reverse.getAndSet(false) ? -1 : 1;
            if (c1 == c2) {
                return 0;
            } else if (c1 == null) {
                return multiplier * 1;
            } else if (c2 == null) {
                return multiplier * -1;
            }
            return multiplier * c1.compareTo(c2);
        }
    }


    private class SortCommandParser extends CommandParser {
        public SortTerm sortTerm(ImapRequestLineReader request) throws ProtocolException {
            SortTerm sortTerm = new SortTerm();

            /* Sort criteria */
            char next = request.nextChar();
            StringBuilder sb = new StringBuilder();
            boolean buffering = false;
            while (next != '\n') {
                if (next != '(' && next != ')' && next != 32 /* space */ && next != 13 /* cr */) {
                    sb.append(next);
                } else {
                    if (buffering) {
                        sortTerm.sortCriteria.add(SortKey.valueOf(sb.toString()));
                        sb = new StringBuilder();
                    } else {
                        buffering = next == '(';
                    }
                }
                request.consume();
                if (next == ')') {
                    break;
                }
                next = request.nextChar();
            }

            /* Charset */
            sortTerm.charset = consumeWord(request, new ATOM_CHARValidator());

            /* Search term */
            sortTerm.searchTerm = searchCommandParser.searchTerm(request);
            searchCommandParser.endLine(request);

            return sortTerm;
        }
    }

    private class SortTerm {
        private final List<SortKey> sortCriteria = new ArrayList<>();
        private String charset;
        private SearchTerm searchTerm;
    }

/*
    BASE.6.4.SORT. SORT Command

   Arguments:  sort program
               charset specification
               searching criteria (one or more)

   Data:       untagged responses: SORT

   Result:     OK - sort completed
               NO - sort error: can't sort that charset or
                    criteria
               BAD - command unknown or arguments invalid

      The SORT command is a variant of SEARCH with sorting semantics for
      the results.  There are two arguments before the searching
      criteria argument: a parenthesized list of sort criteria, and the
      searching charset.

      The charset argument is mandatory (unlike SEARCH) and indicates
      the [CHARSET] of the strings that appear in the searching
      criteria.  The US-ASCII and [UTF-8] charsets MUST be implemented.
      All other charsets are optional.

      There is also a UID SORT command that returns unique identifiers
      instead of message sequence numbers.  Note that there are separate
      searching criteria for message sequence numbers and UIDs; thus,
      the arguments to UID SORT are interpreted the same as in SORT.
      This is analogous to the behavior of UID SEARCH, as opposed to UID
      COPY, UID FETCH, or UID STORE.

      The SORT command first searches the mailbox for messages that
      match the given searching criteria using the charset argument for
      the interpretation of strings in the searching criteria.  It then
      returns the matching messages in an untagged SORT response, sorted
      according to one or more sort criteria.

      Sorting is in ascending order.  Earlier dates sort before later
      dates; smaller sizes sort before larger sizes; and strings are
      sorted according to ascending values established by their
      collation algorithm (see "Internationalization Considerations").

      If two or more messages exactly match according to the sorting
      criteria, these messages are sorted according to the order in
      which they appear in the mailbox.  In other words, there is an
      implicit sort criterion of "sequence number".

      When multiple sort criteria are specified, the result is sorted in
      the priority order that the criteria appear.  For example,
      (SUBJECT DATE) will sort messages in order by their base subject
      text; and for messages with the same base subject text, it will
      sort by their sent date.

      Untagged EXPUNGE responses are not permitted while the server is
      responding to a SORT command, but are permitted during a UID SORT
      command.

      The defined sort criteria are as follows.  Refer to the Formal
      Syntax section for the precise syntactic definitions of the
      arguments.  If the associated RFC-822 header for a particular
      criterion is absent, it is treated as the empty string.  The empty
      string always collates before non-empty strings.

      ARRIVAL
         Internal date and time of the message.  This differs from the
         ON criteria in SEARCH, which uses just the internal date.

      CC
         [IMAP] addr-mailbox of the first "cc" address.

      DATE
         Sent date and time, as described in section 2.2.

      FROM
         [IMAP] addr-mailbox of the first "From" address.

      REVERSE
         Followed by another sort criterion, has the effect of that
         criterion but in reverse (descending) order.
            Note: REVERSE only reverses a single criterion, and does not
            affect the implicit "sequence number" sort criterion if all
            other criteria are identical.  Consequently, a sort of
            REVERSE SUBJECT is not the same as a reverse ordering of a
            SUBJECT sort.  This can be avoided by use of additional
            criteria, e.g., SUBJECT DATE vs. REVERSE SUBJECT REVERSE
            DATE.  In general, however, it's better (and faster, if the
            client has a "reverse current ordering" command) to reverse
            the results in the client instead of issuing a new SORT.


      SIZE
         Size of the message in octets.

      SUBJECT
         Base subject text.

      TO
         [IMAP] addr-mailbox of the first "To" address.

   Example:    C: A282 SORT (SUBJECT) UTF-8 SINCE 1-Feb-1994
               S: * SORT 2 84 882
               S: A282 OK SORT completed
               C: A283 SORT (SUBJECT REVERSE DATE) UTF-8 ALL
               S: * SORT 5 3 4 1 2
               S: A283 OK SORT completed
               C: A284 SORT (SUBJECT) US-ASCII TEXT "not in mailbox"
               S: * SORT
               S: A284 OK SORT completed


     */
}
