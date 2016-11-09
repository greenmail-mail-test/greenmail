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
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context class to be used by all classes which handle the filestore.
 * <p>
 * Contains an UID Generator which assures that a uid is really unique, by providing a public
 * getNextUid() method. By default, the nextUid is stored periodically to the fileStoreSettings
 * file in order for the UID to be unique across multiple start/stop of GreenMail.
 * <p>
 * Contains a factory method to create file-based mailboxes (FileHiearchicalFolder). Please
 * use only this method to create such instances, because we want to have only one instance
 * for each mailbox. So we cache the created instance, and periodically remove instances from
 * the cache which were no longer in use for 12 hours.
 * <p>
 * This should assure that we only have one instance of FileHiearchicalFolder at a time, and
 * only one instance which writes the mailbox settings file, which is quite an important thing
 * because then we can always change a setting of a mailbox in-memory and store in the mailbox
 * setting file, and we don't have to read the (possibly changed) setting file again and again.
 */
class FileBaseContext {
    final Logger log = LoggerFactory.getLogger(FileBaseContext.class);

    private static final long UID_RANGE = 1000;

    // Path to the rootDir of the FileStore
    private final Path mboxFileStoreRootDir;

    // Path to the filebased mailbox settings. These settings exist only once for each FileStore.
    private final Path fileStoreSettings;

    // Cache of all created FileHierarchicalFolder.
    private HashMap<Path, FileHierarchicalFolder> mailboxCache = new HashMap<>();

    private long uidNextRange = -1;
    private long nextUidToUse = 0;

    // When a mailbox is not used for more than 12 hours, assume that nobody is really doing something
    // with it and remove it from cache... I am not 100% sure whether this might work as intented, because
    // other parts of GreenMail might still use the instance which was removed from cache...
    // TODO: Investigate how long-living the instances of FileHierarchicalFolder are really, e.g. when used
    // in an IMAP session.
    private final static long MAX_AGE_IN_CACHE_MILLIS = 12 * 60 * 60 * 1000;

    /**
     * Package-Private constructor, only to be invoked by the filestore package.
     *
     * @param pathToMboxRootDir
     */
    FileBaseContext(Path pathToMboxRootDir) {
        this.mboxFileStoreRootDir = pathToMboxRootDir;
        if (!Files.isDirectory(this.mboxFileStoreRootDir)) {
            // We have to create the directory if it does not exist
            try {
                Files.createDirectories(this.mboxFileStoreRootDir);
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException("IOEXception while creating the directory: '" + this.mboxFileStoreRootDir
                        .toAbsolutePath() + " to store the file-based Greenmail store.'", e);
            }
        }

        this.fileStoreSettings = this.mboxFileStoreRootDir.resolve("greenmail.filestore.binary");
        this.initUidGenerator();
    }

    public Path getMboxFileStoreRootDir() {
        return this.mboxFileStoreRootDir;
    }

    /**
     * Mailbox factory method using a cache.
     * <p>
     * For each path, we only want to have one single FileHierarchicalFolder instance to live at any given
     * time.
     *
     * @param mboxPath - The path to the mailbox
     * @return a FileHierarchicalFolder for this mailbox
     */
    synchronized FileHierarchicalFolder getMailboxForPath(Path mboxPath) {
        this.cleanupCache();

        Path mboxPathNorm = mboxPath.normalize();

        if (mailboxCache.containsKey(mboxPathNorm)) {
            return mailboxCache.get(mboxPathNorm);
        }
        else {
            FileHierarchicalFolder newEntry = new FileHierarchicalFolder(mboxPathNorm, this);
            this.mailboxCache.put(mboxPathNorm, newEntry);
            return newEntry;
        }
    }

    private void cleanupCache() {
        for (FileHierarchicalFolder folder : this.mailboxCache.values()) {
            if (folder.getAgeOfLastAccessInMillis() > MAX_AGE_IN_CACHE_MILLIS) {
                log.debug("Remove mailbox with path '" + folder.getPathToDir() + "' from memory cache, because it was not "
                        + "accessed for longer than " + folder.getAgeOfLastAccessInMillis() + " millis.");
                this.mailboxCache.remove(folder);
            }
        }
        // It's no longer in cache, let the GC do its jobs and remove it.
    }

    /**
     * Removes the MailFolder from the cache:
     */
    synchronized void removeFromCache(Path mboxPath) {
        Path mboxPathNorm = mboxPath.normalize();
        if (mailboxCache.containsKey(mboxPathNorm)) {
            log.debug("Remove mailbox with path '" + mboxPathNorm + "' from memory cache.");
            mailboxCache.remove(mboxPathNorm);
        }
    }

    void deInitUidGenerator() {
        // Make sure that we don't loose the unused UIDs in the UID range, just write down the next
        // UID to use into the settings file.
        this.uidNextRange = nextUidToUse;
        this.storeFileToFS();
    }

    private void initUidGenerator() {
        this.uidNextRange = -1;
        this.loadFileFromFS();
        if (uidNextRange == -1) {
            // No settings file, so we can start anew with UID generator initial values
            this.nextUidToUse = 1;
            this.uidNextRange = nextUidToUse + UID_RANGE;
        }
        else {
            // Range read in from settings file, start with UIDs at the read-in range:
            this.nextUidToUse = this.uidNextRange;
            this.uidNextRange = nextUidToUse + UID_RANGE;
        }
        // Anyhow, we need to store the settings file
        this.storeFileToFS();
    }

    public long getNextUid() {
        long result = nextUidToUse;
        nextUidToUse++;

        if (this.nextUidToUse >= this.uidNextRange) {
            // We have to increase the uidRange about UID_RANGE, because we want to make sure that
            // when we crash and GreenMail is started again, the same UIDs are not reused anymore.
            this.uidNextRange = this.nextUidToUse + UID_RANGE;
            this.storeFileToFS();
        }
        return result;
    }

    /**
     * The settings file for the MBoxFileStore contains binary instance variables which need to be
     * persisted during invocations.
     */
    private void loadFileFromFS() {
        if (Files.isRegularFile(this.fileStoreSettings)) {
            try {
                try (InputStream is = Files.newInputStream(this.fileStoreSettings); DataInputStream dis = new DataInputStream(is)) {
                    this.readFromDIS(dis);
                }
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException("IOException happened while trying to read the Filestore settings file: " +
                        this.fileStoreSettings,e);
            }
        }
    }

    /**
     * The settings file for the MBoxFileStore contains binary instance variables which need to be
     * persisted during invocations.
     */
    private void storeFileToFS() {
        try {
            try (OutputStream os = Files.newOutputStream(this.fileStoreSettings, CREATE, WRITE, TRUNCATE_EXISTING); DataOutputStream
                    dos = new DataOutputStream(os)) {
                this.writeToDOS(dos);
                // Do this in a backward compatible way: Only add additional properties at the end!
                dos.flush();
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException("IOException happened while trying to write the Filestore settings file: " + this.fileStoreSettings, e);
        }
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void writeToDOS(DataOutputStream dos) throws IOException {
        dos.writeLong(this.uidNextRange);
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

    /**
     * Writes a single entry to the DataOutputStream
     */
    private void readFromDIS(DataInputStream dis) throws IOException {
        this.uidNextRange = dis.readLong();
        // Do this in a backward compatible way: Only add additional properties at the end!
    }

}
