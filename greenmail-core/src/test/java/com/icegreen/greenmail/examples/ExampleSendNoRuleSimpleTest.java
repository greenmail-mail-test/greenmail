package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.MessagingException;

import static org.junit.Assert.assertEquals;

public class ExampleSendNoRuleSimpleTest {
    @Test
    public void testSend() throws MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject",
                    "some body"); //replace this with your test message content
            assertEquals("some body", GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
        } finally {
            greenMail.stop();
        }
    }
}
