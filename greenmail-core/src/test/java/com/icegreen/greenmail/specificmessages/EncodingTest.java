package com.icegreen.greenmail.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
            assertThat(msg.getContentType()).startsWith("multipart/alternative");
            Multipart multipartReceived = (Multipart) msg.getContent();

            assertThat(multipartReceived.getContentType()).startsWith("multipart/alternative");

            // QP-encoded
            final BodyPart bodyPart0 = multipartReceived.getBodyPart(0);
            assertThat(bodyPart0.getContentType()).isEqualTo("text/javascript; charset=utf-8");
            assertThat(textQP.getContent()).isEqualTo(EncodingUtil.toString((InputStream) bodyPart0.getContent(), StandardCharsets.UTF_8));

            // 8-BIT-encoded
            final BodyPart bodyPart1 = multipartReceived.getBodyPart(1);
            assertThat(bodyPart1.getContentType()).isEqualTo("text/html; charset=utf-8");
            assertThat(bodyPart1.getContent()).isEqualTo(html.getContent()); // Fails

            final BodyPart bodyPart2 = multipartReceived.getBodyPart(2);
            assertThat(bodyPart2.getContentType()).isEqualTo("text/plain; charset=utf-8");
            assertThat(bodyPart2.getContent()).isEqualTo(text.getContent());

            final BodyPart bodyPart3 = multipartReceived.getBodyPart(3);
            assertThat(bodyPart3.getContentType()).isEqualTo("text/plain; charset=utf-8");
            assertThat(bodyPart3.getContent()).isEqualTo(text2QP.getContent());
        } finally {
            store.close();
        }
    }

    @Test
    public void testTextPlainWithUTF8AndGreenMailApi() throws MessagingException, IOException {
        String content = "This is a test with ünicöde: \uD83C\uDF36";
        String subject = "Some sübject";

        GreenMailUtil.sendTextEmailTest(
            "to@localhost", "from@localhost", subject,
            content
        );

        MimeMessage msg = greenMail.getReceivedMessages()[0];
        assertThat(msg.getSubject()).isEqualTo(subject);
        assertThat(msg.getContentType()).isEqualTo("text/plain; charset=UTF-8");
        assertThat(msg.getContent()).isEqualTo(content);
    }

    @Test(timeout = 10000)
    public void testTextPlainWithUTF8SenderAndReceiverAndGreenMailApi() throws MessagingException, IOException {
        // this intermediate `InternetAddress` is needed because of:
        // https://stackoverflow.com/questions/31859901/java-mail-sender-address-gets-non-ascii-chars-removed/31865820#31865820
        InternetAddress address = new InternetAddress("\"кирилица\" <to@localhost>");
        InternetAddress toAddress = new InternetAddress(address.getAddress(), address.getPersonal());
        address = new InternetAddress("\"кирилица\" <from@localhost>");
        InternetAddress fromAddress = new InternetAddress(address.getAddress(), address.getPersonal());

        sendMessage(fromAddress, toAddress);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getFrom()[0]).hasToString(fromAddress.toString());
        assertThat(Arrays.stream(receivedMessage.getAllRecipients()).map(Object::toString).toArray())
            .isEqualTo(new String[]{toAddress.toString()});

        greenMail.setUser("to@localhost", "pwd");

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("to@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            Message[] messages = new Message[]{null};
            MessageCountListener listener = new MessageCountListener() {
                @Override
                public void messagesRemoved(MessageCountEvent e) {
                }

                @Override
                public void messagesAdded(MessageCountEvent e) {
                    messages[0] = e.getMessages()[0];
                }
            };
            inboxFolder.addMessageCountListener(listener);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // Ignore
                }
                try {
                    sendMessage(fromAddress, toAddress);
                } catch (MessagingException ex) {
                    assertThat(false).isTrue();
                }
            }).start();
            ((IMAPFolder) inboxFolder).idle(true);

            assertThat(messages[0].getFrom()[0]).hasToString(fromAddress.toString());
            assertThat(Arrays.stream(messages[0].getAllRecipients()).map(Object::toString).toArray())
                .isEqualTo(new String[]{toAddress.toString()});

            inboxFolder.close();
        } finally {
            store.close();
        }
    }

    @Test
    public void testAttachmentWithUTF8NameAndGreenMailApi() throws MessagingException, IOException {
        System.setProperty("mail.mime.decodefilename", "true");

        greenMail.setUser("to@localhost", "pwd");
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("to@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            Message[] messages = new Message[] { null };
            MessageCountListener listener = new MessageCountListener() {
                @Override
                public void messagesRemoved(MessageCountEvent e) {
                }

                @Override
                public void messagesAdded(MessageCountEvent e) {
                    messages[0] = e.getMessages()[0];
                }
            };
            inboxFolder.addMessageCountListener(listener);
            String fileName = "кирилица testimage_ünicöde_\uD83C\uDF36";
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // Ignore
                }
                try {
                    GreenMailUtil.sendAttachmentEmail(
                        "to@localhost", "from@localhost", "subject", "body",
                        new byte[]{0, 1, 2}, "image/gif", MimeUtility.encodeText(fileName),
                        "testimage_description", greenMail.getSmtp().getServerSetup());
                } catch (UnsupportedEncodingException ex) {
                    assertThat(false).isTrue();
                }
            }).start();
            ((IMAPFolder) inboxFolder).idle(true);

            assertThat(messages[0].getContent() != null).isTrue();
            assertThat(((Multipart) messages[0].getContent()).getBodyPart(1).getFileName()).isEqualTo(fileName);

            inboxFolder.close();
        } finally {
            store.close();
        }
    }

    private void sendMessage(InternetAddress fromAddress, InternetAddress toAddress) throws MessagingException {
        final Session session = greenMail.getSmtp().createSession();
        MimeMessage message = new MimeMessage(session);
        message.setRecipient(Message.RecipientType.TO, toAddress);
        message.setSubject("Some subject", "UTF-8");
        message.setFrom(fromAddress);
        message.setContent("This is a test", "text/plain; charset=UTF-8");
        message.saveChanges();

        GreenMailUtil.sendMimeMessage(message);
    }

    @Test
    public void testAttachmentWithLongEncodedUTF8Name() throws MessagingException, IOException {
        // Prepare mail
        greenMail.setUser("to@localhost", "pwd");
        String fileName = "кирилица testimage_ünicöde_\uD83C\uDF36";
        final String fileNameEncoded = MimeUtility.encodeText(fileName);
        GreenMailUtil.sendAttachmentEmail(
            "to@localhost", "from@localhost", "subject", "body",
            new byte[]{0, 1, 2}, "image/gif",
            fileNameEncoded,
            "testimage_description", greenMail.getSmtp().getServerSetup());

        greenMail.waitForIncomingEmail(1);

        // Verify
        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("to@localhost", "pwd");
        try {
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            try {
                Message[] messages = inboxFolder.getMessages();
                assertThat(messages).hasSize(1);
                final MimeMultipart content = (MimeMultipart) messages[0].getContent();
                assertThat(messages[0].getContent()).isNotNull();
                String receivedFileName = "";
                for (int i = 0; i < content.getCount(); i++) {
                    final MimeBodyPart bodyPart = (MimeBodyPart) content.getBodyPart(i);
                    if (bodyPart.getContentType().startsWith("IMAGE/GIF")) {
                        receivedFileName = bodyPart.getFileName();
                    }
                }
                assertThat(receivedFileName).isEqualTo(fileNameEncoded);
                assertThat(MimeUtility.decodeText(receivedFileName)).isEqualTo(fileName);
            } finally {
                inboxFolder.close();
            }
        } finally {
            store.close();
        }
    }
}
