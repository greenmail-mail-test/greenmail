/* -------------------------------------------------------------------
 * This software is released under the Apache license 2.0
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;

import javax.mail.Flags;
import java.util.*;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public class ListBasedStoredMessageCollection implements StoredMessageCollection {
    private final List<StoredMessage> mailMessages = Collections.synchronizedList(new ArrayList<>());

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
        // Notify all the listeners of the pending delete
        synchronized (mailboxListeners) {
            deleteMessage(msn);
            for (FolderListener expungeListener : mailboxListeners) {
                expungeListener.expunged(msn);
            }
        }
    }

    private void deleteMessage(int msn) {
        mailMessages.remove(msn - 1); // input is 1 based index
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
        return Collections.unmodifiableList(mailMessages);
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
