/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;

import static javax.mail.Flags.Flag.DELETED;
import static org.junit.Assert.*;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapServerTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    @Test
    public void testRetreiveSimple() throws Exception {
        assertNotNull(greenMail.getImap());
        final String subject = GreenMailUtil.random() + " öäü";
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

    @Test
    public void testImapsReceive() throws Throwable {
        assertNotNull(greenMail.getImaps());
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

    @Test
    public void testRetreiveSimpleWithNonDefaultPassword() throws Exception {
        assertNotNull(greenMail.getImap());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
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

    @Test
    public void testRetriveMultipart() throws Exception {
        assertNotNull(greenMail.getImap());

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

    @Test
    public void testQuota() throws Exception {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(1);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Message[] msgs = folder.getMessages();
            assertTrue(null != msgs && msgs.length == 1);

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            final QuotaAwareStore quotaAwareStore = (QuotaAwareStore) store;
            quotaAwareStore.setQuota(testQuota);

            Quota[] quotas = quotaAwareStore.getQuota("INBOX");
            assertNotNull(quotas);
            assertTrue(quotas.length == 1);
            assertNotNull(quotas[0].resources);
            assertTrue(quotas[0].resources.length == 2);
            assertEquals(testQuota.quotaRoot, quotas[0].quotaRoot);
            assertEquals(quotas[0].resources[0].limit, testQuota.resources[0].limit);
            assertEquals(quotas[0].resources[1].limit, testQuota.resources[1].limit);
            assertEquals(quotas[0].resources[1].usage, 1);
//            assertEquals(quotas[0].resources[0].usage, m.getSize());

            quotas = quotaAwareStore.getQuota("");
            assertNotNull(quotas);
            assertTrue(quotas.length == 0);
            // TODO: Quota on ""
        } finally {
            store.close();
        }
    }

    @Test
    public void testQuotaCapability() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.setQuotaSupported(false);
        final IMAPStore store = (IMAPStore) greenMail.getImap().createStore();
        try {
            store.connect("foo@localhost", "pwd");

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);
            fail("Excepted MessageException since quota capability is turned off");
        } catch (MessagingException ex) {
            assertEquals(ex.getMessage(), "QUOTA not supported");
        } finally {
            store.close();
        }
    }

    @Test
    public void testSetGetFlags() throws MessagingException, InterruptedException {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(1);

        final IMAPStore store = (IMAPStore) greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

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
            } finally {
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
            } finally {
                folder.close(true);
            }
        } finally {
            store.close();
        }
    }

    @Test
    public void testNestedFolders() throws MessagingException, InterruptedException {
        greenMail.setUser("foo@localhost", "pwd");
        final IMAPStore store = (IMAPStore) greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            IMAPFolder newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertTrue(!newFolder.exists());

            assertTrue(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES));

            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertTrue(newFolder.exists());
        } finally {
            store.close();
        }
    }

    @Test
    public void testRenameFolder() throws MessagingException, InterruptedException {
        greenMail.setUser("foo@localhost", "pwd");

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            Folder inboxFolder = store.getFolder("INBOX");
            Folder newFolder = inboxFolder.getFolder("foo-folder");
            assertTrue(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES));
            assertTrue(newFolder.exists());

            Folder renamedFolder = inboxFolder.getFolder("foo-folder-renamed");
            assertTrue(!renamedFolder.exists());

            // Rename
            assertTrue(newFolder.renameTo(renamedFolder));
            assertTrue(!newFolder.exists());
            assertTrue(renamedFolder.exists());

            // Rename with sub folder
            Folder subFolder = renamedFolder.getFolder("bar");
            assertTrue(subFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES));
            assertTrue(subFolder.exists());

            Folder renamedFolder2 = inboxFolder.getFolder("foo-folder-renamed-again");
            assertTrue(renamedFolder.renameTo(renamedFolder2));
            assertTrue(!renamedFolder.exists());
            assertTrue(renamedFolder2.exists());
            assertTrue(renamedFolder2.getFolder("bar").exists()); // check that sub folder still exists

            // Rename to a different parent folder
            // INBOX.foo-folder-renamed-again -> INBOX.foo2.foo3
            Folder foo2Folder = inboxFolder.getFolder("foo2");
            assertTrue(foo2Folder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES));
            assertTrue(foo2Folder.exists());
            Folder foo3Folder = foo2Folder.getFolder("foo3");
            assertTrue(!foo3Folder.exists());

            renamedFolder2.renameTo(foo3Folder);
            assertTrue(inboxFolder.getFolder("foo2.foo3").exists());
            assertTrue(!inboxFolder.getFolder("foo-folder-renamed-again").exists());
        } finally {
            store.close();
        }
    }

    @Test
    public void testFolderRequiringEscaping() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "foo@localhost", "test subject", "", greenMail.getSmtp().getServerSetup());

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);

            final Folder folderRequiringEscaping = inboxFolder.getFolder("requires escaping Ä");
            assertTrue(folderRequiringEscaping.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES));
            folderRequiringEscaping.open(Folder.READ_WRITE);

            assertEquals(0, folderRequiringEscaping.getMessageCount());
            assertEquals(1, inboxFolder.getMessageCount());

            inboxFolder.copyMessages(inboxFolder.getMessages(), folderRequiringEscaping);

            folderRequiringEscaping.expunge(); // invalidates folder cache
            assertEquals(1, folderRequiringEscaping.getMessageCount());
        } finally {
            store.close();
        }
    }

    @Test
    public void testUIDFolder() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder",
                "Test message", ServerSetupTest.SMTP);
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertEquals(1, messages.length);
            Message message = messages[0];

            assert inboxFolder instanceof UIDFolder;
            UIDFolder uidFolder = (UIDFolder) inboxFolder;
            long uid = uidFolder.getUID(message);
            assertEquals(message, uidFolder.getMessageByUID(uid));
            Message[] uidMessages = uidFolder.getMessagesByUID(new long[]{uid});
            assertEquals(1, uidMessages.length);
            assertEquals(message, uidMessages[0]);
            uidMessages = uidFolder.getMessagesByUID(uid, uid);
            assertEquals(1, uidMessages.length);
            assertEquals(message, uidMessages[0]);
        } finally {
            store.close();
        }
    }

    @Test
    public void testExpunge() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        for (int i = 0; i < 6; i++) {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject #" + i,
                    "Test message", ServerSetupTest.SMTP);
        }
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertEquals(6, messages.length);
            inboxFolder.setFlags(new int[]{2, 3}, new Flags(DELETED), true); // 1 and 2, offset is not zero-based

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
        } finally {
            store.close();
        }
    }
}
