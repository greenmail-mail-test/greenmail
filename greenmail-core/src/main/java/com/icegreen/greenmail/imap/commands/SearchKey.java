/* -------------------------------------------------------------------
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap.commands;

import javax.mail.Message;
import javax.mail.search.*;

/**
* SearchKeys as defined in "RFC3501" section "6.4.4. SEARCH Command"
* <p/>
* Read more: http://www.faqs.org/rfcs/rfc3501.html
* <p/>
* TODO: for now only flag related keys and ALL are included. Must certainly be completed...
* <p/>
* <p/>
* ANSWERED
* Messages with the \Answered flag set.
* <p/>
* DELETED
* Messages with the \Deleted flag set.
* <p/>
* DRAFT
* Messages with the \Draft flag set.
* <p/>
* FLAGGED
* Messages with the \Flagged flag set.
* <p/>
* KEYWORD <flag>
* Messages with the specified keyword flag set.
* <p/>
* NEW
* Messages that have the \Recent flag set but not the \Seen flag.
* This is functionally equivalent to "(RECENT UNSEEN)".
* <p/>
* OLD
* Messages that do not have the \Recent flag set.  This is
* functionally equivalent to "NOT RECENT" (as opposed to "NOT
* NEW").
* <p/>
* RECENT
* Messages that have the \Recent flag set.
* <p/>
* SEEN
* Messages that have the \Seen flag set.
* <p/>
* UNANSWERED
* Messages that do not have the \Answered flag set.
* <p/>
* UNDELETED
* Messages that do not have the \Deleted flag set.
* <p/>
* UNDRAFT
* Messages that do not have the \Draft flag set.
* <p/>
* UNFLAGGED
* Messages that do not have the \Flagged flag set.
* <p/>
* UNKEYWORD <flag>
* Messages that do not have the specified keyword flag set.
* <p/>
* UNSEEN
* Messages that do not have the \Seen flag set.
* <p/>
*
* @author Torsten Buchert
* @since 12.01.2010
*/
public enum SearchKey {

   ALL(new SearchTerm() {
       @Override
       public boolean match(Message msg) {
           return true;
       }
   }),

   ANSWERED("ANSWERED", true),
   DELETED("DELETED", true),
   DRAFT("DRAFT", true),
   FLAGGED("FLAGGED", true),
   // todo: check what KEYWORD means
   //KEYWORD(null, false),
   NEW("RECENT", true, "SEEN", false),
   OLD("RECENT", false),
   RECENT("RECENT", true),
   SEEN("SEEN", true),
   UNANSWERED("ANSWERED", false),
   UNDELETED("DELETED", false),
   UNDRAFT("DRAFT", false),
   UNFLAGGED("FLAGGED", false),
   //UNKEYWORD(null, false),
   UNSEEN("SEEN", false);

   private SearchTerm searchTerm;

   SearchKey(SearchTerm pSearchTerm) {
       searchTerm = pSearchTerm;
   }

   SearchKey(String pFlagName, boolean pValue) {
       searchTerm = createSearchTerm(pFlagName, pValue);
   }

   SearchKey(String pFlag, boolean pValue, String pFlag2, boolean pValue2) {
       searchTerm = new AndTerm(createSearchTerm(pFlag, pValue), createSearchTerm(pFlag2, pValue2));
   }

   public SearchTerm getSearchTerm() {
       return searchTerm;
   }

   private SearchTerm createSearchTerm(String pFlagName, boolean pValue) {
       javax.mail.Flags.Flag flag = toFlag(pFlagName);
       return new FlagTerm(new javax.mail.Flags(flag), pValue);
   }

   private javax.mail.Flags.Flag toFlag(String pFlag) {
       if (pFlag == null || pFlag.trim().length() < 1) return null;
       pFlag = pFlag.trim().toUpperCase();
       if (pFlag.equals("ANSWERED")) {
           return javax.mail.Flags.Flag.ANSWERED;
       }
       if (pFlag.equals("DELETED")) {
           return javax.mail.Flags.Flag.DELETED;
       }
       if (pFlag.equals("DRAFT")) {
           return javax.mail.Flags.Flag.DRAFT;
       }
       if (pFlag.equals("FLAGGED")) {
           return javax.mail.Flags.Flag.FLAGGED;
       }
       if (pFlag.equals("RECENT")) {
           return javax.mail.Flags.Flag.RECENT;
       }
       if (pFlag.equals("SEEN")) {
           return javax.mail.Flags.Flag.SEEN;
       }
       return null;
   }

}
