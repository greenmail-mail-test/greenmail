/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail.util;

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


    public static final ServerSetup SMTP = new ServerSetup(25, null, PROTOCOL_SMTP);
    public static final ServerSetup SMTPS = new ServerSetup(465, null, PROTOCOL_SMTPS);
    public static final ServerSetup POP3 = new ServerSetup(110, null, PROTOCOL_POP3);
    public static final ServerSetup POP3S = new ServerSetup(995, null, PROTOCOL_POP3S);
    public static final ServerSetup IMAP = new ServerSetup(143, null, PROTOCOL_IMAP);
    public static final ServerSetup IMAPS = new ServerSetup(993, null, PROTOCOL_IMAPS);

    public static final ServerSetup[] SMTP_POP3 = new ServerSetup[]{SMTP, POP3};
    public static final ServerSetup[] SMTP_IMAP = new ServerSetup[]{SMTP, IMAP};
    public static final ServerSetup[] SMTP_POP3_IMAP = new ServerSetup[]{SMTP, POP3, IMAP};

    public static final ServerSetup[] SMTPS_POP3S = new ServerSetup[]{SMTPS, POP3S};
    public static final ServerSetup[] SMTPS_POP3S_IMAPS = new ServerSetup[]{SMTPS, POP3S, IMAPS};
    public static final ServerSetup[] SMTPS_IMAPS = new ServerSetup[]{SMTPS, IMAPS};

    public static final ServerSetup[] ALL = new ServerSetup[]{SMTP, SMTPS, POP3, POP3S, IMAP, IMAPS};


    private final int port;
    private final String bindAddress;
    private final String protocol;

    public ServerSetup(int port, String bindAddress, String protocol) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.protocol = protocol;
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
}
