package com.icegreen.greenmail.junit5;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GreenMail with custom ServerSetup tests")
class CustomSetupTests {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    @DisplayName("Send test")
    void testSend() {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "some subject", "some body");
        final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        final MimeMessage receivedMessage = receivedMessages[0];
        assertEquals("some body", GreenMailUtil.getBody(receivedMessage));
    }
}
