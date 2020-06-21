package com.icegreen.greenmail.util;

import com.icegreen.greenmail.junit.GreenMailRule;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Simple test case to reproduce GreenMail problem.
 *
 * @author smm
 */
public class GreenMailTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testWaitForIncomingEmailWithTimeout() {
        long start = System.currentTimeMillis();
        final long timeout = 2000L;
        boolean mailReceived = greenMail.waitForIncomingEmail(timeout, 1);
        long finish = System.currentTimeMillis();
        assertFalse(mailReceived);
        final long timePasswdMax = (long) (timeout * 1.1f);
        assertThat(finish - start, is(lessThan(timePasswdMax)));
        final long timePassedMin = (long) (timeout * 0.9f);
        assertThat(finish - start, is(greaterThan(timePassedMin)));
    }

    @Test
    public void getReceivedMessagesForDomainLowerCaseRecipientAddress() {
        final String to = "to@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", "subject", "body");

        final MimeMessage[] receivedMessagesForDomain = greenMail.getReceivedMessagesForDomain(to);
        assertThat(receivedMessagesForDomain.length, is(1));
    }

    @Test
    public void getReceivedMessagesForDomainWithUpperCaseRecipientAddress() {
        final String to = "someReceiver@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", "subject", "body");

        final MimeMessage[] receivedMessagesForDomain = greenMail.getReceivedMessagesForDomain(to);
        assertThat(receivedMessagesForDomain.length, is(1));
    }
}

