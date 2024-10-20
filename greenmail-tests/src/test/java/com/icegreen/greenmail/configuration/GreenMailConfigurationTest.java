package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

import org.junit.jupiter.api.Test;

/**
 * Tests if the configuration is applied correctly to GreenMail instances.
 */
class GreenMailConfigurationTest {
    @Test
    void testUsersAccessible() {
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