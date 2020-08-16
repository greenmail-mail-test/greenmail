package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.store.StoredMessage;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sorts messages by given sort term.
 *
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class StoredMessageSorter implements Comparator<StoredMessage> {

    private final SortTerm sortTerm;

    private final AtomicBoolean reverse = new AtomicBoolean();

    StoredMessageSorter(SortTerm sortTerm) {
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
            throw new IllegalArgumentException(e.getMessage(), e);
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
            return multiplier;
        } else if (c2 == null) {
            return multiplier * -1;
        }
        return multiplier * c1.compareTo(c2);
    }
}
