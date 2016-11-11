package com.icegreen.greenmail.filestore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import com.icegreen.greenmail.filestore.fs.MessageToFS;
import com.icegreen.greenmail.filestore.fs.MultipleElmFilesForMultipleMessages;
import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.imap.commands.ImapFlagConstants;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation is not really production-ready, because when exceptions occur, it just throws a Runtime exception
 * and the filestore can then be left in some incomplete state (e.g. some files are left which should have been deleted
 * etc...). But the use-case for GreenMail is a test-mailserver, and this tradeoff is acceptable.
 * <p>
 * Each mailbox either has messages, or children mailboxes. The format looks like this:
 * <p>
 * For each mailbox, the following directory structure exists:
 * <p>
 * mailbox                                 : Folder with the mailbox, where the foldername is the same as the mailbox name
 * mailbox/greenmail.mailbox.binary        : Binary file with settings for the mailbox
 * mailbox/greenmail.list.binary : Binary file with list for each message the mailbox
 * <p>
 * For each message in a mailbox, a file exists with the following naming format.
 * <p>
 * mailbox/msg_number_uid_millis           : Text file with message content
 */
class FileHierarchicalFolder implements MailFolder, UIDFolder {
    private final Logger log = LoggerFactory.getLogger(FileHierarchicalFolder.class);

    private final List<FolderListener> _mailboxListeners = Collections.synchronizedList(new ArrayList<FolderListener>());
    private final String name;
    private final FileBaseContext ctx;
    private final Path pathToDir;
    private final MailboxSettings settings;
    private final MailboxEntries entries;
    private long lastAccessedMillis = 0L;
    private final MessageToFS mtf;


    /**
     * Package-Private constructor, only to be invoked by the filestore package.
     *
     * @param pathToMailbox - path to the mailbox
     * @param ctx - context to the filestore, e.g. to the root-directory etc...
     */
    FileHierarchicalFolder(Path pathToMailbox, FileBaseContext ctx) {
        this.name = pathToMailbox.getFileName().toString();
        this.pathToDir = pathToMailbox;

        log.debug("Entering FileHierarchicalFolder constructor for path: " + this.pathToDir.toAbsolutePath().toString());

        this.settings = new MailboxSettings(this.pathToDir.resolve("greenmail.mailbox.binary"));
        this.entries = new MailboxEntries(this.pathToDir.resolve("greenmail.messageEntries.binary"));
        this.ctx = ctx;
        this.mtf = new MultipleElmFilesForMultipleMessages(this.getPathToDir());
        this.setLastAccessed();

        try {
            if (!Files.isDirectory(this.pathToDir)) {
                // We have to create the directory if it does not exist
                Files.createDirectories(this.pathToDir);
                this.settings.storeFileToFS();
            }
            else {
                this.settings.loadFileFromFS();
                this.entries.loadFileFromFS(this.mtf);
            }
        }
        catch (IOException io) {
            throw new UncheckedFileStoreException("IOEXception while creating the filestore with path: '" + this.pathToDir
                    .toAbsolutePath() + "'", io);
        }
        log.debug("Leaving FileHierarchicalFolder constructor for path: " + this.pathToDir.toAbsolutePath().toString() + " "
                + "with # of messages: " + this.entries.list.size());
    }

    /**
     * We need to know how old a mailbox is, because a mailbox which has not been accessed for some time
     * can be deleted from memory (e.g. removed fromt he cache in the FileBasedContext).
     */
    private void setLastAccessed() {
        this.lastAccessedMillis = System.currentTimeMillis();
    }

    /**
     * Returns the number of millis since the last access (public method invocation) of this mailfolder. Can be used to
     * evaluate whether to remove this object from memory.
     * @return - number of milliseconds since the last access.
     */
    public long getAgeOfLastAccessInMillis() {
        return System.currentTimeMillis() - this.lastAccessedMillis;
    }

