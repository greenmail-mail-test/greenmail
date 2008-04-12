package com.icegreen.greenmail.webapp;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

/**
 * DESC
 *
 * @author mm
 */
public class Configuration {
    /** Default hostname ({@value} */
    public static final String DEFAULT_HOSTNAME = "localhost";
    /**
     * Default port offset added to SMTP/IMAP/POP3/... default ports ({@value}).
     *
     * Example: A port offset of 10000 results in SMTP port 10025.
     */
    public static final int DEFAULT_PORT_OFFSET = 10000;

    /**
     * A mail service configuration entry.
     *
     * An entry contains a mandatory protocol and optional hostname and port.
     * If the hostname and port are not configured, GreenMail uses
     * the default hostname and the default protocol port plus the port offset.
     */
    static class ServiceConfiguration {
        Protocol protocol;
        String hostname;
        int port;
    }
    static class User {
        String login;
        String password;
        String email;
    }

    private String defaultHostname;
    private int portOffset;
    private List<ServiceConfiguration> services;
    private List<User> users;

    /**
     * Initializes configuration with
     */
    public Configuration() {
        services = new ArrayList<ServiceConfiguration>();
        defaultHostname = DEFAULT_HOSTNAME;
        portOffset = DEFAULT_PORT_OFFSET;
        users = new ArrayList<User>();
    }

    public String getDefaultHostname() {
        return defaultHostname;
    }

    public void setDefaultHostname(final String pDefaultHostname) {
        defaultHostname = pDefaultHostname;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public void setPortOffset(final int pPortOffset) {
        portOffset = pPortOffset;
    }

    public void addUser(final User pUser) {
        users.add(pUser);
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void addServiceConfiguration(final ServiceConfiguration pServiceConfiguration) {
        services.add(pServiceConfiguration);
    }

    public List<ServiceConfiguration> getServiceConfigurations() {
        return Collections.unmodifiableList(services);
    }

    public ServiceConfiguration getServiceConfigurationByProtocol(final Protocol pProtocol) {
        for(ServiceConfiguration c: services) {
            if(pProtocol.equals(c.protocol)) {
                return c;
            }
        }
        return null;
    }
}
