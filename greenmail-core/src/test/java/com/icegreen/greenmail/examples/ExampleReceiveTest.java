package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;

class ExampleReceiveTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void testReceive() {
        GreenMailUser user = greenMail.setUser("to@localhost", "login-id", "password");
        user.deliver(createMimeMessage()); // You can either create a more complex message...
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
                "subject", "body"); // ...or use the default messages

        // --- Place your POP3 or IMAP retrieve code here
        assertThat(greenMail.getReceivedMessages()).hasSize(2);
    }

    private MimeMessage createMimeMessage() {
        return GreenMailUtil.createTextEmail("to@localhost", "from@localhost", "subject", "body",
            greenMail.getImap().getServerSetup());
    }
}
