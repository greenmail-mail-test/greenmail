package com.icegreen.greenmail.util;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesBasedServerSetupBuilderTest {
    @Test
    public void testCreate() {
        Properties properties = new Properties();

        properties.setProperty("greenmail.setup.all", "");
        final PropertiesBasedServerSetupBuilder setupBuilder = new PropertiesBasedServerSetupBuilder();
        ServerSetup[] setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetup.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.imap", "");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetup.IMAP});

        properties.clear();
        properties.setProperty("greenmail.setup.test.all", "");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetupTest.ALL);

        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetupTest.SMTP});

        // With debug
        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty(PropertiesBasedServerSetupBuilder.GREENMAIL_VERBOSE, "");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetupTest.SMTP});
        assertTrue(setups[0].isVerbose());

        // With hostname
        properties.clear();
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty("greenmail.hostname", "0.0.0.0");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, new ServerSetup[]{ServerSetupTest.SMTP.createCopy("0.0.0.0")});
        assertFalse(setups[0].isVerbose());


        // Default test setups
        for(ServerSetup setup: ServerSetupTest.ALL) {
            properties.clear();
            properties.setProperty("greenmail."+setup.getProtocol()+".hostname", "127.0.0.1");
            properties.setProperty("greenmail."+setup.getProtocol()+".port", Integer.toString(setup.getPort()));
            setups = setupBuilder.build(properties);
            assert setups != null;
            assertArrayEquals(setups, new ServerSetup[]{setup});
        }

        // Specific setup
        properties.clear();
        properties.setProperty("greenmail.smtp.hostname", "127.0.0.1");
        properties.setProperty("greenmail.smtp.port", "3025");
        properties.setProperty("greenmail.imap.hostname", "127.0.0.1");
        properties.setProperty("greenmail.imap.port", "3143");
        setups = setupBuilder.build(properties);
        assert setups != null;
        assertArrayEquals(setups, ServerSetupTest.SMTP_IMAP);

    }
}
