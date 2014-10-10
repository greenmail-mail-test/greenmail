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
 * TODO: for now only flag related keys and ALL are included. Must certainly be completed...
 * <p/>
 * <p/>
 * ANSWERED Messages with the \Answered flag set.
 * <p/>
 * DELETED Messages with the \Deleted flag set.
 * <p/>
 * DRAFT Messages with the \Draft flag set.
 * <p/>
 * FLAGGED Messages with the \Flagged flag set.
 * <p/>
 * KEYWORD <flag> Messages with the specified keyword flag set.
 * <p/>
 * NEW Messages that have the \Recent flag set but not the \Seen flag. This is functionally equivalent to "(RECENT
 * UNSEEN)".
 * <p/>
 * OLD Messages that do not have the \Recent flag set.  This is functionally equivalent to "NOT RECENT" (as opposed to
 * "NOT NEW").
 * <p/>
 * RECENT Messages that have the \Recent flag set.
 * <p/>
 * SEEN Messages that have the \Seen flag set.
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
    DELETED(),
    DRAFT(),
    FLAGGED(),
    HEADER(2),
    KEYWORD(1),
    NEW(),
    OLD(),
    RECENT(),
    SEEN(),
    UNANSWERED(),
    UNDELETED(),
    UNDRAFT(),
    UNFLAGGED(),
    UNKEYWORD(1),
    UNSEEN();

    private int minArgs; // expected additional arguments

    SearchKey() {
        minArgs = 0;
    }

    SearchKey(int pMinArgs) {
        minArgs = pMinArgs;
    }

    public int getNumberOfParameters() {
        return minArgs;
    }
}
