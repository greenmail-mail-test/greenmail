package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;

/**
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortCommandParser extends CommandParser {

    private final SearchCommandParser searchCommandParser = new SearchCommandParser();

    public SortTerm sortTerm(ImapRequestLineReader request) throws ProtocolException {
        SortTerm sortTerm = new SortTerm();

        /* Sort criteria */
        char next = request.nextChar();
        StringBuilder sb = new StringBuilder();
        boolean buffering = false;
        while (next != '\n') {
            if (next != '(' && next != ')' && next != 32 /* space */ && next != 13 /* cr */) {
                sb.append(next);
            } else {
                if (buffering) {
                    sortTerm.getSortCriteria().add(SortKey.valueOf(sb.toString()));
                    sb = new StringBuilder();
                } else {
                    buffering = next == '(';
                }
            }
            request.consume();
            if (next == ')') {
                break;
            }
            next = request.nextChar();
        }

        /* Charset */
        sortTerm.setCharset(consumeWord(request, new AtomCharValidator()));

        /* Search term */
        sortTerm.setSearchTerm(searchCommandParser.searchTerm(request));
        searchCommandParser.endLine(request);

        return sortTerm;
    }
}
