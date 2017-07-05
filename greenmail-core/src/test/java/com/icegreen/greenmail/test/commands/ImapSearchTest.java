/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test.commands;

import java.util.Date;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.*;

import com.icegreen.greenmail.imap.commands.SearchKey;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapSearchTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    @Test
    public void testSearch() throws Exception {
        GreenMailUser user = greenMail.setUser("to1@localhost", "pwd");
        assertNotNull(greenMail.getImap());

        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");
        Flags fooFlags = new Flags();
        fooFlags.add("foo");
        storeSearchTestMessages(greenMail.getImap().createSession(), folder, fooFlags);

        greenMail.waitForIncomingEmail(2);

        final Store store = greenMail.getImap().createStore();
        store.connect("to1@localhost", "pwd");
        try {
            Folder imapFolder = store.getFolder("INBOX");
            imapFolder.open(Folder.READ_WRITE);

            Message[] imapMessages = imapFolder.getMessages();
            assertEquals(4, imapMessages.length);
            Message m0 = imapMessages[0];
            assertTrue(m0.getSubject().startsWith("#0"));
            Message m1 = imapMessages[1];
            assertTrue(m1.getSubject().startsWith("#1"));
            Message m2 = imapMessages[2];
            assertTrue(m2.getSubject().startsWith("#2"));
            Message m3 = imapMessages[3];
            assertTrue(m3.getSubject().startsWith("#3"));

            assertTrue(m0.getFlags().contains(Flags.Flag.ANSWERED));

            // Search flags
            imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
            assertEquals(1, imapMessages.length);
            assertEquals(m0, imapMessages[0]);

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, true));
            assertEquals(1, imapMessages.length);
            assertTrue(imapMessages[0].getFlags().contains("foo"));

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, false));
            assertEquals(3, imapMessages.length);
            assertTrue(!imapMessages[0].getFlags().contains(fooFlags));
            assertTrue(!imapMessages[1].getFlags().contains(fooFlags));
            assertTrue(!imapMessages[2].getFlags().contains(fooFlags));

            // Search header ids
            String id = m0.getHeader("Message-ID")[0];
            imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
            assertEquals(1, imapMessages.length);
            assertEquals(m0, imapMessages[0]);

            id = m1.getHeader("Message-ID")[0];
            imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
            assertEquals(1, imapMessages.length);
            assertEquals(m1, imapMessages[0]);

            // Search FROM
            imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from2@localhost")));
            assertEquals(1, imapMessages.length);
            assertEquals(m0, imapMessages[0]);

            imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from3@localhost")));
            assertEquals(1, imapMessages.length);
            assertEquals(m1, imapMessages[0]);

            // Search TO
            imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to2@localhost")));
            assertEquals(1, imapMessages.length);
            assertEquals(m0, imapMessages[0]);

            imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to3@localhost")));
            assertEquals(3, imapMessages.length);
            assertEquals(m1, imapMessages[0]);

            // Search Subject
            imapMessages = imapFolder.search(new SubjectTerm("test0Search"));
            assertEquals(2, imapMessages.length);
            assertTrue(imapMessages[0] == m0);
            imapMessages = imapFolder.search(new SubjectTerm("TeSt0Search")); // Case insensitive
            assertEquals(2, imapMessages.length);
            assertTrue(imapMessages[0] == m0);
            imapMessages = imapFolder.search(new SubjectTerm("0S"));
            assertEquals(2, imapMessages.length);
            assertTrue(imapMessages[0] == m0);
            imapMessages = imapFolder.search(new SubjectTerm("not found"));
            assertEquals(0, imapMessages.length);
            imapMessages = imapFolder.search(new SubjectTerm("test"));
            assertEquals(2, imapMessages.length);

            //Search OrTerm - Search Subject which contains test0Search OR nonexistent
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("test0Search"), new SubjectTerm("nonexistent")));
            assertEquals(2, imapMessages.length);
            assertTrue(imapMessages[0] == m0);

            // OrTerm : two matching sub terms
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("foo"), new SubjectTerm("bar")));
            assertEquals(2, imapMessages.length);
            assertTrue(imapMessages[0] == m2);
            assertTrue(imapMessages[1] == m3);

            // OrTerm : no matching
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("nothing"), new SubjectTerm("nil")));
            assertEquals(0, imapMessages.length);

            //Search AndTerm - Search Subject which contains test0Search AND test1Search
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("test0Search"), new SubjectTerm("test1Search")));
            assertEquals(1, imapMessages.length);
            assertTrue(imapMessages[0] == m1);


            // Content
            final String pattern = "\u00e4\u03A0";
            imapMessages = imapFolder.search(new SubjectTerm(pattern));
            assertEquals(1, imapMessages.length);
            assertTrue(imapMessages[0].getSubject().contains(pattern));
        } finally {
            store.close();
        }
    }

    // Test an unsupported search term for exception. Should be ignored.
    @Test
    public void testUnsupportedSearchWarnsButDoesNotThrowException() throws MessagingException {
        try {
            SearchKey.valueOf("SENTDATE");
            fail("Expected IAE for unimplemented search");
        } catch (IllegalArgumentException ex) {
            // Expected
        }

        greenMail.setUser("to1@localhost", "pwd"); // Create user

        final Store store = greenMail.getImap().createStore();
        store.connect("to1@localhost", "pwd");
        try {
            Folder imapFolder = store.getFolder("INBOX");
            imapFolder.open(Folder.READ_WRITE);
            imapFolder.search(new SentDateTerm(ComparisonTerm.LT, new Date()));
        } finally {
            store.close();
        }
    }

    /**
     * Create the two messages with different recipients, etc. for testing and add them to the folder.
     *
     * @param session Session to set on the messages
     * @param folder  Folder to add to
     * @param flags   Flags to set on both messages
     * @throws Exception
     */
    private void storeSearchTestMessages(Session session, MailFolder folder, Flags flags) throws Exception {
        MimeMessage message0 = new MimeMessage(session);
        message0.setSubject("#0 test0Search");
        message0.setText("content");
        setRecipients(message0, Message.RecipientType.TO, "to", 1, 2);
        setRecipients(message0, Message.RecipientType.CC, "cc", 1, 2);
        setRecipients(message0, Message.RecipientType.BCC, "bcc", 1, 2);
        message0.setFrom(new InternetAddress("from2@localhost"));
        message0.setFlag(Flags.Flag.ANSWERED, true);
        message0.setFlags(flags, true);
        folder.store(message0);

        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("#1 test0Search test1Search \u00c4\u00e4\u03A0", "UTF-8");
        message1.setText("content \u00c4\u00e4\u03A0", "UTF-8");
        setRecipients(message1, Message.RecipientType.TO, "to", 1, 3);
        setRecipients(message1, Message.RecipientType.CC, "cc", 1, 3);
        setRecipients(message1, Message.RecipientType.BCC, "bcc", 1, 3);
        message1.setFrom(new InternetAddress("from3@localhost"));
        message1.setFlag(Flags.Flag.ANSWERED, false);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("#2 OR search : foo");
        message2.setText("content foo");
        setRecipients(message2, Message.RecipientType.TO, "to", 3);
        setRecipients(message2, Message.RecipientType.CC, "cc", 4);
        setRecipients(message2, Message.RecipientType.BCC, "bcc", 5);
        message2.setFrom(new InternetAddress("from4@localhost"));
        message2.setFlag(Flags.Flag.ANSWERED, false);
        message2.setFlags(flags, false);
        folder.store(message2);

        MimeMessage message3 = new MimeMessage(session);
        message3.setSubject("#3 OR search : bar");
        message3.setText("content bar");
        setRecipients(message3, Message.RecipientType.TO, "to", 3);
        setRecipients(message3, Message.RecipientType.CC, "cc", 4);
        setRecipients(message3, Message.RecipientType.BCC, "bcc", 5);
        message3.setFrom(new InternetAddress("from5@localhost"));
        message3.setFlag(Flags.Flag.ANSWERED, false);
        message3.setFlags(flags, false);
        folder.store(message3);
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
