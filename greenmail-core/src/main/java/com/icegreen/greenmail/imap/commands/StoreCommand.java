/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.imap.commands.parsers.StoreCommandParser;
import com.icegreen.greenmail.imap.commands.parsers.store.StoreDirective;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;

import javax.mail.Flags;


/**
 * Handles processeing for the STORE imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class StoreCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "STORE";
    public static final String ARGS = "<Message-set> ['+'|'-']FLAG[.SILENT] <flag-list>";

    private final StoreCommandParser storeParser = new StoreCommandParser();

    public StoreCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        doProcess(request, response, session, false);
    }

    @Override
    public void doProcess(ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids)
            throws ProtocolException, FolderException {
        IdRange[] idSet = storeParser.parseIdRange(request);
        StoreDirective directive = storeParser.storeDirective(request);
        Flags flags = storeParser.flagList(request);
        storeParser.endLine(request);

        ImapSessionFolder mailbox = session.getSelected();
//        IdRange[] uidSet;
//        if (useUids) {
//           uidSet = idSet;
//        } else {
//            uidSet = mailbox.msnsToUids(idSet);
//        }
//        if (directive.getSign() < 0) {
//            mailbox.setFlags(flags, false, uidSet, directive.isSilent());
//        }
//        else if (directive.getSign() > 0) {
//            mailbox.setFlags(flags, true, uidSet, directive.isSilent());
//        }
//        else {
//            mailbox.replaceFlags(flags, uidSet, directive.isSilent());
//        }

        FolderListener silentListener = null;
        if (directive.isSilent()) {
            silentListener = mailbox;
        }

        // TODO do this in one hit.
        long[] uids = mailbox.getMessageUids();
        for (long uid : uids) {
            int msn = mailbox.getMsn(uid);

            if ((useUids && includes(idSet, uid)) ||
                    (!useUids && includes(idSet, msn))) {
                if (directive.getSign() < 0) {
                    mailbox.setFlags(flags, false, uid, silentListener, useUids);
                } else if (directive.getSign() > 0) {
                    mailbox.setFlags(flags, true, uid, silentListener, useUids);
                } else {
                    mailbox.replaceFlags(flags, uid, silentListener, useUids);
                }
            }
        }

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }
}

/*
6.4.6.  STORE Command

   Arguments:  message set
               message data item name
               value for message data item

   Responses:  untagged responses: FETCH

   Result:     OK - store completed
               NO - store error: can't store that data
               BAD - command unknown or arguments invalid

      The STORE command alters data associated with a message in the
      mailbox.  Normally, STORE will return the updated value of the
      data with an untagged FETCH response.  A suffix of ".SILENT" in
      the data item name prevents the untagged FETCH, and the server
      SHOULD assume that the client has determined the updated value
      itself or does not care about the updated value.

         Note: regardless of whether or not the ".SILENT" suffix was
         used, the server SHOULD send an untagged FETCH response if a
         change to a message's flags from an external source is
         observed.  The intent is that the status of the flags is
         determinate without a race condition.

      The currently defined data items that can be stored are:

      FLAGS <flag list>
                     Replace the flags for the message with the
                     argument.  The new value of the flags are returned
                     as if a FETCH of those flags was done.

      FLAGS.SILENT <flag list>
                     Equivalent to FLAGS, but without returning a new
                     value.

      +FLAGS <flag list>
                     Add the argument to the flags for the message.  The
                     new value of the flags are returned as if a FETCH
                     of those flags was done.

      +FLAGS.SILENT <flag list>
                     Equivalent to +FLAGS, but without returning a new
                     value.

      -FLAGS <flag list>
                     Remove the argument from the flags for the message.
                     The new value of the flags are returned as if a
                     FETCH of those flags was done.

      -FLAGS.SILENT <flag list>
                     Equivalent to -FLAGS, but without returning a new
                     value.

   Example:    C: A003 STORE 2:4 +FLAGS (\Deleted)
               S: * 2 FETCH FLAGS (\Deleted \Seen)
               S: * 3 FETCH FLAGS (\Deleted)
               S: * 4 FETCH FLAGS (\Deleted \Flagged \Seen)
               S: A003 OK STORE completed

*/
