package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.search.AndTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

/**
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SearchCommandParser extends CommandParser {
    private final Logger log = LoggerFactory.getLogger(SearchCommandParser.class);

    /**
     * Parses the request argument into a valid search term. Not yet fully implemented - see SearchKey enum.
     * <p>
     * Other searches will return everything for now.
     */
    public SearchTerm searchTerm(ImapRequestLineReader request)
            throws ProtocolException {
        SearchTerm resultTerm = null;
        SearchTermBuilder b = null;
        boolean negated = false;
        // Dummy implementation
        // Consume to the end of the line.
        char next = request.nextChar();
        StringBuilder sb = new StringBuilder();
        while (next != '\n') {
            if (next != 32 /* space */ && next != 13 /* cr */) {
                sb.append(next);
            }
            request.consume();
            next = request.nextChar();
            if (next == 32 || next == '\n') {
                if (log.isDebugEnabled()) {
                    log.debug("Search request is '" + sb.toString() + '\'');
                }
                // Examples:
                // HEADER Message-ID <747621499.0.1264172476711.JavaMail.tbuchert@dev16.int.consol.de> ALL
                // FLAG SEEN ALL
                if (null == b) {
                    try {
                        String keyValue = sb.toString();
                        // Parentheses?
                        if (keyValue.charAt(0) == '('
                                && keyValue.charAt(keyValue.length() - 1) == ')') {
                            keyValue = keyValue.substring(1, keyValue.length() - 1);
                        }
                        SearchKey key = SearchKey.valueOf(keyValue);
                        if (SearchKey.NOT == key) {
                            negated = true;
                        } else {
                            b = SearchTermBuilder.create(key);
                        }
                    } catch (IllegalArgumentException ex) {
                        // Ignore for now instead of breaking. See issue#35 .
                        log.warn("Ignoring not yet implemented search command '" + sb.toString() + "'", ex);
                        negated = false;
                    }
                } else if (b.expectsParameter()) {
                    b = b.addParameter(sb.toString());
                }

                if (b != null && !b.expectsParameter()) {
                    SearchTerm searchTerm = b.build();
                    if (negated) {
                        searchTerm = new NotTerm(searchTerm);
                        negated = false;
                    }
                    b = null;
                    resultTerm = resultTerm == null ? searchTerm : new AndTerm(resultTerm, searchTerm);
                }
                sb = new StringBuilder();
                next = request.nextChar();
            }
        }

        return resultTerm;
    }

}
