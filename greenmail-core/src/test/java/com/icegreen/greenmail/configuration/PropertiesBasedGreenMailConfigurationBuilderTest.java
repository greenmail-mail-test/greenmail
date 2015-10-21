package com.icegreen.greenmail.configuration;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertiesBasedGreenMailConfigurationBuilderTest {
    @Test
    public void testBuildForSingleUser() {
        Properties props = new Properties();
        props.setProperty(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS, "foo1:pwd1@bar.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertEquals(1, config.getUsersToCreate().size());
        assertEquals(new UserBean("foo1@bar.com", "foo1", "pwd1"), config.getUsersToCreate().get(0));
    }

    @Test
    public void testBuildForListOfUsers() {
        Properties props = new Properties();
        props.setProperty(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com,foo2:pwd2,foo3:pwd3@bar3.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertNotNull(config);
        assertEquals(3, config.getUsersToCreate().size());
        assertEquals(new UserBean("foo1@bar.com", "foo1", "pwd1"), config.getUsersToCreate().get(0));
        assertEquals(new UserBean("foo2", "foo2", "pwd2"), config.getUsersToCreate().get(1));
        assertEquals(new UserBean("foo3@bar3.com", "foo3", "pwd3"), config.getUsersToCreate().get(2));
    }
}
