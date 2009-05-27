package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import junit.framework.TestCase;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since May 27th, 2009
 */
public class CatchAllTest extends TestCase {
    GreenMail greenMail;

    protected void tearDown() throws Exception {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testSmtpServerBasic() throws MessagingException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
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
