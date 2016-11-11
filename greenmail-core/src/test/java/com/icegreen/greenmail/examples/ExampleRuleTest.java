package com.icegreen.greenmail.examples;

import static org.junit.Assert.assertEquals;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

public class ExampleRuleTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testSomething() throws MessagingException {
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "subject", "body");
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals("subject", emails[0].getSubject());
        assertEquals("body", GreenMailUtil.getBody(emails[0]));
        // ...
    }
}
