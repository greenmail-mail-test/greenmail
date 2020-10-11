package com.icegreen.greenmail.configuration;

import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

public class PropertiesBasedGreenMailConfigurationBuilderTest {
    @Test
    public void testBuildForSingleUser() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertThat(config).isNotNull();
        assertThat(config.getUsersToCreate()).hasSize(1);
        assertThat(config.getUsersToCreate().get(0)).isEqualTo(new UserBean("foo1@bar.com", "foo1", "pwd1"));
    }

    @Test
    public void testBuildForListOfUsers() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com,foo2:pwd2,foo3:pwd3@bar3.com");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertThat(config).isNotNull();
        assertThat(config.getUsersToCreate()).hasSize(3);
        assertThat(config.getUsersToCreate().get(0)).isEqualTo(new UserBean("foo1@bar.com", "foo1", "pwd1"));
        assertThat(config.getUsersToCreate().get(1)).isEqualTo(new UserBean("foo2", "foo2", "pwd2"));
        assertThat(config.getUsersToCreate().get(2)).isEqualTo(new UserBean("foo3@bar3.com", "foo3", "pwd3"));
    }

    @Test
    public void testBuildForListOfUsersWithEmailAsLogin() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "foo1:pwd1@bar.com,foo2:pwd2,foo3:pwd3@bar3.com");
        props.setProperty(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS_LOGIN,"email");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertThat(config).isNotNull();
        assertThat(config.getUsersToCreate()).hasSize(3);
        assertThat(config.getUsersToCreate().get(0)).isEqualTo(new UserBean("foo1@bar.com", "foo1@bar.com", "pwd1"));
        assertThat(config.getUsersToCreate().get(1)).isEqualTo(new UserBean("foo2", "foo2", "pwd2"));
        assertThat(config.getUsersToCreate().get(2)).isEqualTo(new UserBean("foo3@bar3.com", "foo3@bar3.com", "pwd3"));
    }

    @Test
    public void testBuildWithAuthenticationDisabledSetting() {
        Properties props = createPropertiesFor(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_AUTH_DISABLED, "");
        GreenMailConfiguration config = new PropertiesBasedGreenMailConfigurationBuilder().build(props);

        assertThat(config).isNotNull();
        assertThat(config.isAuthenticationDisabled()).isTrue();
    }

    private Properties createPropertiesFor(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return props;
    }
}
