/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail.test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.HeaderTerm;

import static javax.mail.Flags.Flag.DELETED;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import junit.framework.TestCase;

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
        GreenMailUser user = greenMail.setUser("test@localhost", "pwd");
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
        Flags fooFlags = new Flags();
        fooFlags.add("foo");
        message1.setFlags(fooFlags,true);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("testSearch");
        message2.setText("content");
        message2.setRecipients(Message.RecipientType.TO, new Address[]{
                new InternetAddress("test@localhost")
        });
        message2.setFlag(Flags.Flag.ANSWERED, false);
        folder.store(message2);

        List<StoredMessage> gMsgs = folder.getMessages();
        for (StoredMessage gMsg : gMsgs) {
            MimeMessage mm = gMsg.getMimeMessage();
            for (Flags.Flag f : mm.getFlags().getSystemFlags()) {
                gMsg.getFlags().add(f);
            }
            for (String uf : mm.getFlags().getUserFlags()) {
                gMsg.getFlags().add(uf);
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
        imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m0);

        imapMessages = imapFolder.search(new FlagTerm(fooFlags, true));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0].getFlags().contains("foo"));

        imapMessages = imapFolder.search(new FlagTerm(fooFlags, false));
        assertTrue(imapMessages.length == 1);
        assertTrue(!imapMessages[0].getFlags().contains(fooFlags));

        // Search header ids
        String id = m0.getHeader("Message-ID")[0];
        imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m0);

        id = m1.getHeader("Message-ID")[0];
        imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
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

    public void testQuota() throws Exception {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.start();
        try {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message", ServerSetupTest.SMTP);
            greenMail.waitForIncomingEmail(1);

            Properties p = new Properties();
//            p.setProperty("mail.debug","true");
            Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP, p);
            IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Message[] msgs = folder.getMessages();
            assertTrue(null != msgs && msgs.length == 1);

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);

            Quota[] quotas = store.getQuota("INBOX");
            assertNotNull(quotas);
            assertTrue(quotas.length == 1);
            assertNotNull(quotas[0].resources);
            assertTrue(quotas[0].resources.length == 2);
            assertEquals(testQuota.quotaRoot, quotas[0].quotaRoot);
            assertEquals(quotas[0].resources[0].limit, testQuota.resources[0].limit);
            assertEquals(quotas[0].resources[1].limit, testQuota.resources[1].limit);
            assertEquals(quotas[0].resources[1].usage, 1);
