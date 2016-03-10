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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements SORT command described in <a href="https://tools.ietf.org/html/rfc5256">RFC5256</a>
 * <br><br>
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortCommand extends SelectedStateCommand implements UidEnabledCommand {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    public static final String NAME = "SORT";
    public static final String ARGS = "(<sort criteria>) <charset specification> <search term>";

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

        long[] uids = folder.search(sortTerm.getSearchTerm());
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
                for (SortKey sortKey : sortTerm.getSortCriteria()) {
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

}
