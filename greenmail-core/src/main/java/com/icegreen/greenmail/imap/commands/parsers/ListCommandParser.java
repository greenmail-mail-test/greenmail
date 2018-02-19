package com.icegreen.greenmail.imap.commands.parsers;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.sun.mail.imap.protocol.BASE64MailboxDecoder;

public class ListCommandParser extends CommandParser {
    /**
     * Reads an argument of type "list_mailbox" from the request, which is
     * the second argument for a LIST or LSUB command. Valid values are a "string"
     * argument, an "atom" with wildcard characters.
     *
     * @return An argument of type "list_mailbox"
     */
    public String listMailbox(ImapRequestLineReader request) throws ProtocolException {
        char next = request.nextWordChar();
        String name;
        switch (next) {
        case '"':
            name = consumeQuoted(request);
            break;
        case '{':
            name = consumeLiteral(request);
            break;
        default:
            name = consumeWord(request, new ListCharValidator());
        }
        return BASE64MailboxDecoder.decode(name);
    }

    private class ListCharValidator extends AtomCharValidator {
        @Override
        public boolean isValid(char chr) {
            return isListWildcard(chr) || super.isValid(chr);
        }
    }
}