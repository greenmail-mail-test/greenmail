package com.icegreen.greenmail.pop3.commands;

import java.util.List;
import javax.mail.Flags;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;

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
    public boolean isValidForState(Pop3State state) {
        return true;
    }

    public void execute(Pop3Connection conn, Pop3State state, String cmd) {
        conn.println("+OK");
        try {
            MailFolder inbox = state.getFolder();
            List msgList = inbox.getMessages();
            int count = 0;
            for(int i=0;i<msgList.size();i++) {
                SimpleStoredMessage msg = (SimpleStoredMessage) msgList.get(i);
                Flags flags = msg.getFlags();
                if (flags.contains(Flags.Flag.DELETED)) {
                    count++;
                    flags.remove(Flags.Flag.DELETED);
                }
            }

            conn.println("+OK maildrop has "+count+" messages undeleted.");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}
