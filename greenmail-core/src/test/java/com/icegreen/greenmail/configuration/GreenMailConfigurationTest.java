package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

/**
 * Tests if the configuration is applied correctly to GreenMail instances.
 */
public class GreenMailConfigurationTest {
    @Test
    public void testUsersAccessible() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP).withConfiguration(
                testUsersAccessibleConfig()
        );
        greenMail.start();
        try {
            GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
        } finally {
            greenMail.stop();
        }
    }

}