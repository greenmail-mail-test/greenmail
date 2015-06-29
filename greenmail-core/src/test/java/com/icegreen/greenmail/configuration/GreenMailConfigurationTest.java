package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.MemorySafeManagers;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testConfigWithMemorySafeManagers;
import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;
import static org.junit.Assert.assertSame;

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
        GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
        greenMail.stop();
    }

    @Test
    public void shouldCreateManagersClassByDefault() {
        // Given
        final GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP);

        // When
        final Managers managers = greenMail.createManagers();

        // Then
        assertSame(Managers.class, managers.getClass());
    }

    @Test
    public void shouldCreateMemorySafeManagersWhenConfigured() {
        // Given
        final GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP).withConfiguration
                (testConfigWithMemorySafeManagers());

        // When
        final Managers managers = greenMail.createManagers();

        // Then
        assertSame(MemorySafeManagers.class, managers.getClass());
    }
}