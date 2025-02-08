package com.icegreen.greenmail.webapp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;

/**
 * Test for GreenMailListener.
 */
class GreenMailListenerTest {

    @Test
    void testStart() {
        Map<String, String> paramValues = new HashMap<>();
        paramValues.put("greenmail.defaultHostname", "127.0.0.1");
        paramValues.put("greenmail.portOffset", "20000");
        paramValues.put("greenmail.smtp", "");
        paramValues.put("greenmail.pop3.host", "127.0.0.1");
        paramValues.put("greenmail.pop3.port", "1110");
        paramValues.put("greenmail.imap", "");

        Enumeration<String> params = Collections.enumeration(paramValues.keySet());
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(servletContext.getInitParameterNames()).andReturn(params);
        for (Map.Entry<String, String> entry : paramValues.entrySet()) {
            EasyMock.expect(servletContext.getInitParameter(entry.getKey()))
                .andReturn(entry.getValue());
        }
        servletContext.setAttribute(eq(ContextHelper.ATTRIBUTE_NAME_MANAGERS), anyObject());
        servletContext.setAttribute(eq(ContextHelper.ATTRIBUTE_NAME_CONFIGURATION), anyObject());

        EasyMock.replay(servletContext);

        GreenMailListener listener = new GreenMailListener();
        ServletContextEvent event = new ServletContextEvent(servletContext);

        listener.contextInitialized(event);

        // Would be nice to send and retrieve a test mail here
        listener.contextDestroyed(event);
    }
}
