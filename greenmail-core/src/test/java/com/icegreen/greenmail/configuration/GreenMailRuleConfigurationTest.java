package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.Rule;
import org.junit.Test;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

/**
 * Tests that check if the GreenMailConfiguration is applied correctly to GreenMailRules
 */
public class GreenMailRuleConfigurationTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetup.IMAP).withConfiguration(
            testUsersAccessibleConfig()
    );

    @Test
    public void testUsersAccessible() {
        GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
    }

}