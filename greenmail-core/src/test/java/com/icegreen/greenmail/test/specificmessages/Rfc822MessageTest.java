package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(msg.getContentType().startsWith("multipart/mixed")).isTrue();
            Multipart multipartReceived = (Multipart) msg.getContent();
            assertThat(multipartReceived.getContentType().startsWith("multipart/mixed")).isTrue();
            MimeBodyPart mimeBodyPartReceived = (MimeBodyPart) multipartReceived.getBodyPart(0);
            assertThat(mimeBodyPartReceived.getContentType().toLowerCase().startsWith("message/rfc822")).isTrue();

            MimeMessage msgAttached = (MimeMessage) mimeBodyPartReceived.getContent();
            assertThat(msgAttached.getContentType().toLowerCase()).startsWith("text/plain");
            assertThat(msgAttached.getRecipients(Message.RecipientType.TO)).isEqualTo(msgToBeForwarded.getRecipients(Message.RecipientType.TO));
            assertThat(msgAttached.getFrom()).isEqualTo(msgToBeForwarded.getFrom());
            assertThat(msgAttached.getSubject()).isEqualTo(msgToBeForwarded.getSubject());
            assertThat(msgAttached.getContent()).isEqualTo(msgToBeForwarded.getContent());
        } finally {
            store.close();
        }
    }
}
