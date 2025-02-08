package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Example for loading Emails from EML file.
 * <p>
 * For preloading an existing directory structure, check out {@link  com.icegreen.greenmail.util.PreLoadEmailsTest}
 */
class ExamplePreloadMailFromFsTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);
    public static final String EML_FILE_NAME = ExamplePreloadMailFromFsTest.class.getSimpleName() + ".eml";

    @Test
    void testPreloadMailFromFs() throws Exception {
        final Session session = greenMail.getSmtp().createSession();

        // Create test data
        final MimeMessage msg = new MimeMessage(session);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("foo@localhost"));
        msg.setFrom("bar@localhost");
        msg.setSubject("Hello");
        msg.setText("Test message saved as eml (electronic mail format, aka internet message format)");

        final Path emlFile = Files.createTempDirectory("tmp").resolve(EML_FILE_NAME);
        try (FileOutputStream os = new FileOutputStream(emlFile.toString())) {
            msg.writeTo(os);
        }

        // Load msg from file system
        final ImapHostManager imapHostManager = greenMail.getManagers().getImapHostManager();
        final UserManager userManager = greenMail.getManagers().getUserManager();
        final GreenMailUser user = userManager.createUser("foo@localhost", "foo-login", "secret");
        try (InputStream source = Files.newInputStream(emlFile)) {
            final MimeMessage loadedMsg = new MimeMessage(session, source);
            imapHostManager.getFolder(user, ImapConstants.INBOX_NAME).store(loadedMsg);
        }

        // Verify
        final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);
        final MimeMessage receivedMessage = receivedMessages[0];
        assertThat(receivedMessage.getSubject()).isEqualTo(msg.getSubject());
        assertThat(receivedMessage.getContent()).isEqualTo(msg.getContent());
        assertThat(receivedMessage.getFrom()).isEqualTo(msg.getFrom());
        assertThat(receivedMessage.getRecipients(Message.RecipientType.TO))
            .isEqualTo(msg.getRecipients(Message.RecipientType.TO));
    }
}
