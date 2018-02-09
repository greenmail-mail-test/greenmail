/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.util.Properties;

/**
 * Defines the default ports
 * <table>
 * <tr><td>smtp</td><td>25</td></tr>
 * <tr><td>smtps</td><td>465</td></tr>
 * <tr><td>pop3</td><td>110</td></tr>
 * <tr><td>pop3s</td><td>995</td></tr>
 * <tr><td>imap</td><td>143</td></tr>
 * <tr><td>imaps</td><td>993</td></tr>
 * </table>
 * Use {@link ServerSetupTest} for non-default ports
 *
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 * Use {@link ServerSetupTest} for non-default ports
 */
public class ServerSetup {
    public static final String PROTOCOL_SMTP = "smtp";
    public static final String PROTOCOL_SMTPS = "smtps";
    public static final String PROTOCOL_POP3 = "pop3";
    public static final String PROTOCOL_POP3S = "pop3s";
    public static final String PROTOCOL_IMAP = "imap";
    public static final String PROTOCOL_IMAPS = "imaps";
    public static final String[] PROTOCOLS = {PROTOCOL_SMTP, PROTOCOL_SMTPS, PROTOCOL_IMAP, PROTOCOL_IMAPS, PROTOCOL_POP3, PROTOCOL_POP3S};

    public static final int PORT_SMTP = 25;
    public static final int PORT_SMTPS = 465;
    public static final int PORT_POP3 = 110;
    public static final int PORT_POP3S = 995;
    public static final int PORT_IMAP = 143;
    public static final int PORT_IMAPS = 993;

    public static final ServerSetup SMTP = new ServerSetup(PORT_SMTP, null, PROTOCOL_SMTP);
    public static final ServerSetup SMTPS = new ServerSetup(PORT_SMTPS, null, PROTOCOL_SMTPS);
    public static final ServerSetup POP3 = new ServerSetup(PORT_POP3, null, PROTOCOL_POP3);
    public static final ServerSetup POP3S = new ServerSetup(PORT_POP3S, null, PROTOCOL_POP3S);
    public static final ServerSetup IMAP = new ServerSetup(PORT_IMAP, null, PROTOCOL_IMAP);
    public static final ServerSetup IMAPS = new ServerSetup(PORT_IMAPS, null, PROTOCOL_IMAPS);

    public static final ServerSetup[] SMTP_POP3 = new ServerSetup[]{SMTP, POP3};
    public static final ServerSetup[] SMTP_IMAP = new ServerSetup[]{SMTP, IMAP};
    public static final ServerSetup[] SMTP_POP3_IMAP = new ServerSetup[]{SMTP, POP3, IMAP};

    public static final ServerSetup[] SMTPS_POP3S = new ServerSetup[]{SMTPS, POP3S};
    public static final ServerSetup[] SMTPS_POP3S_IMAPS = new ServerSetup[]{SMTPS, POP3S, IMAPS};
    public static final ServerSetup[] SMTPS_IMAPS = new ServerSetup[]{SMTPS, IMAPS};

    public static final ServerSetup[] ALL = new ServerSetup[]{SMTP, SMTPS, POP3, POP3S, IMAP, IMAPS};


    /**
     * Default socket read timeout. See JavaMail session properties.
     */
    public static final long READ_TIMEOUT = 15000L;
    /**
     * Default socket connection timeout. See JavaMail session properties.
     */
    public static final long CONNECTION_TIMEOUT = 15000L;
    /**
     * Default server startup timeout in milliseconds.
     */
    public static final long SERVER_STARTUP_TIMEOUT = 1000L;

    private static final String MAIL_DOT = "mail.";

    private final int port;
    private final String bindAddress;
    private final String protocol;
    private long readTimeout = -1L;
    private long connectionTimeout = -1L;
    private long writeTimeout = -1L;
    private boolean verbose = false;

    /**
     * Timeout when GreenMail starts a server, in milliseconds.
     */
    private long serverStartupTimeout = SERVER_STARTUP_TIMEOUT;

    public ServerSetup(int port, String bindAddress, String protocol) {
        this.port = port;
        if (null == bindAddress || bindAddress.length() == 0) {
            this.bindAddress = getLocalHostAddress();
        } else {
            this.bindAddress = bindAddress;
        }
        this.protocol = protocol;
    }

    public static String getLocalHostAddress() {
        // Always pretend that we are 127.0.0.1. Doesn't matter what we return here and we have no way of guessing the
        // "correct" address anyways if we have multiple external interfaces.
        // InetAddress.getLocalHost().getHostAddress() is unreliable.
        return "127.0.0.1";
    }

    /**
     * Gets the public default host address "0.0.0.0" .
     *
     * @return the public IP host address.
     */
    public String getDefaultBindAddress() {
        return "0.0.0.0";
    }

    public boolean isSecure() {
        return protocol.endsWith("s");
    }

