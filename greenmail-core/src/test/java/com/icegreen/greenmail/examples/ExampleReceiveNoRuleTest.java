package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class ExampleReceiveNoRuleTest {
    @Test
    public void testReceive() {
        //Start all email servers using non-default ports.
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.start();

            //Use random content to avoid potential residual lingering problems
            final String subject = GreenMailUtil.random();
            final String body = GreenMailUtil.random();
            MimeMessage message = createMimeMessage(subject, body, greenMail); // Construct message
            GreenMailUser user = greenMail.setUser("wael@localhost", "waelc", "soooosecret");
            user.deliver(message);
            assertThat(greenMail.getReceivedMessages().length).isEqualTo(1);

            // --- Place your retrieve code here

        } finally {
            greenMail.stop();
        }
    }

    private MimeMessage createMimeMessage(String subject, String body, GreenMail greenMail) {
        return GreenMailUtil.createTextEmail("to@localhost", "from@localhost", subject, body, greenMail.getImap().getServerSetup());
    }
}
