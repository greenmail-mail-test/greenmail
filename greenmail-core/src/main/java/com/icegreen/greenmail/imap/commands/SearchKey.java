/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap.commands;

/**
 * SearchKey as defined in "RFC3501" section "6.4.4. SEARCH Command"
 * Read more: http://www.faqs.org/rfcs/rfc3501.html
 * <p>
 * TODO: Add search keys that are missing
 * </p>
 * <ul>
 * <li>ALL All messages in the mailbox; the default initial key for ANDing</li>
 * <li>ANSWERED Messages with the \Answered flag set.</li>
 * <li>BCC Messages that contain the specified string in the envelope structure's BCC field.</li>
 * <li>TODO: BEFORE &lt;date&gt;  Messages whose internal date (disregarding time and timezone) is earlier than the specified date.</li>
 * <li>TODO: BODY &lt;string&gt; Messages that contain the specified string in the body of the message.</li>
 * <li>CC &lt;string&gt; Messages that contain the specified string in the envelope structure's CC field.</li>
 * <li>DELETED Messages with the \Deleted flag set.</li>
 * <li>DRAFT Messages with the \Draft flag set.</li>
 * <li>FLAGGED Messages with the \Flagged flag set.</li>
 * <li>FROM &lt;string&gt; Messages that contain the specified string in the envelope structure's FROM field.</li>
 * <li>HEADER &lt;field-name&gt; &lt;string&gt; Messages that have a header with the specified field-name (as defined in [RFC-2822]) and
 * that contains the specified string in the text of the header (what comes after the colon). If the string to search is
 * zero-length, this matches all messages that have a header line with the specified field-name regardless of the contents.</li>
 * <li>KEYWORD &lt;flag&gt; Messages with the specified keyword flag set.</li>
 * <li>TODO: LARGER &lt;n&gt; Messages with an [RFC-2822] size larger than the specified number of octets.</li>
 * <li>NEW Messages that have the \Recent flag set but not the \Seen flag. This is functionally equivalent to "(RECENT UNSEEN)".</li>
 * <li>NOT &lt;search-key&gt; Messages that do not match the specified search key.</li>
 * <li>OLD Messages that do not have the \Recent flag set.  This is functionally equivalent to "NOT RECENT" (as opposed to "NOT NEW").</li>
 * <li>TODO: ON &lt;date&gt; Messages whose internal date (disregarding time and timezone) is within the specified date.</li>
 * <li>TODO: OR &lt;search-key1&gt; &lt;search-key2&gt;  Messages that match either search key.</li>
 * <li>RECENT Messages that have the \Recent flag set.</li>
 * <li>SEEN Messages that have the \Seen flag set.</li>
 * <li>TODO: SENTBEFORE &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is earlier than the specified date.</li>
 * <li>TODO: SENTON &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within the specified date.</li>
 * <li>TODO: SENTSINCE &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within or later than the specified date.</li>
 * <li>TODO: SINCE &lt;date&gt; Messages whose internal date (disregarding time and timezone) is within or later than the specified date.</li>
 * <li>TODO: SMALLER &lt;n&gt; Messages with an [RFC-2822] size smaller than the specified number of octets.</li>
 * <li>SUBJECT &lt;string&gt; Messages that contain the specified string in the envelope structure's SUBJECT field.</li>
 * <li>TODO: TEXT &lt;string&gt; Messages that contain the specified string in the header or body of the message.</li>
 * <li>TO &lt;string&gt; Messages that contain the specified string in the envelope structure's TO field.</li>
 * <li>UID &lt;sequence set&gt; Messages with unique identifiers corresponding to the specified unique identifier set. Sequence set ranges are permitted.</li>
 * <li>UNANSWERED Messages that do not have the \Answered flag set.</li>
 * <li>UNDELETED Messages that do not have the \Deleted flag set.</li>
 * <li>UNDRAFT Messages that do not have the \Draft flag set.</li>
 * <li>UNFLAGGED Messages that do not have the \Flagged flag set.</li>
 * <li>UNKEYWORD &lt;flag&gt; Messages that do not have the specified keyword flag set.</li>
 * <li>UNSEEN Messages that do not have the \Seen flag set.</li>
 * </ul>
 *
 * @author Torsten Buchert
 * @since 12.01.2010
 */
public enum SearchKey {
    ALL(),
    ANSWERED(),
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
    SUBJECT(1),
    TO(1),
    UID(1),
    UNANSWERED(),
    UNDELETED(),
    UNDRAFT(),
    UNFLAGGED(),
    UNKEYWORD(1),
    UNSEEN();

    private int minArgs = 0; // expected additional arguments
    private boolean operator = false; // Is an operator, such as AND, OR, NOT ...

    SearchKey() {
        // Nothing
    }

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
