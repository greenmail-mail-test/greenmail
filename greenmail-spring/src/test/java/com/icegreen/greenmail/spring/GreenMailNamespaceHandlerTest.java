package com.icegreen.greenmail.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.icegreen.greenmail.spring.GreenMailBeanDefinitionParser.DEFAULT_SERVER_STARTUP_TIMEOUT;

/**
 * Tests GreenMailBean configured via xml namespace handler.
 *
 * @author Marcel May (mm)
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GreenMailNamespaceHandlerTest {
    @Autowired
    private GreenMailBean greenMailBean;

    @Test
    public void testCreate() {
        assert null!= greenMailBean;
        assert "127.0.0.1".equals(greenMailBean.getHostname());
        assert greenMailBean.getPortOffset() == 5000;
        assert greenMailBean.getServerStartupTimeout() == DEFAULT_SERVER_STARTUP_TIMEOUT;
    }
}