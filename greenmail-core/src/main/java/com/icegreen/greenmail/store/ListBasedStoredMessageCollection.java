/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.mail.Flags;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public class ListBasedStoredMessageCollection implements StoredMessageCollection {
    final Logger log = LoggerFactory.getLogger(ListBasedStoredMessageCollection.class);

    private final List<StoredMessage> mailMessages = Collections.synchronizedList(new ArrayList<StoredMessage>());

    @Override
    public int size() {
        return mailMessages.size();
    }

    @Override
    public void add(StoredMessage storedMessage) {
        mailMessages.add(storedMessage);
    }

    @Override
    public void clear() {
        mailMessages.clear();
    }

    private void expungeMessage(int msn, Collection<FolderListener> mailboxListeners) {
        log.debug("Expunge message with messageNumber: " + msn);
        // Notify all the listeners of the pending delete
        synchronized (mailboxListeners) {
            deleteMessage(msn);
            log.debug("Deleted message with messageNumber: " + msn);
            for (FolderListener expungeListener : mailboxListeners) {
                log.debug("Informed listener: " + expungeListener);
                expungeListener.expunged(msn);
            }
        }
    }

    private void deleteMessage(int msn) {
        synchronized (mailMessages) {
            mailMessages.remove(msn - 1); // input is 1 based index
        }
    }

    @Override
    public int getFirstUnseen() {
        synchronized (mailMessages) {
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = mailMessages.get(i);
                if (!message.isSet(Flags.Flag.SEEN)) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        synchronized (mailMessages) {
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = mailMessages.get(i);
                if (message.getUid() == uid) {
                    return i + 1;
                }
            }
        }
        throw new FolderException("No such message.");
    }

    @Override
    public List<StoredMessage> getMessages(MsgRangeFilter range) {
        List<StoredMessage> ret = new ArrayList<>();

        synchronized (mailMessages) {
            for (int i = 0; i < mailMessages.size(); i++) {
                if (range.includes(i + 1)) {
                    ret.add(mailMessages.get(i));
                }
            }
        }

        return ret;
    }

    @Override
    public List<StoredMessage> getMessages() {
        synchronized (mailMessages) {
            // Return new list since we don't want to give the caller access to the internal list
            return new ArrayList<>(mailMessages);
        }
    }

    @Override
    public long[] getMessageUids() {
        synchronized (mailMessages) {
            long[] uids = new long[mailMessages.size()];
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = mailMessages.get(i);
                uids[i] = message.getUid();
            }
            return uids;
        }
    }

    /**
     * Returns the message UID of the last message in the mailbox, or -1L to show that no such message exist (e.g. when the
     * mailbox is empty).
     *
     * @return - a valid UID of the last message or -1
     */
    @Override
    public long getLastMessageUid() {
        long result = -1;
        synchronized (mailMessages) {
            if (!this.mailMessages.isEmpty()) {
                result = this.mailMessages.get(this.mailMessages.size() - 1).getUid();
            }
        }
        return result;
    }

    @Override
    public void expunge(List<FolderListener> folderListeners) {
        expunge(folderListeners, null);
    }

    @Override
    public void expunge(List<FolderListener> folderListeners, IdRange[] idRanges) {
        synchronized (mailMessages) {
            for (int i = mailMessages.size() - 1; i >= 0; i--) {
                StoredMessage message = mailMessages.get(i);
                if (message.isSet(Flags.Flag.DELETED) &&
                        (idRanges == null || IdRange.containsUid(idRanges, message.getUid()))) {
                    expungeMessage(i + 1, folderListeners); // MSNs start counting at 1
                }
            }
        }
    }

    @Override
    public StoredMessage get(int i) {
        return mailMessages.get(i);
    }

    @Override
    public Iterator<StoredMessage> iterator() {
        return mailMessages.iterator();
    }
}
