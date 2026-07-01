package com.icegreen.greenmail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the discard-attachments option strips attachment MIME parts
 * before storing a message, while preserving text body parts.
 */
public class DiscardAttachmentsTest {

    private static final String TO = "to@example.com";
    private static final String FROM = "from@example.com";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP)
            .withConfiguration(new GreenMailConfiguration().withDiscardAttachments());

    @Test
    public void attachmentIsDiscarded() throws MessagingException, IOException {
        sendMessageWithAttachment("body text", "attachment.txt", "attachment content");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);

        MimeMessage stored = received[0];
        assertThat(stored.getContent()).isInstanceOf(Multipart.class);

        Multipart multipart = (Multipart) stored.getContent();
        // Only the text body part should remain; the attachment part must be gone
        assertThat(multipart.getCount()).isEqualTo(1);
        assertThat(multipart.getBodyPart(0).getContentType()).startsWith("text/plain");
        assertThat(multipart.getBodyPart(0).getContent()).isEqualTo("body text");
    }

    @Test
    public void bodyTextIsPreserved() throws MessagingException, IOException {
        sendMessageWithAttachment("hello world", "report.pdf", "PDF bytes");

        MimeMessage stored = greenMail.getReceivedMessages()[0];
        Multipart multipart = (Multipart) stored.getContent();
        assertThat(multipart.getBodyPart(0).getContent()).isEqualTo("hello world");
    }

    @Test
    public void plainTextMessageWithoutAttachmentIsUnaffected() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest(TO, FROM, "subject", "plain body");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        // Plain text messages have no Multipart content; content must pass through unchanged
        assertThat(received[0].getContent().toString().trim()).isEqualTo("plain body");
    }

    @Test
    public void multipleAttachmentsAreAllDiscarded() throws MessagingException, IOException {
        Session session = greenMail.getSmtp().createSession();
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        message.setSubject("multi-attachment");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("body");

        MimeBodyPart att1 = new MimeBodyPart();
        att1.setContent("data1", "application/octet-stream");
        att1.setDisposition(Part.ATTACHMENT);
        att1.setFileName("file1.bin");

        MimeBodyPart att2 = new MimeBodyPart();
        att2.setContent("data2", "application/pdf");
        att2.setDisposition(Part.ATTACHMENT);
        att2.setFileName("file2.pdf");

        MimeMultipart mp = new MimeMultipart();
        mp.addBodyPart(textPart);
        mp.addBodyPart(att1);
        mp.addBodyPart(att2);
        message.setContent(mp);

        Transport.send(message);

        MimeMessage stored = greenMail.getReceivedMessages()[0];
        Multipart multipart = (Multipart) stored.getContent();
        assertThat(multipart.getCount()).isEqualTo(1);
        assertThat(multipart.getBodyPart(0).getContent()).isEqualTo("body");
    }

    private void sendMessageWithAttachment(String bodyText, String fileName, String fileContent)
            throws MessagingException {
        Session session = greenMail.getSmtp().createSession();
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        message.setSubject("test with attachment");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setContent(fileContent, "application/octet-stream");
        attachmentPart.setDisposition(Part.ATTACHMENT);
        attachmentPart.setFileName(fileName);

        MimeMultipart mp = new MimeMultipart();
        mp.addBodyPart(textPart);
        mp.addBodyPart(attachmentPart);
        message.setContent(mp);

        Transport.send(message);
    }
}
