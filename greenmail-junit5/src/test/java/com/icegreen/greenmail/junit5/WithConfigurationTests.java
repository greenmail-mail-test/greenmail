package com.icegreen.greenmail.junit5;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GreenMail with configuration tests")
class WithConfigurationTests {
    @RegisterExtension
    GreenMailExtension greenMail = new GreenMailExtension()
        .withConfiguration(GreenMailConfiguration.aConfig()
            .withUser("to@localhost", "login-id", "password"));

    @Test
    @DisplayName("Receive test")
    void testReceive() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "subject", "body");
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        final MimeMessage email = emails[0];
        assertEquals("subject", email.getSubject());
        assertEquals("body", email.getContent());
    }
}
