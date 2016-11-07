package com.icegreen.greenmail.configuration;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that check if the GreenMailConfiguration is applied correctly to GreenMailRules
 */
public class GreenMailRuleConfigurationTest {

    @Rule
    public GreenMailRuleWithStoreChooser
            greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.IMAP, testUsersAccessibleConfig());

    @Test
    @StoreChooser(store="file,memory")
    public void testUsersAccessible() {
        GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
    }

}