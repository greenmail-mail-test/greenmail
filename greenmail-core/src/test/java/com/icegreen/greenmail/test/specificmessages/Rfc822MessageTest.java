package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests forwarding an email.
 * <p>
 * See https://github.com/greenmail-mail-test/greenmail/issues/142
 * See http://www.oracle.com/technetwork/java/faq-135477.html#forward
 */
public class Rfc822MessageTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    /**
     * Structure of test message and content type:
     * <p>
     * Message (multipart/mixed)
     * \--> MultiPart (multipart/mixed)
     * \--> MimeBodyPart (message/rfc822)
     * \--> Message (text/plain)
     */
    @Test
    public void testForwardWithRfc822() throws MessagingException, IOException {
        greenMail.setUser("foo@localhost", "pwd");
        final Session session = greenMail.getSmtp().createSession();

        // Message for forwarding
        Message msgToBeForwarded = GreenMailUtil.createTextEmail(
                "foo@localhost", "foo@localhost", "test newMessageWithForward", "forwarded mail content",
                greenMail.getSmtp().getServerSetup());


        // Create body part containing forwarded message
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(msgToBeForwarded, "message/rfc822");

        // Add message body part to multi part
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // New main message, containing body part
        MimeMessage newMessageWithForward = new MimeMessage(session);
        newMessageWithForward.setRecipient(Message.RecipientType.TO, new InternetAddress("foo@localhost"));
        newMessageWithForward.setSubject("Fwd: " + "test");
        newMessageWithForward.setFrom(new InternetAddress("foo@localhost"));
        newMessageWithForward.setContent(multipart);   //Save changes in newMessageWithForward message
        newMessageWithForward.saveChanges();

        GreenMailUtil.sendMimeMessage(newMessageWithForward);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);
            Message[] messages = inboxFolder.getMessages();
            MimeMessage msg = (MimeMessage) messages[0];
            assertTrue(msg.getContentType().startsWith("multipart/mixed"));
            Multipart multipartReceived = (Multipart) msg.getContent();
            assertTrue(multipartReceived.getContentType().startsWith("multipart/mixed"));
            MimeBodyPart mimeBodyPartReceived = (MimeBodyPart) multipartReceived.getBodyPart(0);
            assertTrue(mimeBodyPartReceived.getContentType().toLowerCase().startsWith("message/rfc822"));

            MimeMessage msgAttached = (MimeMessage) mimeBodyPartReceived.getContent();
            assertThat(msgAttached.getContentType().toLowerCase(), startsWith("text/plain"));
            assertArrayEquals(msgToBeForwarded.getRecipients(Message.RecipientType.TO), msgAttached.getRecipients(Message.RecipientType.TO));
            assertArrayEquals(msgToBeForwarded.getFrom(), msgAttached.getFrom());
            assertEquals(msgToBeForwarded.getSubject(), msgAttached.getSubject());
            assertEquals(msgToBeForwarded.getContent(), msgAttached.getContent());
        } finally {
            store.close();
        }
    }
}
