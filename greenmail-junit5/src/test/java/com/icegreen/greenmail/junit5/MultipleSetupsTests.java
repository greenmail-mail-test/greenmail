package com.icegreen.greenmail.junit5;

import jakarta.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GreenMail with multiple ServerSetups tests")
class MultipleSetupsTests {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    @DisplayName("Receive test")
    void testReceive() {
        greenMail.setUser("to@localhost", "login-id", "password");
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "subject", "body");
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(2, emails.length);
    }
}
