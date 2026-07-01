package com.icegreen.greenmail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that without the discard-attachments option, attachments are stored normally.
 * Kept in a separate class so it can manage its own GreenMail lifecycle without
 * conflicting with port assignments from other test rules.
 */
public class DiscardAttachmentsDefaultBehaviorTest {

    private static final String TO = "to@example.com";
    private static final String FROM = "from@example.com";

    @Test
    public void withoutOptionAttachmentIsStored() throws MessagingException, IOException {
        GreenMail plain = new GreenMail(ServerSetupTest.SMTP);
        plain.start();
        try {
            Session session = plain.getSmtp().createSession();
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
            message.setSubject("no-discard");

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("body");

            MimeBodyPart att = new MimeBodyPart();
            att.setContent("data", "application/octet-stream");
            att.setDisposition(Part.ATTACHMENT);
            att.setFileName("file.bin");

            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(textPart);
            mp.addBodyPart(att);
            message.setContent(mp);

            Transport.send(message);

            MimeMessage stored = plain.getReceivedMessages()[0];
            Multipart multipart = (Multipart) stored.getContent();
            assertThat(multipart.getCount()).isEqualTo(2);
        } finally {
            plain.stop();
        }
    }
}
