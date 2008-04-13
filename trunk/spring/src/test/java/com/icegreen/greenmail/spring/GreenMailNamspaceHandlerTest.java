package com.icegreen.greenmail.spring;

import org.springframework.beans.factory.annotation.Required;
import org.testng.annotations.Test;
import org.testng.spring.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests GreenMailBean configured via xml namespace handler.
 *
 * @author Marcel May (mm)
 */
public class GreenMailNamspaceHandlerTest extends AbstractDependencyInjectionSpringContextTests {
    private GreenMailBean mGreenMailBean;

    @Test
    public void testCreate() {
        assert null!=mGreenMailBean;
        assert "127.0.0.1".equals(mGreenMailBean.getHostname());
        assert mGreenMailBean.getPortOffset() == 5000;
    }

    /**
     * Setter for property 'greenMailBean'.
     *
     * @param pGreenMailBean Value to set for property 'greenMailBean'.
     */
    @Required
    public void setGreenMailBean(final GreenMailBean pGreenMailBean) {
        mGreenMailBean = pGreenMailBean;
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[]{
                "test-greenmail-bean-ctx.xml"
        };
    }
}