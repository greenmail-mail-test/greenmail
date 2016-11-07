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

/**
 * Context class to be used by all classes which handle the filestore.
 *
 * Contains an UID Generator which assures that a uid is really unique, by providing a public
 * getNextUid() method. By default, the nextUid is stored periodically to the mboxSettings
 * file in order for the UID to be unique across multiple start/stop of GreenMail.
 *
 * Contains a factory method to create file-based mailboxes (FileHiearchicalFolder). Please
 * use only this method to create such instances, because we want to have only one instance
 * for each mailbox. So we cache the created instance, and periodically remove instances from
 * the cache which were no longer in use for 30 minutes.
 *
 * TODO: Expiry of 30 minutes not yet implemented.
 *
 * This should assure that we only have one instance of FileHiearchicalFolder at a time, and
 * only one instance which writes the mailbox settings file, which is quite an important thing
 * because then we can always change a setting of a mailbox in-memory and store in the mailbox
 * setting file, and we don't have to read the (possibly changed) setting file again and again.
 *
 * Created by saladin on 11/3/16.
 */
class FileBaseContext {
	// Path to the filebased mailbox settings. These settings exist only once for each FileStore.
	private final Path mboxSettings;

	// Cache of all created FileHierarchicalFolder.
	private HashMap<Path, MBoxCacheEntry>mailboxCache = new HashMap<>();

	// UID Generator values
	private final Path mboxFileStoreRootDir;
	private long uidNextRange = -1;
	private long nextUidToUse = 0;
	private static final long UID_RANGE = 1000;

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

		this.mboxSettings = this.mboxFileStoreRootDir.resolve("greenmail.mbox.binary");
		this.initUidGenerator();
	}

	/**
	 * Private helper class to store the FileHierarchicalFolder cache entries with  their age.
	 */
	private static class MBoxCacheEntry {
		private long lastUsedTimestampMillis = 0;
		private FileHierarchicalFolder mailbox;
	}


	public Path getMboxFileStoreRootDir() {
		return this.mboxFileStoreRootDir;
	}
	/**
	 * Mailbox factory method using a cache.
	 *
	 * For each path, we only want to have one single FileHierarchicalFolder instance to live at any given
	 * time.
	 *
	 * @param mboxPath - The path to the mailbox
	 * @return a FileHierarchicalFolder for this mailbox
	 */
	synchronized FileHierarchicalFolder getMailboxForPath(Path mboxPath) {
		Path mboxPathNorm = mboxPath.normalize();

		if (mailboxCache.containsKey(mboxPathNorm)) {
			MBoxCacheEntry entry = mailboxCache.get(mboxPathNorm);
			entry.lastUsedTimestampMillis = System.currentTimeMillis();
			return entry.mailbox;
		}  else {
			MBoxCacheEntry newEntry = new MBoxCacheEntry();
			newEntry.mailbox = new FileHierarchicalFolder(mboxPathNorm, this);
			newEntry.lastUsedTimestampMillis = System.currentTimeMillis();
			this.mailboxCache.put(mboxPathNorm, newEntry);
			return newEntry.mailbox;
		}
	}


	void deInitUidGenerator() {
		// Make sure that we don't loose the unused UIDs in the UID range, just write down the next
		// UID to use into the settings file.
		this.uidNextRange = nextUidToUse;
		this.writeChangedSettingsFile();
	}

	private void initUidGenerator() {
		this.uidNextRange = -1;
		this.readSettingsFile();
		if (uidNextRange == -1) {
			// No settings file, so we can start anew with UID generator initial values
			this.nextUidToUse = 1;
			this.uidNextRange = nextUidToUse + UID_RANGE;
		} else {
			// Range read in from settings file, start with UIDs at the read-in range:
			this.nextUidToUse = this.uidNextRange;
			this.uidNextRange = nextUidToUse + UID_RANGE;
		}
		// Anyhow, we need to store the settings file
		this.writeChangedSettingsFile();
	}

	public long getNextUid() {
		long result = nextUidToUse;
		nextUidToUse++;

		if (this.nextUidToUse >= this.uidNextRange) {
			// We have to increase the uidRange about UID_RANGE, because we want to make sure that
			// when we crash and GreenMail is started again, the same UIDs are not reused anymore.
			this.uidNextRange = this.nextUidToUse + UID_RANGE;
			this.writeChangedSettingsFile();
		}
		return result;
	}

	/**
	 * The settings file for the MBoxFileStore contains binary instance variables which need to be
	 * persisted during invocations.
	 */
	private void readSettingsFile() {
		if (Files.isRegularFile(this.mboxSettings)) {
			try {
				try (InputStream is = Files.newInputStream(this.mboxSettings); DataInputStream dis = new DataInputStream(is)) {
					this.uidNextRange = dis.readLong();
					// Do this in a backward compatible way: Only add additional properties at the end!
				}
			}
			catch (IOException e) {
				throw new UncheckedFileStoreException(
						"IOException happened while trying to read MBoxFileStore settings file: " + this.mboxSettings,
						e);
			}
		}
	}

	/**
	 * The settings file for the MBoxFileStore contains binary instance variables which need to be
	 * persisted during invocations.
	 */
	private void writeChangedSettingsFile() {
		try {
			try (OutputStream os = Files.newOutputStream(this.mboxSettings, CREATE, WRITE, TRUNCATE_EXISTING); DataOutputStream
					dos = new DataOutputStream(os)) {
				dos.writeLong(this.uidNextRange);
				// Do this in a backward compatible way: Only add additional properties at the end!
				dos.flush();
			}
		}
		catch (IOException e) {
			throw new UncheckedFileStoreException("IOException happened while trying to write MBoxFileStore settings file: " + this
					.mboxSettings,	e);
		}
	}


}
