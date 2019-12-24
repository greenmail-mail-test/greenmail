/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test.commands;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.DateTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.HeaderTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SubjectTerm;

import org.junit.Rule;
import org.junit.Test;

import com.icegreen.greenmail.imap.commands.SearchKey;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;

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
            assertEquals(6, imapMessages.length);
            Message m0 = imapMessages[0];
            assertTrue(m0.getFlags().contains(Flags.Flag.ANSWERED));
            assertTrue(m0.getSubject().startsWith("#0"));
            Message m1 = imapMessages[1];
            assertTrue(m1.getSubject().startsWith("#1"));
            Message m2 = imapMessages[2];
            assertTrue(m2.getSubject().startsWith("#2"));
            Message m3 = imapMessages[3];
            assertTrue(m3.getSubject().startsWith("#3"));
            Message m4 = imapMessages[4];
            assertTrue(m4.getSubject().startsWith("#4"));
            Message m5 = imapMessages[5];
            assertTrue(m5.getSubject().startsWith("#5"));

            // Search flags
            imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
            assertEquals(1, imapMessages.length);
            assertEquals(m0, imapMessages[0]);

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, true));
            assertEquals(1, imapMessages.length);
            assertTrue(imapMessages[0].getFlags().contains("foo"));

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, false));
            assertEquals(5, imapMessages.length);
            assertFalse(imapMessages[0].getFlags().contains(fooFlags));
            assertFalse(imapMessages[1].getFlags().contains(fooFlags));
            assertFalse(imapMessages[2].getFlags().contains(fooFlags));

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
            assertSame(imapMessages[0], m0);
            imapMessages = imapFolder.search(new SubjectTerm("TeSt0Search")); // Case insensitive
            assertEquals(2, imapMessages.length);
            assertSame(imapMessages[0], m0);
            imapMessages = imapFolder.search(new SubjectTerm("0S"));
            assertEquals(2, imapMessages.length);
            assertSame(imapMessages[0], m0);
            imapMessages = imapFolder.search(new SubjectTerm("not found"));
            assertEquals(0, imapMessages.length);
            imapMessages = imapFolder.search(new SubjectTerm("test"));
            assertEquals(2, imapMessages.length);

            //Search OrTerm - Search Subject which contains test0Search OR nonexistent
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("test0Search"), new SubjectTerm("nonexistent")));
            assertEquals(2, imapMessages.length);
            assertSame(imapMessages[0], m0);

            // OrTerm : two matching sub terms
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("foo"), new SubjectTerm("bar")));
            assertEquals(2, imapMessages.length);
            assertSame(imapMessages[0], m2);
            assertSame(imapMessages[1], m3);

            // OrTerm : no matching
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("nothing"), new SubjectTerm("nil")));
            assertEquals(0, imapMessages.length);

            //Search AndTerm - Search Subject which contains test0Search AND test1Search
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("test0Search"), new SubjectTerm("test1Search")));
            assertEquals(1, imapMessages.length);
            assertSame(imapMessages[0], m1);

            testReceivedDateTerms(imapFolder, m0, m1, m2, m3, m4, m5);

            testSentDateTerms(imapFolder, m0, m1, m2, m3, m4, m5);

            // Content
            final String pattern = "\u00e4\u03A0";
            imapMessages = imapFolder.search(new SubjectTerm(pattern));
            assertEquals(1, imapMessages.length);
            assertTrue(imapMessages[0].getSubject().contains(pattern));
        } finally {
            store.close();
        }
    }

    private void testSentDateTerms(Folder imapFolder, Message... m) throws Exception {
        //greater equals, returns all
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.GE, getSampleDate()), m[5]);
        //greater than, does not return sample sent mail
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.GT, getSampleDate()));
        //equals, only returns sample mail
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.EQ, getSampleDate()), m[5]);
        //not equals, does not return sample mail, but all other mails
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.NE, getSampleDate()), m[0], m[1], m[2], m[3], m[4]);
        //less than (sample mail + 1 day), only returns sample mail
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.LT, getSampleDate(2)), m[5]);
        //TODO: less equals: does not work yet, is therefore not included, should only return sample mail
        //see https://github.com/greenmail-mail-test/greenmail/issues/234
        //testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.LE, getSampleDate(2)), m[5]);
    }

    private void testReceivedDateTerms(Folder imapFolder, Message... m) throws Exception {
        //greater equals, returns all
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.GE, getSampleDate()), m[0], m[1], m[2], m[3], m[4], m[5]);
        //greater than, does not return received sample mail
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.GT, getSampleDate()), m[0], m[1], m[2], m[3], m[5]);
        //equals, only returns sample mail
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.EQ, getSampleDate()), m[4]);
        //not equals, does not return sample mail
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.NE, getSampleDate()), m[0], m[1], m[2], m[3], m[5]);
        //less than (sample mail + 1 day), only returns sample mail
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.LT, getSampleDate(2)), m[4]);
        //TODO: less equals: does not work yet, is therefore not included, should only return sample mail
        //see https://github.com/greenmail-mail-test/greenmail/issues/234
        //testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.LE, getSampleDate(2)), m[4]);
    }

    // Test an unsupported search term for exception. Should be ignored.
    @Test
    public void testUnsupportedSearchWarnsButDoesNotThrowException() {
        try {
            SearchKey.valueOf("SENTDATE");
            fail("Expected IAE for unimplemented search");
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    private Date getSampleDate(int day) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse("2010-01-"+day);
        } catch (ParseException e) {
            throw new IllegalStateException("Can not parse date", e);
        }
    }

    private Date getSampleDate() {
        return getSampleDate(1);
    }

    private void testDateTerm(Folder imapFolder, DateTerm term, Message... expectedResults) throws Exception {
        Message[] imapMessages = imapFolder.search(term);
        assertEquals(expectedResults.length, imapMessages.length);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(imapMessages[i], expectedResults[i]);
        }

    }

    /**
     * Create the two messages with different recipients, etc. for testing and add them to the folder.
     *
     * @param session Session to set on the messages
     * @param folder  Folder to add to
     * @param flags   Flags to set on both messages
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

        MimeMessage message4 = new MimeMessage(session);
        message4.setSubject("#4 with received date");
        message4.setText("content received date");
        folder.appendMessage(message4, new Flags(), getSampleDate());

        MimeMessage message5 = new MimeMessage(session);
        message5.setSubject("#5 with sent date");
        message5.setText("content sent date");
        message5.setSentDate(getSampleDate());
        folder.store(message5);

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
