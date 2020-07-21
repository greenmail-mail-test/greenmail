/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import static javax.mail.Flags.Flag.DELETED;
import static org.assertj.core.api.Assertions.*;

/**a
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapServerTest {
    private static final String UMLAUTS = "öäü \u00c4 \u00e4";
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(new ServerSetup[]{
            ServerSetupTest.IMAP,
            ServerSetupTest.IMAPS,
            ServerSetupTest.SMTP,
            ServerSetupTest.SMTPS
    });

    /**
     * Tests simple send and retrieve, including umlauts.
     *
     * @throws Exception on error.
     */
    @Test
    public void testRetreiveSimple() throws Exception {
        assertThat(greenMail.getImap()).isNotNull();
        final String subject = GreenMailUtil.random() + UMLAUTS;
        final String body = GreenMailUtil.random()
                + "\r\n" + " öäü \u00c4 \u00e4"
                + "\r\n" + GreenMailUtil.random();
        final String to = "test@localhost";
        MimeMessage mimeMessage = new MimeMessage(greenMail.getSmtp().createSession());
        mimeMessage.setSentDate(new Date());
        mimeMessage.setFrom("from@localhost");
        mimeMessage.setRecipients(Message.RecipientType.TO, to);

        mimeMessage.setSubject(subject, "UTF-8"); // Need to explicitly set encoding
        mimeMessage.setText(body, "UTF-8");
        Transport.send(mimeMessage);

        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages.length).isEqualTo(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent()).trim()).isEqualTo(body);
        }
    }

    @Test
    public void testImapsReceive() throws Throwable {
        assertThat(greenMail.getImaps()).isNotNull();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImaps())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages.length).isEqualTo(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent()).trim()).isEqualTo(body);
        }
    }

    @Test
    public void testRetreiveSimpleWithNonDefaultPassword() throws Exception {
        assertThat(greenMail.getImap()).isNotNull();
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            try {
                retriever.getMessages(to, "wrongpassword");
                fail("Expected failed login");
            } catch (Throwable e) {
                // ok
            }

            Message[] messages = retriever.getMessages(to, password);
            assertThat(messages.length).isEqualTo(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent()).trim()).isEqualTo(body);
        }
    }

    @Test
    public void testRetriveMultipart() throws Exception {
        assertThat(greenMail.getImap()).isNotNull();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);

            Object o = messages[0].getContent();
            assertThat(o instanceof MimeMultipart).isTrue();
            MimeMultipart mp = (MimeMultipart) o;
            assertThat(mp.getCount()).isEqualTo(2);
            BodyPart bp;
            bp = mp.getBodyPart(0);
            assertThat(GreenMailUtil.getBody(bp).trim()).isEqualTo(body);

            bp = mp.getBodyPart(1);
            assertThat(GreenMailUtil.getBody(bp).trim()).isEqualTo("AAEC");

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            GreenMailUtil.copyStream(bp.getInputStream(), bout);
            byte[] gif = bout.toByteArray();
            for (int i = 0; i < gif.length; i++) {
                assertThat((int)gif[i]).isEqualTo(i);
            }
        }
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
            assertThat(null != msgs && msgs.length == 1).isTrue();

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);

            final QuotaAwareStore quotaAwareStore = store;
            quotaAwareStore.setQuota(testQuota);

            Quota[] quotas = quotaAwareStore.getQuota("INBOX");
            assertThat(quotas).isNotNull();
            assertThat(quotas.length).isEqualTo(1);
            assertThat(quotas[0].resources).isNotNull();
            assertThat(quotas[0].resources.length).isEqualTo(2);
            assertThat(quotas[0].quotaRoot).isEqualTo(testQuota.quotaRoot);
            assertThat(testQuota.resources[0].limit).isEqualTo(quotas[0].resources[0].limit);
            assertThat(testQuota.resources[1].limit).isEqualTo(quotas[0].resources[1].limit);
            assertThat(1).isEqualTo(quotas[0].resources[1].usage);