    /**
     * Make sure that the settings and message list files are removed and deleted.
     */
    public void prepareForDeletion() {
        this.settings.deleteFileFromFS();
        this.entries.deleteFileFromFS();
    }

    public Path getPathToDir() {
        return this.pathToDir;
    }

    public boolean hasChildren() {
        this.setLastAccessed();
        // Decision is not to cache this, because this command is seldom invoked and FS access is feasible.
        File root = this.pathToDir.toFile();
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                // All the directories inside a mailbox must be child-mailboxes
                return true;
            }
        }
        return false;
    }

    public FileHierarchicalFolder getParent() {
        return this.ctx.getMailboxForPath(pathToDir.getParent());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullName() {
        return FileStoreUtil.convertPathToFullName(this.ctx.getMboxFileStoreRootDir().toString(), this.pathToDir);
    }

    @Override
    public Flags getPermanentFlags() {
        return ImapFlagConstants.PERMANENT_FLAGS;
    }

    @Override
    public int getMessageCount() {
        this.setLastAccessed();
        synchronized (this.entries.syncLock) {
            return this.entries.list.size();
        }
    }

    @Override
    public long getUidValidity() {
        // Always return the same UIDVALIDITY, because we store our UIDs inside the message as additional
        // message header, so UIDs will always be valid for a file-based Store.
        // See https://www.ietf.org/rfc/rfc2683.txt, chapter 3.4.3
        return 42L;
    }

    @Override
    public long getUIDValidity() throws MessagingException {
        return getUidValidity();
    }

    @Override
    public long getUidNext() {
        // Use the context to retrieve the next unique UID.
        return this.ctx.getNextUid();
    }

    @Override
    public int getUnseenCount() {
        this.setLastAccessed();
        int numUnSeen = 0;

        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isSeenFlagSet(entry.getFlagBitSet())) {
                    numUnSeen++;
                }
            }
        }
        return numUnSeen;
    }

    /**
     * Returns the 1-based index of the first unseen message. Unless there are outstanding
     * expunge responses in the ImapSessionMailbox, this will correspond to the MSN for
     * the first unseen.
     */
    @Override
    public int getFirstUnseen() {
        this.setLastAccessed();
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isSeenFlagSet(entry.getFlagBitSet())) {
                    return entry.getMsgNum();
                }
            }
        }
        return -1;
    }

    @Override
    public int getRecentCount(boolean reset) {
        this.setLastAccessed();
        int numRecent = 0;
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (FileStoreUtil.isRecentFlagSet(entry.getFlagBitSet())) {
                    numRecent++;
                }
            }
        }
        return numRecent;
    }

    @Override
    public int getMsn(long uid) throws FolderException {
        this.setLastAccessed();
        synchronized (this.entries.syncLock) {
            for (MessageEntry e : this.entries.list) {
                if (uid == e.getUid()) {
                    return e.getMsgNum();
                }
            }
        }
        throw new FolderException("No such message with UID '" + uid + "' in folder: " + this.pathToDir.toString());
    }

    @Override
    public void signalDeletion() {
        this.setLastAccessed();
        // Notify all the listeners of the new message
        synchronized (_mailboxListeners) {
            for (FolderListener listener : _mailboxListeners) {
                listener.mailboxDeleted();
            }
        }

    }

    @Override
    public List<StoredMessage> getMessages(MsgRangeFilter range) {
        this.setLastAccessed();
        ArrayList<MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (range.includes(entry.getMsgNum())) {
                    matchedMessages.add(entry);
                }
            }
        }
        return retrieveAllMessagesFromList(matchedMessages);
    }

    public List<StoredMessage> getMessageEntries() {
        this.setLastAccessed();
        ArrayList<MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                matchedMessages.add(entry);
            }
        }
        return retrieveAllMessagesFromList(matchedMessages);
    }

    private List<StoredMessage> retrieveAllMessagesFromList(ArrayList<MessageEntry> matchedMessages) {
        List<StoredMessage> ret = new ArrayList<>();
        for (MessageEntry entry : matchedMessages) {
            ret.add(retrieveOneMessage(entry));
        }
        return ret;
    }

    private StoredMessage retrieveOneMessage(MessageEntry entry) {
        log.debug("Retrieving one message from store with uid: " + entry.getUid() + " and resetting flags to : " + entry
                .getFlagBitSet());
        try {
            return mtf.retrieveMessage(entry);
        }
        catch (MessagingException e) {
            log.error("MessagingException happened while reading message from disk. Returning null as message.", e);
            return null;
        }
        catch (IOException e) {
            log.error("IOException happened while reading message from disk. Returning null as message.", e);
            return null;
        }
    }

    @Override
    public boolean isSelectable() {
        return this.settings.isSelectable;
    }

    public void setSelectable(boolean selectable) {
        this.setLastAccessed();
        this.settings.isSelectable = selectable;
        this.settings.storeFileToFS();
    }

    @Override
    public long appendMessage(MimeMessage message,
            Flags flags,
            Date receivedDate) {
        this.setLastAccessed();
        log.debug("Entering appendMessage with flags '" + flags + "' and receivedDate: '" + receivedDate);
        try {
            log.debug("  Message has the following sentDate: " + message.getSentDate());
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
        long uid = this.ctx.getNextUid();

        try {
            message.setFlags(flags, true);
            message.setFlag(Flags.Flag.RECENT, true);
        }
        catch (MessagingException e) {
            throw new IllegalStateException("Can not set flags", e);
        }
        StoredMessage storedMessage = new StoredMessage(message, receivedDate, uid);

        MessageEntry entry = new MessageEntry(uid);

        int newIndex = 0;
        synchronized (this.entries.syncLock) {
            entry.setMsgNum(this.entries.list.size() + 1);
            newIndex = this.entries.list.size();
            this.entries.list.add(entry);
        }

        try {
            // Now, adapt the messages:
            this.mtf.addMessage(storedMessage, entry);

            entry.setRecDateMillis(storedMessage.getReceivedDate().getTime());
            entry.setFlagBitSet(FileStoreUtil.convertFlagsToFlagBitSet(storedMessage.getMimeMessage().getFlags()));
            log.debug("Successfully added a new entry to the FS with uid '" + entry.getUid() + "' and flags: " + entry.getFlagBitSet());

            synchronized (this.entries.syncLock) {
                this.entries.storeFileToFSForSingleEntryWithoutSync(newIndex);
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException("IOException happened while writing message to disk: " + uid);
        }
        catch (MessagingException e) {
            throw new UncheckedFileStoreException("MessagingException happened while writing message to disk: " + uid);
        }

        // Notify all the listeners of the new message
        synchronized (_mailboxListeners) {
            for (FolderListener _mailboxListener : _mailboxListeners) {
                _mailboxListener.added(entry.getMsgNum());
            }
        }
        return uid;
    }

    @Override
    public List<StoredMessage> getNonDeletedMessages() {
        this.setLastAccessed();
        ArrayList<MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isDeletedFlagSet(entry.getFlagBitSet())) {
                    matchedMessages.add(entry);
                }
            }
        }
        return retrieveAllMessagesFromList(matchedMessages);
    }

    @Override
    public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid)
            throws FolderException {
        this.setLastAccessed();
        log.debug("Entering setFlags with: ");
        log.debug("  Flags          : " + flags);
        log.debug("  Value          : " + value);
        log.debug("  UID            : " + uid);
        log.debug("  silentListener : " + silentListener);
        log.debug("  addUid         : " + addUid);

        MessageEntry me = null;
        int meIndex = 0;

        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (entry.getUid() == uid) {
                    me = entry;
                    break;
                }
                meIndex++;
            }

            if (me != null) {
                log.debug("Found message where to set the flags: " + me.getUid());
                if (value) {
                    // Set the flags
                    log.debug("  Bitset before setting Flags: " + me.getFlagBitSet());
                    int flagsToSet = FileStoreUtil.convertFlagsToFlagBitSet(flags);
                    int newFlags = me.getFlagBitSet();
                    newFlags |= flagsToSet;
                    me.setFlagBitSet(newFlags);
                    log.debug("  Bitset after  setting Flags: " + me.getFlagBitSet());
                }
                else {
                    // TODO: Delete the flags
                    // if BIT is set, we should delete it in entr.flagBitSet... not yet implemented.
                }
            }
            this.entries.storeFileToFSForSingleEntryWithoutSync(meIndex);
        }

        Long uidNotification = null;
        if (addUid) {
            uidNotification = uid;
        }
        notifyFlagUpdate(me.getMsgNum(), FileStoreUtil.convertFlagBitSetToFlags(me.getFlagBitSet()), uidNotification, silentListener);
    }

    @Override
    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        this.setLastAccessed();
        MessageEntry me = null;
        int meIndex = 0;


        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (entry.getUid() == uid) {
                    me = entry;
                    break;
                }
                meIndex++;
            }
            if (me != null) {
                // Set the flags
                me.setFlagBitSet(FileStoreUtil.convertFlagsToFlagBitSet(flags));
            }

            this.entries.storeFileToFSForSingleEntryWithoutSync(meIndex);
        }


        Long uidNotification = null;
        if (addUid) {
            uidNotification = uid;
        }
        notifyFlagUpdate(me.getMsgNum(), FileStoreUtil.convertFlagBitSetToFlags(me.getFlagBitSet()), uidNotification, silentListener);
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
    public void store(MovingMessage mail) throws Exception {
        this.setLastAccessed();
        store(mail.getMessage());
    }

    @Override
    public void store(MimeMessage message) throws Exception {
        this.setLastAccessed();
        Date receivedDate = new Date();
        Flags flags = new Flags();
        appendMessage(message, flags, receivedDate);
    }

    @Override
    public StoredMessage getMessage(long uid) {
        this.setLastAccessed();
        log.debug("Entering getMessage with uid: " + uid);
        MessageEntry me = null;

        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (entry.getUid() == uid) {
                    me = entry;
                }
            }
        }
        if (me == null) {
            log.debug("Leaving getMessage with null, message does not exist.");
            return null;
        }
        log.debug("Leaving getMessage with uid by trying to retrieve the message from FS.");
        return retrieveOneMessage(me);
    }

    /**
     * Return all message UIDS of all messages in the mailbox.
     *
     * @return - an array of uids, which can be empty
     */
    @Override
    public long[] getMessageUids() {
        this.setLastAccessed();
        log.debug("Entering getMessageUids");
        synchronized (this.entries.syncLock) {
            int num = this.entries.list.size();
            long[] ret = new long[num];
            for (int i = 0; i < num; i++) {
                ret[i] = this.entries.list.get(i).getUid();
            }
            log.debug("Leaving getMessageUids with array with # of entries: " + num);
            return ret;
        }
    }

    /**
     * Return all message UIDS of all messages in the mailbox which match the UID range.
     *
     * @param uidRange - Range of UIDS
     *
     * @return - an array of uids, which can be empty
     */
    @Override
    public long[] getMessageUidsByUidRange(IdRange[] uidRange) {
        this.setLastAccessed();
        log.debug("Entering getMessageUidsByUidRange");
        ArrayList<Long>matchedUids = new ArrayList<>();
        synchronized (this.entries.syncLock) {
            for (MessageEntry e : this.entries.list) {
                if (includes(uidRange, e.getUid())) {
                    matchedUids.add(e.getUid());
                }
            }
        }
        long[] result = new long[matchedUids.size()];
        int index = 0;
        for (Long matchedUid : matchedUids) {
            result[index] = matchedUid.longValue();
            index++;
        }
        log.debug("Leaving getMessageUidsByUidRange with array with # of entries: " + result.length);
        return result;
    }

    /**
     * Return all message UIDS of all messages in the mailbox which match the msgNum range.
     *
     * @param msgNumRange - Range of message numbers
     *
     * @return - an array of uids, which can be empty
     * @throws FolderException
     */
    @Override
    public long[] getMessageUidsByMsgNumRange(IdRange[] msgNumRange) throws FolderException {
        this.setLastAccessed();
        log.debug("Entering getMessageUidsByMsgNumRange");
        ArrayList<Long>matchedUids = new ArrayList<>();
        synchronized (this.entries.syncLock) {
            for (MessageEntry e : this.entries.list) {
                if (includes(msgNumRange, e.getMsgNum())) {
                    matchedUids.add(e.getUid());
                }
            }
        }
        long[] result = new long[matchedUids.size()];
        int index = 0;
        for (Long matchedUid : matchedUids) {
            result[index] = matchedUid.longValue();
            index++;
        }
        log.debug("Leaving getMessageUidsByMsgNumRange with array with # of entries: " + result.length);
        return result;
    }

    /**
     * Returns the message UID of the last message in the mailbox, or -1L to show that no such message exist (e.g. when the
     * mailbox is empty).
     *
     * @return - a valid UID of the last message or -1
     */
    public long getMessageUidOfLastMessage() {
        log.debug("Entering getMessageUidOfLastMessage");
        long result = -1;
        synchronized (this.entries.syncLock) {
            if (!this.entries.list.isEmpty()) {
                result = this.entries.list.get(this.entries.list.size() - 1).getUid();
            }
        }
        log.debug("Leaving getMessageUidOfLastMessage with result: " + result);
        return result;
    }

    protected boolean includes(IdRange[] idSet, long id) {
        for (IdRange idRange : idSet) {
            if (idRange.includes(id)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void deleteAllMessages() {
        this.setLastAccessed();
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                Path fullpath = this.pathToDir.resolve(entry.getShortFileName());
                if (Files.isRegularFile(fullpath)) {
                    try {
                        Files.delete(fullpath);
                    } catch (IOException ign) {
                        log.warn("Ignore IOException while deleting message with filename." + fullpath, ign);
                    }
                }
            }
            this.entries.list.clear();
        }
    }


    @Override
    public long[] search(SearchTerm searchTerm) {
        this.setLastAccessed();
        log.debug("Entering search with : " + searchTerm);

        // This is quite ugly, we need to load all the message into memory and search for each one individually:
        ArrayList<MessageEntry> copyList = new ArrayList<>();
        synchronized (this.entries.syncLock) {
            copyList.addAll(this.entries.list);
        }

        ArrayList<Long> result = new ArrayList<>();
        for (MessageEntry entry : copyList) {
            StoredMessage msg = retrieveOneMessage(entry);
            if (searchTerm.match(msg.getMimeMessage())) {
                result.add(entry.getUid());
            }
        }

        int len = result.size();
        long[] resultArray = new long[len];
        StringBuilder debugStr = new StringBuilder();
        for (int i = 0; i < len; i++) {
            resultArray[i] = result.get(i);
            debugStr.append(resultArray[i]).append(",");
        }
        log.debug("Leaving search with result: [" + debugStr.toString() + "]");
        return resultArray;

    }

    @Override
    public long copyMessage(long uid, MailFolder toFolder)
            throws FolderException {
        this.setLastAccessed();
        StoredMessage originalMessage = getMessage(uid);
        MimeMessage newMime;
        try {
            newMime = new MimeMessage(originalMessage.getMimeMessage());
        }
        catch (MessagingException e) {
            throw new FolderException("Can not copy message " + uid + " to folder " + toFolder, e);
        }

        return toFolder.appendMessage(newMime, originalMessage.getFlags(), originalMessage.getReceivedDate());
    }

    @Override
    public void expunge() throws FolderException {
        expunge(null);
    }

    @Override
    public void expunge(IdRange[] idRanges) {
        this.setLastAccessed();
        log.debug("Entering expunge with id range: " + idRanges);

        ArrayList<MessageEntry> toDelete = new ArrayList<>();
        int numDeleted = 0;

        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (FileStoreUtil.isDeletedFlagSet(entry.getFlagBitSet()) && (idRanges == null || IdRange.containsUid(idRanges,
                        entry.getUid()))) {
                    toDelete.add(entry);
                }
            }

            for (MessageEntry delEntry : toDelete) {
                log.debug("  Expunge message with uid: " + delEntry.getUid() + " and msgNum: " + delEntry.getMsgNum());

                // Step 1: Remove from list
                this.entries.list.remove(delEntry);

                // Step 2: Delete file:
                try {
                    Path toDelPath = this.pathToDir.resolve(delEntry.getShortFileName());
                    log.debug("  Delete file for expunged message: " + toDelPath.toString());
                    Files.delete(toDelPath);
                    numDeleted++;
                }
                catch (IOException io) {
                    // Ugly, but it is really not so important if the file cannog be deleted. Let's log it and go ahead.
                    //TODO: logging
                }
            }

            // Finally, we have to renumber the messages again, because messageNumber is actually just an 1-based index
            // into the list:
            int index = 1;
            for (MessageEntry entry : this.entries.list) {
                entry.setMsgNum(index);
                index++;
            }

            this.entries.storeFileToFSWithoutSync();
        }

        // Finally, inform thee listeners
        // TODO: This has to be done backwards, which is quite ugly. Check out why this is so and fix it, and make the order in
        // which the listener is informed order-independent!
        int numToDel = toDelete.size();
        for (int i = numToDel - 1; i >= 0; i--) {
            MessageEntry delEntry = toDelete.get(i);
            for (FolderListener expungeListener : _mailboxListeners) {
                expungeListener.expunged(delEntry.getMsgNum());
            }
        }

        log.debug("Leaving expunge, deleted # of messages: " + numDeleted);
    }

    @Override
    public void addListener(FolderListener listener) {
        this.setLastAccessed();
        synchronized (_mailboxListeners) {
            _mailboxListeners.add(listener);
        }
    }

    @Override
    public void removeListener(FolderListener listener) {
        this.setLastAccessed();
        synchronized (_mailboxListeners) {
            _mailboxListeners.remove(listener);
        }
    }

    @Override
    public String toString() {
        return "FileHierarchicalFolder{" +
                "name='" + name + '\'' +
                ", path=" + this.pathToDir +
                ", isSelectable=" + this.settings.isSelectable +
                '}';
    }


    @Override
    public Message getMessageByUID(long uid) throws MessagingException {
        this.setLastAccessed();
        StoredMessage sm = getMessage(uid);
        if (sm != null) {
            return sm.getMimeMessage();
        }
        return null;
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) throws MessagingException {
        this.setLastAccessed();
        ArrayList<MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                if (entry.getUid() >= start && entry.getUid() <= end) {
                    matchedMessages.add(entry);
                }
            }
        }

        return retrieveAllMimeMessagesFromList(matchedMessages);
    }

    private Message[] retrieveAllMimeMessagesFromList(ArrayList<MessageEntry> matchedMessages) {
        List<Message> ret = new ArrayList<>();
        for (MessageEntry entry : matchedMessages) {
            ret.add(retrieveOneMessage(entry).getMimeMessage());
        }
        return ret.toArray(new Message[this.entries.list.size()]);
    }


    @Override
    public Message[] getMessagesByUID(long[] uids) throws MessagingException {
        this.setLastAccessed();
        ArrayList<MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MessageEntry entry : this.entries.list) {
                for (long searchUid : uids) {
                    if (entry.getUid() == searchUid) {
                        matchedMessages.add(entry);
                        break;
                    }
                }
            }
        }

        return retrieveAllMimeMessagesFromList(matchedMessages);
    }

    @Override
    public long getUID(Message message) throws MessagingException {
        this.setLastAccessed();
        // We must ressort to our custom UID header here:
        return this.mtf.getUidForMessageFromHeader(message);
    }

}
