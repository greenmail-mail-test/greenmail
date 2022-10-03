/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles processeing for the COPY imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class CopyCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "COPY";
    public static final String ARGS = "<message-set> <mailbox>";

    CopyCommand() {
        super(NAME, ARGS);
    }

    /**
     * @see CommandTemplate#doProcess
     */
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
        IdRange[] idSet = parser.parseIdRange(request);
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        ImapSessionFolder currentMailbox = session.getSelected();
        MailFolder toFolder;
        try {
            toFolder = getMailbox(mailboxName, session, true);
        } catch (FolderException e) {
            e.setResponseCode("TRYCREATE");
            throw e;
        }

        List<Long> uidsFilteredByIdSet = new ArrayList<>();
        currentMailbox.getMessages().forEach(storedMessage -> {
            final long uid = storedMessage.getUid();
            try {
                if( useUids
                    ? includes(idSet, uid)
                    : includes(idSet, currentMailbox.getMsn(uid))
                ) {
                    uidsFilteredByIdSet.add(uid);
                }
            } catch (FolderException e) {
                throw new IllegalStateException("Can not get msn for message in folder "+currentMailbox.getName()+" using uid +"+uid, e);
            }
        });

        List<Long> uidsAfterAction = new ArrayList<>();
        for(long uid:uidsFilteredByIdSet) {
            // Track new uid
            long copiedUid = currentMailbox.copyMessage(uid, toFolder);
            uidsAfterAction.add(copiedUid);
        }

        session.unsolicitedResponses(response);
        response.commandComplete(this, generateCopyUidResponseCode(toFolder, uidsFilteredByIdSet, uidsAfterAction));
    }

    /**
     * Generates <b>COPYUID</b> response code
     * (see <a href="http://tools.ietf.org/html/rfc2359#page-4">http://tools.ietf.org/html/rfc2359</a>)
     * using format : <i>COPYUID UIDVALIDITY SOURCE-UIDS TARGET-UIDS</i>.
     * <p>
     * For example <i>COPYUID 38505 304,319,320 3956,3957,3958</i>
     *
     * @param destinationFolder imap folder which is target of copy command
     * @param copiedUidsFrom    List of source uids which was successfully copied
     * @param copiedUidsTo      List of message uids which was successfully copied
     * @return response code
     */
    static String generateCopyUidResponseCode(MailFolder destinationFolder,
                                              List<Long> copiedUidsFrom, List<Long> copiedUidsTo) {
        return "COPYUID" + SP +
            destinationFolder.getUidValidity() + SP +
            IdRange.uidsToRangeString(copiedUidsFrom) + SP +
            IdRange.uidsToRangeString(copiedUidsTo);
    }
}
