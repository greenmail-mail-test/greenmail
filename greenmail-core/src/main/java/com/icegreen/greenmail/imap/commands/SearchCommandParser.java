/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.search.AndTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import static com.icegreen.greenmail.imap.commands.IdRange.SEQUENCE;

/**
 * Handles processing for the SEARCH imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 */
class SearchCommandParser extends CommandParser {
    private final Logger log = LoggerFactory.getLogger(SearchCommandParser.class);
    private static final String CHARSET_TOKEN = "CHARSET";

    /**
     * Parses the request argument into a valid search term. Not yet fully implemented - see SearchKey enum.
     * <p>
     * Other searches will return everything for now.
     */
    public SearchTerm searchTerm(ImapRequestLineReader request)
            throws ProtocolException, CharacterCodingException {
        SearchTerm resultTerm = null;
        SearchTermBuilder b = null;
        SearchKey key = null;
	boolean orKey = false;
        boolean negated = false;
        // Dummy implementation
        // Consume to the end of the line.
        char next = request.nextChar();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        Charset charset = null;

        while (next != '\n') {
            if (next != '\"' && (quoted || (next != '\"' && next != CHR_SPACE && next != CHR_CR))) {
                sb.append(next);
            }
            request.consume();
            next = request.nextChar();
            if (next == '\"') {
                quoted = !quoted;
                if (quoted) {
                    continue;
                }
            }
            if (!quoted && (next == CHR_SPACE || next == '\n') && sb.length() > 0) {
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

                        // Message set?
                        if (SEQUENCE.matcher(keyValue).matches()) {
                            b = SearchTermBuilder.create(SearchKey.SEQUENCE_SET);

                            // Try to get additional number sequences.
                            // Sequence can be a whitespace separated list of either a number or number range
                            // Example: '2 5:9 9'
                            next = request.nextChar();
                            while (next == CHR_SPACE || Character.isDigit(next) || next == ':') {
                                request.consume();
                                sb.append(next);
                                next = request.nextChar();
                            }
                            b.addParameter(sb.toString());
                        }
                        else if (CHARSET_TOKEN.equals(keyValue)) { // Charset handling
                            request.nextWordChar(); // Skip spaces
                            String c = this.atom(request);
                            if (log.isDebugEnabled()) {
                                log.debug("Searching with given CHARSET <" + c + '>');
                            }
                            charset = Charset.forName(c);
                        } else {
                            // Term?
                            key = SearchKey.valueOf(keyValue);
                            if (SearchKey.NOT == key) {
                                negated = true;
                            } else {
                                b = SearchTermBuilder.create(key);
                            }

                            if (null!=b && b.expectsParameter() && key.isCharsetAware() && null != charset && next == CHR_SPACE) {
                                next = request.nextWordChar();
                                if (next == '{') {
                                    String textOfCharset = new String(consumeLiteralAsBytes(request), charset);
                                    b.addParameter(textOfCharset);
                                    if(log.isDebugEnabled()) {
                                        log.debug("Searching for text <"+textOfCharset+"> of charset "+charset);
                                    }
                                }
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        // Ignore for now instead of breaking. See issue#35 .
                        log.warn("Ignoring not yet implemented search command '" + sb.toString() + "'", ex);
                        negated = false;
                    }
                } else if (b.expectsParameter()) {
                    if (b.isCharsetAware() && null != charset) {
                        request.consume(); // \n
                        next = request.nextChar();
                        final Integer capacity = Integer.valueOf(sb.substring(1, sb.length() - 1));
                        ByteBuffer bb = ByteBuffer.allocate(capacity);
                        while (next != CHR_CR) {
                            request.consume(); // \n
                            sb.append(next);
                            next = request.nextChar();
                        }
                        final String decoded = charset.decode(bb).toString();
                        log.info("Decoded <" + bb + "> into <" + decoded + ">");
                        b = b.addParameter(decoded);
                    } else {
                        b = b.addParameter(sb.toString());
                    }
                }
                if (b != null && !b.expectsParameter()) {
                    SearchTerm searchTerm = b.build();
                    if (negated) {
                        searchTerm = new NotTerm(searchTerm);
                        negated = false;
                    }
                    b = null;
					if (SearchKey.OR == key) {
					resultTerm = resultTerm == null ? searchTerm : new OrTerm(resultTerm, searchTerm);
					orKey = true;
					} else {
						if (orKey) {
							if (SearchKey.ALL == key) {
                    resultTerm = resultTerm == null ? searchTerm : new AndTerm(resultTerm, searchTerm);

							} else {
								resultTerm = resultTerm == null ? searchTerm : new OrTerm(resultTerm, searchTerm);
							}
						} else {
							resultTerm = resultTerm == null ? searchTerm : new AndTerm(resultTerm, searchTerm);
						}
					}                }
                sb = new StringBuilder();
                next = request.nextChar();
            }
        }
        return resultTerm;
    }
}
