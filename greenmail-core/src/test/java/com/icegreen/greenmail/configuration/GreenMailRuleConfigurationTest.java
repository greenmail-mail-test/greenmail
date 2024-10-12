package com.icegreen.greenmail.configuration;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Tests that check if the GreenMailConfiguration is applied correctly to GreenMailRules
 */
class GreenMailRuleConfigurationTest {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.IMAP).withConfiguration(testUsersAccessibleConfig());

    @Test
    void testUsersAccessible() {
        GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
    }

}