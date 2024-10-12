package com.icegreen.greenmail;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserImpl;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class AuthenticationDisabledTest {
    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(
            new ServerSetup[] { ServerSetupTest.SMTP, ServerSetupTest.IMAP })
                    .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Test
    void testSendMailAndReceiveWithAuthDisabled() throws MessagingException, IOException {
        final String to = "to@localhost";
        final String subject = "subject";
        final String body = "body";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails).hasSize(1);
        assertThat(emails[0].getSubject()).isEqualTo(subject);
        assertThat(emails[0].getContent()).isEqualTo(body);

        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(messages[0].getContent()).isEqualTo(body);
        }
    }

    @Test
    void testReceiveWithAuthDisabled() {
        final String to = "to@localhost";

        greenMail.waitForIncomingEmail(500, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).isEmpty();
        }
    }

    @Test
    void testReceiveWithAuthDisabledAndProvisionedUser() {
        final String to = "to@localhost";
        greenMail.setUser(to, to, "secret");

        greenMail.waitForIncomingEmail(500, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).isEmpty();
        }
    }

    @Test
    void testPop3ConnectNoAuth() throws UserException, FolderException {
        UserManager userManager = greenMail.getUserManager();
        Pop3State status = new Pop3State(userManager);
        userManager.setAuthRequired(false);

        UserImpl user = new UserImpl("email@example.com", "user", "pwd", null);
        status.setUser(user);
        status.authenticate("pass");
    }

    @Test
    void testPop3ConnectAuth() {
        UserManager userManager = greenMail.getUserManager();
        Pop3State status = new Pop3State(userManager);
        userManager.setAuthRequired(true);

        UserImpl user = new UserImpl("email@example.com", "user", "pwd", null);
        status.setUser(user);
        assertThatThrownBy(() -> status.authenticate("pass")).isInstanceOf(UserException.class);
    }

}
