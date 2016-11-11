package com.icegreen.greenmail.filestore.fs;

import java.io.IOException;
import java.util.List;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.icegreen.greenmail.filestore.MessageEntry;
import com.icegreen.greenmail.filestore.UncheckedFileStoreException;
import com.icegreen.greenmail.store.StoredMessage;

/**
 * Abstract class to really store/retrieve mail message to the FS.
 */
public abstract class MessageToFS {
    public static final String GREENMAIL_HEADER_UID = "X-Greenmail-UID";

    /**
     * Is invoked after loading the binary file with MailboxEntries. The List<MessageEntry> is still locked,
     * so this method is the only method which is able to cleanup (e.g. remove message which are no longer
     * on the FS, compact folders etc...)
     *
     * @param synchedList - The list with all MessageEntries, read in from the binary file
     * @return true when you have done cleanup (e.g. changed the MessageEntries) or false otherwise
     */
    public abstract boolean cleanupAfterLoading(List<MessageEntry> synchedList);

    /**
     * Abstract method to add a StoredMessage to the file system.
     *
     * @param msg - The message
     * @param entryToUpdate - The corresponding entry
     * @throws IOException
     * @throws MessagingException
     */
    public abstract void addMessage(StoredMessage msg, MessageEntry entryToUpdate) throws IOException, MessagingException;

    /**
     * Abstract method to retrieve a StoredMessage from the file system.
     *
     * @param entry
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public abstract StoredMessage retrieveMessage(MessageEntry entry) throws IOException, MessagingException;

    public long getUidForMessageFromHeader(Message mimeMsg) throws MessagingException {
        long uid = 0;
        String[] uidFromHeader = mimeMsg.getHeader(GREENMAIL_HEADER_UID);
        if (uidFromHeader != null && uidFromHeader.length > 0) {
            try {
                uid = Long.parseLong(uidFromHeader[0].trim());
            }
            catch (NumberFormatException nfe) {
                throw new UncheckedFileStoreException("The message does not contain a header of type " + GREENMAIL_HEADER_UID +
                        "'. Cannot determine the UID.", nfe);
            }
        }
        return uid;
    }

}