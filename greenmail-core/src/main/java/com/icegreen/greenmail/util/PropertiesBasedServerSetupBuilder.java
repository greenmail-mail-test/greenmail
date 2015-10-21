package com.icegreen.greenmail.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Creates a GreenMail server setup configuration based on System properties.
 * <p/>
 * Default setups using well known ports:
 * <ul>
 * <li>greenmail.setup.all : {@link com.icegreen.greenmail.util.ServerSetup#ALL}</li>
 * <li>greenmail.setup.smtp : {@link com.icegreen.greenmail.util.ServerSetup#SMTP}</li>
 * <li>greenmail.setup.smtps : {@link com.icegreen.greenmail.util.ServerSetup#SMTPS}</li>
 * <li>greenmail.setup.imap : {@link com.icegreen.greenmail.util.ServerSetup#IMAP}</li>
 * <li>greenmail.setup.imaps : {@link com.icegreen.greenmail.util.ServerSetup#IMAPS}</li>
 * <li>greenmail.setup.pop3 : {@link com.icegreen.greenmail.util.ServerSetup#POP3}</li>
 * <li>greenmail.setup.pop3s : {@link com.icegreen.greenmail.util.ServerSetup#POP3S}</li>
 * </ul>
 * <p/>
 * Default test setups with added offsets to well known ports:
 * <ul>
 * <li>greenmail.setup.test.all : {@link com.icegreen.greenmail.util.ServerSetupTest#ALL}</li>
 * <li>greenmail.setup.test.smtp : {@link com.icegreen.greenmail.util.ServerSetupTest#SMTP}</li>
 * <li>greenmail.setup.test.smtps : {@link com.icegreen.greenmail.util.ServerSetupTest#SMTPS}</li>
 * <li>greenmail.setup.test.imap : {@link com.icegreen.greenmail.util.ServerSetupTest#IMAP}</li>
 * <li>greenmail.setup.test.imaps : {@link com.icegreen.greenmail.util.ServerSetupTest#IMAPS}</li>
 * <li>greenmail.setup.test.pop3 : {@link com.icegreen.greenmail.util.ServerSetupTest#POP3}</li>
 * <li>greenmail.setup.test.pop3s : {@link com.icegreen.greenmail.util.ServerSetupTest#POP3S}</li>
 * </ul>
 * <p/>
 * <h2>Protocol specific setups</h2>
 * Replace PROTOCOL with a value from {@value com.icegreen.greenmail.util.ServerSetup#PROTOCOL}:
 * <ul>
 * <li>greenmail.PROTOCOL.port</li>
 * <li>greenmail.PROTOCOL.hostname (defaults to {@link ServerSetup#getLocalHostAddress()}</li>
 * </ul>
 */
public class PropertiesBasedServerSetupBuilder {

    /**
     * Creates a server setup based on provided properties.
     *
     * @param properties the properties.
     * @return the server setup, or an empty array.
     */
    public ServerSetup[] build(Properties properties) {
        List<ServerSetup> serverSetups = new ArrayList<ServerSetup>();

        // Default setups
        addDefaultSetups(properties, serverSetups);

        // Default setups for test
        addTestSetups(properties, serverSetups);

        // Default setups
        for (String protocol : ServerSetup.PROTOCOLS) {
            addSetup(protocol, properties, serverSetups);
        }

        return serverSetups.toArray(new ServerSetup[serverSetups.size()]);
    }

    protected void addSetup(String protocol, Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail." + protocol + ".port")) {
            int port = Integer.parseInt(properties.getProperty("greenmail." + protocol + ".port"));
            String hostname = properties.getProperty("greenmail." + protocol + ".hostname", ServerSetup.getLocalHostAddress());
            ServerSetup setup = new ServerSetup(port, hostname, protocol);
            serverSetups.add(setup);
        }
    }


    protected void addTestSetups(Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail.setup.test.all")) {
            serverSetups.addAll(Arrays.asList(ServerSetupTest.ALL));
        }
        if (properties.containsKey("greenmail.setup.test.smtp")) {
            serverSetups.add(ServerSetupTest.SMTP);
        }
        if (properties.containsKey("greenmail.setup.test.smtps")) {
            serverSetups.add(ServerSetupTest.SMTPS);
        }
        if (properties.containsKey("greenmail.setup.test.pop3")) {
            serverSetups.add(ServerSetupTest.POP3);
        }
        if (properties.containsKey("greenmail.setup.test.pop3s")) {
            serverSetups.add(ServerSetupTest.POP3S);
        }
        if (properties.containsKey("greenmail.setup.test.imap")) {
            serverSetups.add(ServerSetupTest.IMAP);
        }
        if (properties.containsKey("greenmail.setup.test.imaps")) {
            serverSetups.add(ServerSetupTest.IMAPS);
        }
    }

    protected void addDefaultSetups(Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail.setup.all")) {
            serverSetups.addAll(Arrays.asList(ServerSetup.ALL));
        }

        if (properties.containsKey("greenmail.setup.smtp")) {
            serverSetups.add(ServerSetup.SMTP);
        }
        if (properties.containsKey("greenmail.setup.smtps")) {
            serverSetups.add(ServerSetup.SMTPS);
        }
        if (properties.containsKey("greenmail.setup.pop3")) {
            serverSetups.add(ServerSetup.POP3);
        }
        if (properties.containsKey("greenmail.setup.pop3s")) {
            serverSetups.add(ServerSetup.POP3S);
        }
        if (properties.containsKey("greenmail.setup.imap")) {
            serverSetups.add(ServerSetup.IMAP);
        }
        if (properties.containsKey("greenmail.setup.imaps")) {
            serverSetups.add(ServerSetup.IMAPS);
        }
    }
}
