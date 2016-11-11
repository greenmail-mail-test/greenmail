package com.icegreen.greenmail.test.specificmessages;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.test.util.GreenMailMimeMessage;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests escaping of message parts
 */
public class EscapingTest {
    @Rule
    public GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    @StoreChooser(store="file,memory")
    public void testEscapeSubject() throws MessagingException, IOException {
        String to = "to@localhost";
        String subject = "Subject?<>/|\\\\.%\\\"*?:{[]}!";
        greenMail.setUser(to, to);
        final String from = "from@localhost";
        GreenMailUtil.sendTextEmail(to, from, subject, "msg", greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(greenMail.getPop3(), to, from, subject);
        retrieveAndCheck(greenMail.getImap(), to, from, subject);
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testEscapeMessageID() throws MessagingException, IOException {
        String to = "foo@localhost";
        String from = "bar`bar <bar@localhost>";
        String subject = "Bad IMAP Envelope";
        String body = "Example text";
        greenMail.setUser(to, to);

        Session smtpSession = greenMail.getSmtp().createSession();
        GreenMailMimeMessage mimeMessage = new GreenMailMimeMessage(smtpSession);

        mimeMessage.setRecipients(Message.RecipientType.TO, to);
        mimeMessage.setFrom(from);
        mimeMessage.setSubject(subject);
        mimeMessage.setText(body);

        GreenMailUtil.sendMimeMessage(mimeMessage);
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(greenMail.getImap(), to, from, subject);
    }

    /**
     * Retrieve message from retriever and check content
     *
     * @param server  Server to read from
     * @param to      Account to retrieve
     * @param subject Subject of message
     */
    private void retrieveAndCheck(AbstractServer server, String to, String from, String subject)
            throws MessagingException, IOException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(1, messages.length);
            Message message = messages[0];

            // Message subject
            assertThat(message.getSubject(), is(subject));
            assertThat(message.getAllRecipients()[0].toString(), is(to));
            assertThat(message.getFrom()[0].toString(), is(from));
        }
    }
}
