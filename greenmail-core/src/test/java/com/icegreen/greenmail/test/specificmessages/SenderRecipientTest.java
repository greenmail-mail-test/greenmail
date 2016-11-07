package com.icegreen.greenmail.test.specificmessages;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.util.UserUtil;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests if senders and recipients of received messages are set correctly and if messages are received by the correct
 * receivers.
 */
public class SenderRecipientTest {
    @Rule
    public GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP_POP3_IMAP);

    private static final InternetAddress[] TO_ADDRESSES;
    private static final InternetAddress[] CC_ADDRESSES;
    private static final InternetAddress[] BCC_ADDRESSES;

    private static final InternetAddress FROM_ADDRESS;

    static {
        try {
            TO_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("to1@localhost", "To 1"),
                    new InternetAddress("to2@localhost", "To 2")
            };
            CC_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("cc1@localhost", "Cc 1"),
                    new InternetAddress("cc2@localhost", "Cc 2")
            };
            BCC_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("bcc1@localhost", "Bcc 1"),
                    new InternetAddress("bcc2@localhost", "Bcc 2")
            };
            FROM_ADDRESS = new InternetAddress("from@localhost", "From");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testSendersAndRecipients() throws MessagingException, IOException {
        UserUtil.createUsers(greenMail, TO_ADDRESSES);
        UserUtil.createUsers(greenMail, CC_ADDRESSES);
        UserUtil.createUsers(greenMail, BCC_ADDRESSES);

        MimeMessage msg = new MimeMessage(greenMail.getSmtp().createSession());
        msg.setRecipients(Message.RecipientType.TO, TO_ADDRESSES);
        msg.setRecipients(Message.RecipientType.CC, CC_ADDRESSES);
        msg.setRecipients(Message.RecipientType.BCC, BCC_ADDRESSES);
        msg.setFrom(FROM_ADDRESS);
        msg.setSubject("Subject");
        msg.setText("text");

        GreenMailUtil.sendMimeMessage(msg);
        greenMail.waitForIncomingEmail(5000, 1);

        for (InternetAddress address : TO_ADDRESSES) {
            retrieveAndCheck(greenMail, address);
        }
        for (InternetAddress address : CC_ADDRESSES) {
            retrieveAndCheck(greenMail, address);
        }
        for (InternetAddress address : BCC_ADDRESSES) {
            retrieveAndCheck(greenMail, address);
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testSendWithoutSubject() throws MessagingException {
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com",
                null, "some subjectless body");
        assertEquals("some subjectless body", GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
    }

    /**
     * Retrieve mail through IMAP and POP3 and check sender and receivers
     *
     * @param greenMail Greenmail instance to read from
     * @param addr      Address of account to retrieve
     */
    private void retrieveAndCheck(GreenMailRuleWithStoreChooser greenMail, InternetAddress addr) throws IOException, MessagingException {
        String address = addr.getAddress();
        retrieveAndCheck(greenMail.getPop3(), address);
        retrieveAndCheck(greenMail.getImap(), address);
    }

    /**
     * Retrieve message from retriever and check the sender and receivers
     *
     * @param server Server to read from
     * @param login  Account to retrieve
     */
    private void retrieveAndCheck(AbstractServer server, String login) throws MessagingException, IOException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(login);
            assertEquals(1, messages.length);
            Message message = messages[0];

            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.TO)), is(TO_ADDRESSES));
            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.CC)), is(CC_ADDRESSES));
            // BCC addresses are not contained in the message since other receivers are not allowed to know the list of
            // BCC recipients
            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.BCC)), nullValue());
        }
    }

    /**
     * Cast input addresses from Address to the more specific InternetAddress
     *
     * @param addrs Input addresses
     * @return Output
     */
    private InternetAddress[] toInetAddr(Address[] addrs) {
        if (addrs == null) {
            return null;
        }
        final InternetAddress[] out = new InternetAddress[addrs.length];
        for (int i = 0; i < addrs.length; i++) {
            out[i] = (InternetAddress) addrs[i];
        }
        return out;
    }


}
