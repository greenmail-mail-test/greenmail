package com.icegreen.greenmail.filestore;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.icegreen.greenmail.filestore.fs.MessageToFS;
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

            try (FileChannel fc = (FileChannel.open(this.mailboxEntriesFile, WRITE, CREATE))) {
                ByteBuffer copy = ByteBuffer.allocate(MessageEntry.MSG_ENTRY_SIZE);
                long pos = index * MessageEntry.MSG_ENTRY_SIZE;
                fc.position(pos);
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
    void loadFileFromFS(MessageToFS mtf) {
        synchronized (this.syncLock) {
            if (Files.isRegularFile(this.mailboxEntriesFile)) {
                try {

                    boolean changedEntries = false;
                    try (InputStream is = Files.newInputStream(this.mailboxEntriesFile);
                            DataInputStream dis = new DataInputStream(is)) {
                        while (true) {
                            try {
                                MessageEntry e = new MessageEntry();
                                this.readFromDIS(e, dis);
                                this.list.add(e);
                            } catch (EOFException eof) {
                                // Good to know, let's get out of the loop now.
                                break;
                            }
                        }
                    }

                   changedEntries = mtf.cleanupAfterLoading(this.list);

                    if (changedEntries) {
                        // cleanupAfterLoading changed the entries in the file, store them immediatly back to the FS
                        this.storeFileToFSWithoutSync();
                    }
                }
                catch (IOException e) {
                    throw new UncheckedFileStoreException(
                            "IOException happened while trying to read message file: " + this.mailboxEntriesFile,e);
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
        dos.writeInt(me.getMsgNum());
        dos.writeLong(me.getUid());
        dos.writeInt(me.getFlagBitSet());
        dos.writeLong(me.getRecDateMillis());

        dos.writeLong(me.getPositionInMboxFile());
        dos.writeInt(me.getLenInMboxFile());
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void readFromDIS(MessageEntry me, DataInputStream dis) throws IOException {
        // Make sure that writing is not exceeding MessageEntry.MSG_ENTRY_SIZE bytes
        me.setMsgNum(dis.readInt());
        me.setUid(dis.readLong());
        me.setFlagBitSet(dis.readInt());
        me.setRecDateMillis(dis.readLong());

        me.setPositionInMboxFile(dis.readLong());
        me.setLenInMboxFile(dis.readInt());
        // Do this in a backward compatible way: Only add additional properties at the end!
    }


}
