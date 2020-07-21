package com.icegreen.greenmail.test.specificmessages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.mail.*;
import javax.mail.internet.*;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for encoding scenarios.
 */
public class EncodingTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    /**
     * Structure of test message and content type:
     * <p>
     * Message (multipart/alternative)
     * \--> Message (text/plain)
     */
    @Test
    public void testTextPlainWithUTF8() throws MessagingException, IOException {
        greenMail.setUser("foo@localhost", "pwd");
        final Session session = greenMail.getSmtp().createSession();

        MimeMultipart multipart = new MimeMultipart("alternative");

        MimeBodyPart textQP = new MimeBodyPart();
        textQP.setContent("QP Content with umlaut \u00FC", "text/javascript; charset=utf-8");
        textQP.setHeader("Content-Transfer-Encoding", "QUOTED-PRINTABLE");
        multipart.addBodyPart(textQP);

        MimeBodyPart html = new MimeBodyPart();
        html.setContent(MimeUtility.encodeText("<!doctype html>" +
                "<html lang=en>" +
                "<head>" +
                "<meta charset=utf-8>" +
                "<title>Title with Umlaut \u00FC</title>" +
                "</head>" +
                "<body>" +
                "<p>8BIT Content with umlaut ü</p>" +
                "</body>" +
                "</html>", "UTF-8", "B"), "text/html; charset=utf-8");
        html.setHeader("Content-Transfer-Encoding", "8BIT");
        multipart.addBodyPart(html);

        MimeBodyPart text = new MimeBodyPart();
        text.setText(MimeUtility.encodeText("8BIT Content with umlaut \u00FC", "UTF-8", "B"), "utf-8");
        text.setHeader("Content-Transfer-Encoding", "8BIT");
        multipart.addBodyPart(text);

        MimeBodyPart text2QP = new MimeBodyPart();
        text2QP.setText(MimeUtility.encodeText("8BIT Content with umlaut \u00FC", "UTF-8", "Q"), "utf-8");
        text2QP.setHeader("Content-Transfer-Encoding", "8BIT");
        multipart.addBodyPart(text2QP);


        // New main message, containing body part
        MimeMessage message = new MimeMessage(session);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("foo@localhost"));
        message.setSubject("Subject ä", "UTF-8");
        message.setFrom(new InternetAddress("foo@localhost"));
        message.setContent(multipart);
        message.saveChanges();

        GreenMailUtil.sendMimeMessage(message);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);
            Message[] messages = inboxFolder.getMessages();
            MimeMessage msg = (MimeMessage) messages[0];
            message.writeTo(new FileOutputStream(new File("t.eml")));
            assertThat(msg.getContentType().startsWith("multipart/alternative")).isTrue();
            Multipart multipartReceived = (Multipart) msg.getContent();

            assertThat(multipartReceived.getContentType().startsWith("multipart/alternative")).isTrue();

            // QP-encoded
            final BodyPart bodyPart0 = multipartReceived.getBodyPart(0);
            assertThat(bodyPart0.getContentType()).isEqualTo("TEXT/JAVASCRIPT; charset=utf-8");
            assertThat(textQP.getContent()).isEqualTo(EncodingUtil.toString((InputStream) bodyPart0.getContent(), StandardCharsets.UTF_8));

            // 8-BIT-encoded
            final BodyPart bodyPart1 = multipartReceived.getBodyPart(1);
            assertThat(bodyPart1.getContentType()).isEqualTo("TEXT/HTML; charset=utf-8");
            assertThat(bodyPart1.getContent()).isEqualTo(html.getContent()); // Fails

            final BodyPart bodyPart2 = multipartReceived.getBodyPart(2);
            assertThat(bodyPart2.getContentType()).isEqualTo("TEXT/PLAIN; charset=utf-8");
            assertThat(bodyPart2.getContent()).isEqualTo(text.getContent());

            final BodyPart bodyPart3 = multipartReceived.getBodyPart(3);
            assertThat(bodyPart3.getContentType()).isEqualTo("TEXT/PLAIN; charset=utf-8");
            assertThat(bodyPart3.getContent()).isEqualTo(text2QP.getContent());
        } finally {
            store.close();
        }
    }
}
