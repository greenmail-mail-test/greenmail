package com.icegreen.greenmail.jboss;

/**
 * The mail service protocols.
 *
 * Basically maps between protocol name and default port.
 *
 * @author mm
 */
public enum ServiceProtocol {
    SMTP("smtp", 25),
    SMTPS("smtps", 465),
    POP3("pop3", 110),
    POP3S("pop3s", 995),
    IMAP("imap", 143),
    IMAPS("imaps", 993);

    private int defaultPort;
    private String name;

    private ServiceProtocol(final String pName, final int pPort) {
        defaultPort = pPort;
        name = pName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    ServiceProtocol findByName(String pName) {
        for(ServiceProtocol protocol: values()) {
            if(protocol.getName().equals(pName)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("No such service protocol "+pName);
    }
}
