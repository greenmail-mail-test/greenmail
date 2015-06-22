package com.icegreen.greenmail.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;

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
    @Ignore
    public void testSmtpServerBasic() throws MessagingException {
        GreenMailUtil.sendTextEmailTest("to11@domain1.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to12@domain1.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to21@domain2.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to31@domain3.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to32@domain3.com", "from@localhost.com", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to33@domain3.com", "from@localhost.com", "subject", "body");
        assertEquals(6, greenMail.getReceivedMessages().length);
        assertEquals(2, greenMail.getReceviedMessagesForDomain("domain1.com").length);
        assertEquals(1, greenMail.getReceviedMessagesForDomain("domain2.com").length);
        assertEquals(3, greenMail.getReceviedMessagesForDomain("domain3.com").length);
    }
}
