package com.icegreen.greenmail.spring;

import com.icegreen.greenmail.util.GreenMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests GreenMailBean.
 *
 * @author Marcel May (mm)
 */
@ContextConfiguration
public class GreenMailBeanTest extends AbstractTestNGSpringContextTests {
    @Autowired
    private GreenMailBean greenMailBean;

    @Test
    public void testCreate() {
        GreenMail greenMail = greenMailBean.getGreenMail();

        // Test if the protocol got activated
        assert (greenMail.getImap() != null) == greenMailBean.isImapProtocol();
        assert (greenMail.getImaps() != null) == greenMailBean.isImapsProtocol();
        assert (greenMail.getPop3() != null) == greenMailBean.isPop3Protocol();
        assert (greenMail.getPop3s() != null) == greenMailBean.isPop3sProtocol();
        assert (greenMail.getSmtp() != null) == greenMailBean.isSmtpProtocol();
        assert (greenMail.getSmtps() != null) == greenMailBean.isSmtpsProtocol();

        assert greenMailBean.getHostname().equals(greenMail.getSmtp().getBindTo());
        assert greenMailBean.getPortOffset()+25 == greenMail.getSmtp().getPort();

        assert greenMailBean.getHostname().equals(greenMail.getPop3().getBindTo());
        assert greenMailBean.getPortOffset()+110 == greenMail.getPop3().getPort();

        greenMailBean.sendEmail("to@localhost","from@localhost","subject", "message");
        assert greenMailBean.getReceivedMessages().length == 1;
    }
}
