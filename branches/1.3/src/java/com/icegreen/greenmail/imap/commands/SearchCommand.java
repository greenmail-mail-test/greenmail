/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

import javax.mail.Message;
import javax.mail.search.SearchTerm;

/**
 * Handles processeing for the SEARCH imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class SearchCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "SEARCH";
    public static final String ARGS = "<search term>";

    private SearchCommandParser parser = new SearchCommandParser();

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        doProcess(request, response, session, false);
    }

    public void doProcess(ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids)
            throws ProtocolException, FolderException {
        // Parse the search term from the request
        SearchTerm searchTerm = parser.searchTerm(request);
        parser.endLine(request);

        MailFolder folder = session.getSelected();
        long[] uids = folder.search(searchTerm);
        StringBuffer idList = new StringBuffer();
        for (int i = 0; i < uids.length; i++) {
            if (i > 0) {
                idList.append(SP);
            }
            long uid = uids[i];
            if (useUids) {
                idList.append(uid);
            } else {
                int msn = folder.getMsn(uid);
                idList.append(msn);
            }
        }

        response.commandResponse(this, idList.toString());

        boolean omitExpunged = (!useUids);
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    public String getArgSyntax() {
        return ARGS;
    }

    private class SearchCommandParser extends CommandParser {
        /**
         * Parses the request argument into a valid search term.
         * Not yet implemented - all searches will return everything for now.
         * TODO implement search
         */
        public SearchTerm searchTerm(ImapRequestLineReader request)
                throws ProtocolException {
            // Dummy implementation
            // Consume to the end of the line.
            char next = request.nextChar();
            while (next != '\n') {
                request.consume();
                next = request.nextChar();
            }

            // Return a search term that matches everything.
            return new SearchTerm() {
                public boolean match(Message message) {
                    return true;
                }
            };
        }

    }
}

/*
6.4.4.  SEARCH Command

   Arguments:  OPTIONAL [CHARSET] specification
               searching criteria (one or more)

   Responses:  REQUIRED untagged response: SEARCH

   Result:     OK - search completed
               NO - search error: can't search that [CHARSET] or
                    criteria
               BAD - command unknown or arguments invalid

      The SEARCH command searches the mailbox for messages that match
      the given searching criteria.  Searching criteria consist of one
      or more search keys.  The untagged SEARCH response from the server
      contains a listing of message sequence numbers corresponding to
      those messages that match the searching criteria.

      When multiple keys are specified, the result is the intersection
      (AND function) of all the messages that match those keys.  For
      example, the criteria DELETED FROM "SMITH" SINCE 1-Feb-1994 refers
      to all deleted messages from Smith that were placed in the mailbox
      since February 1, 1994.  A search key can also be a parenthesized
      list of one or more search keys (e.g. for use with the OR and NOT
      keys).

      Server implementations MAY exclude [MIME-IMB] body parts with
      terminal content media types other than TEXT and MESSAGE from
      consideration in SEARCH matching.

      The OPTIONAL [CHARSET] specification consists of the word
      "CHARSET" followed by a registered [CHARSET].  It indicates the
      [CHARSET] of the strings that appear in the search criteria.
      [MIME-IMB] content transfer encodings, and [MIME-HDRS] strings in
      [RFC-822]/[MIME-IMB] headers, MUST be decoded before comparing
      text in a [CHARSET] other than US-ASCII.  US-ASCII MUST be
      supported; other [CHARSET]s MAY be supported.  If the server does
      not support the specified [CHARSET], it MUST return a tagged NO
      response (not a BAD).

      In all search keys that use strings, a message matches the key if
      the string is a substring of the field.  The matching is case-
      insensitive.

      The defined search keys are as follows.  Refer to the Formal
      Syntax section for the precise syntactic definitions of the
      arguments.

      <message set>  Messages with message sequence numbers
                     corresponding to the specified message sequence
                     number set

      ALL            All messages in the mailbox; the default initial
                     key for ANDing.

      ANSWERED       Messages with the \Answered flag set.

      BCC <string>   Messages that contain the specified string in the
                     envelope structure's BCC field.

      BEFORE <date>  Messages whose internal date is earlier than the
                     specified date.

      BODY <string>  Messages that contain the specified string in the
                     body of the message.

      CC <string>    Messages that contain the specified string in the
                     envelope structure's CC field.

      DELETED        Messages with the \Deleted flag set.

      DRAFT          Messages with the \Draft flag set.

      FLAGGED        Messages with the \Flagged flag set.

      FROM <string>  Messages that contain the specified string in the
                     envelope structure's FROM field.

      HEADER <field-name> <string>
                     Messages that have a header with the specified
                     field-name (as defined in [RFC-822]) and that
                     contains the specified string in the [RFC-822]
                     field-body.

      KEYWORD <flag> Messages with the specified keyword set.

      LARGER <n>     Messages with an [RFC-822] size larger than the
                     specified number of octets.

      NEW            Messages that have the \Recent flag set but not the
                     \Seen flag.  This is functionally equivalent to
                     "(RECENT UNSEEN)".

      NOT <search-key>
                     Messages that do not match the specified search
                     key.

      OLD            Messages that do not have the \Recent flag set.
                     This is functionally equivalent to "NOT RECENT" (as
                     opposed to "NOT NEW").

      ON <date>      Messages whose internal date is within the
                     specified date.

      OR <search-key1> <search-key2>
                     Messages that match either search key.

      RECENT         Messages that have the \Recent flag set.

      SEEN           Messages that have the \Seen flag set.

      SENTBEFORE <date>
                     Messages whose [RFC-822] Date: header is earlier
                     than the specified date.

      SENTON <date>  Messages whose [RFC-822] Date: header is within the
                     specified date.

      SENTSINCE <date>
                     Messages whose [RFC-822] Date: header is within or
                     later than the specified date.

      SINCE <date>   Messages whose internal date is within or later
                     than the specified date.

      SMALLER <n>    Messages with an [RFC-822] size smaller than the
                     specified number of octets.

      SUBJECT <string>
                     Messages that contain the specified string in the
                     envelope structure's SUBJECT field.

      TEXT <string>  Messages that contain the specified string in the
                     header or body of the message.

      TO <string>    Messages that contain the specified string in the
                     envelope structure's TO field.

      UID <message set>
                     Messages with unique identifiers corresponding to
                     the specified unique identifier set.

      UNANSWERED     Messages that do not have the \Answered flag set.

      UNDELETED      Messages that do not have the \Deleted flag set.

      UNDRAFT        Messages that do not have the \Draft flag set.

      UNFLAGGED      Messages that do not have the \Flagged flag set.

      UNKEYWORD <flag>
                     Messages that do not have the specified keyword
                     set.

      UNSEEN         Messages that do not have the \Seen flag set.

   Example:    C: A282 SEARCH FLAGGED SINCE 1-Feb-1994 NOT FROM "Smith"
               S: * SEARCH 2 84 882
               S: A282 OK SEARCH completed



7.2.5.  SEARCH Response

   Contents:   zero or more numbers

      The SEARCH response occurs as a result of a SEARCH or UID SEARCH
      command.  The number(s) refer to those messages that match the
      search criteria.  For SEARCH, these are message sequence numbers;
      for UID SEARCH, these are unique identifiers.  Each number is
      delimited by a space.

   Example:    S: * SEARCH 2 3 6

*/