    public String getProtocol() {
        return protocol;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public int getPort() {
        return port;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public long getServerStartupTimeout() {
        return serverStartupTimeout;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @param verbose if true enables JavaMail debug output by setting JavaMail property  'mail.debug'
     */
    public ServerSetup setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /**
     * Sets the server startup timeout in milliseconds.
     *
     * @param timeoutInMs timeout in milliseconds.
     */
    public void setServerStartupTimeout(long timeoutInMs) {
        this.serverStartupTimeout = timeoutInMs;
    }

    /**
     * Creates default properties for a JavaMail session.
     * Concrete server implementations can add protocol specific settings.
     * <p/>
     * For details see
     * <ul>
     * <li>http://docs.oracle.com/javaee/6/api/javax/mail/package-summary.html for some general settings</li>
     * <li>https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html for valid SMTP properties.</li>
     * <li>https://javamail.java.net/nonav/docs/api/com/sun/mail/imap/package-summary.html for valid IMAP properties</li>
     * <li>https://javamail.java.net/nonav/docs/api/com/sun/mail/pop3/package-summary.html for valid POP3 properties.</li>
     * </ul
     *
     * @param properties additional and optional properties which overwrite automatically added properties. Can be null.
     * @param debug      sets JavaMail debug properties
     * @return default properties.
     */
    public Properties configureJavaMailSessionProperties(Properties properties, boolean debug) {
        Properties props = new Properties();

        if (debug) {
            props.setProperty("mail.debug", "true");
//            System.setProperty("mail.socket.debug", "true");
        }

        // Set local host address (makes tests much faster. If this is not set java mail always looks for the address)
        props.setProperty(MAIL_DOT + getProtocol() + ".localaddress", String.valueOf(ServerSetup.getLocalHostAddress()));
        props.setProperty(MAIL_DOT + getProtocol() + ".port", String.valueOf(getPort()));
        props.setProperty(MAIL_DOT + getProtocol() + ".host", String.valueOf(getBindAddress()));

        if (isSecure()) {
            props.put(MAIL_DOT + getProtocol() + ".starttls.enable", Boolean.TRUE);
            props.setProperty(MAIL_DOT + getProtocol() + ".socketFactory.class", DummySSLSocketFactory.class.getName());
            props.setProperty(MAIL_DOT + getProtocol() + ".socketFactory.fallback", "false");
        }

        // Timeouts
        props.setProperty(MAIL_DOT + getProtocol() + ".connectiontimeout",
                Long.toString(getConnectionTimeout() < 0L ? ServerSetup.CONNECTION_TIMEOUT : getConnectionTimeout()));
        props.setProperty(MAIL_DOT + getProtocol() + ".timeout",
                Long.toString(getReadTimeout() < 0L ? ServerSetup.READ_TIMEOUT : getReadTimeout()));
        // Note: "mail." + setup.getProtocol() + ".writetimeout" breaks TLS/SSL Dummy Socket and makes tests run 6x slower!!!
        //       Therefore we do not by default configure writetimeout.
        if (getWriteTimeout() >= 0L) {
            props.setProperty(MAIL_DOT + getProtocol() + ".writetimeout", Long.toString(getWriteTimeout()));
        }

        // Protocol specific extensions
        if (getProtocol().startsWith(PROTOCOL_SMTP)) {
            props.setProperty("mail.transport.protocol", getProtocol());
            props.setProperty("mail.transport.protocol.rfc822", getProtocol());
        }

        // Auto configure stores.
        props.setProperty("mail.store.protocol", getProtocol());

        // Merge with optional additional properties
        if (null != properties && !properties.isEmpty()) {
            props.putAll(properties);
        }

        return props;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerSetup)) return false;

        ServerSetup that = (ServerSetup) o;

        if (port != that.port) return false;
        if (readTimeout != that.readTimeout) return false;
        if (connectionTimeout != that.connectionTimeout) return false;
        if (writeTimeout != that.writeTimeout) return false;
        if (serverStartupTimeout != that.serverStartupTimeout) return false;
        if (bindAddress != null ? !bindAddress.equals(that.bindAddress) : that.bindAddress != null) return false;
        return !(protocol != null ? !protocol.equals(that.protocol) : that.protocol != null);

    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (bindAddress != null ? bindAddress.hashCode() : 0);
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (int) (readTimeout ^ (readTimeout >>> 32));
        result = 31 * result + (int) (connectionTimeout ^ (connectionTimeout >>> 32));
        result = 31 * result + (int) (writeTimeout ^ (writeTimeout >>> 32));
        result = 31 * result + (int) (serverStartupTimeout ^ (serverStartupTimeout >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ServerSetup{" +
                "port=" + port +
                ", bindAddress='" + bindAddress + '\'' +
                ", protocol='" + protocol + '\'' +
                ", readTimeout=" + readTimeout +
                ", connectionTimeout=" + connectionTimeout +
                ", writeTimeout=" + writeTimeout +
                ", serverStartupTimeout=" + serverStartupTimeout +
                ", verbose=" + isVerbose() +
                '}';
    }

    /**
     * Create a deep copy.
     *
     * @return a copy of the server setup configuration.
     */
    public ServerSetup createCopy() {
        return createCopy(getBindAddress());
    }

    /**
     * Create a deep copy.
     *
     * @param bindAddress overwrites bind address when creating deep copy.
     * @return a copy of the server setup configuration.
     */
    public ServerSetup createCopy(String bindAddress) {
        ServerSetup setup = new ServerSetup(getPort(), bindAddress, getProtocol());
        setup.setServerStartupTimeout(getServerStartupTimeout());
        setup.setConnectionTimeout(getConnectionTimeout());
        setup.setReadTimeout(getReadTimeout());
        setup.setWriteTimeout(getWriteTimeout());
        setup.setVerbose(isVerbose());

        return setup;
    }

    /**
     * Creates a copy with verbose mode enabled.
     *
     * @param serverSetups the server setups.
     * @return copies of server setups with verbose mode enabled.
     */
    public static ServerSetup[] verbose(ServerSetup[] serverSetups) {
        ServerSetup[] copies = new ServerSetup[serverSetups.length];
        for (int i = 0; i < serverSetups.length; i++) {
            copies[i] = serverSetups[i].createCopy().setVerbose(true);
        }
        return copies;
    }
}
