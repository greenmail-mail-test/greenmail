package com.icegreen.greenmail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since May 27th, 2009
 */
class CatchAllTest {
    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void testSmtpServerBasic() {
        GreenMailUtil.sendTextEmailTest("to11@domain1.com", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to12@domain1.com", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to21@domain2.com", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to31@domain3.com", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to32@domain3.com", "from@localhost", "subject", "body");
        GreenMailUtil.sendTextEmailTest("to33@domain3.com", "from@localhost", "subject", "body");
        assertThat(greenMail.getReceivedMessages()).hasSize(6);
        assertThat(greenMail.getReceivedMessagesForDomain("domain1.com")).hasSize(2);
        assertThat(greenMail.getReceivedMessagesForDomain("domain2.com")).hasSize(1);
        assertThat(greenMail.getReceivedMessagesForDomain("domain3.com")).hasSize(3);
    }
}
