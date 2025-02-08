package com.icegreen.greenmail.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;

class SmtpSecureServerTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(new ServerSetup[]{ServerSetupTest.SMTPS});

    @Test
    void testSmtpsServerReceive() throws Throwable {
        assertThat(greenMail.getReceivedMessages()).isEmpty();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailSecureTest("test@localhost", "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails).hasSize(1);
        assertThat(emails[0].getSubject()).isEqualTo(subject);
        assertThat(emails[0].getContent()).isEqualTo(body);
    }
}
