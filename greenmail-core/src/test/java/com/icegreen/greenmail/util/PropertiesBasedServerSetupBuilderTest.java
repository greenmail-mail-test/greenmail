package com.icegreen.greenmail.util;

import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PropertiesBasedServerSetupBuilderTest {
    @Test
    public void testCreate() {
        Properties properties = new Properties();

        properties.setProperty("greenmail.setup.all", "");
        final PropertiesBasedServerSetupBuilder setupBuilder = new PropertiesBasedServerSetupBuilder();
        ServerSetup[] setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(ServerSetup.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.imap", "");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(new ServerSetup[]{ServerSetup.IMAP});

        properties.clear();
        properties.setProperty("greenmail.setup.test.all", "");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(ServerSetupTest.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(new ServerSetup[]{ServerSetupTest.SMTP});

        // With debug
        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty(PropertiesBasedServerSetupBuilder.GREENMAIL_VERBOSE, "");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(new ServerSetup[]{ServerSetupTest.SMTP});
        assertThat(setups[0].isVerbose()).isTrue();

        // With hostname
        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty("greenmail.hostname", "0.0.0.0");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(new ServerSetup[]{ServerSetupTest.SMTP.createCopy("0.0.0.0")});
        assertThat(setups[0].isVerbose()).isFalse();


        // Default test setups
        for (ServerSetup setup : ServerSetupTest.ALL) {
            properties.clear();
            properties.setProperty("greenmail." + setup.getProtocol() + ".hostname", "127.0.0.1");
            properties.setProperty("greenmail." + setup.getProtocol() + ".port", Integer.toString(setup.getPort()));
            setups = setupBuilder.build(properties);
            assertThat(setups).isEqualTo(new ServerSetup[]{setup});
        }

        // Specific setup
        properties.clear();
        properties.setProperty("greenmail.smtp.hostname", "127.0.0.1");
        properties.setProperty("greenmail.smtp.port", "3025");
        properties.setProperty("greenmail.imap.hostname", "127.0.0.1");
        properties.setProperty("greenmail.imap.port", "3143");
        setups = setupBuilder.build(properties);
        assertThat(setups).isEqualTo(ServerSetupTest.SMTP_IMAP);
    }
}
