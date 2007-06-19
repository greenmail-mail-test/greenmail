package com.icegreen.greenmail.spring;

import com.icegreen.greenmail.util.GreenMail;
import org.testng.annotations.Test;
import org.testng.spring.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests GreenMailBean.
 *
 * @author Marcel May (mm)
 */
public class GreenMailBeanTest extends AbstractDependencyInjectionSpringContextTests {
    private GreenMailBean mGreenMailBean;

    @Test
    public void testCreate() {
        GreenMail greenMail = mGreenMailBean.getGreenMail();

        // Test if the protocol got activated
        assert (greenMail.getImap() != null) == mGreenMailBean.isImapProtocoll();
        assert (greenMail.getImaps() != null) == mGreenMailBean.isImapsProtocoll();
        assert (greenMail.getPop3() != null) == mGreenMailBean.isPop3Protocoll();
        assert (greenMail.getPop3s() != null) == mGreenMailBean.isPop3sProtocoll();
        assert (greenMail.getSmtp() != null) == mGreenMailBean.isSmtpProtocoll();
        assert (greenMail.getSmtps() != null) == mGreenMailBean.isSmtpsProtocoll();

        assert mGreenMailBean.getHostname().equals(greenMail.getSmtp().getBindTo());
        assert mGreenMailBean.getPortOffset()+25 == greenMail.getSmtp().getPort();

        assert mGreenMailBean.getHostname().equals(greenMail.getPop3().getBindTo());
        assert mGreenMailBean.getPortOffset()+110 == greenMail.getPop3().getPort();

        mGreenMailBean.sendEmail("to@localhost","from@localhost","subject", "message");
        assert mGreenMailBean.getReceivedMessages().length == 1;
    }

    /**
     * Setter for property 'greenMailBean'.
     *
     * @param pGreenMailBean Value to set for property 'greenMailBean'.
     */
    public void setGreenMailBean(final GreenMailBean pGreenMailBean) {
        mGreenMailBean = pGreenMailBean;
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[]{
                "test-ctx.xml"
        };
    }
}
