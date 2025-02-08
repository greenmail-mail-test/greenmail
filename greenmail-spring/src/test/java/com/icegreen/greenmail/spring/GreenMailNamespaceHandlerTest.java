package com.icegreen.greenmail.spring;

import static com.icegreen.greenmail.spring.GreenMailBeanDefinitionParser.DEFAULT_SERVER_STARTUP_TIMEOUT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests GreenMailBean configured via xml namespace handler.
 *
 * @author Marcel May (mm)
 */
@ContextConfiguration
@SpringJUnitConfig
class GreenMailNamespaceHandlerTest {
    @Autowired
    private GreenMailBean greenMailBean;

    @Test
    void testCreate() {
        assert null!= greenMailBean;
        assert "127.0.0.1".equals(greenMailBean.getHostname());
        assert greenMailBean.getPortOffset() == 5000;
        assert greenMailBean.getServerStartupTimeout() == DEFAULT_SERVER_STARTUP_TIMEOUT;
    }
}