//            assertEquals(quotas[0].resources[0].usage, m.getSize());

            quotas = store.getQuota("");
            assertNotNull(quotas);
            assertTrue(quotas.length == 0);
            // TODO: Quota on ""
        }
        finally {
            greenMail.stop();
        }
    }

    public void testQuotaCapability() throws MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.start();
        greenMail.setQuotaSupported(false);
        try {
            Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP);
            IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);
            fail("Excepted MessageException since quota capability is turned of");
        } catch (MessagingException ex) {
            assertEquals(ex.getMessage(), "QUOTA not supported");
        }
        finally {
            greenMail.stop();
        }
    }

    public void testSetGetFlags() throws MessagingException, InterruptedException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.start();
        try {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message", ServerSetupTest.SMTP);
            greenMail.waitForIncomingEmail(1);

            Properties p = new Properties();
//            p.setProperty("mail.debug","true");
            Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP, p);
            IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");

            // Set some flags
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            try {
                Message[] msgs = folder.getMessages();
                assertTrue(null != msgs && msgs.length == 1);

                Message m = msgs[0];

                Flags f = m.getFlags();
                assertFalse(f.contains(Flags.Flag.DRAFT));
                assertFalse(f.contains("foobar"));
                m.setFlag(Flags.Flag.DRAFT, true);
                final Flags foobar = new Flags("foobar");
                m.setFlags(foobar, true);
                assertTrue(m.getFlags().contains(Flags.Flag.DRAFT));
                assertTrue(m.getFlags().contains("foobar"));
            }
            finally {
                folder.close(true);
            }


            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            try {
                Message[] msgs = folder.getMessages();
                assertTrue(null != msgs && msgs.length == 1);
                Message m = msgs[0];
                Flags f = m.getFlags();
                assertTrue(f.contains(Flags.Flag.DRAFT));
                assertTrue(f.contains("foobar"));
            }
            finally {
                folder.close(true);
            }

        }
        finally {
            greenMail.stop();
        }
    }

    public void testNestedFolders() throws MessagingException, InterruptedException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.start();
        try {
            Properties p = new Properties();
//            p.setProperty("mail.debug","true");
            Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP, p);
            IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");

            // Create some folders
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            IMAPFolder newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertTrue(!newFolder.exists());

            assertTrue(newFolder.create(Folder.HOLDS_FOLDERS|Folder.HOLDS_MESSAGES));

            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertTrue(newFolder.exists());
        }
        finally {
            greenMail.stop();
        }
    }

    public void testRenameFolder() throws MessagingException, InterruptedException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.start();
        try {
            Properties p = new Properties();
//            p.setProperty("mail.debug","true");
            Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP, p);
            IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");

            // Create some folders
            Folder inboxFolder = store.getFolder("INBOX");
            Folder newFolder = inboxFolder.getFolder("foo-folder");
            assertTrue(newFolder.create(Folder.HOLDS_FOLDERS|Folder.HOLDS_MESSAGES));
            assertTrue(newFolder.exists());

            Folder renamedFolder = inboxFolder.getFolder("foo-folder-renamed");
            assertTrue(!renamedFolder.exists());

            // Rename
            assertTrue(newFolder.renameTo(renamedFolder));
            assertTrue(!newFolder.exists());
            assertTrue(renamedFolder.exists());

            // Rename with sub folder
            Folder subFolder = renamedFolder.getFolder("bar");
            assertTrue(subFolder.create(Folder.HOLDS_FOLDERS|Folder.HOLDS_MESSAGES));
            assertTrue(subFolder.exists());

            Folder renamedFolder2 = inboxFolder.getFolder("foo-folder-renamed-again");
            assertTrue(renamedFolder.renameTo(renamedFolder2));
            assertTrue(!renamedFolder.exists());
            assertTrue(renamedFolder2.exists());
            assertTrue(renamedFolder2.getFolder("bar").exists()); // check that sub folder still exists

            // Rename to a different parent folder
            // INBOX.foo-folder-renamed-again -> INBOX.foo2.foo3
            Folder foo2Folder = inboxFolder.getFolder("foo2");
            assertTrue(foo2Folder.create(Folder.HOLDS_FOLDERS|Folder.HOLDS_MESSAGES));
            assertTrue(foo2Folder.exists());
            Folder foo3Folder = foo2Folder.getFolder("foo3");
            assertTrue(!foo3Folder.exists());

            renamedFolder2.renameTo(foo3Folder);
            assertTrue(inboxFolder.getFolder("foo2.foo3").exists());
            assertTrue(!inboxFolder.getFolder("foo-folder-renamed-again").exists());
        }
        finally {
            greenMail.stop();
        }
    }

    public void testExpunge() throws MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
                greenMail.setUser("foo@localhost", "pwd");
                greenMail.start();
        try {
            for(int i=0;i<6;i++) {
                GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject #"+i,
                        "Test message", ServerSetupTest.SMTP);
            }
            final Session session = GreenMailUtil.getSession(ServerSetupTest.IMAP);
            final IMAPStore store = (IMAPStore) session.getStore("imap");
            store.connect("foo@localhost", "pwd");
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertEquals(6, messages.length);
            inboxFolder.setFlags(new int[]{2, 3}, new Flags(DELETED), true); // 1 and 2, offset is not zero-based
//            for(int i=0;i<messages.length;i++) {
//                System.out.println(messages[i].getSubject() + " : "+messages[i].isSet(DELETED));
//            }

            assertEquals(false, inboxFolder.getMessage(1).isSet(DELETED));
            assertEquals(true, inboxFolder.getMessage(2).isSet(DELETED));
            assertEquals(true, inboxFolder.getMessage(3).isSet(DELETED));
            assertEquals(false, inboxFolder.getMessage(4).isSet(DELETED));
            assertEquals(false, inboxFolder.getMessage(5).isSet(DELETED));
            assertEquals(false, inboxFolder.getMessage(6).isSet(DELETED));
            assertEquals(2, inboxFolder.getDeletedMessageCount());
            Message[] expunged = inboxFolder.expunge();
            assertEquals(2, expunged.length);

            messages = inboxFolder.getMessages();
            assertEquals(4, messages.length);
            assertEquals("Test subject #0", messages[0].getSubject());
            assertEquals("Test subject #3", messages[1].getSubject());
            assertEquals("Test subject #4", messages[2].getSubject());
            assertEquals("Test subject #5", messages[3].getSubject());
        }  finally {
            greenMail.stop();
        }
    }
}
