package com.icegreen.greenmail.filestore;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
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
 *
 * Created by saladin on 11/1/16.
 */
public class FileHierarchicalFolder implements MailFolder, UIDFolder {
    final Logger log = LoggerFactory.getLogger(FileHierarchicalFolder.class);


	private final List<FolderListener> _mailboxListeners = Collections.synchronizedList(new ArrayList<FolderListener>());
	protected String name;
	private Path pathToDir;
	private Path settingsPath;
	private Path messagesPath;
	private FileBaseContext ctx;

	// Settings which are stored in the settingsFile:
	private boolean isSelectable = false;
	// Mailbox entries which are stored in the mboxFile, every change here must be synced to the FS:
	private ArrayList<MessageEntry> messageEntries = new ArrayList<>();
	private final Object messageEntriesLock = new Object();

	protected FileHierarchicalFolder(Path pathToMailbox, FileBaseContext ctx) {
		this.name = pathToMailbox.getFileName().toString();
		this.pathToDir = pathToMailbox;

        log.debug("Entering FileHierarchicalFolder constructor for path: " + this.pathToDir.toAbsolutePath().toString());
		this.settingsPath = this.pathToDir.resolve("greenmail.mailbox.binary");
		this.messagesPath = this.pathToDir.resolve("greenmail.messageEntries.binary");
		this.ctx = ctx;

		try {
			if (!Files.isDirectory(this.pathToDir)) {
				// We have to create the directory if it does not exist
				Files.createDirectories(this.pathToDir);
				this.writeChangedSettingsFile();
			}
			else {
				this.readSettingsFile();
				this.readMessageEntriesFile();
			}
		} catch (IOException io) {
			throw new UncheckedFileStoreException("IOEXception while creating the filestore with path: '" + this.pathToDir
					.toAbsolutePath() + "'", io);
		}
	}

	private static class MessageEntry {
		private int msgNum;
		private long uid;
		private int flagBitSet;
		private String shortFilename;

        private static final int MSG_ENTRY_SIZE = 16;
        private static final int COUNTER_SIZE = 4;
	}

	/**
	 * Make sure that the settings and message entries files are removed and deleted.
	 */
	public void prepareForDeletion() {
		this.deleteSettingsFile();
		this.deleteMessageEntriesFile();
	}

	public Path getPathToDir() {
		return this.pathToDir;
	}

