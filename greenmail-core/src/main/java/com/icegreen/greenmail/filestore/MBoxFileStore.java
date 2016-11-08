package com.icegreen.greenmail.filestore;

import static com.icegreen.greenmail.imap.ImapConstants.HIERARCHY_DELIMITER;
import static com.icegreen.greenmail.imap.ImapConstants.USER_NAMESPACE;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.mail.Quota;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by saladin on 11/1/16.
 *
 * TODO: When we have a message store which is file-based, we should implement a user-store which is file-based
 * as well... Otherwise, users which were created automatically no longer exists after a restart and cannot be
 * used to login.
 *
 */
public class MBoxFileStore implements Store {
	final Logger log = LoggerFactory.getLogger(MBoxFileStore.class);

	private FileBaseContext ctx;
	private final Path rootDir;
	private final Path userListFile;
	private final Path pidFile;


	/**
	 * All classes implementing Store must have a public constructor which takes the configuration as parameter.
	 *
	 * @param startupConfig - startup configuration
	 */
	public MBoxFileStore(GreenMailConfiguration startupConfig) {
		this.rootDir = Paths.get(startupConfig.getFileStoreRootDirectory());
		this.pidFile = this.rootDir.resolve("greenmail.pid");
        if (Files.isRegularFile(this.pidFile)) {
            throw new UncheckedFileStoreException("Greenmail PID file '" + this.pidFile.toAbsolutePath().toString() + "' "
                    + "already exists. No two running Greenmail instances can access the same filestore. Please make sure that "
                    + "the process referred in the PID file is no longer running, and then delete the PID file manually. Thanks"
                    + " you.");
        }

        this.userListFile = this.rootDir.resolve("userlist");
        this.ctx = new FileBaseContext(this.rootDir);
		this.writePIDFile();
	}

	public void stop() {
		// Make sure that the UUID generator is stopped correctly and the nextUID persisted to file-system
		this.ctx.deInitUidGenerator();
        this.deletePIDFile();
	}

