package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import jakarta.mail.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Youssuf ElKalay.
 * Example using GreenMailConfiguration to test authenticating against SMTP/IMAP/POP3 with no password required.
 */
public class ExampleDisableAuthenticationTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Test
    public void testNoAuthIMAP() {
        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages("foo@localhost");
            assertThat(messages).isEmpty();
        }
    }

    @Test
    public void testExistingUserNotRecreated() {
        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages("foo@localhost");
            assertThat(messages).isEmpty();
            assertThat(greenMail.getUserManager().hasUser("foo@localhost")).isTrue();
        }
    }
}
