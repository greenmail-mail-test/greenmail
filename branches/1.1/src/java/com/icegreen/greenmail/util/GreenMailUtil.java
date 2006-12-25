/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail.util;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.Properties;
import java.util.Random;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006
 */
public class GreenMailUtil {
    /**
     * used internally for {@link #random()}
     */
    private static int generateCount = 0;
    private static final String generateSet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPRSTUVWXYZ23456789";
    private static final int generateSetSize = generateSet.length();

    private static GreenMailUtil instance = new GreenMailUtil();
    private GreenMailUtil() {
        //empty
    }

    public static GreenMailUtil instance() {
        return instance;
    }

    /**
     * Writes the content of an input stream to an output stream
     *
     * @throws IOException
     */
    public void copyStream(final InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = src.read(buffer)) > -1) {
            dest.write(buffer, 0, read);
        }
        dest.flush();
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from an input stream
     *
     * @throws MessagingException
     */
    public MimeMessage newMimeMessage(InputStream inputStream) throws MessagingException {
        return new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from a string
     *
     * @throws MessagingException
     */
    public MimeMessage newMimeMessage(String mailString) throws MessagingException {
        try {
            byte[] bytes = mailString.getBytes("US-ASCII");
            return newMimeMessage(new ByteArrayInputStream(bytes));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasNonTextAttachments(Part m) {
        try {
            Object content = m.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart) content;
                for (int i=0;i<mm.getCount();i++) {
                    BodyPart p = mm.getBodyPart(i);
                    if (hasNonTextAttachments(p)) {
                        return true;
                    }
                }
                return false;
            } else {
                return !m.getContentType().trim().toLowerCase().startsWith("text");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Returns the number of lines in any string
     */
    public int getLineCount(String str) {
        BufferedReader reader = new BufferedReader(new StringReader(str));
        int ret = 0;
        try {
            while (reader.readLine() != null) {
                ret++;
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The content of an email (or a Part)
     */
    public String getBody(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(i + 4, all.length());
    }

    /**
     * @return The headers of an email (or a Part)
     */
    public String getHeaders(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(0, i);
    }

    /**
     * @return The both header and body for an email (or a Part)
     */
    public String getWholeMessage(Part msg) {
        try {
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
            msg.writeTo(bodyOut);
            return bodyOut.toString("US-ASCII").trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBodyAsBytes(Part msg) {
        return getBody(msg).getBytes();
    }

    public byte[] getHeaderAsBytes(Part part) {
        return getHeaders(part).getBytes();
    }

    /**
     * @return same as {@link #getWholeMessage(javax.mail.Part)} }
     */
    public String toString(Part msg) {
        return getWholeMessage(msg);
    }


    /**
     * Generates a random generated password consisting of letters and digits
     * with a length variable between 5 and 8 characters long.
     * Passwords are further optimized for displays
     * that could potentially display the characters <i>1,l,I,0,O,Q</i> in a way
     * that a human could easily mix them up.
     *
     * @return
     */
    public String random() {
        Random r = new Random();
        int nbrOfLetters = r.nextInt(3) + 5;
        return random(nbrOfLetters);
    }

    public String random(int nbrOfLetters) {
        Random r = new Random();
        StringBuffer ret = new StringBuffer();
        for (/* empty */; nbrOfLetters > 0; nbrOfLetters--) {
            int pos = (r.nextInt(generateSetSize) + (++generateCount)) % generateSetSize;
            ret.append(generateSet.charAt(pos));
        }
        return ret.toString();
    }

    public void sendTextEmailTest(String to, String from, String subject, String msg) throws MessagingException {
        sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTP);
    }

    public void sendTextEmailSecureTest(String to, String from, String subject, String msg) throws MessagingException {
        sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTPS);
    }

    private void sendTextEmail(String to, String from, String subject, String msg, final ServerSetup setup) throws MessagingException {
        Session session = getSession(setup);

        Address[] tos = new InternetAddress[]{new InternetAddress(to)};
        Address[] froms = new InternetAddress[]{new InternetAddress(from)};
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setFrom(froms[0]);

        mimeMessage.setText(msg);
        Transport.send(mimeMessage, tos);
    }

    public static Session getSession(final ServerSetup setup) {
        Properties props = new Properties();
        props.put("mail.smtps.starttls.enable", Boolean.TRUE);
        if (setup.isSecure()) {
            props.setProperty("mail.smtp.socketFactory.class", DummySSLSocketFactory.class.getName());
        }
        props.setProperty("mail.transport.protocol", setup.getProtocol());
        props.setProperty("mail.smtp.port", String.valueOf(setup.getPort()));
        props.setProperty("mail.smtps.port", String.valueOf(setup.getPort()));
        Session session = Session.getInstance(props, null);
        return session;
    }

    public void sendAttachmentEmail(String to, String from, String subject, String msg, final byte[] attachment, final String contentType, final String filename, final String description, final ServerSetup setup) throws MessagingException, IOException {
        Session session = getSession(setup);

        Address[] tos = new InternetAddress[]{new InternetAddress(to)};
        Address[] froms = new InternetAddress[]{new InternetAddress(from)};
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setFrom(froms[0]);

        MimeMultipart multiPart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        multiPart.addBodyPart(textPart);
        textPart.setText(msg);

        MimeBodyPart binaryPart = new MimeBodyPart();
        multiPart.addBodyPart(binaryPart);

        DataSource ds = new DataSource() {
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(attachment);
            }

            public OutputStream getOutputStream() throws IOException {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                byteStream.write(attachment);
                return byteStream;
            }

            public String getContentType() {
                return contentType;
            }

            public String getName() {
                return filename;
            }
        };
        binaryPart.setDataHandler(new DataHandler(ds));
        binaryPart.setFileName(filename);
        binaryPart.setDescription(description);

        mimeMessage.setContent(multiPart);
        Transport.send(mimeMessage, tos);
    }
}
