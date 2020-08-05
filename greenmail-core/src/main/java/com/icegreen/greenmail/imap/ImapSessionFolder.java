/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import java.util.*;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

public class ImapSessionFolder implements MailFolder, FolderListener, UIDFolder {
    private MailFolder folder;
    private ImapSession session;
    private boolean readonly;
    private volatile boolean sizeChanged;
    private final List<Integer> expungedMsns = Collections.synchronizedList(new LinkedList<Integer>());
    private final Map<Integer, FlagUpdate> modifiedFlags = Collections.synchronizedMap(new TreeMap<Integer, FlagUpdate>());

    public ImapSessionFolder(MailFolder folder, ImapSession session, boolean readonly) {
        this.folder = folder;
        this.session = session;
        this.readonly = readonly;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        this.folder.addListener(this);
    }

    public void deselect() {
        folder.removeListener(this);
        folder = null;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        long[] uids = folder.getMessageUids();
        for (int i = 0; i < uids.length; i++) {
            long messageUid = uids[i];
            if (uid == messageUid) {
                return i + 1;
            }
        }
        throw new FolderException("No such message with uid " + uid + " in folder " + folder.getName());
    }

    @Override
    public void signalDeletion() {
        folder.signalDeletion();
    }

    @Override
    public List<StoredMessage> getMessages(MsgRangeFilter msgRangeFilter) {
        return folder.getMessages(msgRangeFilter);
    }

    @Override
    public List<StoredMessage> getMessages() {
        return folder.getMessages();
    }

    @Override
    public List<StoredMessage> getNonDeletedMessages() {
        return folder.getNonDeletedMessages();
    }

    public boolean isReadonly() {
        return readonly;
    }

    public int[] getExpunged() {
        synchronized (expungedMsns) {
            int[] expungedMsnsArray = new int[this.expungedMsns.size()];
            for (int i = 0; i < expungedMsnsArray.length; i++) {
                int msn = this.expungedMsns.get(i);
                expungedMsnsArray[i] = msn;
            }
            this.expungedMsns.clear();

            // TODO - renumber any cached ids (for now we assume the modifiedFlags has been cleared)\
            if (!(modifiedFlags.isEmpty() && !sizeChanged)) {
                throw new IllegalStateException("Need to do this properly...");
            }
            return expungedMsnsArray;
        }
    }

    public List<ImapSessionFolder.FlagUpdate> getFlagUpdates() {
        if (modifiedFlags.isEmpty()) {
            return Collections.emptyList();
        }

        List<FlagUpdate> retVal = new ArrayList<>(modifiedFlags.values());
        modifiedFlags.clear();
        return retVal;
    }

    @Override
    public void expunged(int msn) {
        synchronized (expungedMsns) {
            expungedMsns.add(msn);
        }
    }

    @Override
    public void added(int msn) {
        sizeChanged = true;
    }

    @Override
    public void flagsUpdated(int msn, Flags flags, Long uid) {
        // This will overwrite any earlier changes
        modifiedFlags.put(msn, new FlagUpdate(msn, uid, flags));
    }

    @Override
    public void mailboxDeleted() {
        session.closeConnection("Mailbox " + folder.getName() + " has been deleted");
    }

    @Override
    public String getName() {
        return folder.getName();
    }

    @Override
    public String getFullName() {
        return folder.getFullName();
    }

    @Override
    public Flags getPermanentFlags() {
        return folder.getPermanentFlags();
    }

    @Override
    public int getMessageCount() {
        return folder.getMessageCount();
    }

    @Override
    public int getRecentCount(boolean reset) {
        return folder.getRecentCount(reset);
    }

    @Override
    public long getUidValidity() {
        return folder.getUidValidity();
    }

    @Override
    public int getFirstUnseen() {
        return correctForExpungedMessages(folder.getFirstUnseen());
    }

    /**
     * Adjust an actual mailbox msn for the expunged messages in this mailbox that have not
     * yet been notified.
     * TODO - need a test for this
     */
    private int correctForExpungedMessages(int absoluteMsn) {
        int correctedMsn = absoluteMsn;
        // Loop through the expunged list backwards, adjusting the msn as we go.
        for (int i = expungedMsns.size() - 1; i >= 0; i--) {
            int expunged = expungedMsns.get(i);
            if (expunged <= absoluteMsn) {
                correctedMsn++;
            }
        }
        return correctedMsn;
    }

    @Override
    public boolean isSelectable() {
        return folder.isSelectable();
    }

    @Override
    public long getUidNext() { // TODO: Remove in 1.7
        return getUIDNext();
    }

    @Override
    public int getUnseenCount() {
        return folder.getUnseenCount();
    }

    @Override
    public long appendMessage(MimeMessage message, Flags flags, Date receivedDate) {
        return folder.appendMessage(message, flags, receivedDate);
    }

    @Override
    public void store(MovingMessage mail) throws Exception {
        folder.store(mail);
    }

    @Override
    public void store(MimeMessage mail) throws Exception {
        folder.store(mail);
    }

    @Override
    public StoredMessage getMessage(long uid) {
        return folder.getMessage(uid);
    }

    @Override
    public long[] getMessageUids() {
        return folder.getMessageUids();
    }

    @Override
    public void expunge() throws FolderException {
        folder.expunge();
    }

    @Override
    public void expunge(IdRange[] idRanges) {
        folder.expunge(idRanges);
    }

    @Override
    public long[] search(SearchTerm searchTerm) {
        return folder.search(searchTerm);
    }

    @Override
    public long copyMessage(long uid, MailFolder toFolder) throws FolderException {
        return folder.copyMessage(uid, toFolder);
    }

    @Override
    public void addListener(FolderListener listener) {
        folder.addListener(listener);
    }

    @Override
    public void removeListener(FolderListener listener) {
        folder.removeListener(listener);
    }

    @Override
    public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        folder.setFlags(flags, value, uid, silentListener, addUid);
    }

    @Override
    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        folder.replaceFlags(flags, uid, silentListener, addUid);
    }

    @Override
    public void deleteAllMessages() {
        folder.deleteAllMessages();
    }

    public boolean isSizeChanged() {
        return sizeChanged;
    }

    public void setSizeChanged(boolean sizeChanged) {
        this.sizeChanged = sizeChanged;
    }

    private UIDFolder unwrapUIDFolder() {
        if (folder instanceof UIDFolder) {
            return (UIDFolder) folder;
        }
        throw new IllegalStateException("No UIDFolder supported by "+ folder.getClass());
    }

    @Override
    public long getUIDValidity() throws MessagingException {
        return unwrapUIDFolder().getUIDValidity();
    }

    @Override
    public Message getMessageByUID(long uid) throws MessagingException {
        return unwrapUIDFolder().getMessageByUID(uid);
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) throws MessagingException {
        return unwrapUIDFolder().getMessagesByUID(start, end);
    }

    @Override
    public Message[] getMessagesByUID(long[] uids) throws MessagingException {
        return unwrapUIDFolder().getMessagesByUID(uids);
    }

    @Override
    public long getUID(Message message) throws MessagingException {
        return unwrapUIDFolder().getUID(message);
    }

    @Override
    public long getUIDNext() {
        return folder.getUIDNext();
    }

    static final class FlagUpdate {
        private int msn;
        private Long uid;
        private Flags flags;

        public FlagUpdate(int msn, Long uid, Flags flags) {
            this.msn = msn;
            this.uid = uid;
            this.flags = flags;
        }

        public int getMsn() {
            return msn;
        }

        public Long getUid() {
            return uid;
        }

        public Flags getFlags() {
            return flags;
        }
    }

}
