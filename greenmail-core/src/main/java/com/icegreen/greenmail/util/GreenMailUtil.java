/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.icegreen.greenmail.user.GreenMailUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006
 */
public class GreenMailUtil {
    private static final Logger log = LoggerFactory.getLogger(GreenMailUtil.class);
    /**
     * used internally for {@link #random()}
     */
    private static int generateCount = 0;
    private static final String GENERATE_SET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPRSTUVWXYZ23456789";
    private static final int GENERATE_SET_SIZE = GENERATE_SET.length();

    private static GreenMailUtil instance = new GreenMailUtil();

    private GreenMailUtil() {
        //empty
    }

    /**
     * @deprecated As of 1.5 and to be removed in 1.6. No need to instantiate static helper class
     */
    @Deprecated
    public static GreenMailUtil instance() {
        return instance;
    }

    /**
     * Writes the content of an input stream to an output stream
     *
     * @throws IOException
     */
    public static void copyStream(final InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = src.read(buffer)) > -1) {
            dest.write(buffer, 0, read);
        }
        dest.flush();
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from an input stream.
     *
     * @return the created mime message.
     */
    public static MimeMessage newMimeMessage(InputStream inputStream) {
        try {
            return new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Can not generate mime message for input stream " + inputStream, e);
        }
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from a string
     *
     * @throws MessagingException
     */
    public static MimeMessage newMimeMessage(String mailString) throws MessagingException {
        return newMimeMessage(EncodingUtil.toStream(mailString, EncodingUtil.CHARSET_EIGHT_BIT_ENCODING));
    }

    public static boolean hasNonTextAttachments(Part m) {
        try {
            Object content = m.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart) content;
                for (int i = 0; i < mm.getCount(); i++) {
                    BodyPart p = mm.getBodyPart(i);
                    if (hasNonTextAttachments(p)) {
                        return true;
                    }
                }
                return false;
            } else {
                return !m.getContentType().trim().toLowerCase().startsWith("text");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Counts the number of lines.
     *
     * @param str the input string
     * @return Returns the number of lines terminated by '\n' in string
     */
    public static int getLineCount(String str) {
        if (null == str || str.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (char c : str.toCharArray()) {
            if ('\n' == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return The content of an email (or a Part)
     */
    public static String getBody(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return i < 0 ? "" /* empty body */ : all.substring(i + 4, all.length());
    }

    /**
     * @return The headers of an email (or a Part)
     */
    public static String getHeaders(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return i < 0 ? all : all.substring(0, i);
    }

    /**
     * @return The both header and body for an email (or a Part)
     */
    public static String getWholeMessage(Part msg) {
        try {
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
            msg.writeTo(bodyOut);
            return bodyOut.toString(EncodingUtil.EIGHT_BIT_ENCODING).trim();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] getBodyAsBytes(Part msg) {
        return getBody(msg).getBytes(EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);
    }

    public static byte[] getHeaderAsBytes(Part part) {
        return getHeaders(part).getBytes(EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);
    }

    /**
     * @return same as {@link #getWholeMessage(javax.mail.Part)} }
     */
    public static String toString(Part msg) {
        return getWholeMessage(msg);
    }


    /**
     * Generates a random generated password consisting of letters and digits
     * with a length variable between 5 and 8 characters long.
     * Passwords are further optimized for displays
     * that could potentially display the characters <i>1,l,I,0,O,Q</i> in a way
     * that a human could easily mix them up.
     *
     * @return the random string.
     */
    public static String random() {
        Random r = new Random();
        int nbrOfLetters = r.nextInt(3) + 5;
        return random(nbrOfLetters);
    }

    public static String random(int nbrOfLetters) {
        Random r = new Random();
        StringBuilder ret = new StringBuilder();
        for (/* empty */; nbrOfLetters > 0; nbrOfLetters--) {
            int pos = (r.nextInt(GENERATE_SET_SIZE) + (++generateCount)) % GENERATE_SET_SIZE;
            ret.append(GENERATE_SET.charAt(pos));
        }
        return ret.toString();
    }

    /**
     * Sends a text message using the default test setup for SMTP.
     *
     * @param to      the to address.
     * @param from    the from address.
     * @param subject the subject.
     * @param msg     the text message.
     * @see ServerSetupTest#SMTP
     */
    public static void sendTextEmailTest(String to, String from, String subject, String msg) {
        sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTP);
    }

    /**
     * Sends a text message using the default test setup for SMTPS.
     *
     * @param to      the to address.
     * @param from    the from address.
     * @param subject the subject.
     * @param msg     the text message.
     * @see ServerSetupTest#SMTPS
     */
    public static void sendTextEmailSecureTest(String to, String from, String subject, String msg) {
        sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTPS);
    }

    public static String getAddressList(Address[] addresses) {
        if (null == addresses) {
            return null;
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(addresses[i].toString());
        }
        return ret.toString();
    }

    public static MimeMessage createTextEmail(String to, String from, String subject, String msg, final ServerSetup setup) {
        try {
            Session session = getSession(setup);

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(subject);
            mimeMessage.setSentDate(new Date());
            mimeMessage.setFrom(from);
            mimeMessage.setRecipients(Message.RecipientType.TO, to);

            mimeMessage.setText(msg);
            return mimeMessage;
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Can not generate message", e);
        }
    }

    /**
     * Sends a text message using given server setup for SMTP.
     *
     * @param to      the to address.
     * @param from    the from address.
     * @param subject the subject.
     * @param msg     the test message.
     * @param setup   the SMTP setup.
     */
    public static void sendTextEmail(String to, String from, String subject, String msg, final ServerSetup setup) {
        sendMimeMessage(createTextEmail(to, from, subject, msg, setup));
    }

    /**
     * Send the message using the JavaMail session defined in the message
     *
     * @param mimeMessage Message to send
     */
    public static void sendMimeMessage(MimeMessage mimeMessage) {
        try {
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not send message " + mimeMessage, e);
        }
    }

    /**
     * Send the message with the given attributes and the given body using the specified SMTP settings
     *
     * @param to          Destination address(es)
     * @param from        Sender address
     * @param subject     Message subject
     * @param body        Message content. May either be a MimeMultipart or another body that java mail recognizes
     * @param contentType MIME content type of body
     * @param serverSetup Server settings to use for connecting to the SMTP server
     */
    public static void sendMessageBody(String to, String from, String subject, Object body, String contentType, ServerSetup serverSetup) {
        try {
            Session smtpSession = getSession(serverSetup);
            MimeMessage mimeMessage = new MimeMessage(smtpSession);

            mimeMessage.setRecipients(Message.RecipientType.TO, to);
            mimeMessage.setFrom(from);
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(body, contentType);
            sendMimeMessage(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Can not send message", e);
        }
    }

    public static void sendAttachmentEmail(String to, String from,
                                           String subject, String msg, final byte[] attachment,
                                           final String contentType, final String filename,
                                           final String description, final ServerSetup setup) {
        MimeMultipart multiPart = createMultipartWithAttachment(msg, attachment, contentType, filename, description);
        sendMessageBody(to, from, subject, multiPart, null, setup);
    }

    /**
     * Create new multipart with a text part and an attachment
     *
     * @param msg         Message text
     * @param attachment  Attachment data
     * @param contentType MIME content type of body
     * @param filename    File name of the attachment
     * @param description Description of the attachment
     * @return New multipart
     */
    public static MimeMultipart createMultipartWithAttachment(String msg, final byte[] attachment, final String contentType,
                                                               final String filename, String description) {
        try {
            MimeMultipart multiPart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            multiPart.addBodyPart(textPart);
            textPart.setText(msg);

            MimeBodyPart binaryPart = new MimeBodyPart();
            multiPart.addBodyPart(binaryPart);

            DataSource ds = new DataSource() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(attachment);
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byteStream.write(attachment);
                    return byteStream;
                }

                @Override
                public String getContentType() {
                    return contentType;
                }

                @Override
                public String getName() {
                    return filename;
                }
            };
            binaryPart.setDataHandler(new DataHandler(ds));
            binaryPart.setFileName(filename);
            binaryPart.setDescription(description);
            return multiPart;
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Can not create multipart message with attachment", e);
        }
    }

    public static Session getSession(final ServerSetup setup) {
        return getSession(setup, null);
    }

    /**
     * Gets a JavaMail Session for given server type such as IMAP and additional props for JavaMail.
     *
     * @param setup     the setup type, such as <code>ServerSetup.IMAP</code>
     * @param mailProps additional mail properties.
     * @return the JavaMail session.
     */
    public static Session getSession(final ServerSetup setup, Properties mailProps) {
        Properties props = setup.configureJavaMailSessionProperties(mailProps, false);

        log.debug("Mail session properties are {}", props);

        return Session.getInstance(props, null);
    }

    /**
     * Sets a quota for a users.
     *
     * @param user  the user.
     * @param quota the quota.
     */
    public static void setQuota(final GreenMailUser user, final Quota quota) {
        Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP);
        try {
            Store store = session.getStore("imap");
            store.connect(user.getEmail(), user.getPassword());
            try {
                ((QuotaAwareStore) store).setQuota(quota);
            } finally {
                store.close();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Can not set quota " + quota
                    + " for user " + user, ex);
        }
    }

    /**
     * Gets the quotas for the user.
     *
     * @param user      the user.
     * @param quotaRoot the quota root, eg 'INBOX.
     * @return array of current quotas, or an empty array if not set.
     */
    public static Quota[] getQuota(final GreenMailUser user, final String quotaRoot) {
        Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP);
        try {
            Store store = session.getStore("imap");
            store.connect(user.getEmail(), user.getPassword());
            try {
                return ((QuotaAwareStore) store).getQuota(quotaRoot);
            } finally {
                store.close();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Can not get quota for quota root "
                    + quotaRoot + " for user " + user, ex);
        }
    }
}
