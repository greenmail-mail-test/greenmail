package com.icegreen.greenmail.filestore.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.filestore.FileStoreUtil;
import com.icegreen.greenmail.filestore.MessageEntry;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageToFS implementation which stores each message in a single file (ending with *.eml).
 *
 * Works fine on modern OS, tested with up to 130'000 messages (files) in a single directory. The advantage is
 * that the end-user can then manually delete *.eml files on the FS, and then the messages are deleted as well.
 *
 */
public class MultipleElmFilesForMultipleMessages extends MessageToFS {
    private final Logger log = LoggerFactory.getLogger("filestore");

    public static final String FILE_ENDING = ".eml";
    public static final int FILE_ENDING_LEN = FILE_ENDING.length();

    private final Path mailboxDir;

    public MultipleElmFilesForMultipleMessages(Path dir) {
        this.mailboxDir = dir;
    }

    /**
     * Returns true when some entries from the synchedList no longer exist on the file system and have been deleted.
     *
     **/
    public boolean cleanupAfterLoading(List<MessageEntry> synchedList) {
        // We don't store the filename in the binary format (only the UID), make sure the populate the filename:
        for (File f : this.mailboxDir.toFile().listFiles()) {
            if (f.isFile()) {
                String fName = f.getName();
                if (fName.endsWith(FILE_ENDING)) {
                    String uidStr = fName.substring(0, fName.length() - FILE_ENDING_LEN);
                    try {
                        long uid = Long.parseLong(uidStr);

                        // Store the filename in the list:
                        for (MessageEntry e : synchedList) {
                            if (uid == e.getUid()) {
                                e.setShortFileName(f.getName());
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

        ArrayList<MessageEntry>toDeleteList = new ArrayList<>();
        for (MessageEntry e : synchedList) {
            if (e.getShortFileName() == null) {
                log.debug("cleanupAfterLoading: Delete entry with uid: " + e.getUid() + ", because file with ELM message does "
                        + "not exist.");
                toDeleteList.add(e);
            }
        }

        int numDeleted = 0;
        for (MessageEntry toDelete : toDeleteList) {
            synchedList.remove(toDelete);
            numDeleted++;
        }

        log.info("cleanupAfterLoading delete #" + numDeleted  +" entries because the files no longer existed on FS.");
        return (numDeleted != 0);
   }

    public void addMessage(StoredMessage msg, MessageEntry entryToUpdate) throws IOException, MessagingException {
        final Flags FLAGS_SEEN = new Flags(Flags.Flag.SEEN);
        msg.getMimeMessage().setFlags(FLAGS_SEEN, true);

        String uidStr = Long.toString(msg.getUid());
        String fileName = uidStr + FILE_ENDING;
        msg.getMimeMessage().setHeader(GREENMAIL_HEADER_UID, uidStr);

        // MimeMessage = file Content
        try (OutputStream ostream = Files.newOutputStream(this.mailboxDir.resolve(fileName))) {
            msg.getMimeMessage().writeTo(ostream);
        }

        entryToUpdate.setShortFileName(fileName);
    }

    public StoredMessage retrieveMessage(MessageEntry entry)
            throws IOException, MessagingException {

        Session session = Session.getInstance(new Properties());

        MimeMessage mimeMsg = null;
        try (InputStream str = Files.newInputStream(this.mailboxDir.resolve(entry.getShortFileName()))) {
            mimeMsg = new MimeMessage(session, str);
        }

        // First, make sure to delete all Flags
        mimeMsg.setFlags(MessageFlags.ALL_FLAGS, false);
        // And then set only the flags that we need
        mimeMsg.setFlags(FileStoreUtil.convertFlagBitSetToFlags(entry.getFlagBitSet()), true);

        // Set the sent date
        return new StoredMessage(mimeMsg, new Date(entry.getRecDateMillis()), entry.getUid());
    }


}
