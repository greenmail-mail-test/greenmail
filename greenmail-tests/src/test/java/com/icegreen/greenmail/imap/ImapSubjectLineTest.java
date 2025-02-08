/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.imap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

class ImapSubjectLineTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.IMAP);

    @Test
    void testLongSubjectLine() throws Exception {
        String longSubjectLine = "test subject line jumped over the lazy lorem ipsum looking dog, after which the subject line became long enough to trigger some folding.               it was a dark and stormy night, after all.  pretty sure the initial trigger " +
                "for this test case did not have a subject line longer than 1000 chars, but there you go.  not sure what exactly what wouldve caused the folding, but maybe it happened on a somewhat shorter subject line.  whatever.";

        testSubject(longSubjectLine);
    }

    @Test
    void testSubjectWithEmbeddedSpaces() throws Exception {
        String subjectWithEmbeddedSpaces = "test subject line jumped over the lazy lorem ipsum looking dog, after which the subject line became long enough to trigger some folding.               it was a dark and stormy night, after all.  pretty sure the initial trigger " +
                "for this test case did not have a subject line longer than 1000 chars, but there you go.  not sure what exactly what would've caused the folding, but maybe it happened on a somewhat shorter subject line.  whatever.";

        testSubject(subjectWithEmbeddedSpaces);
    }

    @Test
    void testSubjectWithSingleQuote() throws Exception {
        String subjectWithSingleQuote = "This is'nt a bad subject";

        testSubject(subjectWithSingleQuote);
    }

    @Test
    void testSubjectWithDoubleQuote() throws Exception {
        String subjectWithDoubleQuote = "This is\"nt a bad subject";

        testSubject(subjectWithDoubleQuote);
    }

    @Test
    void testSubjectWithTabCharacter() throws Exception {
        String subjectWithTabCharacter = "The tab\t was there.";

        testSubject(subjectWithTabCharacter);
    }

    @Test
    void testSubjectWithBackslashCharacter() throws Exception {
        String subjectWithBackslashCharacter = "With \\back slash.";

        testSubject(subjectWithBackslashCharacter);
    }

    private void testSubject(String subject) throws Exception {
        GreenMailUser user = greenMail.setUser("to1@localhost", "pwd");
        assertThat(greenMail.getImap()).isNotNull();

        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");
        storeSearchTestMessages(greenMail.getImap().createSession(), folder, subject);

        greenMail.waitForIncomingEmail(1);

        final Store store = greenMail.getImap().createStore();
        store.connect("to1@localhost", "pwd");
        try {
            Folder imapFolder = store.getFolder("INBOX");
            imapFolder.open(Folder.READ_ONLY);

            Message[] imapMessages = imapFolder.getMessages();
            assertThat(null != imapMessages && imapMessages.length == 1).isTrue();
            Message imapMessage = imapMessages[0];
            assertThat(imapMessage.getSubject().replaceAll("\\s+","")).isEqualTo(subject.replaceAll("\\s+",""));
        } finally {
            store.close();
        }
    }

    /**
     * Create a message with a very long subject line for testing and add it to the folder.
     *
     * @param session Session to set on the messages
     * @param folder Folder to add to
     */
    private void storeSearchTestMessages(Session session, MailFolder folder, String subject) throws Exception {
        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject(subject);
        message1.setText("content");
        setRecipients(message1, Message.RecipientType.TO, "to", 1, 2);
        setRecipients(message1, Message.RecipientType.CC, "cc", 1, 2);
        setRecipients(message1, Message.RecipientType.BCC, "bcc", 1, 2);
        message1.setFrom(new InternetAddress("from2@localhost"));
        folder.store(message1);
    }

    /**
     * Set the recipient list of this message to a list containing two recipients of the specified type.
     *
     * @param message       Message to modify
     * @param recipientType Type of recipient to set
     * @param prefix        Prefix for recipient names. For prefix "to" and indexes {1,2} the actual names will be e.g. "to1@localhost", "to2@locahost"
     * @param indexes       List of indexes for which the addresses are generated, see doc for prefix
     */
    private void setRecipients(MimeMessage message, Message.RecipientType recipientType, String prefix, int... indexes)
            throws MessagingException {
        Address[] arr = new InternetAddress[indexes.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new InternetAddress(prefix + indexes[i] + "@localhost");
        }
        message.setRecipients(recipientType, arr);
    }

}
