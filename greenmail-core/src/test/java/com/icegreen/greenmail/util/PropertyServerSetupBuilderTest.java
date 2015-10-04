package com.icegreen.greenmail.util;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;

public class PropertyServerSetupBuilderTest {
    @Test
    public void testCreate() {
        Properties properties = new Properties();

        properties.setProperty("greenmail.setup.all", "");
        final PropertyServerSetupBuilder setupBuilder = new PropertyServerSetupBuilder();
        ServerSetup[] setups = setupBuilder.create(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetup.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.imap", "");
        setups = setupBuilder.create(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetup.IMAP});

        properties.clear();
        properties.setProperty("greenmail.setup.test.all", "");
        setups = setupBuilder.create(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetupTest.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        setups = setupBuilder.create(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetupTest.SMTP});

        // Default test setups
        for(ServerSetup setup: ServerSetupTest.ALL) {
            properties.clear();
            properties.setProperty("greenmail."+setup.getProtocol()+".hostname", "127.0.0.1");
            properties.setProperty("greenmail."+setup.getProtocol()+".port", Integer.toString(setup.getPort()));
            setups = setupBuilder.create(properties);
            assert setups != null;
            assertArrayEquals(setups, new ServerSetup[]{setup});
        }

        // Specific setup
        properties.clear();
        properties.setProperty("greenmail.smtp.hostname", "127.0.0.1");
        properties.setProperty("greenmail.smtp.port", "3025");
        properties.setProperty("greenmail.imap.hostname", "127.0.0.1");
        properties.setProperty("greenmail.imap.port", "3143");
        setups = setupBuilder.create(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetupTest.SMTP_IMAP);

    }
}
