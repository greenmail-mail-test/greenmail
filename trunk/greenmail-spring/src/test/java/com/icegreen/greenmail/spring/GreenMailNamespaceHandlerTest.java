package com.icegreen.greenmail.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests GreenMailBean configured via xml namespace handler.
 *
 * @author Marcel May (mm)
 */
@ContextConfiguration
public class GreenMailNamespaceHandlerTest extends AbstractTestNGSpringContextTests {
    @Autowired
    private GreenMailBean greenMailBean;

    @Test
    public void testCreate() {
        assert null!= greenMailBean;
        assert "127.0.0.1".equals(greenMailBean.getHostname());
        assert greenMailBean.getPortOffset() == 5000;
    }
}