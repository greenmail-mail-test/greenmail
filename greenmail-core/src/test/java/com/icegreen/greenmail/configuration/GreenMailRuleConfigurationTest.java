package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

/**
 * Tests that check if the GreenMailConfiguration is applied correctly to GreenMailRules
 */
public class GreenMailRuleConfigurationTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP).withConfiguration(
            testUsersAccessibleConfig()
    );

    @Test
    public void testUsersAccessible() {
        GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
    }

}