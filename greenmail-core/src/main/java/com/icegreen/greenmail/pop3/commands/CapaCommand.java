package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;

/**
 * Handles the CAPA command.
 *
 * See https://www.ietf.org/rfc/rfc2449.txt
 *
 * Arguments: none
 *
 * Restrictions:
 *   None
 *
 * Discussion:
 *
 *
 * Possible Responses:
 *   +OK
 *   List of capabilities
 *
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
        // We don't support any additional capabilities
        conn.println("+OK");
        conn.println("UIDL");
        conn.println("SASL "+ AuthCommand.Pop3SaslAuthMechanism.list());
        conn.println(".");
    }
}
