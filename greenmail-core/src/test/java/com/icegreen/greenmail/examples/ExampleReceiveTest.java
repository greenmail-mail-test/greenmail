package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class ExampleReceiveTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testReceive() throws MessagingException {
        GreenMailUser user = greenMail.setUser("to@localhost.com", "login-id", "password");
        user.deliver(createMimeMessage()); // You can either create a more complex message...
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com",
                "subject", "body"); // ...or use the default messages

        assertThat(greenMail.getReceivedMessages().length).isEqualTo(2); // // --- Place your POP3 or IMAP retrieve code here
    }

    private MimeMessage createMimeMessage() {
        return GreenMailUtil.createTextEmail("to@localhost.com", "from@localhost.com", "subject", "body", greenMail.getImap().getServerSetup());
    }
}
