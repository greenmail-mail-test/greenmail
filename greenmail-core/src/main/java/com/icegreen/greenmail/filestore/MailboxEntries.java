package com.icegreen.greenmail.filestore;

import static com.icegreen.greenmail.filestore.OneMessagePerFileStore.FILE_ENDING;
import static com.icegreen.greenmail.filestore.OneMessagePerFileStore.FILE_ENDING_LEN;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binary file which contains some information for each message in a mailbox.
 * File name is normally greenmail.messageEntries.binary
 * See methods writeToDOS() and readFromDIS() for more information about the content of the file.
 */
class MailboxEntries {

    final Logger log = LoggerFactory.getLogger(FileHierarchicalFolder.class);

    final Object syncLock = new Object();

    // Mailbox list which are stored in the mailboxEntriesFile, every change here must be synced to the FS:
    ArrayList<MessageEntry> list = new ArrayList<>();

    private Path mailboxEntriesFile;

    MailboxEntries(Path pathToEntriesFile) {
        this.mailboxEntriesFile = pathToEntriesFile;
    }

    void storeFileToFS() {
        synchronized (this.syncLock) {
            this.storeFileToFSWithoutSync();
        }
    }

    void storeFileToFSWithoutSync() {
        try {
            try (OutputStream os = Files.newOutputStream(this.mailboxEntriesFile, CREATE, WRITE, TRUNCATE_EXISTING);
                    DataOutputStream
                            dos = new DataOutputStream(os)) {
                dos.writeInt(this.list.size());
                for (MessageEntry me : list) {
                    this.writeToDOS(me, dos);
                }
                dos.flush();
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException("IOException happened while trying to write message file: " + this
                    .mailboxEntriesFile, e);
        }
    }

    /**
     * Writes only one single MessageEntry to file
     *
     * @param index - zero-based index into the list of MessageEntries
     */
    void storeFileToFSForSingleEntryWithoutSync(int index) {
        ByteBuffer toWriteBuffer;
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(MessageEntry.MSG_ENTRY_SIZE);
                    DataOutputStream dos = new DataOutputStream(bos);) {
                MessageEntry me = this.list.get(index);
                this.writeToDOS(me, dos);
                toWriteBuffer = ByteBuffer.wrap(bos.toByteArray());
            }

            try (FileChannel fc = (FileChannel.open(this.mailboxEntriesFile, WRITE))) {
                ByteBuffer copy = ByteBuffer.allocate(MessageEntry.MSG_ENTRY_SIZE);
                fc.position(MessageEntry.COUNTER_SIZE + (index * MessageEntry.MSG_ENTRY_SIZE));
                fc.write(toWriteBuffer);
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException(
                    "IOException happened while trying to write one entry (index: " + index + ") to message file: "
                            + this.mailboxEntriesFile,
                    e);
        }
    }

    /**
     * Load the settings from the settings file from the file system.
     */
    void loadFileFromFS() {
        synchronized (this.syncLock) {
            if (Files.isRegularFile(this.mailboxEntriesFile)) {
                try {
                    try (InputStream is = Files.newInputStream(this.mailboxEntriesFile);
                            DataInputStream dis = new DataInputStream(is)) {
                        // First entry: Number of list
                        int numEntries = dis.readInt();
                        for (int i = 0; i < numEntries; i++) {
                            // Then, the list begin:
                            MessageEntry e = new MessageEntry();
                            this.readFromDIS(e, dis);
                            this.list.add(e);
                        }
                    }

                    // FIXME: Now, we still rely on different file names in the directory, so we need to match the filenames
                    // with the list in list
                    for (File f : this.mailboxEntriesFile.getParent().toFile().listFiles()) {
                        if (f.isFile()) {
                            String fName = f.getName();
                            if (fName.endsWith(FILE_ENDING)) {
                                String uidStr = fName.substring(0, fName.length() - FILE_ENDING_LEN);
                                try {
                                    long uid = Long.parseLong(uidStr);

                                    // Store the filename in the list:
                                    for (MessageEntry e : this.list) {
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
                            "IOException happened while trying to read message file: " + this.mailboxEntriesFile,
                            e);
                }
            }
        }
    }

    /**
     * Delete the setting file from the filesystem.
     */
    void deleteFileFromFS() {
        if (Files.isRegularFile(this.mailboxEntriesFile)) {
            try {
                Files.delete(this.mailboxEntriesFile);
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException("IOException happened while trying to delete message file: " + this
                        .mailboxEntriesFile, e);
            }
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
        dos.writeLong(me.recDateMillis);
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
        me.recDateMillis = dis.readLong();
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

    /**
     * Internal class to describe a message entry in memory.
     */
    static class MessageEntry {
        static final int MSG_ENTRY_SIZE = 20;
        static final int COUNTER_SIZE = 4;

        int msgNum;
        long uid;
        int flagBitSet;
        long recDateMillis;
        String shortFilename;
    }

}
