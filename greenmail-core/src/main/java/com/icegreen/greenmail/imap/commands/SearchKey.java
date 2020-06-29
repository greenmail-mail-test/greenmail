/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap.commands;

/**
 * SearchKey as defined in "RFC3501" section "6.4.4. SEARCH Command"
 * <p>
 * Read more: https://tools.ietf.org/html/rfc3501
 * </p>
 * <ul>
 * <li>SEQUENCE_SET &lt;SEQUENCE SET&gt; of message ids</li>
 * <li>ALL All messages in the mailbox; the default initial key for ANDing</li>
 * <li>ANSWERED Messages with the \Answered flag set.</li>
 * <li>BCC Messages that contain the specified string in the envelope structure's BCC field.</li>
 * <li>BEFORE &lt;date&gt;  Messages whose internal date (disregarding time and timezone) is earlier than the specified date.</li>
 * <li>BODY &lt;string&gt; Messages that contain the specified string in the body of the message.</li>
 * <li>CC &lt;string&gt; Messages that contain the specified string in the envelope structure's CC field.</li>
 * <li>DELETED Messages with the \Deleted flag set.</li>
 * <li>DRAFT Messages with the \Draft flag set.</li>
 * <li>FLAGGED Messages with the \Flagged flag set.</li>
 * <li>FROM &lt;string&gt; Messages that contain the specified string in the envelope structure's FROM field.</li>
 * <li>HEADER &lt;field-name&gt; &lt;string&gt; Messages that have a header with the specified field-name (as defined in [RFC-2822]) and
 * that contains the specified string in the text of the header (what comes after the colon). If the string to search is
 * zero-length, this matches all messages that have a header line with the specified field-name regardless of the contents.</li>
 * <li>KEYWORD &lt;flag&gt; Messages with the specified keyword flag set.</li>
 * <li>LARGER &lt;n&gt; Messages with an [RFC-2822] size larger than the specified number of octets.</li>
 * <li>NEW Messages that have the \Recent flag set but not the \Seen flag. This is functionally equivalent to "(RECENT UNSEEN)".</li>
 * <li>NOT &lt;search-key&gt; Messages that do not match the specified search key.</li>
 * <li>OLD Messages that do not have the \Recent flag set.  This is functionally equivalent to "NOT RECENT" (as opposed to "NOT NEW").</li>
 * <li>ON &lt;date&gt; Messages whose internal date (disregarding time and timezone) is within the specified date.</li>
 * <li>OR &lt;search-key1&gt; &lt;search-key2&gt;  Messages that match either search key.</li>
 * <li>RECENT Messages that have the \Recent flag set.</li>
 * <li>SEEN Messages that have the \Seen flag set.</li>
 * <li>SENTBEFORE &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is earlier than the specified date.</li>
 * <li>SENTON &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within the specified date.</li>
 * <li>SENTSINCE &lt;date&gt; Messages whose [RFC-2822] Date: header (disregarding time and timezone) is within or later than the specified date.</li>
 * <li>SINCE &lt;date&gt; Messages whose internal date (disregarding time and timezone) is within or later than the specified date.</li>
 * <li>SMALLER &lt;n&gt; Messages with an [RFC-2822] size smaller than the specified number of octets.</li>
 * <li>SUBJECT &lt;string&gt; Messages that contain the specified string in the envelope structure's SUBJECT field.</li>
 * <li>TEXT &lt;string&gt; Messages that contain the specified string in the header or body of the message.</li>
 * <li>TO &lt;string&gt; Messages that contain the specified string in the envelope structure's TO field.</li>
 * <li>UID &lt;sequence set&gt; Messages with unique identifiers corresponding to the specified unique identifier set. Sequence set ranges are permitted.
 * <ul>
 *  <li>uid-set         = (uniqueid / uid-range) *("," uid-set)</li>
 *  <li>uid-range       = (uniqueid ":" uniqueid)
 * <p>
 *   Two uniqueid values and all values between these two regards of order.
 * </p>
 * <p>
 *   Example: 2:4 and 4:2 are equivalent.
 * </p>
 *  </li>
 * </ul>
 * </li>
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
    BCC(1, true),
    BODY(1, true),
    CC(1, true),
    DELETED(),
    DRAFT(),
    FLAGGED(),
    FROM(1, true),
    HEADER(2, true),
    KEYWORD(1),
    NEW(),
    NOT(true),
    OLD(),
    RECENT(),
    SEEN(),
    SUBJECT(1, true),
    TO(1, true),
    TEXT(1, true),
    UID(1),
    UNANSWERED(),
    UNDELETED(),
    UNDRAFT(),
    UNFLAGGED(),
    UNKEYWORD(1),
    UNSEEN(),
    /**
     * &lt;sequence set&gt; - Messages with message sequence numbers corresponding
     * to the specified message sequence number set.
     */
    SEQUENCE_SET(1),
    OR(2),
    SINCE(1),
    ON(1),
    BEFORE(1),
    SENTSINCE(1),
    SENTON(1),
    SENTBEFORE(1),
    LARGER(1),
    SMALLER(1);

    private int minArgs = 0; // expected additional arguments
    private boolean operator = false; // Is an operator, such as AND, OR, NOT ...
    private boolean charsetAware = false;

    SearchKey() {
        // Nothing
    }

    SearchKey(int pMinArgs) {
        minArgs = pMinArgs;
    }

    SearchKey(int pMinArgs, boolean charsetAware) {
        this(pMinArgs);
        this.charsetAware = charsetAware;
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

    public boolean isCharsetAware() {
        return charsetAware;
    }
}
