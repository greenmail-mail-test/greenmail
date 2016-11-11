package com.icegreen.greenmail.filestore;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binary file which contains the necessary settings per mailbox.
 * File name is normally greenmail.mailbox.binary
 * See methods writeToDOS() and readFromDIS() for more information about the content of the file.
 */
class MailboxSettings {

    final Logger log = LoggerFactory.getLogger(FileHierarchicalFolder.class);

    private final Object syncLock = new Object();

    // Settings which are stored in the mailboxSettingsFile:
    boolean isSelectable = false;

    private Path mailboxSettingsFile;

    MailboxSettings(Path pathToSettingsFile) {
        this.mailboxSettingsFile = pathToSettingsFile;
    }

    /**
     * Load the settings from the settings file from the file system.
     */
    void loadFileFromFS() {
        synchronized (this.syncLock) {
            if (Files.isRegularFile(this.mailboxSettingsFile)) {
                try {
                    try (InputStream is = Files.newInputStream(this.mailboxSettingsFile);
                            DataInputStream dis = new DataInputStream(is)) {
                        this.readFromDIS(dis);
                        // Do this in a backward compatible way: Only add additional properties at the end!
                    }
                }
                catch (IOException e) {
                    throw new UncheckedFileStoreException(
                            "IOException happened while trying to read settings file: " + this.mailboxSettingsFile,
                            e);
                }
            }
        }
    }

    /**
     * Store the setting file with the settings to the file system. Overwrite existing files.
     */
    void storeFileToFS() {
        synchronized (this.syncLock) {
            try {
                try (OutputStream os = Files.newOutputStream(this.mailboxSettingsFile, CREATE, WRITE, TRUNCATE_EXISTING);
                        DataOutputStream
                                dos =
                                new
                                        DataOutputStream(os)) {
                    this.writeToDOS(dos);
                    dos.flush();
                }
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException("IOException happened while trying to write settings file: " + this
                        .mailboxSettingsFile, e);
            }
        }
    }

    /**
     * Delete the setting file from the filesystem.
     */
    void deleteFileFromFS() {
        synchronized (this.syncLock) {
            if (Files.isRegularFile(this.mailboxSettingsFile)) {
                try {
                    Files.delete(this.mailboxSettingsFile);
                }
                catch (IOException e) {
                    throw new UncheckedFileStoreException(
                            "IOException happened while trying to delete settings: " + this.mailboxSettingsFile, e);
                }
            }
        }
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void writeToDOS(DataOutputStream dos) throws IOException {
        dos.writeBoolean(this.isSelectable);
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void readFromDIS(DataInputStream dis) throws IOException {
        this.isSelectable = dis.readBoolean();
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

}
