package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.server.BuildInfo;

/**
 * Handles the CAPA command.
 * <p>
 * See <a href="https://www.ietf.org/rfc/rfc2449.txt">"https://www.ietf.org/rfc/rfc2449.txt</a>
 * <p>
 * Arguments: none
 * <p>
 * Restrictions:
 *   None
 * <p>
 * Discussion:
 * <p>
 *
 * Possible Responses:
 *   +OK
 *   List of capabilities
 * <p>
 * Examples:
 *   C: CAPA
 *   S: +OK
 *   S: UIDL
 *   S: .
 */
public class CapaCommand extends Pop3Command {
    @Override
    public boolean isValidForState(Pop3State state) {
        return true;
    }

    @Override
    public void execute(Pop3Connection conn, Pop3State state, String cmd) {
        conn.println("+OK");
        conn.println("IMPLEMENTATION GreenMail Server v" + BuildInfo.INSTANCE.getProjectVersion());
        conn.println("UIDL");
        conn.println("TOP");
        conn.println("USER");
        conn.println("SASL "+ AuthCommand.Pop3SaslAuthMechanism.list());
        conn.println(".");
    }
}
