package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExampleSendNoRuleSimpleTest {
    @Test
    public void testSend() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "some subject",
                    "some body"); //replace this with your test message content
            assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).isEqualTo("some body");
        } finally {
            greenMail.stop();
        }
    }
}