	public boolean hasChildren() {
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
		synchronized (this.messageEntriesLock) {
			return this.messageEntries.size();
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
		// Use the passed context to retrieve the next unique UID.
		return this.ctx.getNextUid();
	}

	@Override
	public int getUnseenCount() {
        int numUnSeen = 0;

        synchronized (messageEntriesLock) {
            for (MessageEntry entry : this.messageEntries) {
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
       synchronized (messageEntriesLock) {
            for (MessageEntry entry : this.messageEntries) {
                if (!FileStoreUtil.isSeenFlagSet(entry.flagBitSet)) {
                    return entry.msgNum;
                }
            }
        }
		return -1;
	}

	@Override
	public int getRecentCount(boolean reset) {
        int numRecent = 0;
        synchronized (messageEntriesLock) {
            for (MessageEntry entry : this.messageEntries) {
                if (FileStoreUtil.isRecentFlagSet(entry.flagBitSet)) {
                    numRecent++;
                }
            }
        }
        return numRecent;
	}

	@Override
	public int getMsn(long uid) throws FolderException {
		synchronized (messageEntriesLock) {
			for (MessageEntry e : this.messageEntries) {
				if (uid == e.uid) {
					return e.msgNum;
				}
			}
		}
		throw new FolderException("No such message with UID '" + uid + "' in folder: " + this.pathToDir.toString());
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
		ArrayList<MessageEntry>matchedMessages = new ArrayList<>();

		// First: Filter the messages which we are going to return:
		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
				if (range.includes(entry.msgNum)) {
					matchedMessages.add(entry);
				}
			}
		}
		return retrieveAllMessagesFromList(matchedMessages);
	}

	public List<StoredMessage> getMessageEntries() {
		ArrayList<MessageEntry>matchedMessages = new ArrayList<>();

		// First: Filter the messages which we are going to return:
		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
				matchedMessages.add(entry);
			}
		}
		return retrieveAllMessagesFromList(matchedMessages);
	}

	private List<StoredMessage>retrieveAllMessagesFromList(ArrayList<MessageEntry> matchedMessages) {
		List<StoredMessage> ret = new ArrayList<>();
		for (MessageEntry entry : matchedMessages) {
			ret.add(retrieveOneMessage(entry));
		}
		return ret;
	}

	private StoredMessage retrieveOneMessage(MessageEntry entry) {
        log.debug("Retrieving one message from store with uid: " + entry.shortFilename + " and resetting flags to : " + entry
                .flagBitSet);
		try {
			OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
			return store.retrieveMessage(entry.shortFilename, entry.flagBitSet);
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
		return isSelectable;
	}

	public void setSelectable(boolean selectable) {
		isSelectable = selectable;
		this.writeChangedSettingsFile();
	}

	@Override
	public long appendMessage(MimeMessage message,
			Flags flags,
			Date receivedDate) {
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
		} catch (MessagingException e) {
			throw new IllegalStateException("Can not set flags", e);
		}
		StoredMessage storedMessage = new StoredMessage(message,
				receivedDate, uid);

		MessageEntry entry = new MessageEntry();
		synchronized (this.messageEntriesLock) {
			entry.uid = uid;
			entry.msgNum = this.messageEntries.size() + 1;
			this.messageEntries.add(entry);
		}

		OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
		try {
			String fileName = store.addMessage(storedMessage, entry.msgNum);
			// Now, adapt the messages:
			entry.shortFilename = fileName;
            entry.flagBitSet = FileStoreUtil.convertFlagsToFlagBitSet(storedMessage.getMimeMessage().getFlags());
            log.debug("Successfully added a new entry to the FS with uid '" + entry.uid + "' and flags: " + entry.flagBitSet);
			this.writeMessageEntriesFile();
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
		ArrayList<MessageEntry>matchedMessages = new ArrayList<>();

		// First: Filter the messages which we are going to return:
		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
				if (!FileStoreUtil.isDeletedFlagSet(entry.flagBitSet)) {
					matchedMessages.add(entry);
				}
			}
		}
		return retrieveAllMessagesFromList(matchedMessages);
	}

	@Override
	public void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
        log.debug("Entering setFlags with: ");
        log.debug("  Flags          : " + flags);
        log.debug("  Value          : " + value);
        log.debug("  UID            : " + uid);
        log.debug("  silentListener : " + silentListener);
        log.debug("  addUid         : " + addUid);

		MessageEntry me = null;
        int meIndex = 0;

		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
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
            this.writeMessageEntriesFileWithSingleEntryWithoutSync(meIndex);
        }

		Long uidNotification = null;
		if (addUid) {
			uidNotification = uid;
		}
		notifyFlagUpdate(me.msgNum, FileStoreUtil.convertFlagBitSetToFlags(me.flagBitSet), uidNotification, silentListener);
	}

	@Override
	public void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException {
		MessageEntry me = null;
        int meIndex = 0;

		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
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

            this.writeMessageEntriesFileWithSingleEntryWithoutSync(meIndex);
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
		store(mail.getMessage());
	}

	@Override
	public void store(MimeMessage message) throws Exception {
		Date receivedDate = new Date();
		Flags flags = new Flags();
		appendMessage(message, flags, receivedDate);
	}

	@Override
	public StoredMessage getMessage(long uid) {
        log.debug("Entering getMessage with uid: " + uid);
		MessageEntry me = null;

		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
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
		synchronized (messageEntriesLock) {
			int num = this.messageEntries.size();
			long[] ret = new long[num];
			for (int i = 0; i < num; i++) {
				ret[i] = this.messageEntries.get(i).uid;
			}
			return ret;
		}
	}

	@Override
	public void deleteAllMessages() {
		synchronized (messageEntriesLock) {
			try {
				for (MessageEntry entry : this.messageEntries) {
					Path fullpath = this.pathToDir.resolve(entry.shortFilename);
					Files.delete(fullpath);
				}
				this.messageEntries.clear();
			} catch (IOException io) {
				throw new UncheckedFileStoreException(
						"IOException happened while trying to delete all message in directory: " + this.pathToDir, io);
			}
		}
	}


	@Override
	public long[] search(SearchTerm searchTerm) {
        log.debug("Entering search with : " + searchTerm);

        // This is quite ugly, we need to load all the message into memory and search for each one individually:
        ArrayList<MessageEntry> copyList = new ArrayList<>();
        synchronized (this.messageEntriesLock) {
            copyList.addAll(this.messageEntries);
        }

        ArrayList<Long> result = new ArrayList<>();
        for (MessageEntry entry : copyList) {
            StoredMessage msg = retrieveOneMessage(entry);
            if (searchTerm.match(msg.getMimeMessage())) {
                result.add(entry.uid);
            }
        }

        int len = result.size();
        long[] resultArray = new long[len];
        StringBuilder debugStr = new StringBuilder();
        for (int i=0;i<len;i++) {
            resultArray[i] = result.get(i);
            debugStr.append(resultArray[i]).append(",");
        }
        log.debug("Leaving search with result: [" + debugStr.toString() + "]");
        return resultArray;

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
	public void expunge() throws FolderException {
		expunge(null);
	}

	@Override
	public void expunge(IdRange[] idRanges) {
        log.debug("Entering expunge with id range: " + idRanges);

        ArrayList<MessageEntry>toDelete = new ArrayList<>();
        int numDeleted = 0;

		synchronized (this.messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
				if (FileStoreUtil.isDeletedFlagSet(entry.flagBitSet) && (idRanges == null || IdRange.containsUid(idRanges,
						entry.uid ))) {
					toDelete.add(entry);
				}
			}

			for (MessageEntry delEntry : toDelete) {
                log.debug("  Expunge message with uid: " + delEntry.uid + " and msgNum: " + delEntry.msgNum);

				// Step 1: Remove from list
				this.messageEntries.remove(delEntry);

				// Step 2: Delete file:
				try {
                    Path toDelPath =this.pathToDir.resolve(delEntry.shortFilename);
                    log.debug("  Delete file for expunged message: " + toDelPath.toString());
                    Files.delete(toDelPath);
                    numDeleted++;
                } catch (IOException io) {
                    // Ugly, but it is really not so important if the file cannog be deleted. Let's log it and go ahead.
                    //TODO: logging
                }
			}

			// Finally, we have to renumber the messages again, because messageNumber is actually just an 1-based index
            // into the list:
            int index = 1;
            for (MessageEntry entry : this.messageEntries) {
                entry.msgNum = index;
                index++;
            }

			this.writeMessageEntriesFileWithoutSync();
		}

		// Finally, inform thee listeners
        // TODO: This has to be done backwards, which is quite ugly. Check out why this is so and fix it, and make the order in
        // which the listener is informed order-independent!
        int numToDel = toDelete.size();
        for (int i = numToDel - 1; i >= 0; i--) {
            MessageEntry delEntry = toDelete.get(i);
            for (FolderListener expungeListener : _mailboxListeners) {
                expungeListener.expunged(delEntry.msgNum);
            }
        }

        log.debug("Leaving expunge, deleted # of messages: " + numDeleted);
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
		return "FileHierarchicalFolder{" +
				"name='" + name + '\'' +
				", path=" + this.pathToDir +
				", isSelectable=" + this.isSelectable +
				'}';
	}


	@Override
	public Message getMessageByUID(long uid) throws MessagingException {
		return getMessage(uid).getMimeMessage();
	}

	@Override
	public Message[] getMessagesByUID(long start, long end) throws MessagingException {
		ArrayList<MessageEntry>matchedMessages = new ArrayList<>();

		// First: Filter the messages which we are going to return:
		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
				if (entry.uid >= start && entry.uid <= end) {
					matchedMessages.add(entry);
				}
			}
		}

		return retrieveAllMimeMessagesFromList(matchedMessages);
	}

	private Message[]retrieveAllMimeMessagesFromList(ArrayList<MessageEntry> matchedMessages) {
		List<Message> ret = new ArrayList<>();
		for (MessageEntry entry : matchedMessages) {
			ret.add(retrieveOneMessage(entry).getMimeMessage());
		}
		return ret.toArray(new Message[messageEntries.size()]);
	}


	@Override
	public Message[] getMessagesByUID(long[] uids) throws MessagingException {
		ArrayList<MessageEntry>matchedMessages = new ArrayList<>();

		// First: Filter the messages which we are going to return:
		synchronized (messageEntriesLock) {
			for (MessageEntry entry : this.messageEntries) {
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
		// We must ressort to our custom UID header here:
		OneMessagePerFileStore store = new OneMessagePerFileStore(this.pathToDir);
		return store.getUidForMessage(message);
	}

	private void readSettingsFile() {
		if (Files.isRegularFile(this.settingsPath)) {
			try {
				try (InputStream is = Files.newInputStream(this.settingsPath); DataInputStream dis = new DataInputStream(is)) {
					this.isSelectable = dis.readBoolean();
					// Do this in a backward compatible way: Only add additional properties at the end!
				}
			}
			catch (IOException e) {
				throw new UncheckedFileStoreException(
						"IOException happened while trying to read settings file: " + this.settingsPath,
						e);
			}
		}
	}

	private void writeChangedSettingsFile() {
		try {
			try (OutputStream os = Files.newOutputStream(this.settingsPath, CREATE, WRITE, TRUNCATE_EXISTING); DataOutputStream
					dos =
					new
							DataOutputStream(os)) {
				dos.writeBoolean(this.isSelectable);
				// Do this in a backward compatible way: Only add additional properties at the end!
				dos.flush();
			}
		}
		catch (IOException e) {
			throw new UncheckedFileStoreException("IOException happened while trying to write settings file: " + this
					.settingsPath,	e);
		}
	}

	private void deleteSettingsFile() {
		if (Files.isRegularFile(this.settingsPath)) {
			try {
				Files.delete(this.settingsPath);
			}
			catch (IOException e) {
				throw new UncheckedFileStoreException("IOException happened while trying to delete settings: " + this.settingsPath,	e);
			}
		}
	}

	private void deleteMessageEntriesFile() {
		if (Files.isRegularFile(this.messagesPath)) {
			try {
				Files.delete(this.messagesPath);
			}
			catch (IOException e) {
				throw new UncheckedFileStoreException("IOException happened while trying to delete message file: " + this
						.messagesPath,	e);
			}
		}
	}

	private void writeMessageEntriesFile() {
		synchronized (this.messageEntriesLock) {
            this.writeMessageEntriesFileWithoutSync();
		}
	}

	private void writeMessageEntriesFileWithoutSync() {
        try {
            try (OutputStream os = Files.newOutputStream(this.messagesPath, CREATE, WRITE, TRUNCATE_EXISTING);
                    DataOutputStream
                            dos = new DataOutputStream(os)) {
                dos.writeInt(this.messageEntries.size());
                for (MessageEntry me : messageEntries) {
                    this.writeToDOS(me, dos);
                }
                dos.flush();
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException("IOException happened while trying to write message file: " + this
                    .messagesPath, e);
        }
    }

    /**
     * Writes only one single MessageEntry to file
     *
     * @param index
     */
    private void writeMessageEntriesFileWithSingleEntryWithoutSync(int index) {
        ByteBuffer toWriteBuffer;
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(MessageEntry.MSG_ENTRY_SIZE);
                    DataOutputStream dos = new DataOutputStream(bos);) {
                MessageEntry me = this.messageEntries.get(index);
                this.writeToDOS(me, dos);
                toWriteBuffer = ByteBuffer.wrap(bos.toByteArray());
            }

            try (FileChannel fc = (FileChannel.open(this.messagesPath, WRITE))) {
                ByteBuffer copy = ByteBuffer.allocate(MessageEntry.MSG_ENTRY_SIZE);
                fc.position(MessageEntry.COUNTER_SIZE + (index * MessageEntry.MSG_ENTRY_SIZE));
                fc.write(toWriteBuffer);
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException(
                    "IOException happened while trying to write message file: " + this.messagesPath,
                    e);
        }
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void writeToDOS(MessageEntry me, DataOutputStream dos) throws IOException {
        // Make sure that writing is not exceeding MessageEntry.MSG_ENTRY_SIZE bytes
        dos.writeInt(me.msgNum);
        dos.writeLong(me.uid);
        dos.writeInt(me.flagBitSet);
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void readFromDIS(MessageEntry me, DataInputStream dis) throws IOException {
        // Make sure that writing is not exceeding MessageEntry.MSG_ENTRY_SIZE bytes
        me.msgNum = dis.readInt();
        me.uid = dis.readLong();
        me.flagBitSet = dis.readInt();
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

	private void readMessageEntriesFile() {
		synchronized (this.messageEntriesLock) {
			if (Files.isRegularFile(this.messagesPath)) {
				try {
					try (InputStream is = Files.newInputStream(this.messagesPath);
							DataInputStream dis = new DataInputStream(is)) {
						// First entry: Number of entries
						int numEntries = dis.readInt();
						for (int i = 0; i < numEntries; i++) {
							// Then, the entries begin:
							MessageEntry e = new MessageEntry();
                            this.readFromDIS(e, dis);
							this.messageEntries.add(e);
						}
					}

					// FIXME: Now, we still rely on different file names in the directory, so we need to match the filenames
					// with the entries in messageEntries
					for (File f : this.pathToDir.toFile().listFiles()) {
						if (f.isFile()) {
							StringTokenizer fileNameToken = new StringTokenizer(f.getName(), "_");
							if (fileNameToken.countTokens() == 3) {
								String msgNumStr = fileNameToken.nextToken();
								String uidStr = fileNameToken.nextToken();
								try {
									long uid = Long.parseLong(uidStr);

									// Store the filename in the entries:
									for (MessageEntry e : this.messageEntries) {
										if (uid == e.uid) {
											e.shortFilename = f.getName();
											break;
										}
									}
								}
								catch (NumberFormatException nfe) {
									// Ignore files which do not match naming convention and continue with next file.
								}
							}
						}
					}
				}
				catch (IOException e) {
					throw new UncheckedFileStoreException(
							"IOException happened while trying to read message file: " + this.messagesPath,
							e);
				}
			}
		}
	}

}
