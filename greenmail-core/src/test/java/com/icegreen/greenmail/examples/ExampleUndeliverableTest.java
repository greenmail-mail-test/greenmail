package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.MessageDeliveryHandler;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ExampleUndeliverableTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testSend() throws MessagingException, UserException {
        final UserManager userManager = greenMail.getUserManager();
        userManager.createUser("from@localhost", "from@localhost", "from@localhost");
        MessageDeliveryHandler defaultMessageDeliveryHandler = userManager.getMessageDeliveryHandler();
        userManager.setMessageDeliveryHandler((msg, mailAddress) -> {
            String email = mailAddress.getEmail();
            GreenMailUser user = userManager.getUserByEmail(email);
            if (user == null) {
                user = userManager.getUserByEmail(msg.getReturnPath().getEmail());
                if (user != null) {
                    MimeMessage dsnMessage = new MimeMessage(msg.getMessage().getSession());
                    dsnMessage.setRecipients(RecipientType.TO, msg.getReturnPath().getEmail());
                    dsnMessage.setSubject("Delivery Report");
                    dsnMessage.setText("...");
                    msg.setMimeMessage(dsnMessage);
                } else {
                    user = defaultMessageDeliveryHandler.handle(msg, mailAddress);
                    // user = userManager.createUser(mailAddress.getEmail(), mailAddress.getEmail(), mailAddress.getEmail());
                }
            }
            return user;
        });
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
                "some subject", "some body"); // --- Place your sending code here instead
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("Delivery Report");
        assertThat(greenMail.getReceivedMessages()[0].getRecipients(RecipientType.TO)[0].toString()).isEqualTo("from@localhost");
    }
}
