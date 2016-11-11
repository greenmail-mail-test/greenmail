/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.Flags;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.util.MaxSizeLinkedHashMap;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public class MapBasedStoredMessageCollection implements StoredMessageCollection {
    private final Map<Long, StoredMessage> mailMessages;

    public MapBasedStoredMessageCollection(final int maximumMapSize) {
        mailMessages = Collections.synchronizedMap(new MaxSizeLinkedHashMap<Long, StoredMessage>(maximumMapSize));
    }

    @Override
    public int size() {
        return mailMessages.size();
    }

    @Override
    public void add(StoredMessage storedMessage) {
        mailMessages.put(storedMessage.getUid(), storedMessage);
    }

    @Override
    public void clear() {
        mailMessages.clear();
    }

    @Override
    public int getFirstUnseen() {
        synchronized (mailMessages) {
            int i = 1;
            for (StoredMessage message : mailMessages.values()) {
                if (!message.isSet(Flags.Flag.SEEN)) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        synchronized (mailMessages) {
            if (mailMessages.containsKey(uid)) {
                int i = 1;
                for (Long messageUid : mailMessages.keySet()) {
                    if (messageUid.equals(uid)) {
                        return i;
                    }
                    i++;
                }
            }
            throw new FolderException("No such message.");
        }
    }

    @Override
    public List<StoredMessage> getMessages(MsgRangeFilter range) {
        final List<StoredMessage> messagesInRange = new ArrayList<>();
        int i = 0;
        synchronized (mailMessages) {
            for (final StoredMessage message : mailMessages.values()) {
                if (range.includes(i)) {
                    messagesInRange.add(message);
                }
                i++;
            }
        }
        return messagesInRange;
    }

    @Override
    public List<StoredMessage> getMessages() {
        synchronized (mailMessages) {
            return new ArrayList<>(mailMessages.values());
        }
    }

    @Override
    public long[] getMessageUids() {
        synchronized (mailMessages) {
            final long[] uids = new long[mailMessages.size()];
            int i = 0;
            for (final Long uid : mailMessages.keySet()) {
                uids[i] = uid;
                i++;
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
                StoredMessage[] msgList = this.mailMessages.values().toArray(new StoredMessage[0]);
                if (msgList.length != 0) {
                    result = msgList[msgList.length - 1].getUid();
                }
            }
        }
        return result;
    }

    @Override
    public void expunge(List<FolderListener> mailboxListeners) {
        expunge(mailboxListeners, null);
    }

    @Override
    public void expunge(List<FolderListener> mailboxListeners, IdRange[] idRanges) {
        int i = 1;
        synchronized (mailMessages) {
            for (final Iterator<Map.Entry<Long, StoredMessage>> messageEntryIt = mailMessages.entrySet().iterator(); messageEntryIt.hasNext(); ) {
                final Map.Entry<Long, StoredMessage> messageEntry = messageEntryIt.next();
                if (messageEntry.getValue().isSet(Flags.Flag.DELETED) &&
                        (idRanges == null || IdRange.containsUid(idRanges, messageEntry.getValue().getUid()))) {
                    // Notify all the listeners of the pending delete
                    synchronized (mailboxListeners) {
                        messageEntryIt.remove();
                        for (FolderListener expungeListener : mailboxListeners) {
                            expungeListener.expunged(i);
                        }
                    }
                }
                i++;
            }
        }
    }

    @Override
    public StoredMessage get(final int messageIndex) {
        synchronized (mailMessages) {
            int i = 0;
            for (final StoredMessage message : mailMessages.values()) {
                if (messageIndex == i) {
                    return message;
                }
                i++;
            }
        }
        throw new IllegalArgumentException(format("No message with index %d found", messageIndex));
    }

    @Override
    public Iterator<StoredMessage> iterator() {
        return mailMessages.values().iterator();
    }
}