//            assertThat(m.getSize()).isEqualTo(quotas[0].resources[0].usage);

            quotas = quotaAwareStore.getQuota("");
            assertThat(quotas).isNotNull();
            assertThat(quotas.length).isEqualTo(0);
            // TODO: Quota on ""
        } finally {
            store.close();
        }
    }

    @Test
    public void testQuotaCapability() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.setQuotaSupported(false);
        final IMAPStore store = greenMail.getImap().createStore();
        try {
            store.connect("foo@localhost", "pwd");

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);
            fail("Excepted MessageException since quota capability is turned off");
        } catch (MessagingException ex) {
            assertThat("QUOTA not supported").isEqualTo(ex.getMessage());
        } finally {
            store.close();
        }
    }

    @Test
    public void testSetGetFlags() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(1);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Set some flags
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            try {
                Message[] msgs = folder.getMessages();
                assertThat(null != msgs && msgs.length == 1).isTrue();

                Message m = msgs[0];

                Flags f = m.getFlags();
                assertThat(f.contains(Flags.Flag.DRAFT)).isFalse();
                assertThat(f.contains("foobar")).isFalse();
                m.setFlag(Flags.Flag.DRAFT, true);
                final Flags foobar = new Flags("foobar");
                m.setFlags(foobar, true);
                assertThat(m.getFlags().contains(Flags.Flag.DRAFT)).isTrue();
                assertThat(m.getFlags().contains("foobar")).isTrue();
            } finally {
                folder.close(true);
            }


            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            try {
                Message[] msgs = folder.getMessages();
                assertThat(null != msgs && msgs.length == 1).isTrue();
                Message m = msgs[0];
                Flags f = m.getFlags();
                assertThat(f.contains(Flags.Flag.DRAFT)).isTrue();
                assertThat(f.contains("foobar")).isTrue();
            } finally {
                folder.close(true);
            }
        } finally {
            store.close();
        }
    }

    @Test
    public void testNestedFolders() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            IMAPFolder newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertThat(newFolder.exists()).isFalse();

            assertThat(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();

            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertThat(newFolder.exists()).isTrue();
        } finally {
            store.close();
        }
    }

    /**
     * 
     * https://tools.ietf.org/html/rfc3501#page-37 :
     * <q>
     *     Renaming INBOX is permitted, and has special behavior.  It moves
     *     all messages in INBOX to a new mailbox with the given name,
     *     leaving INBOX empty.  If the server implementation supports
     *     inferior hierarchical names of INBOX, these are unaffected by a
     *     rename of INBOX.
     *  </q>
     *
     * @throws MessagingException
     */
    @Test
    public void testRenameINBOXFolder() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject",
                "Test message", greenMail.getSmtp().getServerSetup());

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            Folder inboxFolder = store.getFolder("INBOX");
            assertThat(inboxFolder.exists()).isTrue();
            inboxFolder.open(Folder.READ_ONLY);
            assertThat(inboxFolder.getMessages().length).isEqualTo(1);

            Folder inboxRenamedFolder = store.getFolder("INBOX-renamed");
            assertThat(inboxRenamedFolder.exists()).isFalse();

            inboxFolder.close(true);
            inboxFolder.renameTo(inboxRenamedFolder);
            assertThat(inboxRenamedFolder.exists()).isTrue();
            inboxRenamedFolder.open(Folder.READ_ONLY);
            assertThat(inboxRenamedFolder.getMessages().length).isEqualTo(1);

            inboxFolder = store.getFolder("INBOX");
            assertThat(inboxFolder.exists()).isTrue();
            inboxFolder.open(Folder.READ_ONLY);
            assertThat(inboxFolder.getMessages().length).isEqualTo(0);
        } finally {
            store.close();
        }
    }

    @Test
    public void testRenameFolder() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {

            // Create some folders
            Folder inboxFolder = store.getFolder("INBOX");
            Folder newFolder = inboxFolder.getFolder("foo-folder");
            assertThat(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(newFolder.exists()).isTrue();

            Folder renamedFolder = inboxFolder.getFolder("foo-folder-renamed");
            assertThat(renamedFolder.exists()).isFalse();

            // Rename
            assertThat(newFolder.renameTo(renamedFolder)).isTrue();
            assertThat(newFolder.exists()).isFalse();
            assertThat(renamedFolder.exists()).isTrue();

            // Rename with sub folder
            Folder subFolder = renamedFolder.getFolder("bar");
            assertThat(subFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(subFolder.exists()).isTrue();

            Folder renamedFolder2 = inboxFolder.getFolder("foo-folder-renamed-again");
            assertThat(renamedFolder.renameTo(renamedFolder2)).isTrue();
            assertThat(renamedFolder.exists()).isFalse();
            assertThat(renamedFolder2.exists()).isTrue();
            assertThat(renamedFolder2.getFolder("bar").exists()).isTrue(); // check that sub folder still exists

            // Rename to a different parent folder
            // INBOX.foo-folder-renamed-again -> INBOX.foo2.foo3
            Folder foo2Folder = inboxFolder.getFolder("foo2");
            assertThat(foo2Folder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(foo2Folder.exists()).isTrue();
            Folder foo3Folder = foo2Folder.getFolder("foo3");
            assertThat(foo3Folder.exists()).isFalse();

            renamedFolder2.renameTo(foo3Folder);
            assertThat(inboxFolder.getFolder("foo2.foo3").exists()).isTrue();
            assertThat(inboxFolder.getFolder("foo-folder-renamed-again").exists()).isFalse();
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
            assertThat(folderRequiringEscaping.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            folderRequiringEscaping.open(Folder.READ_WRITE);

            assertThat(folderRequiringEscaping.getMessageCount()).isEqualTo(0);
            assertThat(inboxFolder.getMessageCount()).isEqualTo(1);

            inboxFolder.copyMessages(inboxFolder.getMessages(), folderRequiringEscaping);

            folderRequiringEscaping.expunge(); // invalidates folder cache
            assertThat(folderRequiringEscaping.getMessageCount()).isEqualTo(1);
        } finally {
            store.close();
        }
    }

    @Test
    public void testUIDFolder() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder",
                "Test message", greenMail.getSmtp().getServerSetup());
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(1);
            Message message = messages[0];

            assert inboxFolder instanceof UIDFolder;
            UIDFolder uidFolder = (UIDFolder) inboxFolder;
            long uid = uidFolder.getUID(message);
            assertThat(uidFolder.getMessageByUID(uid)).isEqualTo(message);
            Message[] uidMessages = uidFolder.getMessagesByUID(new long[]{uid});
            assertThat(uidMessages.length).isEqualTo(1);
            assertThat(uidMessages[0]).isEqualTo(message);
            uidMessages = uidFolder.getMessagesByUID(uid, uid);
            assertThat(uidMessages.length).isEqualTo(1);
            assertThat(uidMessages[0]).isEqualTo(message);
        } finally {
            store.close();
        }
    }

    @Test
    public void testUIDExpunge() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        // Create some test emails
        int numberOfEmails = 10;
        long[] uids = new long[numberOfEmails];
        for (int i = 0; i < numberOfEmails; i++) {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UID expunge #" + i,
                    "Test message", greenMail.getSmtp().getServerSetup());
        }

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();
            assertThat(messages.length).isEqualTo(numberOfEmails);

            // Mark even as deleted ...
            Message[] msgsForDeletion = new Message[uids.length / 2];
            for (int i = 0; i < messages.length; i++) {
                assertThat(messages[i].getFlags().contains(Flags.Flag.DELETED)).isFalse();
                uids[i] = folder.getUID(messages[i]);
                if (i % 2 == 0) { // Deleted
                    messages[i].setFlag(Flags.Flag.DELETED, true);
                    msgsForDeletion[i / 2] = messages[i];
                }
            }

            // ... and expunge (with UID)
            folder.expunge(msgsForDeletion);

            // Check
            for (int i = 0; i < uids.length; i++) {
                final Message message = folder.getMessageByUID(uids[i]);
                if (i % 2 == 0) { // Deleted
                    assertThat(message).isNull();
                } else {
                    assertThat(message.isExpunged()).as("" + i).isFalse();
                    assertThat(message.getFlags().contains(DELETED)).as("" + i).isFalse();
                }
            }
        } finally {
            store.close();
        }
    }

    @Test
    public void testAppend() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test Append",
                "Test message", greenMail.getSmtp().getServerSetup());

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder inboxFolder = (IMAPFolder) store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(1);
            Message message = messages[0];

            Message[] toBeAppended = new Message[]{
                    new MimeMessage((MimeMessage) message) // Copy
            };
            toBeAppended[0].setSubject("testAppend#1");

            inboxFolder.appendMessages(toBeAppended);
            messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(2);

            // UIDPLUS
            toBeAppended[0] = new MimeMessage((MimeMessage) message);
            toBeAppended[0].setSubject("testAppend#2");

            final AppendUID[] appendUIDs = inboxFolder.appendUIDMessages(toBeAppended); // Copy again
            long uid = appendUIDs[0].uid;
            Message newMsg = inboxFolder.getMessageByUID(uid);
            assertThat(newMsg.getSubject()).isEqualTo(toBeAppended[0].getSubject());
            assertThat(inboxFolder.getUIDValidity()).isEqualTo(appendUIDs[0].uidvalidity);
            messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(3);
        } finally {
            store.close();
        }
    }

    @Test
    public void testUIDFetchWithWildcard() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder",
                "Test message", ServerSetupTest.SMTP);

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder 2",
                "Test message 2", ServerSetupTest.SMTP);
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(2);
            Message message = messages[1];

            assert inboxFolder instanceof UIDFolder;
            UIDFolder uidFolder = (UIDFolder) inboxFolder;
            long uid = uidFolder.getUID(message);
            assertThat(uidFolder.getMessageByUID(uid)).isEqualTo(message);
            Message[] uidMessages = uidFolder.getMessagesByUID(uid, UIDFolder.LASTUID);
            assertThat(uidMessages.length).isEqualTo(1);
            assertThat(uidMessages[0]).isEqualTo(message);
            uidMessages = uidFolder.getMessagesByUID(uid + 1, UIDFolder.LASTUID);
            assertThat(uidMessages.length).isEqualTo(1);
            assertThat(uidMessages[0]).isEqualTo(message);
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
            assertThat(messages.length).isEqualTo(6);
            inboxFolder.setFlags(new int[]{2, 3}, new Flags(DELETED), true); // 1 and 2, offset is not zero-based

            assertThat(inboxFolder.getMessage(1).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(2).isSet(DELETED)).isTrue();
            assertThat(inboxFolder.getMessage(3).isSet(DELETED)).isTrue();
            assertThat(inboxFolder.getMessage(4).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(5).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(6).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getDeletedMessageCount()).isEqualTo(2);
            Message[] expunged = inboxFolder.expunge();
            assertThat(expunged.length).isEqualTo(2);

            messages = inboxFolder.getMessages();
            assertThat(messages.length).isEqualTo(4);
            assertThat(messages[0].getSubject()).isEqualTo("Test subject #0");
            assertThat(messages[1].getSubject()).isEqualTo("Test subject #3");
            assertThat(messages[2].getSubject()).isEqualTo("Test subject #4");
            assertThat(messages[3].getSubject()).isEqualTo("Test subject #5");
        } finally {
            store.close();
        }
    }
}
