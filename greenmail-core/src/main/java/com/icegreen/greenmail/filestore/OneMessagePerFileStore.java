package com.icegreen.greenmail.filestore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.StoredMessage;

/**
 * Created by saladin on 11/2/16.
 */
public class OneMessagePerFileStore {

	private static final String GREENMAIL_HEADER_UID = "X-Greenmail-UID";
	private Path parentDir;



	public OneMessagePerFileStore(Path dir) {
		this.parentDir = dir;
	}

	public String addMessage(StoredMessage msg, int messageNumber) throws IOException, MessagingException {
		final Flags FLAGS_SEEN = new Flags(Flags.Flag.SEEN);
		msg.getMimeMessage().setFlags(FLAGS_SEEN, true);

		String uidStr = Long.toString(msg.getUid());
		String recDateStr = Long.toString(msg.getReceivedDate().getTime());


		String msgNumberStr = Integer.toString(messageNumber);

		String fileName = msgNumberStr + "_" + uidStr + "_" + recDateStr;
		msg.getMimeMessage().setHeader(GREENMAIL_HEADER_UID, uidStr);


		// MimeMessage = file Content
		try (OutputStream ostream = Files.newOutputStream(this.parentDir.resolve(fileName))) {
			msg.getMimeMessage().writeTo(ostream);
		}
		return fileName;
	}

	public StoredMessage retrieveMessage(String fileName, int flagBitSet) throws IOException, MessagingException {
		Session session = Session.getInstance(new Properties());
		MimeMessage mimeMsg = null;

		try (InputStream str = Files.newInputStream(this.parentDir.resolve(fileName))) {
			mimeMsg = new MimeMessage(session, str);
		}

		// First, make sure to delete all Flags
		mimeMsg.setFlags(MessageFlags.ALL_FLAGS, false);
		// And then set only the flags that we need
		mimeMsg.setFlags(FileStoreUtil.convertFlagBitSetToFlags(flagBitSet), true);


		StringTokenizer fileNameToken = new StringTokenizer(fileName, "_");
		if (fileNameToken.countTokens() != 3) {
			throw new UncheckedFileStoreException("The filename '" + fileName + "' does not match the expected naming "
					+ "convention which is msgnum_uid_daterecmillis whereas msgnum is int and  uid and daterecmillis are "
					+ "longs.");
		}

		String msgNumStr = fileNameToken.nextToken();
		String uidStr = fileNameToken.nextToken();
		String recDateStr = fileNameToken.nextToken();

		int msgNum;
		long recDateMillis;

		try {
			msgNum = Integer.parseInt(msgNumStr);
		} catch (NumberFormatException nfe) {
			throw new UncheckedFileStoreException("The filename '" + fileName + "' does not match the expected naming "
					+ "convention which is uid_daterecmillis whereas both uid and daterecmillis are longs. msgnum is not a "
					+ "number", nfe);
		}

		try {
			recDateMillis = Long.parseLong(recDateStr);
		} catch (NumberFormatException nfe) {
			throw new UncheckedFileStoreException("The filename '" + fileName + "' does not match the expected naming "
					+ "convention which is uid_daterecmillis whereas both uid and daterecmillis are longs. recdatemillis is not"
					+ " a number", nfe);
		}

		long uid = getUidForMessage(mimeMsg);

		// Set the sent date
		return new StoredMessage(mimeMsg, new Date(recDateMillis), uid);
	}

	public long getUidForMessage(Message mimeMsg) throws MessagingException {
		long uid = 0;
		String[] uidFromHeader = mimeMsg.getHeader(GREENMAIL_HEADER_UID);
		if (uidFromHeader != null && uidFromHeader.length > 0) {
			try {
				uid = Long.parseLong(uidFromHeader[0].trim());
			} catch (NumberFormatException nfe) {
				throw new UncheckedFileStoreException("The message does not contain a header of type " + GREENMAIL_HEADER_UID +
						"'. Cannot determine the UID.", nfe);
			}
		}
		return uid;
	}


}
