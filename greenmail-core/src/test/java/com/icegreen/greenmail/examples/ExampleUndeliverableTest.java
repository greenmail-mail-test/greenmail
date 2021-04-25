package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UndeliverableHandler;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.MessagingException;

public class ExampleUndeliverableTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testSend() throws MessagingException {
        greenMail.getManagers().getUserManager().setUndeliverableHandler(new UndeliverableHandler() {
            @Override
            public GreenMailUser handle(MovingMessage msg, MailAddress mailAddress)
                    throws MessagingException, UserException {
                msg.getMessage().setSubject("Delivery Report");
                return super.handle(msg, mailAddress);
            }
        });
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
                "some subject", "some body"); // --- Place your sending code here instead
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("Delivery Report");
    }
}