	public MailFolder getMailbox(String absoluteMailboxName) {
		log.debug("Entering getMailbox with absoluteMailboxName: '" + absoluteMailboxName + "'");
		if (absoluteMailboxName == null || USER_NAMESPACE.equals(absoluteMailboxName)) {
            MailFolder result = ctx.getMailboxForPath(this.rootDir.resolve(ImapConstants.USER_NAMESPACE));
            log.debug("Leaving getMailbox with mailbox for root: " + result);
			return result;
		}
		StringTokenizer tokens = new StringTokenizer(absoluteMailboxName, HIERARCHY_DELIMITER);
		// The first token must be "#mail"
		if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE)) {
            throw new UncheckedFileStoreException("Mailbox with absolute name '" + absoluteMailboxName + "' is not valid "
                    + "because it does not start with '" + USER_NAMESPACE + "'");
		}
		Path mboxPath = FileStoreUtil.convertFullNameToPath(this.rootDir.toAbsolutePath().toString(), absoluteMailboxName);
        if (Files.isDirectory(mboxPath)) {
            MailFolder result = ctx.getMailboxForPath(mboxPath);
            log.debug("Leaving getMailbox with existing mailbox: " + result);
            return result;
        }
        log.debug("Leaving getMailbox with null because mailbox is not existing.");
        return null;
	}

    public MailFolder getMailbox(MailFolder parent, String mailboxName) {
        log.debug("Enterint getMailbox with parent '" + parent + "' and mailboxName '" + mailboxName + "'");
        if (!(parent instanceof FileHierarchicalFolder)) {
            throw new UncheckedFileStoreException("Cannot create a MBoxFileStore mailbox from a parent which is not of type FileHierarchicalFolder");
        }
        FileHierarchicalFolder parentCasted = (FileHierarchicalFolder)parent;
        Path parentPath = parentCasted.getPathToDir();
        Path mboxPath = parentPath.resolve(mailboxName);

        if (Files.isDirectory(mboxPath)) {
            FileHierarchicalFolder child = ctx.getMailboxForPath(parentPath.resolve(mailboxName));
            log.debug("Leaving getMailbox(parent,name) with mailbox: " + child);
            return child;
        }

        log.debug("Leaving getMailbox(parent,name) with null because mailbox is not existing.");
        return null;
    }


	public MailFolder createMailbox(MailFolder parent, String mailboxName, boolean selectable) throws FolderException {
		log.debug("Entering createMailbox with parent: '" + parent + "' and name '" + mailboxName + "', and selectable: " + selectable);

		if (!(parent instanceof FileHierarchicalFolder)) {
			throw new UncheckedFileStoreException("Cannot create a MBoxFileStore mailbox from a parent which is not of type "
                    + "FileHierarchicalFolder, parent is of type: " + parent.getClass().toString());
		}
		FileHierarchicalFolder parentCasted = (FileHierarchicalFolder)parent;
		Path parentPath = parentCasted.getPathToDir();
		FileHierarchicalFolder child = ctx.getMailboxForPath(parentPath.resolve(mailboxName));
		child.setSelectable(selectable);
		return child;
	}

	public Collection<MailFolder> listMailboxes(String searchPattern) throws FolderException {
        log.debug("Entering listMailboxes with searchPattern: '" + searchPattern + "'");

		int starIdx = searchPattern.indexOf('*');
		int percentIdx = searchPattern.indexOf('%');
		int searchPatLenMinus1 = searchPattern.length() - 1;

		// We only handle wildcard at the end of the search pattern.
		if ((starIdx > -1 && starIdx < searchPatLenMinus1) || (percentIdx > -1 && percentIdx < searchPatLenMinus1)) {
			throw new FolderException("Wildcard characters are only handled as the last character of a list argument.");
		}

		List<MailFolder> result = new ArrayList<>();
		if (starIdx != -1 || percentIdx != -1) {
			int lastDot = searchPattern.lastIndexOf(HIERARCHY_DELIMITER);
			String parentName;
			if (lastDot < 0) {
				parentName = USER_NAMESPACE;
			} else {
				parentName = searchPattern.substring(0, lastDot);
			}

			String matchPattern = searchPattern.substring(lastDot + 1, searchPattern.length() - 1);
			FileHierarchicalFolder parent = (FileHierarchicalFolder) getMailbox(parentName);
			if (parent != null) {
				File rootFile = parent.getPathToDir().toFile();
				for (File f : rootFile.listFiles()) {
					if (f.isDirectory()) {
						if (f.getName().startsWith(matchPattern)) {
							FileHierarchicalFolder mbox = ctx.getMailboxForPath(f.toPath());
							result.add(mbox);
							if (starIdx != -1) {
								addAllChildren(mbox, result);
							}
						}
					}
				}
			}
		} else {
			// Exact match needed
			MailFolder folder = this.getMailbox(searchPattern);
			if (folder != null) {
				result.add(folder);
			}
		}
		return result;
	}

	private void addAllChildren(FileHierarchicalFolder mailbox, Collection<MailFolder> addToThisList) {
		File rootFile = mailbox.getPathToDir().toFile();
		for (File f : rootFile.listFiles()) {
			if (f.isDirectory()) {
				// A directory must be a mailbox, add it to the list:
				FileHierarchicalFolder mbox = ctx.getMailboxForPath(f.toPath());
				addToThisList.add(mbox);
				addAllChildren(mbox, addToThisList);
			}
		}
	}

	public Collection<MailFolder> getChildren(MailFolder parent) {
		if (!(parent instanceof FileHierarchicalFolder)) {
			throw new UncheckedFileStoreException("Cannot create a MBoxFileStore mailbox from a parent which is not of type FileHierarchicalFolder");
		}
		List<MailFolder>result = new ArrayList<>();
		FileHierarchicalFolder parentCasted = (FileHierarchicalFolder)parent;
		File parentDir = parentCasted.getPathToDir().toFile();
		for (File kid : parentDir.listFiles()) {
			if (kid.isDirectory()) {
				// All directories inside mailbox folders are other mailboxes:
				result.add(ctx.getMailboxForPath(Paths.get(kid.getAbsolutePath())));
			}
		}
		return Collections.<MailFolder>unmodifiableCollection(result);
	}

	public MailFolder setSelectable(MailFolder folder, boolean selectable) {
		if (!(folder instanceof FileHierarchicalFolder)) {
			throw new UncheckedFileStoreException("Cannot set the selectable flag from a mailfolder of type: " + folder
					.getClass().toString());
		}
		FileHierarchicalFolder realFolder = (FileHierarchicalFolder)folder;
		realFolder.setSelectable(selectable);
		return realFolder;
	}

	public void deleteMailbox(MailFolder folder) throws FolderException {
		if (!(folder instanceof FileHierarchicalFolder)) {
			throw new UncheckedFileStoreException("Cannot delete a mailbox of type: " + folder.getClass().toString() + ". We "
					+ "can only delete mailboxes of type FileHierarchicalFolder.");
		}

		FileHierarchicalFolder toDelete = (FileHierarchicalFolder) folder;
		if (!toDelete.hasChildren()) {
			throw new FolderException("Cannot delete mailbox with children.");
		}
		if (toDelete.getMessageCount() != 0) {
			throw new FolderException("Cannot delete non-empty mailbox");
		}

		// OK, now delete mailbox:
		toDelete.prepareForDeletion();

		// OK, now, we should be able to delete the directory:
		try {
			Files.delete(toDelete.getPathToDir());
		}
		catch (IOException e) {
			throw new FolderException("IOException occurred while deleting mailbox folder '" + toDelete.getPathToDir()
					.toAbsolutePath() + "'.",	e);
		}
	}

	public void renameMailbox(MailFolder existingFolder, String newName) throws FolderException {
		// TODO: Implement it
		throw new UncheckedFileStoreException("The Store MBoxFileStore does not support renaming mailboxes.");
	}

	public Quota[] getQuota(String root, String qualifiedRootPrefix) {
		// TODO: Implement it
		throw new UncheckedFileStoreException("The Store MBoxFileStore does not support quotas.");
	}

	public void setQuota(Quota quota, String qualifiedRootPrefix) {
		// TODO: Implement it
		throw new UncheckedFileStoreException("The Store MBoxFileStore does not support quotas.");
	}

	public boolean isQuotaSupported() {
		// TODO: Implement it
		// In the first version of the filestore, quotas are not supported
		return false;
	}

	public void setQuotaSupported(boolean pQuotaSupported) {
		// TODO: Implement it
		if (pQuotaSupported) {
			throw new UncheckedFileStoreException("The Store MBoxFileStore does not support quotas.");
		}
	}

	/**
	 * Writes the whole userList to disk
	 *
	 * @param userList
	 */
	synchronized public void writeUserStore(Collection<GreenMailUser>userList) {
		log.info("Writing Greemail users to file: " + this.userListFile.toAbsolutePath().toString());

        try (BufferedWriter bw = Files.newBufferedWriter(userListFile, java.nio.charset.Charset.forName("UTF-8"), CREATE, WRITE,
                TRUNCATE_EXISTING)) {
            for (GreenMailUser user : userList) {
                bw.write(user.toSingleLine());
                bw.newLine();
            }
        }
        catch (IOException e) {
            throw new UncheckedFileStoreException(
                    "IOException happened while trying to write file with Greemail users " + this.userListFile, e);
        }
	}

	private void writePIDFile() {
		int processId = FileStoreUtil.getProcessId();
		log.info("Writing current process id: " + processId + " to '" + this.pidFile.toAbsolutePath().toString() + "'");
		try (BufferedWriter bw = Files.newBufferedWriter(pidFile, java.nio.charset.Charset.forName("UTF-8"), CREATE, WRITE,
				TRUNCATE_EXISTING)) {
			bw.write(Integer.toString(processId));
			bw.newLine();
		}
		catch (IOException e) {
			throw new UncheckedFileStoreException(
					"IOException happened while trying to write PID file for Greenmail: " + this.pidFile, e);
		}
	}

    private void deletePIDFile() {
        if (Files.isRegularFile(this.pidFile)) {
            try {
                Files.delete(this.pidFile);
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException("IOException happened while trying to delete PID file: " + this.pidFile, e);
            }
        }
    }


	synchronized public Collection<GreenMailUser> readUserStore(ImapHostManager imapHostManager) {
        ArrayList<GreenMailUser> result = new ArrayList<>();
        if (Files.isRegularFile(userListFile)) {
            log.info("Reading Greemail users from file: " + this.userListFile.toAbsolutePath().toString());
            try {
                List<String> allLines = readAllLines(this.userListFile, java.nio.charset.Charset.forName("UTF-8"));
                for (String line : allLines) {
                    log.info("  Adding user: " + line);
                    result.add(new UserImpl(line, imapHostManager));
                }
            }
            catch (IOException e) {
                throw new UncheckedFileStoreException(
                        "IOException happened while trying to read file with Greemail users " + this.userListFile, e);
            }
        }
        return result;
    }

}
