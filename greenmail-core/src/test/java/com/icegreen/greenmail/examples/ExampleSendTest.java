package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;

class ExampleSendTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void testSend() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
            "some subject", "some body"); // --- Place your sending code here instead
        assertThat(greenMail.getReceivedMessages()[0].getContent()).isEqualTo("some body");
    }

    @Test
    void testSendWithAvailablePortDetection() throws MessagingException, IOException {
        GreenMail greenMailWithDynamicPort = new GreenMail(ServerSetupTest.SMTP.dynamicPort()); // Port would collide with ExampleSendTest.greenMail
        greenMailWithDynamicPort.start();
        try { // Be nice and always close started instance
            GreenMailUtil.sendTextEmail(
                "to@localhost", "from@localhost", "some subject", "Sent using available port detection",
                greenMailWithDynamicPort.getSmtp().getServerSetup()); // Important: Pass dynamic port setup here
            assertEquals("Sent using available port detection", greenMailWithDynamicPort.getReceivedMessages()[0].getContent());
            assertEquals(ServerSetupTest.SMTP.getPort(), greenMail.getSmtp().getPort(), "Unexpected default test port");
            assertNotEquals(ServerSetupTest.SMTP.getPort(), greenMailWithDynamicPort.getSmtp().getPort(), "Dynamic port must differ");
        } finally {
            greenMailWithDynamicPort.stop();
        }
    }
}
