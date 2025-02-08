package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;

class ExampleSendNoRuleSimpleTest {
    @Test
    void testSend() throws MessagingException, IOException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "some subject",
                    "some body"); //replace this with your test message content
            assertThat((greenMail.getReceivedMessages()[0].getContent())).isEqualTo("some body");
        } finally {
            greenMail.stop();
        }
    }
}
