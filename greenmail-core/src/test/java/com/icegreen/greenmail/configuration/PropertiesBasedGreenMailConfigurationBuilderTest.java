package com.icegreen.greenmail.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class PropertiesBasedGreenMailConfigurationBuilderTest {

    @Test
    public void testBuildForSingleUser() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertEquals(1, config.getUsersToCreate().size());
        assertEquals(new UserBean("foo1@bar.com", "foo1", "pwd1"), config.getUsersToCreate().get(0));
        assertNull(config.getMailsinkUser());
        assertFalse(config.hasMailsinkUser());
    }

    @Test
    public void testBuildForListOfUsers() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com,foo2:pwd2,foo3:pwd3@bar3.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertEquals(3, config.getUsersToCreate().size());
        assertEquals(new UserBean("foo1@bar.com", "foo1", "pwd1"), config.getUsersToCreate().get(0));
        assertEquals(new UserBean("foo2", "foo2", "pwd2"), config.getUsersToCreate().get(1));
        assertEquals(new UserBean("foo3@bar3.com", "foo3", "pwd3"), config.getUsersToCreate().get(2));
        assertNull(config.getMailsinkUser());
        assertFalse(config.hasMailsinkUser());
    }

    @Test
    public void testBuildWithAuthenticationDisabledSetting() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_AUTH_DISABLED, "");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertTrue(config.isAuthenticationDisabled());
        assertNull(config.getMailsinkUser());
        assertFalse(config.hasMailsinkUser());
    }

    @Test
    public void testBuildWithMailsinkUserWithDomain() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_MAILSINK_USER, "foo1:pwd1@bar.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertNotNull(config.getMailsinkUser());
        assertTrue(config.hasMailsinkUser());
        assertEquals(new UserBean("foo1@bar.com", "foo1", "pwd1"), config.getMailsinkUser());
        assertTrue(config.keepMailsinkInOriginalMailboxes());
    }

    @Test
    public void testBuildWithMailsinkUserWithoutDomain() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_MAILSINK_USER, "foo1:pwd1");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);
        assertNotNull(config);
        assertNotNull(config.getMailsinkUser());
        assertTrue(config.hasMailsinkUser());
        assertEquals(new UserBean("foo1", "foo1", "pwd1"), config.getMailsinkUser());
        assertTrue(config.keepMailsinkInOriginalMailboxes());
    }

    @Test
    public void testBuildWithMailsinkPropertyFlag() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder
                .GREENMAIL_MAILSINK_KEEP_IN_ORIG_MBOX, "false");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);
        assertNotNull(config);
        assertFalse(config.keepMailsinkInOriginalMailboxes());
        assertNull(config.getMailsinkUser());
        assertFalse(config.hasMailsinkUser());
    }

    private Properties createPropertiesFor(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return props;
    }
}
