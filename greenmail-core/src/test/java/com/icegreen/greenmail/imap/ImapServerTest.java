/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.imap;

import static com.icegreen.greenmail.util.GreenMailUtil.createTextEmail;
import static jakarta.mail.Flags.Flag.DELETED;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.eclipse.angus.mail.imap.AppendUID;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Quota;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

class ImapServerTest {
    private static final String UMLAUTS = "öäü \u00c4 \u00e4";
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(new ServerSetup[]{
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
    void testRetrieveSimple() throws Exception {
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
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent())).isEqualTo(body);
        }
    }

    @Test
    void testImapsReceive() throws Throwable {
        assertThat(greenMail.getImaps()).isNotNull();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImaps())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent())).isEqualTo(body);
        }
    }

    @Test
    void testRetrieveSimpleWithNonDefaultPassword() throws Exception {
        assertThat(greenMail.getImap()).isNotNull();
        final String to = "test@localhost";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            assertThatThrownBy(() -> retriever.getMessages(to, "wrongpassword"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(AuthenticationFailedException.class);

            Message[] messages = retriever.getMessages(to, password);
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(((String) messages[0].getContent())).isEqualTo(body);
        }
    }

    @Test
    void testRetrieveMultipart() throws Exception {
        assertThat(greenMail.getImap()).isNotNull();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);

            Object o = messages[0].getContent();
            assertThat(o).isInstanceOf(MimeMultipart.class);
            MimeMultipart mp = (MimeMultipart) o;
            assertThat(mp.getCount()).isEqualTo(2);
            BodyPart bp;
            bp = mp.getBodyPart(0);
            assertThat(bp.getContent()).isEqualTo(body);

            bp = mp.getBodyPart(1);
            assertThat(GreenMailUtil.getBody(bp)).isEqualTo("AAEC");

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            GreenMailUtil.copyStream(bp.getInputStream(), bout);
            byte[] gif = bout.toByteArray();
            for (int i = 0; i < gif.length; i++) {
                assertThat((int) gif[i]).isEqualTo(i);
            }
        }
    }

    @Test
    void testQuota() throws Exception {
        GreenMailUser user = greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject",
            "Test message", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(1);

        IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try (IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX")) {
            folder.open(Folder.READ_ONLY);
            Message[] msgs = folder.getMessages();
            assertThat(null != msgs && msgs.length == 1).isTrue();

            Quota[] initialQuota = store.getQuota("INBOX");
            assertThat(initialQuota).isEmpty();

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);

            Quota[] quotas = store.getQuota("INBOX");
            assertThat(quotas).isNotNull();
            assertThat(quotas).hasSize(1);
            assertThat(quotas[0].resources).isNotNull();
            assertThat(quotas[0].resources).hasSize(2);
            assertThat(quotas[0].quotaRoot).isEqualTo(testQuota.quotaRoot);
            assertThat(testQuota.resources[0].name).isEqualTo("STORAGE");
            assertThat(testQuota.resources[0].limit).isEqualTo(quotas[0].resources[0].limit);
            assertThat(quotas[0].resources[0].usage).isEqualTo(12L);
            assertThat(testQuota.resources[1].name).isEqualTo("MESSAGES");
            assertThat(testQuota.resources[1].limit).isEqualTo(quotas[0].resources[1].limit);
            assertThat(quotas[0].resources[1].usage).isEqualTo(1L);

            quotas = store.getQuota("");
            assertThat(quotas).isNotNull();
            assertThat(quotas).isEmpty();
        } finally {
            store.close();
        }

        // Deleting/recreating a user should delete the quota
        final UserManager userManager = greenMail.getUserManager();
        userManager.deleteUser(user);
        userManager.createUser(user.getEmail(), user.getLogin(), user.getPassword());
        store.connect("foo@localhost", "pwd");
        try {
            assertThat(store.getQuota("INBOX")).isEmpty();
            assertThat(store.getQuota("")).isEmpty();
        } finally {
            store.close();
        }
    }

    @Test
    void testQuotaInvalidResourceLimit() throws Exception {
        greenMail.setUser("foo@localhost", "pwd");

        try (IMAPStore store = greenMail.getImap().createStore()) {
            store.connect("foo@localhost", "pwd");
            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("MESSAGES", -5L);
            assertThatThrownBy(() -> store.setQuota(testQuota))
                .hasMessageContaining("NO SETQUOTA failed. Can not parse command SETQUOTA: " +
                    "Failed to parse quota INBOX resource limit MESSAGES value:" +
                    " Expected number (positive integer) but got -5");
            testQuota.setResourceLimit("MESSAGES", 5L);
            store.setQuota(testQuota);
        }
    }

    @Test
    void testQuotaCapability() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        greenMail.setQuotaSupported(false);
        try (IMAPStore store = greenMail.getImap().createStore()) {
            store.connect("foo@localhost", "pwd");

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);

            assertThatThrownBy(() -> store.setQuota(testQuota))
                .hasMessage("QUOTA not supported");
        }
    }

    @Test
    void testSetGetFlags() throws MessagingException {
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
    void testNestedFolders() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            // Create some folders
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            IMAPFolder newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertThat(newFolder.exists()).isFalse();

            assertThat(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isFalse();

            // Re-read and validate
            folder = (IMAPFolder) store.getFolder("INBOX");
            newFolder = (IMAPFolder) folder.getFolder("foo-folder");
            assertThat(newFolder.exists()).isTrue();
        } finally {
            store.close();
        }
    }

    /**
     * <a href="https://tools.ietf.org/html/rfc3501#page-37">RFC3501</a> :
     * <q>
     * Renaming INBOX is permitted, and has special behavior.  It moves
     * all messages in INBOX to a new mailbox with the given name,
     * leaving INBOX empty.  If the server implementation supports
     * inferior hierarchical names of INBOX, these are unaffected by a
     * rename of INBOX.
     * </q>
     */
    @Test
    void testRenameINBOXFolder() throws MessagingException {
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
            assertThat(inboxFolder.getMessages()).hasSize(1);

            Folder inboxRenamedFolder = store.getFolder("INBOX-renamed");
            assertThat(inboxRenamedFolder.exists()).isFalse();

            inboxFolder.close(true);
            inboxFolder.renameTo(inboxRenamedFolder);
            assertThat(inboxRenamedFolder.exists()).isTrue();
            inboxRenamedFolder.open(Folder.READ_ONLY);
            assertThat(inboxRenamedFolder.getMessages()).hasSize(1);

            inboxFolder = store.getFolder("INBOX");
            assertThat(inboxFolder.exists()).isTrue();
            inboxFolder.open(Folder.READ_ONLY);
            assertThat(inboxFolder.getMessages()).isEmpty();
        } finally {
            store.close();
        }
    }

    @Test
    void testRenameFolder() throws MessagingException {
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
    void testFolderMoveMessages() throws MessagingException {
        int msgCount = 3;
        sendMessages(msgCount);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder inboxFolder = (IMAPFolder) store.getFolder("INBOX");

            // Target folder
            IMAPFolder targetFolder = (IMAPFolder) inboxFolder.getFolder("target-folder");
            assertThat(targetFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(targetFolder.exists()).isTrue();

            inboxFolder.open(Folder.READ_WRITE);
            try {
                final Message[] messages = inboxFolder.getMessages();
                assertThat(messages).hasSize(msgCount);
                assertThat(targetFolder.getMessageCount()).isZero();
                inboxFolder.moveMessages(messages, targetFolder);
            } finally {
                inboxFolder.close();
            }

            targetFolder.open(Folder.READ_ONLY); // Refresh for new messages
            try {
                assertThat(targetFolder.getMessageCount()).isEqualTo(msgCount);
            } finally {
                targetFolder.close();
            }
        } finally {
            store.close();
        }
    }

    @Test
    void testFolderMoveMessagesBasedOnUid() throws MessagingException {
        int msgCount = 3;
        sendMessages(msgCount);
        IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder inboxFolder = (IMAPFolder) store.getFolder("INBOX");

            // Target folder
            IMAPFolder targetFolder = (IMAPFolder) inboxFolder.getFolder("target-folder");
            assertThat(targetFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(targetFolder.exists()).isTrue();

            inboxFolder.open(Folder.READ_WRITE);

            Message[] existingMessages = inboxFolder.getMessages();
            assertThat(existingMessages).hasSize(3);
            for (int i = 0; i < 3; i++) {
                final int uid = i + 1;
                Message[] messages = inboxFolder.getMessagesByUID(
                    new long[]{uid});
                assertThat(messages).hasSize(1);
                inboxFolder.moveMessages(messages, targetFolder);
            }
            targetFolder.open(Folder.READ_ONLY); // Refresh for new messages
            try {
                assertThat(targetFolder.getMessageCount()).isEqualTo(msgCount);
            } finally {
                targetFolder.close();
            }
        } finally {
            store.close();
        }
    }

    private void sendMessages(int n) {
        greenMail.setUser("foo@localhost", "pwd");

        for (int i = 0; i < n; i++) {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject #" + i,
                "Test message", ServerSetupTest.SMTP);
        }
        greenMail.waitForIncomingEmail(n);

    }

    @Test
    void testFolderMoveUIDMessages() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        int msgCount = 3;
        for (int i = 0; i < 3; i++) {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject #" + i,
                "Test message", ServerSetupTest.SMTP);
        }
        greenMail.waitForIncomingEmail(msgCount);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder inboxFolder = (IMAPFolder) store.getFolder("INBOX");

            // Target folder
            IMAPFolder targetFolder = (IMAPFolder) inboxFolder.getFolder("target-folder");
            assertThat(targetFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)).isTrue();
            assertThat(targetFolder.exists()).isTrue();

            inboxFolder.open(Folder.READ_WRITE);
            try {
                final Message[] messages = inboxFolder.getMessages();
                assertThat(messages).hasSize(msgCount);
                assertThat(targetFolder.getMessageCount()).isZero();
                inboxFolder.moveUIDMessages(messages, targetFolder);
            } finally {
                inboxFolder.close();
            }

            targetFolder.open(Folder.READ_ONLY); // Refresh for new messages
            try {
                assertThat(targetFolder.getMessageCount()).isEqualTo(msgCount);
            } finally {
                targetFolder.close();
            }
        } finally {
            store.close();
        }
    }

    @Test
    void testFolderRequiringEscaping() throws MessagingException {
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

            assertThat(folderRequiringEscaping.getMessageCount()).isZero();
            assertThat(inboxFolder.getMessageCount()).isEqualTo(1);

            inboxFolder.copyMessages(inboxFolder.getMessages(), folderRequiringEscaping);

            folderRequiringEscaping.expunge(); // invalidates folder cache
            assertThat(folderRequiringEscaping.getMessageCount()).isEqualTo(1);
        } finally {
            store.close();
        }
    }

    @Test
    void testUIDFolder() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder",
            "Test message", greenMail.getSmtp().getServerSetup());
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(1);
            Message message = messages[0];

            assert inboxFolder instanceof UIDFolder;
            UIDFolder uidFolder = (UIDFolder) inboxFolder;
            long uid = uidFolder.getUID(message);
            assertThat(uidFolder.getMessageByUID(uid)).isEqualTo(message);
            Message[] uidMessages = uidFolder.getMessagesByUID(new long[]{uid});
            assertThat(uidMessages).hasSize(1);
            assertThat(uidMessages[0]).isEqualTo(message);
            uidMessages = uidFolder.getMessagesByUID(uid, uid);
            assertThat(uidMessages).hasSize(1);
            assertThat(uidMessages[0]).isEqualTo(message);
        } finally {
            store.close();
        }
    }

    @Test
    void testUIDExpunge() throws MessagingException {
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
            assertThat(messages).hasSize(numberOfEmails);

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
    void testAppend() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test Append",
            "Test message", greenMail.getSmtp().getServerSetup());

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder inboxFolder = (IMAPFolder) store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);

            Message[] messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(1);
            Message message = messages[0];

            Message[] toBeAppended = new Message[]{
                new MimeMessage((MimeMessage) message) // Copy
            };
            toBeAppended[0].setSubject("testAppend#1");

            inboxFolder.appendMessages(toBeAppended);
            messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(2);

            // UIDPLUS
            toBeAppended[0] = new MimeMessage((MimeMessage) message);
            toBeAppended[0].setSubject("testAppend#2");

            final AppendUID[] appendUIDs = inboxFolder.appendUIDMessages(toBeAppended); // Copy again
            long uid = appendUIDs[0].uid;
            Message newMsg = inboxFolder.getMessageByUID(uid);
            assertThat(newMsg.getSubject()).isEqualTo(toBeAppended[0].getSubject());
            assertThat(inboxFolder.getUIDValidity()).isEqualTo(appendUIDs[0].uidvalidity);
            messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(3);
        } finally {
            store.close();
        }
    }

    @Test
    void testUIDFetchWithWildcard() throws MessagingException {
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
            assertThat(messages).hasSize(2);
            Message message = messages[1];

            assert inboxFolder instanceof UIDFolder;
            UIDFolder uidFolder = (UIDFolder) inboxFolder;
            long uid = uidFolder.getUID(message);
            assertThat(uidFolder.getMessageByUID(uid)).isEqualTo(message);
            Message[] uidMessages = uidFolder.getMessagesByUID(uid, UIDFolder.LASTUID);
            assertThat(uidMessages).hasSize(1);
            assertThat(uidMessages[0]).isEqualTo(message);
            uidMessages = uidFolder.getMessagesByUID(uid + 1, UIDFolder.LASTUID);
            assertThat(uidMessages).hasSize(1);
            assertThat(uidMessages[0]).isEqualTo(message);
        } finally {
            store.close();
        }
    }

    @Test
    void testExpunge() throws MessagingException {
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
            assertThat(messages).hasSize(6);
            inboxFolder.setFlags(new int[]{2, 3}, new Flags(DELETED), true); // 1 and 2, offset is not zero-based

            assertThat(inboxFolder.getMessage(1).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(2).isSet(DELETED)).isTrue();
            assertThat(inboxFolder.getMessage(3).isSet(DELETED)).isTrue();
            assertThat(inboxFolder.getMessage(4).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(5).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getMessage(6).isSet(DELETED)).isFalse();
            assertThat(inboxFolder.getDeletedMessageCount()).isEqualTo(2);
            Message[] expunged = inboxFolder.expunge();
            assertThat(expunged).hasSize(2);

            messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(4);
            assertThat(messages[0].getSubject()).isEqualTo("Test subject #0");
            assertThat(messages[1].getSubject()).isEqualTo("Test subject #3");
            assertThat(messages[2].getSubject()).isEqualTo("Test subject #4");
            assertThat(messages[3].getSubject()).isEqualTo("Test subject #5");
        } finally {
            store.close();
        }
    }

    @Test(timeout = 10000)
    void testIdle() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            int[] messages = new int[]{0};
            MessageCountListener listener = new MessageCountListener() {
                @Override
                public void messagesRemoved(MessageCountEvent e) {
                }

                @Override
                public void messagesAdded(MessageCountEvent e) {
                    messages[0] = e.getMessages()[0].getMessageNumber();
                }
            };
            inboxFolder.addMessageCountListener(listener);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // Ignore
                }
                GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message",
                    ServerSetupTest.SMTP);
            }).start();
            ((IMAPFolder) inboxFolder).idle(true);
            assertThat(messages).hasSize(1);
            assertThat(messages[0]).isPositive();
            inboxFolder.close();
        } finally {
            store.close();
        }
    }

    @Test
    void testSendWithCC() throws MessagingException {
        GreenMailUser userTo = greenMail.setUser("to-user@localhost", "pwd");
        GreenMailUser userCC = greenMail.setUser("cc-userTo@locahost", "other-pwd");

        // Create and send test mail
        final MimeMessage email = createTextEmail(userTo.getEmail(), userTo.getEmail(), "testSendWithCC",
            "Test message", greenMail.getSmtp().getServerSetup());
        email.addRecipients(Message.RecipientType.CC, userCC.getEmail());
        GreenMailUtil.sendMimeMessage(email);

        final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(2); // Expect two received messages, for TO and CC

        // Check via IMAP if TO-user received msg
        try (final IMAPStore store = greenMail.getImap().createStore()) {
            store.connect(userTo.getEmail(), userTo.getPassword());
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            final Message[] messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(1);
            Message msg = messages[0];
            assertThat(msg.getSubject()).isEqualTo(email.getSubject());
        }

        // Check via IMAP if CC-user received msg
        try (final IMAPStore store = greenMail.getImap().createStore()) {
            store.connect(userCC.getEmail(), userCC.getPassword());
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            final Message[] messages = inboxFolder.getMessages();
            assertThat(messages).hasSize(1);
            Message msg = messages[0];
            assertThat(msg.getSubject()).isEqualTo(email.getSubject());
        }
    }
}
