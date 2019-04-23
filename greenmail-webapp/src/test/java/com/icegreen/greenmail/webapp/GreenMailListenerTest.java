package com.icegreen.greenmail.webapp;

import org.easymock.EasyMock;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for GreenMailListener.
 *
 * @author mm
 */
public class GreenMailListenerTest {

    @Test
    public void testStart() {
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
        servletContext.setAttribute(EasyMock.anyString(), EasyMock.anyObject() );
        EasyMock.expectLastCall().times(2);
        servletContext.removeAttribute(EasyMock.anyString() );
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(servletContext);

        GreenMailListener listener = new GreenMailListener();
        ServletContextEvent event = new ServletContextEvent(servletContext);

        listener.contextInitialized(event);

        // Would be nice to send and retrieve a test mail here
        listener.contextDestroyed(event);
    }
}