package com.icegreen.greenmail.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.Rule;
import org.junit.Test;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests escaping of message parts
 */
public class EscapingTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    public void testEscapeSubject() throws MessagingException {
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
    public void testEscapeMessageID() throws MessagingException {
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
            throws MessagingException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).hasSize(1);
            Message message = messages[0];

            // Message subject
            assertThat(message.getSubject()).isEqualTo(subject);
            assertThat(message.getAllRecipients()[0]).hasToString(to);
            assertThat(message.getFrom()[0]).hasToString(from);
        }
    }

    /* Utility class that extends MimeMessage forcing every message-id to contain characters that need to be properly escaped
     * Javamail by default will set the message-id when the MimeMessage is being processed.
     * See http://www.oracle.com/technetwork/java/faq-135477.html#msgid for more details
     */
    static class GreenMailMimeMessage extends MimeMessage {

        public GreenMailMimeMessage(Session session) {
            super(session);
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            String messageID = "<11111.22222.3333.JavaMail.\"foo.bar\\domain\"@localhost>";
            setHeader("Message-ID", messageID);
        }

    }
}
