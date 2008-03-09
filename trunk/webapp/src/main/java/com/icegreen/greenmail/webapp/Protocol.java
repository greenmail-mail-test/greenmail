package com.icegreen.greenmail.webapp;

/**
 * Defines the supported mail protocols and default ports.
 */
enum Protocol {
    /** SMTP */
    SMTP(25),
    /** Secure SMTP */
    SMTPS(465),
    /** POP3 */
    POP3(110),
    /** Secure POP3 */
    POP3S(995),
    /** IMAP */
    IMAP(143),
    /** Secure IMAP */
    IMAPS(993);
    /** Private constructor, including default port */
    private Protocol(final int pPort) {
        port = pPort;
    }

    /** The default port. */
    int port;

    /**
     * Finds the protocol by its default port.
     *
     * @param pPort the default port.
     * @return the protocol.
     */
    static Protocol findByPort(int pPort) {
        for(Protocol p: values()) {
            if(pPort == p.port) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown port "+pPort+", supported ports are "+values());
    }

    @Override
    public String toString() {
        return name() + '(' +Integer.toString(port)+')';
    }
}
