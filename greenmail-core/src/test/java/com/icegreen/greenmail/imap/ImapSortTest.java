package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.SortTerm;
import org.junit.Rule;
import org.junit.Test;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
public class ImapSortTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    @Test
    public void testSort() throws Exception {
        GreenMailUser user = greenMail.setUser("to1@localhost", "pwd");
        assertThat(greenMail.getImap()).isNotNull();

        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");
        Flags fooFlags = new Flags();
        fooFlags.add("foo");
        storeSortTestMessages(greenMail.getImap().createSession(), folder, fooFlags);

        greenMail.waitForIncomingEmail(2);

        final Store store = greenMail.getImap().createStore();
        store.connect("to1@localhost", "pwd");
        try {
            IMAPFolder imapFolder = (IMAPFolder) store.getFolder("INBOX");
            imapFolder.open(Folder.READ_WRITE);

            Message[] imapMessages = imapFolder.getMessages();
            assertThat(null != imapMessages && imapMessages.length == 2).isTrue();
            Message m0 = imapMessages[0];
            Message m1 = imapMessages[1];
            assertThat(m0.getFlags().contains(Flags.Flag.ANSWERED)).isTrue();

            imapMessages = imapFolder.getSortedMessages(new SortTerm[]{SortTerm.TO});
            assertThat(imapMessages).hasSize(2);
            assertThat(m0).isSameAs(imapMessages[0]);
            assertThat(m1).isSameAs(imapMessages[1]);

            imapMessages = imapFolder.getSortedMessages(new SortTerm[]{SortTerm.REVERSE, SortTerm.TO});
            assertThat(imapMessages).hasSize(2);
            assertThat(m1).isSameAs(imapMessages[0]);
            assertThat(m0).isSameAs(imapMessages[1]);

            imapMessages = imapFolder.getSortedMessages(new SortTerm[]{SortTerm.TO}, new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
            assertThat(imapMessages).hasSize(1);
            assertThat(m0).isSameAs(imapMessages[0]);

        } finally {
            store.close();
        }
    }

    @Test
    public void testSortBySubjectBaseSubject() throws Exception {
        GreenMailUser user = greenMail.setUser("to2@localhost", "pwd");
        MailFolder folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");
        Session session = greenMail.getImap().createSession();

        MimeMessage msgA = new MimeMessage(session);
        msgA.setSubject("Subject A");
        msgA.setText("content");
        folder.store(msgA);

        MimeMessage msgB = new MimeMessage(session);
        msgB.setSubject("Re: Subject B");
        msgB.setText("content");
        folder.store(msgB);

        MimeMessage msgC = new MimeMessage(session);
        msgC.setSubject("[fwd: Subject C]");
        msgC.setText("content");
        folder.store(msgC);

        MimeMessage msgD = new MimeMessage(session);
        msgD.setSubject("Subject D (fwd)");
        msgD.setText("content");
        folder.store(msgD);

        MimeMessage msgE = new MimeMessage(session);
        msgE.setSubject("[list] Fwd: Subject E");
        msgE.setText("content");
        folder.store(msgE);

        greenMail.waitForIncomingEmail(5);

        final Store store = greenMail.getImap().createStore();
        store.connect("to2@localhost", "pwd");
        try {
            IMAPFolder imapFolder = (IMAPFolder) store.getFolder("INBOX");
            imapFolder.open(Folder.READ_WRITE);

            Message[] imapMessages = imapFolder.getSortedMessages(new SortTerm[]{SortTerm.SUBJECT});
            assertThat(imapMessages).hasSize(5);

            assertThat(imapMessages[0].getSubject()).isEqualTo("Subject A");
            assertThat(imapMessages[1].getSubject()).isEqualTo("Re: Subject B");
            assertThat(imapMessages[2].getSubject()).isEqualTo("[fwd: Subject C]");
            assertThat(imapMessages[3].getSubject()).isEqualTo("Subject D (fwd)");
            assertThat(imapMessages[4].getSubject()).isEqualTo("[list] Fwd: Subject E");
        } finally {
            store.close();
        }
    }

    /**
     * Create the two messages with different recipients, etc. for testing and add them to the folder.
     *
     * @param session Session to set on the messages
     * @param folder Folder to add to
     * @param flags Flags to set on both messages
     */
    private void storeSortTestMessages(Session session, MailFolder folder, Flags flags) throws Exception {
        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("Re: testB");
        message1.setText("content");

        int[] message1RecipientsSuffixes = new int[]{1, 2};
        setRecipients(message1, Message.RecipientType.TO, "to", message1RecipientsSuffixes);
        setRecipients(message1, Message.RecipientType.CC, "cc", message1RecipientsSuffixes);
        setRecipients(message1, Message.RecipientType.BCC, "bcc", message1RecipientsSuffixes);
        message1.setFrom(new InternetAddress("from2@localhost"));
        message1.setFlag(Flags.Flag.ANSWERED, true);
        message1.setFlags(flags, true);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("testA");
        message2.setText("content");
        int[] message2RecipientsSuffixes = new int[]{2, 3};
        setRecipients(message2, Message.RecipientType.TO, "to", message2RecipientsSuffixes);
        setRecipients(message2, Message.RecipientType.CC, "cc", message2RecipientsSuffixes);
        setRecipients(message2, Message.RecipientType.BCC, "bcc", message2RecipientsSuffixes);
        message2.setFrom(new InternetAddress("from3@localhost"));
        message2.setFlag(Flags.Flag.ANSWERED, false);
        folder.store(message2);
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
