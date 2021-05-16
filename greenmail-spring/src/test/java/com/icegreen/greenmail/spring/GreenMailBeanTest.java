package com.icegreen.greenmail.spring;

import com.icegreen.greenmail.util.GreenMail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.icegreen.greenmail.spring.GreenMailBeanDefinitionParser.DEFAULT_SERVER_STARTUP_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests GreenMailBean.
 *
 * @author Marcel May (mm)
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GreenMailBeanTest {
    @Autowired
    private GreenMailBean greenMailBean;

    @Test
    public void testCreate() {
        GreenMail greenMail = greenMailBean.getGreenMail();

        // Test if the protocol got activated
        assertThat(greenMail.getImap() != null).isEqualTo(greenMailBean.isImapProtocol());
        assertThat(greenMail.getImaps() != null).isEqualTo(greenMailBean.isImapsProtocol());
        assertThat(greenMail.getPop3() != null).isEqualTo(greenMailBean.isPop3Protocol());
        assertThat(greenMail.getPop3s() != null).isEqualTo(greenMailBean.isPop3sProtocol());
        assertThat(greenMail.getSmtp() != null).isEqualTo(greenMailBean.isSmtpProtocol());
        assertThat(greenMail.getSmtps() != null).isEqualTo(greenMailBean.isSmtpsProtocol());

        assertThat(greenMailBean.getHostname()).isEqualTo(greenMail.getSmtp().getBindTo());
        assertThat(greenMailBean.getPortOffset() + 25).isEqualTo(greenMail.getSmtp().getPort());

        assertThat(greenMailBean.getHostname()).isEqualTo(greenMail.getPop3().getBindTo());
        assertThat(greenMailBean.getPortOffset() + 110).isEqualTo(greenMail.getPop3().getPort());

        greenMailBean.sendEmail("to@localhost", "from@localhost", "subject", "message");
        assertThat(greenMailBean.getReceivedMessages()).hasSize(1);

        assertThat(greenMailBean.getServerStartupTimeout()).isEqualTo(DEFAULT_SERVER_STARTUP_TIMEOUT);
        assertThat(greenMailBean.getUserManager()).isEqualTo(greenMail.getUserManager());
    }
}
