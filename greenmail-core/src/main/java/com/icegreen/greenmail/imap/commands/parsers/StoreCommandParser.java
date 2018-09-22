package com.icegreen.greenmail.imap.commands.parsers;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.imap.commands.parsers.store.StoreDirective;

public class StoreCommandParser extends CommandParser {
    public StoreDirective storeDirective(ImapRequestLineReader request) throws ProtocolException {
        int sign = 0;
        boolean silent = false;

        char next = request.nextWordChar();
        if (next == '+') {
            sign = 1;
            request.consume();
        } else if (next == '-') {
            sign = -1;
            request.consume();
        } else {
            sign = 0;
        }

        String directive = consumeWord(request, new NoopCharValidator());
        if ("FLAGS".equalsIgnoreCase(directive)) {
            silent = false;
        } else if ("FLAGS.SILENT".equalsIgnoreCase(directive)) {
            silent = true;
        } else {
            throw new ProtocolException("Invalid Store Directive: '" + directive + '\'');
        }
        return new StoreDirective(sign, silent);
    }
}