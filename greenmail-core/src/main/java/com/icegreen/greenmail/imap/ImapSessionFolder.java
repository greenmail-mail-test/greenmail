/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.*;

public class ImapSessionFolder implements MailFolder, FolderListener, UIDFolder {
    private MailFolder _folder;
    private ImapSession _session;
    private boolean _readonly;
    private boolean _sizeChanged;
    private final List<Integer> _expungedMsns = Collections.synchronizedList(new LinkedList<Integer>());
    private final Map<Integer, FlagUpdate> _modifiedFlags = Collections.synchronizedMap(new TreeMap<Integer, FlagUpdate>());

    public ImapSessionFolder(MailFolder folder, ImapSession session, boolean readonly) {
        _folder = folder;
        _session = session;
        _readonly = readonly;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        _folder.addListener(this);
    }

    public void deselect() {
        _folder.removeListener(this);
        _folder = null;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        long[] uids = _folder.getMessageUids();
        for (int i = 0; i < uids.length; i++) {
            long messageUid = uids[i];
            if (uid == messageUid) {
                return i + 1;
            }
        }
        throw new FolderException("No such message.");
    }

    @Override
    public void signalDeletion() {
        _folder.signalDeletion();
    }

    @Override
    public List getMessages(MsgRangeFilter msgRangeFilter) {
        return _folder.getMessages(msgRangeFilter);
    }

    @Override
    public List<StoredMessage> getMessages() {
        return _folder.getMessages();
    }

    @Override
    public List<StoredMessage> getNonDeletedMessages() {
        return _folder.getNonDeletedMessages();
    }

    public boolean isReadonly() {
        return _readonly;
    }

    public int[] getExpunged() throws FolderException {
        synchronized (_expungedMsns) {
            int[] expungedMsns = new int[_expungedMsns.size()];
            for (int i = 0; i < expungedMsns.length; i++) {
                int msn = _expungedMsns.get(i);
                expungedMsns[i] = msn;
            }
            _expungedMsns.clear();

            // TODO - renumber any cached ids (for now we assume the _modifiedFlags has been cleared)\
            if (!(_modifiedFlags.isEmpty() && !_sizeChanged)) {
                throw new IllegalStateException("Need to do this properly...");
            }
            return expungedMsns;
        }
    }

    public List<ImapSessionFolder.FlagUpdate> getFlagUpdates() throws FolderException {
        if (_modifiedFlags.isEmpty()) {
            return Collections.emptyList();
        }

        List<FlagUpdate> retVal = new ArrayList<FlagUpdate>();
        retVal.addAll(_modifiedFlags.values());
        _modifiedFlags.clear();
        return retVal;
    }

    @Override
    public void expunged(int msn) {
        synchronized (_expungedMsns) {
            _expungedMsns.add(msn);
        }
    }

    @Override
    public void added(int msn) {
        _sizeChanged = true;
    }

    @Override
    public void flagsUpdated(int msn, Flags flags, Long uid) {
        // This will overwrite any earlier changes
        _modifiedFlags.put(msn, new FlagUpdate(msn, uid, flags));
    }

    @Override
    public void mailboxDeleted() {
        _session.closeConnection("Mailbox " + _folder.getName() + " has been deleted");
    }

    @Override
    public String getName() {
        return _folder.getName();
    }

    @Override
    public String getFullName() {
        return _folder.getFullName();
    }

    @Override
    public Flags getPermanentFlags() {
        return _folder.getPermanentFlags();
    }

    @Override
    public int getMessageCount() {
        return _folder.getMessageCount();
    }

    @Override
    public int getRecentCount(boolean reset) {
        return _folder.getRecentCount(reset);
    }

    @Override
    public long getUidValidity() {
        return _folder.getUidValidity();
    }

    @Override
    public int getFirstUnseen() {
        return correctForExpungedMessages(_folder.getFirstUnseen());
    }

    /**
     * Adjust an actual mailbox msn for the expunged messages in this mailbox that have not
     * yet been notified.
     * TODO - need a test for this
     */
    private int correctForExpungedMessages(int absoluteMsn) {
        int correctedMsn = absoluteMsn;
        // Loop throught the expunged list backwards, adjusting the msn as we go.
        for (int i = _expungedMsns.size() - 1; i >= 0; i--) {
            int expunged = _expungedMsns.get(i);
            if (expunged <= absoluteMsn) {
                correctedMsn++;
            }
        }
        return correctedMsn;
    }

    @Override
    public boolean isSelectable() {
        return _folder.isSelectable();
    }

    @Override
    public long getUidNext() {
        return _folder.getUidNext();
    }

    @Override
    public int getUnseenCount() {
        return _folder.getUnseenCount();
    }

    @Override
    public long appendMessage(MimeMessage message, Flags flags, Date receivedDate) {
        return _folder.appendMessage(message, flags, receivedDate);
    }

    @Override
    public void store(MovingMessage mail) throws Exception {
        _folder.store(mail);
    }

    @Override
    public void store(MimeMessage mail) throws Exception {
        _folder.store(mail);
    }

    @Override
    public StoredMessage getMessage(long uid) {
        return _folder.getMessage(uid);
    }

    @Override
    public long[] getMessageUids() {
        return _folder.getMessageUids();
    }

    @Override
    public void expunge() throws FolderException {
        _folder.expunge();
    }

    @Override
    public long[] search(SearchTerm searchTerm) {
        return _folder.search(searchTerm);
    }

    @Override
    public long copyMessage(long uid, MailFolder toFolder) throws FolderException {
        return _folder.copyMessage(uid, toFolder);
    }

    @Override
    public void addListener(FolderListener listener) {
        _folder.addListener(listener);
    }

    @Override
    public void removeListener(FolderListener listener) {
        _folder.removeListener(listener);
    }

    @Override
    public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        _folder.setFlags(flags, value, uid, silentListener, addUid);
    }

    @Override
    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        _folder.replaceFlags(flags, uid, silentListener, addUid);
    }

    @Override
    public void deleteAllMessages() {
        _folder.deleteAllMessages();
    }

    public boolean isSizeChanged() {
        return _sizeChanged;
    }

    public void setSizeChanged(boolean sizeChanged) {
        _sizeChanged = sizeChanged;
    }

    private UIDFolder unwrapUIDFolder() {
        if (_folder instanceof UIDFolder) {
            return (UIDFolder) _folder;
        }
        throw new IllegalStateException("No UIDFolder supported by "+_folder.getClass());
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
