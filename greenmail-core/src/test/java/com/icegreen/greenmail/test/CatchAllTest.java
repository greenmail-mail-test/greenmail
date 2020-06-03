package com.icegreen.greenmail.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import jakarta.mail.MessagingException;

import static org.junit.Assert.assertEquals;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since May 27th, 2009
 */
public class CatchAllTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void testSmtpServerBasic() {
        GreenMailUtil.sendTextEmailTest("to11@domain1.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to12@domain1.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to21@domain2.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to31@domain3.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to32@domain3.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to33@domain3.com", "from@localhost.com", "subject", "body");
        assertEquals(6, greenMail.getReceivedMessages().length);
        assertEquals(2, greenMail.getReceivedMessagesForDomain("domain1.com").length);
        assertEquals(1, greenMail.getReceivedMessagesForDomain("domain2.com").length);
        assertEquals(3, greenMail.getReceivedMessagesForDomain("domain3.com").length);
    }
}
