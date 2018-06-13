/* -------------------------------------------------------------------
 * This software is released under the Apache license 2.0
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;

import java.util.*;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.mail.MovingMessage;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
class HierarchicalFolder implements MailFolder, UIDFolder {
    private static final Flags PERMANENT_FLAGS = new Flags();

    static {
        PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
        PERMANENT_FLAGS.add(Flags.Flag.DELETED);
        PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
        PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
        PERMANENT_FLAGS.add(Flags.Flag.SEEN);
    }

    private final StoredMessageCollection mailMessages = new ListBasedStoredMessageCollection();
    private final List<FolderListener> _mailboxListeners = Collections.synchronizedList(new ArrayList<FolderListener>());
    protected String name;
    private final Collection<HierarchicalFolder> children;
    private HierarchicalFolder parent;
    private boolean isSelectable = false;
    private long nextUid = 1;
    private long uidValidity;

    protected HierarchicalFolder(HierarchicalFolder parent, String name) {
        this.name = name;
        this.parent = parent;
        children = new ArrayList<>();
        // From https://tools.ietf.org/html/rfc3501#section-2.3.1.1 :
        // "A good UIDVALIDITY value to use in this case
        //  is a 32-bit representation of the creation date/time of
        //  the mailbox."
        uidValidity = System.currentTimeMillis()/1000L; // Must fit in unsigned 32bit, which works till > 2100
        if(uidValidity >= 2L * Integer.MAX_VALUE) { // Enforce max
            throw new IllegalStateException(
                    "UIDVALIDITY value " + uidValidity + " does not fit as unsigned 32 bit int " +
                            2L * Integer.MAX_VALUE);
        }
    }

    /**
     * An immutable collection of children.
     *
     * @return the children.
     */
    public Collection<HierarchicalFolder> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    public HierarchicalFolder getParent() {
        return parent;
    }

    void moveToNewParent(HierarchicalFolder newParent) {
        synchronized (newParent.children) {
            if (!newParent.children.contains(this)) {
                parent = newParent;
                parent.children.add(this);
            }
        }
    }

    HierarchicalFolder getChild(String name) {
        synchronized (children) {
            for (HierarchicalFolder child : children) {
                if (child.getName().equalsIgnoreCase(name)) {
                    return child;
                }
            }
        }
        return null;
    }


    HierarchicalFolder createChild(String mailboxName) {
        HierarchicalFolder child = new HierarchicalFolder(this, mailboxName);
        synchronized (children) {
            children.add(child);
        }
        return child;
    }

    void removeChild(HierarchicalFolder toDelete) {
        synchronized (children) {
            children.remove(toDelete);
        }
    }

    boolean hasChildren() {
        synchronized (children) {
            return !children.isEmpty();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getFullName() {
        return parent.getFullName() + ImapConstants.HIERARCHY_DELIMITER_CHAR + name;
    }

    @Override
    public Flags getPermanentFlags() {
        return PERMANENT_FLAGS;
    }

    @Override
    public int getMessageCount() {
        synchronized (mailMessages) {
            return mailMessages.size();
        }
    }

    @Override
    public long getUidValidity() {
        return uidValidity;
    }

    @Override
    public long getUidNext() {
        return nextUid;
    }

    @Override
    public int getUnseenCount() {
        int count = 0;
        synchronized (mailMessages) {
            for (StoredMessage message : mailMessages) {
                if (!message.isSet(Flags.Flag.SEEN)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the 1-based index of the first unseen message. Unless there are outstanding
     * expunge responses in the ImapSessionMailbox, this will correspond to the MSN for
     * the first unseen.
     */
    @Override
    public int getFirstUnseen() {
        return mailMessages.getFirstUnseen();
    }

    @Override
    public int getRecentCount(boolean reset) {
        int count = 0;
        synchronized (mailMessages) {
            for (StoredMessage message : mailMessages) {
                if (message.isSet(Flags.Flag.RECENT)) {
                    count++;
                    if (reset) {
                        message.setFlag(Flags.Flag.RECENT, false);
                    }
                }
            }
        }
        return count;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        return mailMessages.getMsn(uid);
    }

    @Override
    public void signalDeletion() {
        // Notify all the listeners of the new message
        synchronized (_mailboxListeners) {
            for (FolderListener listener : _mailboxListeners) {
                listener.mailboxDeleted();
            }
        }

    }

    @Override
    public List<StoredMessage> getMessages(MsgRangeFilter range) {
        return mailMessages.getMessages(range);
    }

    @Override
    public List<StoredMessage> getMessages() {
        return mailMessages.getMessages();
    }

    @Override
    public List<StoredMessage> getNonDeletedMessages() {
        List<StoredMessage> ret = new ArrayList<>();

        synchronized (mailMessages) {
            for (StoredMessage mailMessage : mailMessages) {
                if (!mailMessage.getFlags().contains(Flags.Flag.DELETED)) {
                    ret.add(mailMessage);
                }
            }
        }

        return ret;
    }

    @Override
    public boolean isSelectable() {
        return isSelectable;
    }

    public void setSelectable(boolean selectable) {
        isSelectable = selectable;
    }

    @Override
    public long appendMessage(MimeMessage message,
                              Flags flags,
                              Date receivedDate) {
        long uid = nextUid;
        nextUid++;

        try {
            message.setFlags(flags, true);
            message.setFlag(Flags.Flag.RECENT, true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not set flags", e);
        }
        StoredMessage storedMessage = new StoredMessage(message,
                receivedDate, uid);

        int newMsn;
        synchronized (mailMessages) {
            mailMessages.add(storedMessage);
            newMsn = mailMessages.size();
        }

        // Notify all the listeners of the new message
        synchronized (_mailboxListeners) {
            for (FolderListener _mailboxListener : _mailboxListeners) {
                _mailboxListener.added(newMsn);
            }
        }

        return uid;
    }

    @Override
    public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        int msn = getMsn(uid);
        StoredMessage message = mailMessages.get(msn - 1);

        message.setFlags(flags, value);

        Long uidNotification = null;
        if (addUid) {
            uidNotification = uid;
        }
        notifyFlagUpdate(msn, message.getFlags(), uidNotification, silentListener);
    }

    @Override
    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        int msn = getMsn(uid);
        StoredMessage message = mailMessages.get(msn - 1);
        message.setFlags(MessageFlags.ALL_FLAGS, false);
        message.setFlags(flags, true);

        Long uidNotification = null;
        if (addUid) {
            uidNotification = uid;
        }
        notifyFlagUpdate(msn, message.getFlags(), uidNotification, silentListener);
    }

    private void notifyFlagUpdate(int msn, Flags flags, Long uidNotification, FolderListener silentListener) {
        synchronized (_mailboxListeners) {
            for (FolderListener listener : _mailboxListeners) {
                if (listener == silentListener) {
                    continue;
                }

                listener.flagsUpdated(msn, flags, uidNotification);
            }
        }
    }

    @Override
    public void deleteAllMessages() {
        synchronized (mailMessages) {
            mailMessages.clear();
        }
    }

    @Override
    public void store(MovingMessage mail) {
        store(mail.getMessage());
    }


    @Override
    public void store(MimeMessage message) {
        Date receivedDate = new Date();
        Flags flags = new Flags();
        appendMessage(message, flags, receivedDate);
    }

    @Override
    public StoredMessage getMessage(long uid) {
        synchronized (mailMessages) {
            for (StoredMessage mailMessage : mailMessages) {
                if (mailMessage.getUid() == uid) {
                    return mailMessage;
                }
            }
        }
        return null;
    }

    @Override
    public long[] getMessageUids() {
        return mailMessages.getMessageUids();
    }

    @Override
    public long[] search(SearchTerm searchTerm) {
        List<StoredMessage> matchedMessages = new ArrayList<>();

        synchronized (mailMessages) {
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage mailMessage = mailMessages.get(i);
                // Update message sequence number for potential sequence set search
                // https://tools.ietf.org/html/rfc3501#page-10
                mailMessage.updateMessageNumber(i + 1);
                if (searchTerm.match(mailMessage.getMimeMessage())) {
                    matchedMessages.add(mailMessage);
                }
            }
        }

        long[] matchedUids = new long[matchedMessages.size()];
        for (int i = 0; i < matchedUids.length; i++) {
            StoredMessage storedMessage = matchedMessages.get(i);
            long uid = storedMessage.getUid();
            matchedUids[i] = uid;
        }
        return matchedUids;
    }

    @Override
    public long copyMessage(long uid, MailFolder toFolder)
            throws FolderException {
        StoredMessage originalMessage = getMessage(uid);
        MimeMessage newMime;
        try {
            newMime = new MimeMessage(originalMessage.getMimeMessage());
        } catch (MessagingException e) {
            throw new FolderException("Can not copy message " + uid + " to folder " + toFolder, e);
        }

        return toFolder.appendMessage(newMime, originalMessage.getFlags(), originalMessage.getReceivedDate());
    }

    @Override
    public void expunge() {
        mailMessages.expunge(_mailboxListeners);
    }

    @Override
    public void expunge(IdRange[] idRanges) {
        mailMessages.expunge(_mailboxListeners, idRanges);
    }

    @Override
    public void addListener(FolderListener listener) {
        synchronized (_mailboxListeners) {
            _mailboxListeners.add(listener);
        }
    }

    @Override
    public void removeListener(FolderListener listener) {
        synchronized (_mailboxListeners) {
            _mailboxListeners.remove(listener);
        }
    }

    @Override
    public String toString() {
        return "HierarchicalFolder{" +
                "name='" + name + '\'' +
                ", parent=" + parent +
                ", isSelectable=" + isSelectable +
                '}';
    }

    @Override
    public long getUIDValidity() {
        return getUidValidity();
    }

    @Override
    public Message getMessageByUID(long uid) {
        return getMessage(uid).getMimeMessage();
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) {
        synchronized (mailMessages) {
            List<Message> messages = new ArrayList<>();
            for (StoredMessage mailMessage : mailMessages) {
                final long uid = mailMessage.getUid();
                if (uid >= start && uid <= end) {
                    messages.add(mailMessage.getMimeMessage());
                }
            }
            return messages.toArray(new Message[messages.size()]);
        }
    }

    @Override
    public Message[] getMessagesByUID(long[] uids) {
        synchronized (mailMessages) {
            List<Message> messages = new ArrayList<>(uids.length);
            Map<Long, StoredMessage> uid2Msg = new HashMap<>(mailMessages.size());
            for (StoredMessage mailMessage : mailMessages) {
                uid2Msg.put(mailMessage.getUid(), mailMessage);
            }
            for (long uid : uids) {
                final StoredMessage storedMessage = uid2Msg.get(uid);
                if (storedMessage != null) {
                    messages.add(storedMessage.getMimeMessage());
                }
            }
            return messages.toArray(new Message[messages.size()]);
        }
    }

    @Override
    public long getUID(Message message) {
        // Check if we have a message with same object reference ... otherwise, not supported.
        synchronized (mailMessages) {
            for (StoredMessage mailMessage : mailMessages) {
                if (mailMessage.getMimeMessage() == message) {
                    return mailMessage.getUid();
                }
            }
        }
        throw new IllegalStateException("No match found for " + message);
    }

}
