package com.icegreen.greenmail.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.MimeMessageHelper;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;


/**
 * Simple test case to reproduce GreenMail problem.
 *
 * @author smm
 */
class GreenMailTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void testWaitForIncomingEmailWithTimeout() {
        long start = System.currentTimeMillis();
        final long timeout = 2000L;
        boolean mailReceived = greenMail.waitForIncomingEmail(timeout, 1);
        long finish = System.currentTimeMillis();
        assertThat(mailReceived).isFalse();
        final long timePasswdMax = (long) (timeout * 1.1f);
        assertThat(finish - start).isLessThan(timePasswdMax);
        final long timePassedMin = (long) (timeout * 0.9f);
        assertThat(finish - start).isGreaterThan(timePassedMin);
    }

    @Test
    void getReceivedMessagesForDomainLowerCaseRecipientAddress() {
        final String to = "to@localhost";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", "subject", "body");

        final MimeMessage[] receivedMessagesForDomain = greenMail.getReceivedMessagesForDomain(to);
        assertThat(receivedMessagesForDomain).hasSize(1);
    }

    @Test
    void getReceivedMessagesForDomainWithUpperCaseRecipientAddress() {
        final String to = "someReceiver@localhost";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", "subject", "body");

        final MimeMessage[] receivedMessagesForDomain = greenMail.getReceivedMessagesForDomain(to);
        assertThat(receivedMessagesForDomain).hasSize(1);
    }

    @Test
    void testFindReceivedMessages() {
        GreenMailUtil.sendTextEmailTest("foo@localhost", "from@localhost", "#1", "body");
        GreenMailUtil.sendTextEmailTest("foo@localhost", "from@localhost", "#2 match", "body"); // Should match
        GreenMailUtil.sendTextEmailTest("bar@localhost", "from@localhost", "#3 match", "body"); // Should match
        GreenMailUtil.sendTextEmailTest("bar@other-domain", "from@localhost", "#4 match", "body");

        // All messages received for emails ending with "localhost" and subject containing "match"
        final List<MimeMessage> messages1 = greenMail.findReceivedMessages(
            u -> u.getEmail().toLowerCase().endsWith("localhost"),
            m -> MimeMessageHelper.getSubject(m, "").contains("match")).collect(Collectors.toList());
        assertThat(messages1).hasSize(2);
        assertThat(messages1.stream().filter(m -> MimeMessageHelper.getSubject(m, "").startsWith("#2"))).hasSize(1);
        assertThat(messages1.stream().filter(m -> MimeMessageHelper.getSubject(m, "").startsWith("#3"))).hasSize(1);

        // All messages received for any email and subject containing "match"
        final List<MimeMessage> messages2 = greenMail.findReceivedMessages(
            u -> true, // any user
            m -> MimeMessageHelper.getSubject(m, "").contains("match")).collect(Collectors.toList());
        assertThat(messages2).hasSize(3);
        assertThat(messages2.stream().filter(m -> MimeMessageHelper.getSubject(m, "").startsWith("#2"))).hasSize(1);
        assertThat(messages2.stream().filter(m -> MimeMessageHelper.getSubject(m, "").startsWith("#3"))).hasSize(1);
        assertThat(messages2.stream().filter(m -> MimeMessageHelper.getSubject(m, "").startsWith("#4"))).hasSize(1);
    }

    @Test
    void testIsRunning() {
        assertThat(greenMail.isRunning()).isTrue();
        greenMail.stop();
        assertThat(greenMail.isRunning()).isFalse();
        greenMail.start();
        assertThat(greenMail.isRunning()).isTrue();
    }
}

