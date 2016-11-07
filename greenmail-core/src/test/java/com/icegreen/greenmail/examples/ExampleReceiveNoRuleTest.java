package com.icegreen.greenmail.examples;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

public class ExampleReceiveNoRuleTest {

    @Test
    public void testReceiveWithMemoryStore() throws MessagingException, IOException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);

        try {
            greenMail.start();

            //Use random content to avoid potential residual lingering problems
            final String subject = GreenMailUtil.random();
            final String body = GreenMailUtil.random();
            MimeMessage message = createMimeMessage(subject, body, greenMail); // Construct message
            GreenMailUser user = greenMail.setUser("wael@localhost.com", "waelc", "soooosecret");
            user.deliver(message);
            assertEquals(1, greenMail.getReceivedMessages().length);

            // --- Place your retrieve code here

        } finally {
            greenMail.stop();
        }
    }

    private MimeMessage createMimeMessage(String subject, String body, GreenMail greenMail) {
        return GreenMailUtil.createTextEmail("to@localhost.com", "from@localhost.com", subject, body, greenMail.getImap().getServerSetup());
    }
}
