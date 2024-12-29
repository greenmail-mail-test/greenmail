package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;

/**
 * Created by Youssuf ElKalay.
 * Example using GreenMailConfiguration to test authenticating against SMTP/IMAP/POP3 with no password required.
 */
class ExampleDisableAuthenticationTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Test
    void testNoAuthIMAP() {
        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages("foo@localhost");
            assertThat(messages).isEmpty();
        }
    }

    @Test
    void testExistingUserNotRecreated() {
        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages("foo@localhost");
            assertThat(messages).isEmpty();
            assertThat(greenMail.getUserManager().hasUser("foo@localhost")).isTrue();
        }
    }
}
