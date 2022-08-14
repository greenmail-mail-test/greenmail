package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ExamplePreloadMailFromFsTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);
    public static final String EML_FILE_NAME = ExamplePreloadMailFromFsTest.class.getName() + ".eml";

    @Test
    public void testPreloadMailFromFs() throws Exception {
        final Session session = greenMail.getSmtp().createSession();

        // Create test data
        final MimeMessage msg = new MimeMessage(session);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("foo@localhost"));
        msg.setFrom("bar@localhost");
        msg.setSubject("Hello");
        msg.setText("Test message saved as eml (electronic mail format, aka internet message format)");
        try (FileOutputStream os = new FileOutputStream(EML_FILE_NAME)) {
            msg.writeTo(os);
        }

        // Load msg from file system
        final ImapHostManager imapHostManager = greenMail.getManagers().getImapHostManager();
        final UserManager userManager = greenMail.getManagers().getUserManager();
        final GreenMailUser user = userManager.createUser("foo@localhost", "foo-login", "secret");
        try (InputStream source = Files.newInputStream(Paths.get(EML_FILE_NAME))) {
            final MimeMessage loadedMsg = new MimeMessage(session, source);
            imapHostManager.getFolder(user, "INBOX").store(loadedMsg);
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
