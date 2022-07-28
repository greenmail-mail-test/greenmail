package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ExampleSendTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testSend() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
            "some subject", "some body"); // --- Place your sending code here instead
        assertThat(greenMail.getReceivedMessages()[0].getContent()).isEqualTo("some body");
    }

    @Test
    public void testSendWithAvailablePortDetection() throws MessagingException, IOException {
        GreenMail greenMailWithDynamicPort = new GreenMail(ServerSetupTest.SMTP.dynamicPort()); // Port would collide with ExampleSendTest.greenMail
        greenMailWithDynamicPort.start();
        try { // Be nice and always close started instance
            GreenMailUtil.sendTextEmail(
                "to@localhost", "from@localhost", "some subject", "Sent using available port detection",
                greenMailWithDynamicPort.getSmtp().getServerSetup()); // Important: Pass dynamic port setup here
            assertEquals("Sent using available port detection",
                greenMailWithDynamicPort.getReceivedMessages()[0].getContent());
            assertEquals("Unexpected default test port",
                ServerSetupTest.SMTP.getPort(), greenMail.getSmtp().getPort());
            assertNotEquals("Dynamic port must differ",
                ServerSetupTest.SMTP.getPort(), greenMailWithDynamicPort.getSmtp().getPort());
        } finally {
            greenMailWithDynamicPort.stop();
        }
    }
}
