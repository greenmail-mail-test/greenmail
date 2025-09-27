package com.icegreen.greenmail.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.*;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if senders and recipients of received messages are set correctly
 * and if messages are received by the correct receivers.
 */
public class SenderRecipientTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

    private static final InternetAddress[] TO_ADDRESSES = new InternetAddress[]{
        internetAddress("to1@localhost", "To 1"),
        internetAddress("to2@localhost", "To 2")
    };
    private static final InternetAddress[] CC_ADDRESSES = new InternetAddress[]{
        internetAddress("cc1@localhost", "Cc 1"),
        internetAddress("cc2@localhost", "Cc 2")
    };
    private static final InternetAddress[] BCC_ADDRESSES = new InternetAddress[]{
        internetAddress("bcc1@localhost", "Bcc 1"),
        internetAddress("bcc2@localhost", "Bcc 2")
    };
    private static final InternetAddress FROM_ADDRESS = internetAddress("from@localhost", "From");

    private static InternetAddress internetAddress(String address, String personal) {
        try {
            return new InternetAddress(address, personal);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Can not create internet address of address  part <"
                + address + "> and personal part <" + personal + ">", e);
        }
    }

    @Test
    public void testSendersAndRecipients() throws MessagingException {
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
        assertThat(greenMail.waitForIncomingEmail(5000,
                TO_ADDRESSES.length + CC_ADDRESSES.length + BCC_ADDRESSES.length)).isTrue();

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
    public void testSendWithoutSubject() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
                null, "some subject less body");
        assertThat(greenMail.getReceivedMessages()[0].getContent()).isEqualTo("some subject less body");
    }

    @Test
    public void testSendAndReceiveWithQuotedAddress() throws MessagingException, IOException {
        // See https://en.wikipedia.org/wiki/Email_address#Local-part
        String[] toList = {
            "\"John..Doe\"@localhost",
            "abc.'defghi'.xyz@localhost",
            "abc.\"defghi\".xyz@localhost", // Requires strict=false
             "\"abcdefghixyz\"@localhost",
             "\"Foo Bar\"admin@localhost"   // Requires strict=false
        };
        Properties properties = new Properties();
        properties.setProperty("mail.mime.address.strict", "false");

        for(String to: toList) {
            greenMail.setUser(to, "pwd");
            InternetAddress[] toAddress = InternetAddress.parse(to);
            String from = to; // Same from and to address for testing correct escaping of both

            final String subject = "testSendAndReceiveWithQuotedAddress";
            final String content = "some body";
            GreenMailUtil.sendTextEmailTest(to, from,
                    subject, content);

            assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

            final IMAPStore store = (IMAPStore) greenMail.getImap().createSession(properties).getStore();
            store.connect(to, "pwd");
            try {
                IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                Message[] msgs = folder.getMessages();
                assertThat(null != msgs && msgs.length == 1).isTrue();
                final Message msg = msgs[0];
                assertThat(((InternetAddress)msg.getRecipients(Message.RecipientType.TO)[0]).getAddress()).isEqualTo(to);
                assertThat(((InternetAddress)msg.getFrom()[0]).getAddress()).isEqualTo(from);
                assertThat(msg.getSubject()).isEqualTo(subject);
                assertThat(msg.getContent()).hasToString(content);
                assertThat(msg.getRecipients(Message.RecipientType.TO)).isEqualTo(toAddress);
            } catch (MessagingException e) {
                throw new IllegalStateException("Can not fetch message for recipient <" + to + ">", e);
            }finally {
                store.close();
            }
        }
    }

    /**
     * Retrieve mail through IMAP and POP3 and check sender and receivers
     *
     * @param greenMail Greenmail instance to read from
     * @param addr      Address of account to retrieve
     */
    private void retrieveAndCheck(GreenMailRule greenMail, InternetAddress addr) throws MessagingException {
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
    private void retrieveAndCheck(AbstractServer server, String login) throws MessagingException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(login);
            assertThat(messages).hasSize(1);
            Message message = messages[0];

            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.TO))).isEqualTo(TO_ADDRESSES);
            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.CC))).isEqualTo(CC_ADDRESSES);
            // BCC addresses are not contained in the message since other receivers are not allowed to know the list of
            // BCC recipients
            assertThat(toInetAddr(message.getRecipients(Message.RecipientType.BCC))).isNull();
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
