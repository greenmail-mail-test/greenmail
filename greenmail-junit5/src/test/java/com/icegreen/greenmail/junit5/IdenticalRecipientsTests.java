package com.icegreen.greenmail.junit5;


import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Date;

import static com.icegreen.greenmail.util.GreenMailUtil.getSession;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for sending emails with identical recipients.
 * <p>
 * This class contains tests to verify the behavior of sending emails with identical recipients in the TO, CC, and BCC
 * fields. It ensures that the email server handles these cases correctly and that only one email is received by the
 * recipient.
 */
public class IdenticalRecipientsTests {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
        .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser("test@localhost.com", "login-id", "password"));

    /**
     * Tests sending an email to one destination with multiple of the same BCC recipients and verifies the received
     * message including only one email is received.
     *
     * @throws MessagingException If there is an error in message creation or sending.
     */
    @Test
    @DisplayName("Send test to with identical recipients with one CC")
    public void testSendingEmailWithOneCC() throws MessagingException, IOException {
        Session session = getSession(ServerSetupTest.SMTP);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSentDate(new Date());
        mimeMessage.setSubject("Subject");
        mimeMessage.setText("Body");
        mimeMessage.setFrom("test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.TO, "test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.CC, "test@localhost.com");

        GreenMailUtil.sendMimeMessage(mimeMessage, "login-id", "password");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage message = messages[0];
        assertEquals("Subject", message.getSubject());
        assertEquals("Body", message.getContent());

        assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.TO)[0].toString());

        assertEquals(1, message.getRecipients(Message.RecipientType.CC).length);
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.CC)[0].toString());
    }

    /**
     * Tests sending an email to one destination with multiple of the same CC recipients and verifies the received
     * message including only one email is received.
     *
     * @throws MessagingException If there is an error in message creation or sending.
     */
    @Test
    @DisplayName("Send test to with identical recipients with multiple CCs")
    public void testReceivingEmailsWithMultipleCCs() throws MessagingException, IOException {
        Session session = getSession(ServerSetupTest.SMTP);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSentDate(new Date());
        mimeMessage.setSubject("Subject");
        mimeMessage.setText("Body");
        mimeMessage.setFrom("test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.TO, "test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.CC,
            "test@localhost.com,test@localhost.com,test@localhost.com"
        );

        GreenMailUtil.sendMimeMessage(mimeMessage, "login-id", "password");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage message = messages[0];
        assertEquals("Subject", message.getSubject());
        assertEquals("Body", message.getContent());

        assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.TO)[0].toString());

        assertEquals(3, message.getRecipients(Message.RecipientType.CC).length);
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.CC)[0].toString());
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.CC)[1].toString());
        assertEquals("test@localhost.com", message.getRecipients(Message.RecipientType.CC)[2].toString());
    }

    /**
     * Tests sending an email to one destination with one of the same BCC recipients and verifies the received
     * message including only one email is received.
     *
     * @throws MessagingException If there is an error in message creation or sending.
     */
    @Test
    @DisplayName("Send test to identical recipients with one BCC")
    public void testReceivingEmailsWithOneBCC() throws MessagingException {
        Session session = getSession(ServerSetupTest.SMTP);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSentDate(new Date());
        mimeMessage.setSubject("Subject");
        mimeMessage.setText("Body");
        mimeMessage.setFrom("test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.TO, "test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.BCC, "test@localhost.com");

        GreenMailUtil.sendMimeMessage(mimeMessage, "login-id", "password");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
    }

    /**
     * Tests sending an email to one destination with multiple of the same BCC recipients and verifies the received
     * message including only one email is received.
     *
     * @throws MessagingException If there is an error in message creation or sending.
     */
    @Test
    @DisplayName("Send test to identical recipients with multiple BCCs")
    public void testReceivingEmailsWithMultipleBCC() throws MessagingException {
        Session session = getSession(ServerSetupTest.SMTP);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSentDate(new Date());
        mimeMessage.setSubject("Subject");
        mimeMessage.setText("Body");
        mimeMessage.setFrom("test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.TO, "test@localhost.com");
        mimeMessage.setRecipients(Message.RecipientType.BCC, "test@localhost.com,test@localhost.com,test@localhost.com");

        GreenMailUtil.sendMimeMessage(mimeMessage, "login-id", "password");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
    }
}

