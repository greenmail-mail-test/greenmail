package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

import jakarta.mail.Flags;
import java.util.List;

/**
 * Handles the RSET command.
 *
 * See http://www.ietf.org/rfc/rfc1939.txt:
 *
 * Arguments: none
 *
 * Restrictions:
 *   May only be given in the TRANSACTION state.
 *
 * Discussion:
 *   If any messages have been marked as deleted by the POP3
 *   server, they are unmarked.  The POP3 server then replies
 *   with a positive response.
 *
 * Possible Responses:
 *   +OK
 *
 * Examples:
 *   C: RSET
 *   S: +OK maildrop has 2 messages (320 octets)
 *
 * @author Marcel May
 * @version $Id: $
 * @since Dec 21, 2006
 */
public class RsetCommand extends Pop3Command {
    @Override
    public boolean isValidForState(Pop3State state) {
        return true;
    }

    @Override
    public void execute(Pop3Connection conn, Pop3State state, String cmd) {
        conn.println("+OK");
        try {
            MailFolder inbox = state.getFolder();
            List<StoredMessage> msgList = inbox.getMessages();
            int count = 0;
            for (StoredMessage msg : msgList) {
                if (msg.isSet(Flags.Flag.DELETED)) {
                    count++;
                    msg.setFlag(Flags.Flag.DELETED, false);
                }
            }

            conn.println("+OK maildrop has "+count+" messages undeleted.");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}
