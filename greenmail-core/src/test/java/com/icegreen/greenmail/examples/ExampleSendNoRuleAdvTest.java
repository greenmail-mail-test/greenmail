package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExampleSendNoRuleAdvTest {
    @Test
    public void testSend() throws MessagingException, IOException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.start();

            //Use random content to avoid potential residual lingering problems
            final String subject = GreenMailUtil.random();
            final String body = GreenMailUtil.random();

            sendTestMails(subject, body); // Place your sending code here

            //wait for max 5s for 1 email to arrive
            //waitForIncomingEmail() is useful if you're sending stuff asynchronously in a separate thread
            assertThat(greenMail.waitForIncomingEmail(5000, 2)).isTrue();

            //Retrieve using GreenMail API
            Message[] messages = greenMail.getReceivedMessages();
            assertThat(messages.length).isEqualTo(2);

            // Simple message
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(GreenMailUtil.getBody(messages[0]).trim()).isEqualTo(body);

            //if you send content as a 2 part multipart...
            assertThat(messages[1].getContent() instanceof MimeMultipart).isTrue();
            MimeMultipart mp = (MimeMultipart) messages[1].getContent();
            assertThat(mp.getCount()).isEqualTo(2);
            assertThat(GreenMailUtil.getBody(mp.getBodyPart(0)).trim()).isEqualTo("body1");
            assertThat(GreenMailUtil.getBody(mp.getBodyPart(1)).trim()).isEqualTo("body2");
        } finally {
            greenMail.stop();
        }
    }

    private void sendTestMails(String subject, String body) throws MessagingException {
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", subject, body);

        // Create multipart
        MimeMultipart multipart = new MimeMultipart();
        final MimeBodyPart part1 = new MimeBodyPart();
        part1.setContent("body1", "text/plain");
        multipart.addBodyPart(part1);
        final MimeBodyPart part2 = new MimeBodyPart();
        part2.setContent("body2", "text/plain");
        multipart.addBodyPart(part2);

        GreenMailUtil.sendMessageBody("to@localhost", "from@localhost", subject + "__2", multipart, null, ServerSetupTest.SMTP);
    }
}
