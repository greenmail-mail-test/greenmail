/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test.commands;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.*;

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
        assertThat(greenMail.getImap()).isNotNull();

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

            Message[] allImapMessages = imapFolder.getMessages();
            assertThat(allImapMessages.length).isEqualTo(6);
            Message m0 = allImapMessages[0];
            assertThat(m0.getFlags().contains(Flags.Flag.ANSWERED)).isTrue();
            assertThat(m0.getSubject().startsWith("#0")).isTrue();
            Message m1 = allImapMessages[1];
            assertThat(m1.getSubject().startsWith("#1")).isTrue();
            Message m2 = allImapMessages[2];
            assertThat(m2.getSubject().startsWith("#2")).isTrue();
            Message m3 = allImapMessages[3];
            assertThat(m3.getSubject().startsWith("#3")).isTrue();
            Message m4 = allImapMessages[4];
            assertThat(m4.getSubject().startsWith("#4")).isTrue();
            Message m5 = allImapMessages[5];
            assertThat(m5.getSubject().startsWith("#5")).isTrue();

            // Search BODY
            Message[] imapMessages = imapFolder.search(new BodyTerm("tent"));
            assertThat(imapMessages.length).isEqualTo(allImapMessages.length);

            imapMessages = imapFolder.search(new BodyTerm("tent#2"));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m2);

            imapMessages = imapFolder.search(new BodyTerm("from"));
            assertThat(imapMessages.length).isEqualTo(0);

            // Search flags
            imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m0);

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, true));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0].getFlags().contains("foo")).isTrue();

            imapMessages = imapFolder.search(new FlagTerm(fooFlags, false));
            assertThat(imapMessages.length).isEqualTo(5);
            assertThat(imapMessages[0].getFlags().contains(fooFlags)).isFalse();
            assertThat(imapMessages[1].getFlags().contains(fooFlags)).isFalse();
            assertThat(imapMessages[2].getFlags().contains(fooFlags)).isFalse();

            // Search header ids
            String id = m0.getHeader("Message-ID")[0];
            imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m0);

            id = m1.getHeader("Message-ID")[0];
            imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m1);

            // Search FROM
            imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from2@localhost")));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m0);

            imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from3@localhost")));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m1);

            // Search TO
            imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to2@localhost")));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0]).isEqualTo(m0);

            imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to3@localhost")));
            assertThat(imapMessages.length).isEqualTo(3);
            assertThat(imapMessages[0]).isEqualTo(m1);

            // Search Subject
            imapMessages = imapFolder.search(new SubjectTerm("test0Search"));
            assertThat(imapMessages.length).isEqualTo(2);
            assertThat(m0).isSameAs(imapMessages[0]);
            imapMessages = imapFolder.search(new SubjectTerm("TeSt0Search")); // Case insensitive
            assertThat(imapMessages.length).isEqualTo(2);
            assertThat(m0).isSameAs(imapMessages[0]);
            imapMessages = imapFolder.search(new SubjectTerm("0S"));
            assertThat(imapMessages.length).isEqualTo(2);
            assertThat(m0).isSameAs(imapMessages[0]);
            imapMessages = imapFolder.search(new SubjectTerm("not found"));
            assertThat(imapMessages.length).isEqualTo(0);
            imapMessages = imapFolder.search(new SubjectTerm("test"));
            assertThat(imapMessages.length).isEqualTo(2);

            //Search OrTerm - Search Subject which contains test0Search OR nonexistent
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("test0Search"), new SubjectTerm("nonexistent")));
            assertThat(imapMessages.length).isEqualTo(2);
            assertThat(m0).isSameAs(imapMessages[0]);

            // OrTerm : two matching sub terms
            imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("foo"), new SubjectTerm("bar")));
            assertThat(imapMessages.length).isEqualTo(2);
            assertThat(m2).isSameAs(imapMessages[0]);
            assertThat(m3).isSameAs(imapMessages[1]);

            // OrTerm : no matching
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("nothing"), new SubjectTerm("nil")));
            assertThat(imapMessages.length).isEqualTo(0);

            //Search AndTerm - Search Subject which contains test0Search AND test1Search
            imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("test0Search"), new SubjectTerm("test1Search")));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(m1).isSameAs(imapMessages[0]);

            testReceivedDateTerms(imapFolder, m0, m1, m2, m3, m4, m5);

            testSentDateTerms(imapFolder, m0, m1, m2, m3, m4, m5);

            // Content
            final String pattern = "\u00e4\u03A0";
            imapMessages = imapFolder.search(new SubjectTerm(pattern));
            assertThat(imapMessages.length).isEqualTo(1);
            assertThat(imapMessages[0].getSubject().contains(pattern)).isTrue();
        } finally {
            store.close();
        }
    }

    @Test
    public void testSearchIssue319() throws Exception {
        String from = "from@localhost";
        String to = from;
        String subject = "Greenmail";
        String content = "Hello";

        // Setup test emails
        GreenMailUser user = greenMail.setUser(to, "pwd");
        Session session = greenMail.getImap().createSession();
        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");
        storeMessage(session, folder, to, from, subject, content); // Match
        storeMessage(session, folder, to, from, subject, content); // Match
        storeMessage(session, folder, to, from, "No-match", content); // No match
        storeMessage(session, folder, "otheraddress@localhost", "otheraddress@localhost", subject, content); // No match
        assertThat(folder.getMessageCount()).isEqualTo(4);

        // Run search test
        final Store store = greenMail.getImap().createStore();
        store.connect(to, "pwd");
        try {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            SearchTerm fromTerm = new FromStringTerm(from);
            SearchTerm toTerm = new RecipientStringTerm(Message.RecipientType.TO, from);

            AndTerm and = new AndTerm(new SubjectTerm(subject),
                    new OrTerm(toTerm, fromTerm));

            Message[] messages = inbox.search(and);

            assertThat(2).as("Failure on AND search").isEqualTo(messages.length);
        } finally {
            store.close();
        }
    }

    private void storeMessage(Session session, MailFolder folder, String to, String from, String subject, String content)
            throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        folder.store(message);
    }

    private void testSentDateTerms(Folder imapFolder, Message... m) throws Exception {
        // m0.sentDate : now()
        // m1.sentDate : now()
        // m2.sentDate : now()
        // m3.sentDate : now()
        // m4.sentDate : now()
        // m5.sentDate : Fri Jan 01 00:00:00 CET 2010

        final Date sampleDate = getSampleDate(); // Fri Jan 01 00:00:00 CET 2010

        //greater equals, returns all
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.GE, sampleDate), m);
        //greater than, does not return sample sent mail m5
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.GT, sampleDate), m[0], m[1],m[2],m[3],m[4]);
        //equals, only returns sample mail
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.EQ, sampleDate), m[5]);
        //not equals, does not return sample mail, but all other mails
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.NE, sampleDate), m[0], m[1], m[2], m[3], m[4]);
        //less than (sample mail + 1 day), only returns sample mail
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.LT, getSampleDate(2)), m[5]);
        //less equal
        testDateTerm(imapFolder, new SentDateTerm(ComparisonTerm.LE, getSampleDate(2)), m[5]);
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
        //less equal
        testDateTerm(imapFolder, new ReceivedDateTerm(ComparisonTerm.LE, getSampleDate(2)), m[4]);
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
            return new SimpleDateFormat("yyyy-MM-dd").parse("2010-01-" + day);
        } catch (ParseException e) {
            throw new IllegalStateException("Can not parse date", e);
        }
    }

    private Date getSampleDate() {
        return getSampleDate(1);
    }

    private void testDateTerm(Folder imapFolder, DateTerm term, Message... expectedResults) throws Exception {
        Message[] imapMessages = imapFolder.search(term);
        assertThat(imapMessages.length).isEqualTo(expectedResults.length);
        for (int i = 0; i < expectedResults.length; i++) {
            assertThat(expectedResults[i]).isSameAs(imapMessages[i]);
        }
    }

    /**
     * Create messages in folder
     *
     * @param session Session to set on the messages
     * @param folder  Folder to add to
     * @param flags   Flags to set on both messages
     */
    private void storeSearchTestMessages(Session session, MailFolder folder, Flags flags) throws Exception {
        MimeMessage message0 = new MimeMessage(session);
        message0.setSubject("#0 test0Search");
        message0.setText("content#0");
        setRecipients(message0, Message.RecipientType.TO, "to", 1, 2);
        setRecipients(message0, Message.RecipientType.CC, "cc", 1, 2);
        setRecipients(message0, Message.RecipientType.BCC, "bcc", 1, 2);
        message0.setFrom(new InternetAddress("from2@localhost"));
        message0.setFlag(Flags.Flag.ANSWERED, true);
        message0.setFlags(flags, true);
        folder.store(message0);

        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("#1 test0Search test1Search \u00c4\u00e4\u03A0", "UTF-8");
        message1.setText("content#1 \u00c4\u00e4\u03A0", "UTF-8");
        setRecipients(message1, Message.RecipientType.TO, "to", 1, 3);
        setRecipients(message1, Message.RecipientType.CC, "cc", 1, 3);
        setRecipients(message1, Message.RecipientType.BCC, "bcc", 1, 3);
        message1.setFrom(new InternetAddress("from3@localhost"));
        message1.setFlag(Flags.Flag.ANSWERED, false);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("#2 OR search : foo");
        message2.setText("content#2 foo");
        setRecipients(message2, Message.RecipientType.TO, "to", 3);
        setRecipients(message2, Message.RecipientType.CC, "cc", 4);
        setRecipients(message2, Message.RecipientType.BCC, "bcc", 5);
        message2.setFrom(new InternetAddress("from4@localhost"));
        message2.setFlag(Flags.Flag.ANSWERED, false);
        message2.setFlags(flags, false);
        folder.store(message2);

        MimeMessage message3 = new MimeMessage(session);
        message3.setSubject("#3 OR search : bar");
        message3.setText("content#3 bar");
        setRecipients(message3, Message.RecipientType.TO, "to", 3);
        setRecipients(message3, Message.RecipientType.CC, "cc", 4);
        setRecipients(message3, Message.RecipientType.BCC, "bcc", 5);
        message3.setFrom(new InternetAddress("from5@localhost"));
        message3.setFlag(Flags.Flag.ANSWERED, false);
        message3.setFlags(flags, false);
        folder.store(message3);

        MimeMessage message4 = new MimeMessage(session);
        message4.setSubject("#4 with received date");
        message4.setText("content#4 received date");
        folder.appendMessage(message4, new Flags(), getSampleDate());

        MimeMessage message5 = new MimeMessage(session);
        message5.setSubject("#5 with sent date");
        message5.setText("content#5 sent date");
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
