/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.util;

/**
 * Defines a series of non-default ports for test purposes.
 * The ports for the various protocols are the default ones plus an offset which is 3000.
 * i.e.
 * <table>
 * <tr><td>smtp</td><td>3025</td></tr>
 * <tr><td>smtps</td><td>3465</td></tr>
 * <tr><td>pop3</td><td>3110</td></tr>
 * <tr><td>pop3s</td><td>3995</td></tr>
 * <tr><td>imap</td><td>3143</td></tr>
 * <tr><td>imaps</td><td>3993</td></tr>
 * </table>
 * Use {@link ServerSetup} for default ports
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 * @see {@link ServerSetup}
 */
public class ServerSetupTest {
    public static int portOffset = 3000;

    public static final ServerSetup SMTP = new ServerSetup(25+portOffset, null, ServerSetup.PROTOCOL_SMTP);
    public static final ServerSetup SMTPS = new ServerSetup(465+portOffset, null, ServerSetup.PROTOCOL_SMTPS);
    public static final ServerSetup POP3 = new ServerSetup(110+portOffset, null, ServerSetup.PROTOCOL_POP3);
    public static final ServerSetup POP3S = new ServerSetup(995+portOffset, null, ServerSetup.PROTOCOL_POP3S);
    public static final ServerSetup IMAP = new ServerSetup(143+portOffset, null, ServerSetup.PROTOCOL_IMAP);
    public static final ServerSetup IMAPS = new ServerSetup(993+portOffset, null, ServerSetup.PROTOCOL_IMAPS);

    public static final ServerSetup[] SMTP_POP3 = new ServerSetup[]{SMTP, POP3};
    public static final ServerSetup[] SMTP_IMAP = new ServerSetup[]{SMTP, IMAP};
    public static final ServerSetup[] SMTP_POP3_IMAP = new ServerSetup[]{SMTP, POP3, IMAP};

    public static final ServerSetup[] SMTPS_POP3S = new ServerSetup[]{SMTPS, POP3S};
    public static final ServerSetup[] SMTPS_POP3S_IMAPS = new ServerSetup[]{SMTPS, POP3S, IMAPS};
    public static final ServerSetup[] SMTPS_IMAPS = new ServerSetup[]{SMTPS, IMAPS};

    public static final ServerSetup[] ALL = new ServerSetup[]{SMTP, SMTPS, POP3, POP3S, IMAP, IMAPS};

    public static int getPortOffset() {
        return portOffset;
    }

    public static void setPortOffset(int portOffset) {
        ServerSetupTest.portOffset = portOffset;
    }

}
