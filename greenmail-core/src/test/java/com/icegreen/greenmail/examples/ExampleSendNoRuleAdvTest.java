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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
            assertTrue(greenMail.waitForIncomingEmail(5000, 2));

            //Retrieve using GreenMail API
            Message[] messages = greenMail.getReceivedMessages();
            assertEquals(2, messages.length);

            // Simple message
            assertEquals(subject, messages[0].getSubject());
            assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());

            //if you send content as a 2 part multipart...
            assertTrue(messages[1].getContent() instanceof MimeMultipart);
            MimeMultipart mp = (MimeMultipart) messages[1].getContent();
            assertEquals(2, mp.getCount());
            assertEquals("body1", GreenMailUtil.getBody(mp.getBodyPart(0)).trim());
            assertEquals("body2", GreenMailUtil.getBody(mp.getBodyPart(1)).trim());
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
