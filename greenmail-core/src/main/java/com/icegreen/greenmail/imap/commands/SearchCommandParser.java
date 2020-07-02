/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import javax.mail.search.AndTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.icegreen.greenmail.imap.commands.IdRange.SEQUENCE;

/**
 * Handles processing for the SEARCH imap command.
 * <p>
 * https://tools.ietf.org/html/rfc3501#section-6.4.4
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @author Marcel May
 */
class SearchCommandParser extends CommandParser {
    private final Logger log = LoggerFactory.getLogger(SearchCommandParser.class);
    private static final String CHARSET_TOKEN = "CHARSET";

    /**
     * Marker for stack when parsing search
     */
    protected enum SearchOperator {
        AND, OR, NOT, GROUP /* Pseudo operator */
    }

    /**
     * Parses the request argument into a valid search term. Not yet fully implemented - see SearchKey enum.
     * <p>
     * Other searches will return everything for now.
     *
     * Throws an UnsupportedCharsetException if provided CHARSET is not supported.
     */
    public SearchTerm searchTerm(ImapRequestLineReader request)
            throws ProtocolException {
        Charset charset = StandardCharsets.US_ASCII; // Default
        // Stack contains mix of SearchOperator and SearchTerm instances
        // and will be processed in two steps
        Deque<Object> stack = new LinkedList<>();
        stack.push(SearchOperator.GROUP); // So that the final list of terms will be wrapped in an AndTerm

        // Phase one : parse search query into SearchOperators/simple search terms and put them on the stack.
        char next;
        while ((next = request.nextChar()) != '\n' && next != CHR_CR /* \r */) {
            next = request.consumeAll(CHR_SPACE);

            if (isAtomSpecial(next)) {
                // Parentheses?
                if (next == '(') {
                    request.consume();
                    request.consumeAll(CHR_SPACE);

                    stack.push(SearchOperator.GROUP);
                } else if (next == ')') {
                    request.consume();
                    request.consumeAll(CHR_SPACE);

                    LinkedList<SearchTerm> groupItems = new LinkedList<>();
                    Object item;
                    while ((item = stack.pop()) != SearchOperator.GROUP) {
                        groupItems.addFirst((SearchTerm) item);
                    }
                    if (groupItems.size() == 1) {
                        stack.push(groupItems.get(0)); // Single item
                    } else {
                        stack.push(new AndTerm(groupItems.toArray(new SearchTerm[0])));
                    }
                } else {
                    throw new IllegalStateException("Unsupported atom special char <" + next + ">");
                }
            } else {
                String token = atomOnly(request);
                // Sequence-set?
                if (SEQUENCE.matcher(token).matches()) {
                    stack.push(SearchTermBuilder.create(SearchKey.SEQUENCE_SET).addParameter(token).build());
                }
                // Charset?
                else if (CHARSET_TOKEN.equals(token)) {
                    // If the server does not support the specified [CHARSET], it MUST
                    // return a tagged NO response (not a BAD).  This response SHOULD
                    // contain the BADCHARSET response code, which MAY list the
                    // [CHARSET]s supported by the server.
                    request.consumeAll(CHR_SPACE);
                    final String charsetName = astring(request);
                    try {
                        charset = Charset.forName(charsetName);
                    } catch (UnsupportedCharsetException ex) {
                        log.error("Unsupported charset '{}", charsetName);
                        throw ex;
                    }
                } else {
                    // Term?
                    SearchKey key = SearchKey.valueOf(token);
                    // Operator?
                    if (key == SearchKey.NOT) {
                        stack.push(SearchOperator.NOT);
                    } else if (key == SearchKey.OR) {
                        stack.push(SearchOperator.OR);
                    } else {
                        // No operator
                        SearchTermBuilder b = SearchTermBuilder.create(key);
                        if (b.expectsParameter()) {
                            for (int pi = 0; pi < key.getNumberOfParameters(); pi++) {
                                request.consumeAll(CHR_SPACE);
                                String paramValue = string(request, charset);
                                b.addParameter(paramValue);
                            }
                        }
                        stack.push(b.build());
                    }

                }
            }
        }

        // Phase two : Build search terms by operators
        return handleOperators(stack);
    }

    private SearchTerm handleOperators(Deque<Object> stack) {
        LinkedList<SearchTerm> params = new LinkedList<>();
        while (!stack.isEmpty()) {
            final Object o = stack.pop();
            if (SearchOperator.OR == o) {
                final SearchTerm term1 = params.pop();
                final SearchTerm term2 = params.pop();
                params.push(new OrTerm(term1, term2));
            } else if (SearchOperator.NOT == o) {
                params.push(new NotTerm(params.pop()));
            } else if (SearchOperator.AND == o) {
                final SearchTerm term1 = params.pop();
                final SearchTerm term2 = params.pop();
                params.push(new AndTerm(term1, term2));
            } else if (SearchOperator.GROUP == o) {
                // Size 0: Empty braces? Do nothing.
                // Size 1: Do nothing, keep item on params stack, no wrapping needed
                // Size > 1 : Wrap with AndTerm
                if (params.size() > 1) {
                    SearchTerm[] items = params.toArray(new SearchTerm[0]);
                    params.clear();
                    params.push(new AndTerm(items));
                }
            } else if (o instanceof SearchTerm) {
                params.push((SearchTerm) o);
            } else {
                throw new IllegalStateException("Unsupported stack item " + o);
            }
        }

        if (params.size() != 1) {
            throw new IllegalStateException("Expected exactly one root search term but got " + params);
        }
        return params.pop();
    }

}
