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
        this.setLastAccessed();

        try {
            if (!Files.isDirectory(this.pathToDir)) {
                // We have to create the directory if it does not exist
                Files.createDirectories(this.pathToDir);
                this.settings.storeFileToFS();
            }
            else {
                this.settings.loadFileFromFS();
                this.entries.loadFileFromFS();
            }
        }
        catch (IOException io) {
            throw new UncheckedFileStoreException("IOEXception while creating the filestore with path: '" + this.pathToDir
                    .toAbsolutePath() + "'", io);
        }
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
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isSeenFlagSet(entry.flagBitSet)) {
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
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isSeenFlagSet(entry.flagBitSet)) {
                    return entry.msgNum;
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
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (FileStoreUtil.isRecentFlagSet(entry.flagBitSet)) {
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
            for (MailboxEntries.MessageEntry e : this.entries.list) {
                if (uid == e.uid) {
                    return e.msgNum;
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
        ArrayList<MailboxEntries.MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (range.includes(entry.msgNum)) {
                    matchedMessages.add(entry);
                }
            }
        }
        return retrieveAllMessagesFromList(matchedMessages);
    }

    public List<StoredMessage> getMessageEntries() {
        this.setLastAccessed();
        ArrayList<MailboxEntries.MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                matchedMessages.add(entry);
            }
        }
        return retrieveAllMessagesFromList(matchedMessages);
    }

    private List<StoredMessage> retrieveAllMessagesFromList(ArrayList<MailboxEntries.MessageEntry> matchedMessages) {
        List<StoredMessage> ret = new ArrayList<>();
        for (MailboxEntries.MessageEntry entry : matchedMessages) {
            ret.add(retrieveOneMessage(entry));
        }
        return ret;
    }

    private StoredMessage retrieveOneMessage(MailboxEntries.MessageEntry entry) {
        log.debug("Retrieving one message from store with uid: " + entry.shortFilename + " and resetting flags to : " + entry
                .flagBitSet);
        try {
            OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
            return store.retrieveMessage(entry.shortFilename, entry.msgNum, entry.flagBitSet, entry.recDateMillis, entry.uid);
        }
        catch (MessagingException e) {
            throw new UncheckedFileStoreException("MessagingException happened while reading message from disk: " +
                    entry.shortFilename, e);
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException("IOException happened while reading message from disk: " + entry.shortFilename,
                    e);
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
        StoredMessage storedMessage = new StoredMessage(message,
                receivedDate, uid);

        MailboxEntries.MessageEntry entry = new MailboxEntries.MessageEntry();
        synchronized (this.entries.syncLock) {
            entry.uid = uid;
            entry.msgNum = this.entries.list.size() + 1;
            this.entries.list.add(entry);
        }

        OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
        try {
            // Now, adapt the messages:
            entry.shortFilename = store.addMessage(storedMessage, entry.msgNum);
            entry.recDateMillis = storedMessage.getReceivedDate().getTime();
            entry.flagBitSet = FileStoreUtil.convertFlagsToFlagBitSet(storedMessage.getMimeMessage().getFlags());
            log.debug("Successfully added a new entry to the FS with uid '" + entry.uid + "' and flags: " + entry.flagBitSet);
            this.entries.storeFileToFS();
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
                _mailboxListener.added(entry.msgNum);
            }
        }
        return uid;
    }

    @Override
    public List<StoredMessage> getNonDeletedMessages() {
        this.setLastAccessed();
        ArrayList<MailboxEntries.MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (!FileStoreUtil.isDeletedFlagSet(entry.flagBitSet)) {
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

        MailboxEntries.MessageEntry me = null;
        int meIndex = 0;

        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (entry.uid == uid) {
                    me = entry;
                    break;
                }
                meIndex++;
            }

            if (me != null) {
                log.debug("Found message where to set the flags: " + me.shortFilename);
                if (value) {
                    // Set the flags
                    log.debug("  Bitset before setting Flags: " + me.flagBitSet);
                    int flagsToSet = FileStoreUtil.convertFlagsToFlagBitSet(flags);
                    me.flagBitSet |= flagsToSet;
                    log.debug("  Bitset after  setting Flags: " + me.flagBitSet);
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
        notifyFlagUpdate(me.msgNum, FileStoreUtil.convertFlagBitSetToFlags(me.flagBitSet), uidNotification, silentListener);
    }

    @Override
    public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        this.setLastAccessed();
        MailboxEntries.MessageEntry me = null;
        int meIndex = 0;


        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (entry.uid == uid) {
                    me = entry;
                    break;
                }
                meIndex++;
            }
            if (me != null) {
                // Set the flags
                me.flagBitSet = FileStoreUtil.convertFlagsToFlagBitSet(flags);
            }

            this.entries.storeFileToFSForSingleEntryWithoutSync(meIndex);
        }


        Long uidNotification = null;
        if (addUid) {
            uidNotification = uid;
        }
        notifyFlagUpdate(me.msgNum, FileStoreUtil.convertFlagBitSetToFlags(me.flagBitSet), uidNotification, silentListener);
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
        MailboxEntries.MessageEntry me = null;

        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (entry.uid == uid) {
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

    @Override
    public long[] getMessageUids() {
        this.setLastAccessed();
        log.debug("Entering getMessageUids");
        synchronized (this.entries.syncLock) {
            int num = this.entries.list.size();
            long[] ret = new long[num];
            for (int i = 0; i < num; i++) {
                ret[i] = this.entries.list.get(i).uid;
            }
            log.debug("Leaving getMessageUids with list: " + num);
            return ret;
        }
    }

    @Override
    public void deleteAllMessages() {
        this.setLastAccessed();
        synchronized (this.entries.syncLock) {
            try {
                for (MailboxEntries.MessageEntry entry : this.entries.list) {
                    Path fullpath = this.pathToDir.resolve(entry.shortFilename);
                    Files.delete(fullpath);
                }
                this.entries.list.clear();
            }
            catch (IOException io) {
                throw new UncheckedFileStoreException(
                        "IOException happened while trying to delete all message in directory: " + this.pathToDir, io);
            }
        }
    }


    @Override
    public long[] search(SearchTerm searchTerm) {
        this.setLastAccessed();
        log.debug("Entering search with : " + searchTerm);

        // This is quite ugly, we need to load all the message into memory and search for each one individually:
        ArrayList<MailboxEntries.MessageEntry> copyList = new ArrayList<>();
        synchronized (this.entries.syncLock) {
            copyList.addAll(this.entries.list);
        }

        ArrayList<Long> result = new ArrayList<>();
        for (MailboxEntries.MessageEntry entry : copyList) {
            StoredMessage msg = retrieveOneMessage(entry);
            if (searchTerm.match(msg.getMimeMessage())) {
                result.add(entry.uid);
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

        ArrayList<MailboxEntries.MessageEntry> toDelete = new ArrayList<>();
        int numDeleted = 0;

        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (FileStoreUtil.isDeletedFlagSet(entry.flagBitSet) && (idRanges == null || IdRange.containsUid(idRanges,
                        entry.uid))) {
                    toDelete.add(entry);
                }
            }

            for (MailboxEntries.MessageEntry delEntry : toDelete) {
                log.debug("  Expunge message with uid: " + delEntry.uid + " and msgNum: " + delEntry.msgNum);

                // Step 1: Remove from list
                this.entries.list.remove(delEntry);

                // Step 2: Delete file:
                try {
                    Path toDelPath = this.pathToDir.resolve(delEntry.shortFilename);
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
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                entry.msgNum = index;
                index++;
            }

            this.entries.storeFileToFSWithoutSync();
        }

        // Finally, inform thee listeners
        // TODO: This has to be done backwards, which is quite ugly. Check out why this is so and fix it, and make the order in
        // which the listener is informed order-independent!
        int numToDel = toDelete.size();
        for (int i = numToDel - 1; i >= 0; i--) {
            MailboxEntries.MessageEntry delEntry = toDelete.get(i);
            for (FolderListener expungeListener : _mailboxListeners) {
                expungeListener.expunged(delEntry.msgNum);
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
        return getMessage(uid).getMimeMessage();
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) throws MessagingException {
        this.setLastAccessed();
        ArrayList<MailboxEntries.MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                if (entry.uid >= start && entry.uid <= end) {
                    matchedMessages.add(entry);
                }
            }
        }

        return retrieveAllMimeMessagesFromList(matchedMessages);
    }

    private Message[] retrieveAllMimeMessagesFromList(ArrayList<MailboxEntries.MessageEntry> matchedMessages) {
        List<Message> ret = new ArrayList<>();
        for (MailboxEntries.MessageEntry entry : matchedMessages) {
            ret.add(retrieveOneMessage(entry).getMimeMessage());
        }
        return ret.toArray(new Message[this.entries.list.size()]);
    }


    @Override
    public Message[] getMessagesByUID(long[] uids) throws MessagingException {
        this.setLastAccessed();
        ArrayList<MailboxEntries.MessageEntry> matchedMessages = new ArrayList<>();

        // First: Filter the messages which we are going to return:
        synchronized (this.entries.syncLock) {
            for (MailboxEntries.MessageEntry entry : this.entries.list) {
                for (long searchUid : uids) {
                    if (entry.uid == searchUid) {
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
        OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
        return store.getUidForMessageFromHeader(message);
    }

}
