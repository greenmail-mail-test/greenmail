package com.icegreen.greenmail.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

public class AuthenticationDisabledTest {
    @Rule
    public final GreenMailRuleWithStoreChooser
            greenMail = new GreenMailRuleWithStoreChooser(new ServerSetup[]{ServerSetupTest.SMTP, ServerSetupTest
            .IMAP}, GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Test
    @StoreChooser(store="file,memory")
    public void testSendMailAndReceiveWithAuthDisabled() throws MessagingException, IOException {
        final String to = "to@localhost.com";
        final String subject = "subject";
        final String body = "body";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(emails[0]));

        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(1, messages.length);
            assertEquals(subject, messages[0].getSubject());
            assertEquals(body, messages[0].getContent());
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testReceiveWithAuthDisabled() throws MessagingException, IOException {
        final String to = "to@localhost.com";

        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(0, messages.length);
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testReceiveWithAuthDisabledAndProvisionedUser() throws MessagingException, IOException {
        final String to = "to@localhost.com";
        greenMail.setUser(to,"to","secret");

        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(0, messages.length);
        }
    }
}
