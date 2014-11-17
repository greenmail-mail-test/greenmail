/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap.commands;

/**
 * SearchKey as defined in "RFC3501" section "6.4.4. SEARCH Command"
 * <p/>
 * Read more: http://www.faqs.org/rfcs/rfc3501.html
 * <p/>
 * TODO: Add search keys that are missing
 * <p/>
 * ALL All messages in the mailbox; the default initial key for ANDing
 * <p/>
 * ANSWERED Messages with the \Answered flag set.
 * <p/>
 * BCC Messages that contain the specified string in the envelope structure's BCC field.
 * <p/>
 * TODO: BEFORE <date>  Messages whose internal date (disregarding time and timezone) is earlier than the specified date.
 * <p/>
 * TODO: BODY <string> Messages that contain the specified string in the body of the message.
 * <p/>
 * CC <string> Messages that contain the specified string in the envelope structure's CC field.
 * <p/>
 * DELETED Messages with the \Deleted flag set.
 * <p/>
 * DRAFT Messages with the \Draft flag set.
 * <p/>
 * FLAGGED Messages with the \Flagged flag set.
 * <p/>
 * FROM <string> Messages that contain the specified string in the envelope structure's FROM field.
 * <p/>
 * HEADER <field-name> <string> Messages that have a header with the specified field-name (as defined in [RFC-2822]) and
 * that contains the specified string in the text of the header (what comes after the colon). If the string to search is
 * zero-length, this matches all messages that have a header line with the specified field-name regardless of the contents.
 * <p/>
 * KEYWORD <flag> Messages with the specified keyword flag set.
 * <p/>
 * TODO: LARGER <n> Messages with an [RFC-2822] size larger than the specified number of octets.
 * <p/>
 * NEW Messages that have the \Recent flag set but not the \Seen flag. This is functionally equivalent to "(RECENT
 * UNSEEN)".
 * <p/>
 * NOT <search-key> Messages that do not match the specified search key.
 * <p/>
 * OLD Messages that do not have the \Recent flag set.  This is functionally equivalent to "NOT RECENT" (as opposed to
 * "NOT NEW").
 * <p/>
 * TODO: ON <date> Messages whose internal date (disregarding time and timezone) is within the specified date.
 * <p/>
 * TODO: OR <search-key1> <search-key2>  Messages that match either search key.
 * <p/>
 * RECENT Messages that have the \Recent flag set.
 * <p/>
 * SEEN Messages that have the \Seen flag set.
 * <p/>
 * TODO: SENTBEFORE <date> Messages whose [RFC-2822] Date: header (disregarding time and timezone) is earlier than the specified date.
 * <p/>
 * TODO: SENTON <date> Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within the specified date.
 * <p/>
 * TODO: SENTSINCE <date> Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within or later than the specified date.
 * <p/>
 * TODO: SINCE <date> Messages whose internal date (disregarding time and timezone) is within or later than the specified date.
 * <p/>
 * TODO: SMALLER <n> Messages with an [RFC-2822] size smaller than the specified number of octets.
 * <p/>
 * TODO: SUBJECT <string> Messages that contain the specified string in the envelope structure's SUBJECT field.
 * <p/>
 * TODO: TEXT <string> Messages that contain the specified string in the header or body of the message.
 * <p/>
 * TO <string> Messages that contain the specified string in the envelope structure's TO field.
 * <p/>
 * TODO: UID <sequence set> Messages with unique identifiers corresponding to the specified unique identifier set. Sequence set ranges are permitted.
 * <p/>
 * UNANSWERED Messages that do not have the \Answered flag set.
 * <p/>
 * UNDELETED Messages that do not have the \Deleted flag set.
 * <p/>
 * UNDRAFT Messages that do not have the \Draft flag set.
 * <p/>
 * UNFLAGGED Messages that do not have the \Flagged flag set.
 * <p/>
 * UNKEYWORD <flag> Messages that do not have the specified keyword flag set.
 * <p/>
 * UNSEEN Messages that do not have the \Seen flag set.
 * <p/>
 *
 * @author Torsten Buchert
 * @since 12.01.2010
 */
public enum SearchKey {
    ALL(),
    ANSWERED,
    BCC(1),
    CC(1),
    DELETED(),
    DRAFT(),
    FLAGGED(),
    FROM(1),
    HEADER(2),
    KEYWORD(1),
    NEW(),
    NOT(true),
    OLD(),
    RECENT(),
    SEEN(),
    TO(1),
    UNANSWERED(),
    UNDELETED(),
    UNDRAFT(),
    UNFLAGGED(),
    UNKEYWORD(1),
    UNSEEN();

    private int minArgs = 0; // expected additional arguments
    private boolean operator = false; // Is an operator, such as AND, OR, NOT ...

    SearchKey() {}

    SearchKey(int pMinArgs) {
        minArgs = pMinArgs;
    }
    SearchKey(boolean pOperator) {
        operator = pOperator;
    }

    public int getNumberOfParameters() {
        return minArgs;
    }

    public boolean isOperator() {
        return operator;
    }
}
