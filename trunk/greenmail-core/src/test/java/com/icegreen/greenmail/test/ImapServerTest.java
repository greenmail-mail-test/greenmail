/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.*;
import junit.framework.TestCase;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.HeaderTerm;
import javax.mail.search.SearchTerm;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapServerTest extends TestCase {

    GreenMail greenMail;

    protected void tearDown() throws Exception {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testSearch() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        GreenMailUser user = greenMail.setUser("test@localhost","pwd");
        assertNotNull(greenMail.getImap());
        greenMail.start();


        Properties p = new Properties();
//            p.setProperty("mail.host","localhost");
//            p.setProperty("mail.debug","true");
        Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP, p);
        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");

        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("testSearch");
        message1.setText("content");
        message1.setRecipients(Message.RecipientType.TO, new Address[]{
                new InternetAddress("test@localhost")
        });
        message1.setFlag(Flags.Flag.ANSWERED, true);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("testSearch");
        message2.setText("content");
        message2.setRecipients(Message.RecipientType.TO, new Address[]{
                new InternetAddress("test@localhost")
        });
        message2.setFlag(Flags.Flag.ANSWERED,false);
        folder.store(message2);

        List<SimpleStoredMessage> gMsgs = folder.getMessages();
        for (SimpleStoredMessage gMsg : gMsgs) {
            MimeMessage mm = gMsg.getMimeMessage();
            for (Flags.Flag f : mm.getFlags().getSystemFlags()) {
                gMsg.getFlags().add(f);
            }
            mm.saveChanges();
        }

        greenMail.waitForIncomingEmail(2);

        Store store = session.getStore("imap");
        store.connect("test@localhost", "pwd");
        Folder imapFolder = store.getFolder("INBOX");
        imapFolder.open(Folder.READ_WRITE);

        Message[] imapMessages = imapFolder.getMessages();
        assertTrue(null != imapMessages && imapMessages.length == 2);
        Message m0 = imapMessages[0];
        Message m1 = imapMessages[1];
        assertTrue(m0.getFlags().contains(Flags.Flag.ANSWERED));

        // Search flags
        imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED),true));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m0);

        // Search header ids
        String id = m0.getHeader("Message-ID")[0];
        imapMessages = imapFolder.search(new HeaderTerm("Message-ID",id));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m0);

        id = m1.getHeader("Message-ID")[0];
        imapMessages = imapFolder.search(new HeaderTerm("Message-ID",id));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m1);
    }

    public void testRetreiveSimple() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        final String to = "test@localhost";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testImapsReceive() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTPS_IMAPS);
        assertNull(greenMail.getImap());
        assertNotNull(greenMail.getImaps());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImaps());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testRetreiveSimpleWithNonDefaultPassword() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
        boolean login_failed = false;
        try {
            retriever.getMessages(to, "wrongpassword");
        } catch (Throwable e) {
            login_failed = true;
        }
        assertTrue(login_failed);

        Message[] messages = retriever.getMessages(to, password);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testRetriveMultipart() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        greenMail.start();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
        Message[] messages = retriever.getMessages(to);

        Object o = messages[0].getContent();
        assertTrue(o instanceof MimeMultipart);
        MimeMultipart mp = (MimeMultipart) o;
        assertEquals(2, mp.getCount());
        BodyPart bp;
        bp = mp.getBodyPart(0);
        assertEquals(body, GreenMailUtil.getBody(bp).trim());

        bp = mp.getBodyPart(1);
        assertEquals("AAEC", GreenMailUtil.getBody(bp).trim());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GreenMailUtil.copyStream(bp.getInputStream(), bout);
        byte[] gif = bout.toByteArray();
        for (int i = 0; i < gif.length; i++) {
            assertEquals(i, gif[i]);
        }
        retriever.logout();
    }
}
