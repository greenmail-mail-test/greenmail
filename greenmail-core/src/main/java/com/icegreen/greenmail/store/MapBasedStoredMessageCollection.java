/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.util.MaxSizeLinkedHashMap;

import javax.mail.Flags;
import java.util.*;

import static java.lang.String.format;

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

    @Override
    public void expunge(List<FolderListener> mailboxListeners) {
        int i = 1;
        synchronized (mailMessages) {
            for (final Iterator<Map.Entry<Long, StoredMessage>> messageEntryIt = mailMessages.entrySet().iterator(); messageEntryIt.hasNext(); ) {
                final Map.Entry<Long, StoredMessage> messageEntry = messageEntryIt.next();
                if (messageEntry.getValue().isSet(Flags.Flag.DELETED)) {
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
