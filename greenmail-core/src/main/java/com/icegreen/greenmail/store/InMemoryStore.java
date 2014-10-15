/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.mail.MovingMessage;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.*;

/**
 * A simple in-memory implementation of {@link Store}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class InMemoryStore
        implements Store, ImapConstants {
    private RootFolder rootMailbox = new RootFolder();
    private static final Flags PERMANENT_FLAGS = new Flags();
    boolean quotaSupported = true;

    static {
        PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
        PERMANENT_FLAGS.add(Flags.Flag.DELETED);
        PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
        PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
        PERMANENT_FLAGS.add(Flags.Flag.SEEN);
    }

    private Map<String,Set<Quota>> quotaMap = new HashMap<String, Set<Quota>>();
    private static final Quota[] EMPTY_QUOTAS = new Quota[0];

    public MailFolder getMailbox(String absoluteMailboxName) {
        StringTokenizer tokens = new StringTokenizer(absoluteMailboxName, HIERARCHY_DELIMITER);

        // The first token must be "#mail"
        if (!tokens.hasMoreTokens() ||
                !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE)) {
            return null;
        }

        HierarchicalFolder parent = rootMailbox;
        while (parent != null && tokens.hasMoreTokens()) {
            String childName = tokens.nextToken();
            parent = parent.getChild(childName);
        }
        return parent;
    }

    public MailFolder getMailbox(MailFolder parent, String name) {
        return ((HierarchicalFolder) parent).getChild(name);
    }

    public MailFolder createMailbox(MailFolder parent,
                                    String mailboxName,
                                    boolean selectable)
            throws FolderException {
        if (mailboxName.indexOf(HIERARCHY_DELIMITER_CHAR) != -1) {
            throw new FolderException("Invalid mailbox name.");
        }
        HierarchicalFolder castParent = (HierarchicalFolder) parent;
        HierarchicalFolder child = new HierarchicalFolder(castParent, mailboxName);
        castParent.getChildren().add(child);
        child.setSelectable(selectable);
        return child;
    }

    public void deleteMailbox(MailFolder folder) throws FolderException {
        HierarchicalFolder toDelete = (HierarchicalFolder) folder;

        if (!toDelete.getChildren().isEmpty()) {
            throw new FolderException("Cannot delete mailbox with children.");
        }

        if (toDelete.getMessageCount() != 0) {
            throw new FolderException("Cannot delete non-empty mailbox");
        }

        HierarchicalFolder parent = toDelete.getParent();
        parent.getChildren().remove(toDelete);
    }

    public void renameMailbox(MailFolder existingFolder, String newName) throws FolderException {
        HierarchicalFolder toRename = (HierarchicalFolder) existingFolder;
        HierarchicalFolder parent = toRename.getParent();

        int idx = newName.lastIndexOf(ImapConstants.HIERARCHY_DELIMITER_CHAR);
        String newFolderName;
        String newFolderPathWithoutName;
        if(idx>0) {
            newFolderName = newName.substring(idx+1);
            newFolderPathWithoutName = newName.substring(0,idx);
        }
        else {
            newFolderName = newName;
            newFolderPathWithoutName = "";
        }

        if(parent.getName().equals(newFolderPathWithoutName)) {
            // Simple rename
            toRename.setName(newFolderName);
        }
        else {
            // Hierarchy change
            parent.getChildren().remove(toRename);
            HierarchicalFolder userFolder = findParentByName(toRename, ImapConstants.INBOX_NAME).getParent();
            String[] path = newName.split('\\'+ImapConstants.HIERARCHY_DELIMITER);
            HierarchicalFolder newParent = userFolder;
            for(int i=0;i<path.length-1;i++) {
                newParent = newParent.getChild(path[i]);
            }
            toRename.moveToNewParent(newParent);
            toRename.setName(newFolderName);
        }
    }

    private HierarchicalFolder findParentByName(HierarchicalFolder folder, String parentName) {
        while(null!=folder && !parentName.equals(folder.getName())) {
            folder = folder.getParent();
        }
        return folder;
    }
    public Collection getChildren(MailFolder parent) {
        Collection children = ((HierarchicalFolder) parent).getChildren();
        return Collections.unmodifiableCollection(children);
    }

    public MailFolder setSelectable(MailFolder folder, boolean selectable) {
        ((HierarchicalFolder) folder).setSelectable(selectable);
        return folder;
    }

    /**
     * @see com.icegreen.greenmail.store.Store#listMailboxes
     */
    public Collection listMailboxes(String searchPattern)
            throws FolderException {
        int starIndex = searchPattern.indexOf('*');
        int percentIndex = searchPattern.indexOf('%');

        // We only handle wildcard at the end of the search pattern.
        if ((starIndex > -1 && starIndex < searchPattern.length() - 1) ||
                (percentIndex > -1 && percentIndex < searchPattern.length() - 1)) {
            throw new FolderException("WIldcard characters are only handled as the last character of a list argument.");
        }

        ArrayList mailboxes = new ArrayList();
        if (starIndex != -1 || percentIndex != -1) {
            int lastDot = searchPattern.lastIndexOf(HIERARCHY_DELIMITER);
            String parentName;
            if (lastDot < 0) {
                parentName = USER_NAMESPACE;
            } else {
                parentName = searchPattern.substring(0, lastDot);
            }
            String matchPattern = searchPattern.substring(lastDot + 1, searchPattern.length() - 1);

            HierarchicalFolder parent = (HierarchicalFolder) getMailbox(parentName);
            // If the parent from the search pattern doesn't exist,
            // return empty.
            if (parent != null) {
                for (final Object o : parent.getChildren()) {
                    HierarchicalFolder child = (HierarchicalFolder) o;
                    if (child.getName().startsWith(matchPattern)) {
                        mailboxes.add(child);

                        if (starIndex != -1) {
                            addAllChildren(child, mailboxes);
                        }
                    }
                }
            }

        } else {
            MailFolder folder = getMailbox(searchPattern);
            if (folder != null) {
                mailboxes.add(folder);
            }
        }

        return mailboxes;
    }

    public Quota[] getQuota(final String root, final String qualifiedRootPrefix) {
        Set<String> rootPaths = new HashSet();
        if(root.indexOf(ImapConstants.HIERARCHY_DELIMITER)<0) {
            rootPaths.add(qualifiedRootPrefix+root);
        }
        else {
            for(String r: root.split(ImapConstants.HIERARCHY_DELIMITER)) {
                rootPaths.add(qualifiedRootPrefix+r);
            }
        }
        rootPaths.add(qualifiedRootPrefix); // Add default root

        Set<Quota> collectedQuotas = new HashSet<Quota>();
        for(String p: rootPaths) {
            Set<Quota> quotas = quotaMap.get(p);
            if(null!=quotas) {
                collectedQuotas.addAll(quotas);
            }
        }
        updateQuotas(collectedQuotas, qualifiedRootPrefix);
        return collectedQuotas.toArray(EMPTY_QUOTAS);
    }

    private void updateQuotas(final Set<Quota> quotas,
                              final String qualifiedRootPrefix) {
        for(Quota q: quotas) {
            updateQuota(q, qualifiedRootPrefix);
        }
    }

    private void updateQuota(final Quota quota, final String pQualifiedRootPrefix) {
        MailFolder folder = getMailbox(
                ImapConstants.USER_NAMESPACE+ImapConstants.HIERARCHY_DELIMITER+
                        pQualifiedRootPrefix+ImapConstants.HIERARCHY_DELIMITER+
                        quota.quotaRoot);
        try {
            for (Quota.Resource r : quota.resources) {
                if (STORAGE.equals(r.name)) {
                    long size = 0;
                    for (StoredMessage m : folder.getMessages()) {
                        size += m.getMimeMessage().getSize();
                    }
                    r.usage = size;
                }
                else if(MESSAGES.equals(r.name)) {
                    r.usage = folder.getMessageCount();
                }
                else {
                    throw new IllegalStateException("Quota " + r.name + " not supported");
                }
            }
        } catch (MessagingException ex) {
            throw new IllegalStateException("Can not update/verify quota " + quota, ex);
        }
    }

    public void setQuota(final Quota quota, String qualifiedRootPrefix) {
        // Validate
        for(Quota.Resource r: quota.resources) {
            if(!STORAGE.equals(r.name) && !MESSAGES.equals(r.name)) {
                throw new IllegalStateException("Quota "+r.name+" not supported");
            }
        }

        // Save quota
        Set<Quota> quotas = quotaMap.get(qualifiedRootPrefix+quota.quotaRoot);
        if(null == quotas) {
            quotas = new HashSet<Quota>();
            quotaMap.put(qualifiedRootPrefix+quota.quotaRoot, quotas);
        }
        else {
            quotas.clear(); // " Any previous resource limits for the named quota root are discarded"
        }
        quotas.add(quota);
    }

    private void addAllChildren(HierarchicalFolder mailbox, Collection mailboxes) {
        Collection children = mailbox.getChildren();
        Iterator iterator = children.iterator();
        while (iterator.hasNext()) {
            HierarchicalFolder child = (HierarchicalFolder) iterator.next();
            mailboxes.add(child);
            addAllChildren(child, mailboxes);
        }
    }

    private class RootFolder extends HierarchicalFolder {
        public RootFolder() {
            super(null, ImapConstants.USER_NAMESPACE);
        }

        public String getFullName() {
            return name;
        }
    }

    private class HierarchicalFolder implements MailFolder {
        private Collection<HierarchicalFolder> children;
        private HierarchicalFolder parent;

        protected String name;
        private boolean isSelectable = false;

        private List<StoredMessage> mailMessages = Collections.synchronizedList(new LinkedList<StoredMessage>());
        private long nextUid = 1;
        private long uidValidity;

        private final List<FolderListener> _mailboxListeners = Collections.synchronizedList(new LinkedList<FolderListener>());

        public HierarchicalFolder(HierarchicalFolder parent,
                                  String name) {
            this.name = name;
            this.children = new ArrayList<HierarchicalFolder>();
            this.parent = parent;
            this.uidValidity = System.currentTimeMillis();
        }

        public Collection<HierarchicalFolder> getChildren() {
            return children;
        }

        public HierarchicalFolder getParent() {
            return parent;
        }

        public void moveToNewParent(HierarchicalFolder newParent) {
            if(!newParent.getChildren().contains(this)) {
                parent = newParent;
                parent.getChildren().add(this);
            }
        }

        public HierarchicalFolder getChild(String name) {
            for (HierarchicalFolder child : children) {
                if (child.getName().equalsIgnoreCase(name)) {
                    return child;
                }
            }
            return null;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return parent.getFullName() + HIERARCHY_DELIMITER_CHAR + name;
        }

        public Flags getPermanentFlags() {
            return PERMANENT_FLAGS;
        }

        public int getMessageCount() {
            return mailMessages.size();
        }

        public long getUidValidity() {
            return uidValidity;
        }

        public long getUidNext() {
            return nextUid;
        }

        public int getUnseenCount() {
            int count = 0;
            for (StoredMessage mailMessage : mailMessages) {
                StoredMessage message = (StoredMessage) mailMessage;
                if (!message.isSet(Flags.Flag.SEEN)) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Returns the 1-based index of the first unseen message. Unless there are outstanding
         * expunge responses in the ImapSessionMailbox, this will correspond to the MSN for
         * the first unseen.
         */
        public int getFirstUnseen() {
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = (StoredMessage) mailMessages.get(i);
                if (!message.isSet(Flags.Flag.SEEN)) {
                    return i + 1;
                }
            }
            return -1;
        }

        public int getRecentCount(boolean reset) {
            int count = 0;
            for (StoredMessage mailMessage : mailMessages) {
                StoredMessage message = (StoredMessage) mailMessage;
                if (message.isSet(Flags.Flag.RECENT)) {
                    count++;
                    if (reset) {
                        message.setFlag(Flags.Flag.RECENT, false);
                    }
                }
            }
            return count;
        }

        public int getMsn(long uid) throws FolderException {
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = (StoredMessage) mailMessages.get(i);
                if (message.getUid() == uid) {
                    return i + 1;
                }
            }
            throw new FolderException("No such message.");
        }

        public void signalDeletion() {
            // Notify all the listeners of the new message
            synchronized (_mailboxListeners) {
                for (FolderListener listener : _mailboxListeners) {
                    listener.mailboxDeleted();
                }
            }

        }

        public List getMessages(MsgRangeFilter range) {
            List<StoredMessage> ret = new ArrayList<StoredMessage>();
            for (int i = 0; i < mailMessages.size(); i++) {
                if (range.includes(i+1)) {
                    ret.add(mailMessages.get(i));
                }
            }

            return ret;
        }

        public List<StoredMessage> getMessages() {
            return mailMessages;
        }

        public List<StoredMessage> getNonDeletedMessages() {
            List<StoredMessage> ret = new ArrayList<StoredMessage>();
            for (StoredMessage mailMessage : mailMessages) {
                if (!mailMessage.getFlags().contains(Flags.Flag.DELETED)) {
                    ret.add(mailMessage);
                }
            }
            return ret;
        }

        public boolean isSelectable() {
            return isSelectable;
        }

        public void setSelectable(boolean selectable) {
            isSelectable = selectable;
        }

        public long appendMessage(MimeMessage message,
                                  Flags flags,
                                  Date internalDate) {
            long uid = nextUid;
            nextUid++;

            try {
                message.setFlags(flags, true);
                message.setFlag(Flags.Flag.RECENT, true);
            } catch (MessagingException e) {
                throw new IllegalStateException("Can not set flags", e);
            }
            StoredMessage storedMessage = new StoredMessage(message,
                    internalDate, uid);

            mailMessages.add(storedMessage);
            int newMsn = mailMessages.size();

            // Notify all the listeners of the new message
            synchronized (_mailboxListeners) {
                for (FolderListener _mailboxListener : _mailboxListeners) {
                    _mailboxListener.added(newMsn);
                }
            }

            return uid;
        }

        public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
            int msn = getMsn(uid);
            StoredMessage message = (StoredMessage) mailMessages.get(msn - 1);

            message.setFlags(flags, value);

            Long uidNotification = null;
            if (addUid) {
                uidNotification = uid;
            }
            notifyFlagUpdate(msn, message.getFlags(), uidNotification, silentListener);
        }

        public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
            int msn = getMsn(uid);
            StoredMessage message = (StoredMessage) mailMessages.get(msn - 1);
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

        public void deleteAllMessages() {
            mailMessages.clear();
        }

        public void store(MovingMessage mail) throws Exception {
            store(mail.getMessage());
        }


        public void store(MimeMessage message) throws Exception {
            Date internalDate = new Date();
            Flags flags = new Flags();
            appendMessage(message, flags, internalDate);
        }

        public StoredMessage getMessage(long uid) {
            for (StoredMessage mailMessage : mailMessages) {
                if (mailMessage.getUid() == uid) {
                    return mailMessage;
                }
            }
            return null;
        }

        public long[] getMessageUids() {
            long[] uids = new long[mailMessages.size()];
            for (int i = 0; i < mailMessages.size(); i++) {
                StoredMessage message = (StoredMessage) mailMessages.get(i);
                uids[i] = message.getUid();
            }
            return uids;
        }

        private void deleteMessage(int msn) {
            mailMessages.remove(msn - 1); //NOTE BY WAEL: is this really correct, the number of items in the iterating list is changed see expunge()
        }

        public long[] search(SearchTerm searchTerm) {
            ArrayList<StoredMessage> matchedMessages = new ArrayList<StoredMessage>();

            for (StoredMessage mailMessage : mailMessages) {
                if (searchTerm.match(mailMessage.getMimeMessage())) {
                    matchedMessages.add(mailMessage);
                }
            }

            long[] matchedUids = new long[matchedMessages.size()];
            for (int i = 0; i < matchedUids.length; i++) {
                StoredMessage storedMessage = (StoredMessage) matchedMessages.get(i);
                long uid = storedMessage.getUid();
                matchedUids[i] = uid;
            }
            return matchedUids;
        }

        public void copyMessage(long uid, MailFolder toFolder)
                throws FolderException {
            StoredMessage originalMessage = getMessage(uid);
            MimeMessage newMime;
            try {
                newMime = new MimeMessage(originalMessage.getMimeMessage());
            } catch (MessagingException e) {
                throw new FolderException("Can not copy message "+uid+" to folder " + toFolder, e);
            }
            Date newDate = originalMessage.getInternalDate();

            toFolder.appendMessage(newMime, originalMessage.getFlags(), newDate);
        }

        public void expunge() throws FolderException {
            for (int i = mailMessages.size()-1; i >= 0; i--) {
                StoredMessage message = (StoredMessage) mailMessages.get(i);
                if (message.isSet(Flags.Flag.DELETED)) {
                    expungeMessage(i+1); // MSNs start counting at 1
                }
            }
        }

        private void expungeMessage(int msn) {
            // Notify all the listeners of the pending delete
            synchronized (_mailboxListeners) {
                deleteMessage(msn);
                for (FolderListener expungeListener : _mailboxListeners) {
                    expungeListener.expunged(msn);
                }
            }
        }

        public void addListener(FolderListener listener) {
            synchronized (_mailboxListeners) {
                _mailboxListeners.add(listener);
            }
        }

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
    }

    public boolean isQuotaSupported() {
        return quotaSupported;
    }

    public void setQuotaSupported(final boolean pQuotaSupported) {
        quotaSupported = pQuotaSupported;
    }
}
