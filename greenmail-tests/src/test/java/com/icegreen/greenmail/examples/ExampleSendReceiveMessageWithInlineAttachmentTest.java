package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;


/**
 * See https://github.com/greenmail-mail-test/greenmail/issues/113
 *
 * @author https://github.com/DavidWhitlock
 */
class ExampleSendReceiveMessageWithInlineAttachmentTest {

    private final String emailAddress = "test@email.com";
    private final String imapUserName = "emailUser";
    private final String imapPassword = "emailPassword";

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void sendAndFetchMailMessageWithInlineAttachment() throws IOException, MessagingException {
        greenMail.setUser(emailAddress, imapUserName, imapPassword);

        sendMailMessageWithInlineAttachment();
        fetchEmailWithInlineAttachment();
    }

    private void fetchEmailWithInlineAttachment() throws MessagingException, IOException {
        Store store = connectToIMAPServer();
        Folder folder = openFolder(store, "INBOX");

        Message[] messages = folder.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getContentType()).startsWith("multipart/mixed;");

        final Multipart part = (Multipart) messages[0].getContent();
        assertThat(part.getCount()).isEqualTo(1);

        final BodyPart bodyPart = part.getBodyPart(0);
        assertThat(bodyPart.getContentType()).isEqualTo("text/plain; charset=us-ascii");
        assertThat(bodyPart.getDisposition()).isEqualTo("inline");
        assertThat(bodyPart.getContent()).isEqualTo("This is some text to be displayed inline");
    }

    private Folder openFolder(Store store, String folderName) throws MessagingException {
        Folder folder = store.getDefaultFolder();
        folder = folder.getFolder(folderName);
        folder.open(Folder.READ_WRITE);
        return folder;
    }

    private Store connectToIMAPServer() throws MessagingException {
        Store store = greenMail.getImap().createStore();
        store.connect(imapUserName, imapPassword);

        return store;
    }

    protected MimeMessage newEmailTo(Session session, String recipient, String subject) throws MessagingException {
        MimeMessage message = new MimeMessage(session);

        InternetAddress[] to = {new InternetAddress(recipient)};
        message.setRecipients(Message.RecipientType.TO, to);
        message.setSubject(subject);
        return message;
    }

    protected Session newEmailSession(boolean debug) {
        Session session = greenMail.getSmtp().createSession();
        session.setDebug(debug);
        return session;
    }

    private void sendMailMessageWithInlineAttachment() throws MessagingException {
        MimeMessage message = newEmailTo(newEmailSession(false), emailAddress, "Message with inline attachment");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent("This is some text to be displayed inline", "text/plain");

        // Try not to display text as separate attachment
        textPart.setDisposition(Part.INLINE);

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(textPart);

        message.setContent(mp);

        Transport.send(message);
    }

}
