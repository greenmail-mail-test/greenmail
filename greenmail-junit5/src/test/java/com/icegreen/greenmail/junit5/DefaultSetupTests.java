package com.icegreen.greenmail.junit5;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GreenMail with default ServerSetups tests")
class DefaultSetupTests {
    @RegisterExtension
    GreenMailExtension greenMail = new GreenMailExtension();

    @Test
    @DisplayName("Send test")
    void testSend() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "subject", "body");
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        final MimeMessage email = emails[0];
        assertEquals("subject", email.getSubject());
        assertEquals("body", email.getContent());
    }
}
