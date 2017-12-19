package com.icegreen.greenmail.util;

import java.util.ArrayList;
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
 * Replace PROTOCOL with a value from {@link com.icegreen.greenmail.util.ServerSetup#PROTOCOLS}:
 * <ul>
 * <li>greenmail.PROTOCOL.port</li>
 * <li>greenmail.PROTOCOL.hostname (defaults to {@link ServerSetup#getLocalHostAddress()}</li>
 * </ul>
 * <h2>General settings</h2>
 * <ul>
 * <li>greenmail.startup.timeout : timeout for server startup (defaults to {@link ServerSetup#SERVER_STARTUP_TIMEOUT}<</li>
 * <li>greenmail.hostname : The default hostname to bind to, eg localhost or 0.0.0.0</li>
 * <li>greenmail.verbose : Enables verbose mode including debug output</li>
 * </ul>
 */
public class PropertiesBasedServerSetupBuilder {

    /**
     * Enables verbose JavaMail debug output by setting JavaMail 'mail.debug' property.
     */
    public static final String GREENMAIL_VERBOSE = "greenmail.verbose";

    /**
     * Creates a server setup based on provided properties.
     *
     * @param properties the properties.
     * @return the server setup, or an empty array.
     */
    public ServerSetup[] build(Properties properties) {
        List<ServerSetup> serverSetups = new ArrayList<>();

        String hostname = properties.getProperty("greenmail.hostname", ServerSetup.getLocalHostAddress());
        long serverStartupTimeout =
                Long.parseLong(properties.getProperty("greenmail.startup.timeout", "-1"));

        // Default setups
        addDefaultSetups(hostname, properties, serverSetups);

        // Default setups for test
        addTestSetups(hostname, properties, serverSetups);

        // Default setups
        for (String protocol : ServerSetup.PROTOCOLS) {
            addSetup(hostname, protocol, properties, serverSetups);
        }

        for (ServerSetup setup : serverSetups) {
            if (properties.containsKey(GREENMAIL_VERBOSE)) {
                setup.setVerbose(true);
            }
            if (serverStartupTimeout >= 0L) {
                setup.setServerStartupTimeout(serverStartupTimeout);
            }
        }

        return serverSetups.toArray(new ServerSetup[serverSetups.size()]);
    }

    protected void addSetup(String hostname, String protocol, Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail." + protocol + ".port")) {
            int port = Integer.parseInt(properties.getProperty("greenmail." + protocol + ".port"));
            String setupHostname = properties.getProperty("greenmail." + protocol + ".hostname", hostname);
            ServerSetup setup = new ServerSetup(port, setupHostname, protocol);
            serverSetups.add(setup);
        }
    }


    protected void addTestSetups(String hostname, Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail.setup.test.all")) {
            for (ServerSetup setup : ServerSetupTest.ALL) {
                serverSetups.add(setup.createCopy(hostname));
            }
        }
        if (properties.containsKey("greenmail.setup.test.smtp")) {
            serverSetups.add(ServerSetupTest.SMTP.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.test.smtps")) {
            serverSetups.add(ServerSetupTest.SMTPS.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.test.pop3")) {
            serverSetups.add(ServerSetupTest.POP3.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.test.pop3s")) {
            serverSetups.add(ServerSetupTest.POP3S.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.test.imap")) {
            serverSetups.add(ServerSetupTest.IMAP.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.test.imaps")) {
            serverSetups.add(ServerSetupTest.IMAPS.createCopy(hostname));
        }
    }

    protected void addDefaultSetups(String hostname, Properties properties, List<ServerSetup> serverSetups) {
        if (properties.containsKey("greenmail.setup.all")) {
            for (ServerSetup setup : ServerSetup.ALL) {
                serverSetups.add(setup.createCopy(hostname));
            }
        }
        if (properties.containsKey("greenmail.setup.smtp")) {
            serverSetups.add(ServerSetup.SMTP.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.smtps")) {
            serverSetups.add(ServerSetup.SMTPS.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.pop3")) {
            serverSetups.add(ServerSetup.POP3.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.pop3s")) {
            serverSetups.add(ServerSetup.POP3S.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.imap")) {
            serverSetups.add(ServerSetup.IMAP.createCopy(hostname));
        }
        if (properties.containsKey("greenmail.setup.imaps")) {
            serverSetups.add(ServerSetup.IMAPS.createCopy(hostname));
        }
    }
}
