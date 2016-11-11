package com.icegreen.greenmail.filestore.fs;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.filestore.FileStoreUtil;
import com.icegreen.greenmail.filestore.MessageEntry;
import com.icegreen.greenmail.filestore.UncheckedFileStoreException;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ***********************************************************************************************************
 * *** This class is incomplete, please do not use it without modifying it first *****************************
 * ***********************************************************************************************************
 *
 * This class is currently not in use, because decision was made to use the MultipleElmFilesForMultipleMessages
 * for Greenmail. The main advantages MultipleElmFilesForMultipleMessages:
 *
 * a) On modern OS the same performance as a single MBox file.
 * b) Single MBOX file handling is more complex and therefore error-prone (see this class).
 * c) Deletion with MBOX file handling is more complex (e.g. compacting the MBOX file from time to time, not yet implemented)
 * d) Single file per message provides simple interface for end-user to interfere. E.g. delete all old mails is quite simple:
 * Just delete all the old .elm files on the file system (e.g. using a Unix cronjob with find ... -exec rm -rf {} \;
 *
 */
public class SingleMboxFileForMultipleMessages extends MessageToFS {
    private final Logger log = LoggerFactory.getLogger("filestore");

    private final Path mboxFile;

    public SingleMboxFileForMultipleMessages(Path dir) {
        mboxFile = dir.resolve("messages.mbox");
    }

    public boolean cleanupAfterLoading(List<MessageEntry> synchedList) {
        // TODO: Do nothing, Compacting and deleting is not supported yet with MBox file.
        return false;
    }

    /**
     * Adds a message to the MBOX file, create the MBOX file if it does not exist, and updates the message entry for this
     * file (position and length)
     *
     * @param msg - The StoreMessage to add
     * @param entryToUpdate - The MessageEntry to update
     *
     * @throws IOException
     * @throws MessagingException
     */
    public void addMessage(StoredMessage msg, MessageEntry entryToUpdate) throws IOException, MessagingException {
        log.debug("Entering addMessageToMBoxFileAndUpdateEntry");
        String fromString = createRfc2822ConformantFrom(msg.getMimeMessage().getFrom());
        String dateString = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy").format(msg.getReceivedDate());

        ByteBuffer prefix = createFromLine(fromString, dateString);
        ByteBuffer msgBuffer = createByteBufferFromMessage(msg);
        ByteBuffer suffix = createEmptyLine();

        int numWritten = 0;
        long newPosition = 0L;

        try (FileChannel fc = (FileChannel.open(mboxFile, CREATE, WRITE, APPEND))) {
            // Retrieve current position with option: APPEND
            newPosition = fc.position();

            // Make sure that we know how many bytes we have written
            numWritten += fc.write(prefix);
            numWritten += fc.write(msgBuffer);
            numWritten += fc.write(suffix);
        }
        entryToUpdate.setPositionInMboxFile (newPosition);
        entryToUpdate.setLenInMboxFile(numWritten);

        log.debug("Leaving addMessageToMBoxFileAndUpdateEntry with position/len: " + newPosition + "/" + numWritten);
    }

    public StoredMessage retrieveMessage(MessageEntry entry) throws IOException, MessagingException {

        ByteBuffer wholeBuf = ByteBuffer.allocate(entry.getLenInMboxFile());
        try (FileChannel fc = (FileChannel.open(mboxFile, READ))) {
            fc.position(entry.getPositionInMboxFile());
            fc.read(wholeBuf);
        }

        // Search the first 0xa, which is the end of the first From line:
        wholeBuf.rewind();
        while (wholeBuf.hasRemaining()) {
            if ((byte)0xa == wholeBuf.get()) {
                break;
            }
        }
        if (!wholeBuf.hasRemaining()) {
            throw new UncheckedFileStoreException("The MBOX file does not contain a valid From line (see 4155, Appendix A)");
        }

        log.debug("Buffer calculation: " + wholeBuf.position());
        // Remove the last empty line (-1)
        byte[] messageBuffer = new byte[entry.getLenInMboxFile() - wholeBuf.position() - 1];
        wholeBuf.get(messageBuffer);

        Session session = Session.getInstance(new Properties());

        MimeMessage mimeMsg = null;
        try (InputStream str = new ByteArrayInputStream(messageBuffer)) {
            mimeMsg = new MimeMessage(session, str);
        }

        // First, make sure to delete all Flags
        mimeMsg.setFlags(MessageFlags.ALL_FLAGS, false);
        // And then set only the flags that we need
        mimeMsg.setFlags(FileStoreUtil.convertFlagBitSetToFlags(entry.getFlagBitSet()), true);

        // Set the sent date
        return new StoredMessage(mimeMsg, new Date(entry.getRecDateMillis()), entry.getUid());
    }

    /**
     * Retrieves the most likely From address from the list of addresses.
     */
    private String createRfc2822ConformantFrom(Address[] from) {
        if (from == null) {
            // The minus-sign is used by Thunderbird, so this might be a good default if nothing better is available
            return "-";
        }
        if (from.length == 0) {
            // The minus-sign is used by Thunderbird, so this might be a good default if nothing better is available
            return "-";
        }

        Address useAddress = null;
        for (Address a : from) {
            if ("rfc822".equalsIgnoreCase(a.getType())) {
                useAddress = a;
                break;
            }
        }
        if (useAddress == null) {
            // Just use the first address when we have no address of type "rfc822"
            useAddress = from[0];
        }
        return useAddress.toString();
    }

    private ByteBuffer createEmptyLine() {
        StringBuilder b = new StringBuilder();
        b.append((char)0xa);
        byte buf[] = b.toString().getBytes(Charset.forName("ISO-8859-1"));
        return createByteBuffer(buf);
    }

    /**
     * Creates a fromLine which conforms to the MBOX format as defined in Appendix A in RFC 4155.
     *
     * @param from
     * @param recDateAsString
     * @return
     */
    private ByteBuffer createFromLine(String from, String recDateAsString) {
        StringBuilder b = new StringBuilder();
        b.append("From");
        b.append(" ");
        b.append(from);
        b.append(" ");
        b.append(recDateAsString);
        b.append((char)0xa);
        byte buf[] = b.toString().getBytes(Charset.forName("ISO-8859-1"));

        return createByteBuffer(buf);
    }

    private ByteBuffer createByteBufferFromMessage(StoredMessage msg) throws IOException, MessagingException {
        byte buf[];
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            msg.getMimeMessage().writeTo(bos);
            buf = bos.toByteArray();
        }
        if (buf == null) {
            throw new UncheckedFileStoreException("Cannot write message to byte buffer, buffer is null.");
        }
        return createByteBuffer(buf);
    }

    private ByteBuffer createByteBuffer(byte[] buf) {
        ByteBuffer bbuf = ByteBuffer.allocate(buf.length);
        bbuf.put(buf);
        bbuf.rewind();
        return bbuf;
    }

}
