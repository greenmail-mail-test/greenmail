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
public class CopyCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "COPY";
    public static final String ARGS = "<message-set> <mailbox>";

    public CopyCommand() {
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

        List<Long> copiedUidsOld = new ArrayList<>();
        List<Long> copiedUidsNew = new ArrayList<>();

        long[] uids = currentMailbox.getMessageUids();
        for (long uid : uids) {
            boolean inSet;
            if (useUids) {
                inSet = includes(idSet, uid);
            } else {
                int msn = currentMailbox.getMsn(uid);
                inSet = includes(idSet, msn);
            }

            if (inSet) {
                long copiedUid = currentMailbox.copyMessage(uid, toFolder);
                copiedUidsOld.add(uid);
                copiedUidsNew.add(copiedUid);
            }
        }

        session.unsolicitedResponses(response);
        response.commandComplete(this, generateCopyUidResponseCode(currentMailbox, copiedUidsOld, copiedUidsNew));
    }

    /**
     * Generates <b>COPYUID</b> response code
     * (see <a href="http://tools.ietf.org/html/rfc2359#page-4">http://tools.ietf.org/html/rfc2359</a>)
     * using format : <i>COPYUID UIDVALIDITY SOURCE-UIDS TARGET-UIDS</i>.
     *
     * For example <i>COPYUID 38505 304,319,320 3956,3957,3958</i>
     *
     * @param currentMailbox imap folder which is target of copy command
     * @param copiedUidsFrom List of source uids which was successfully copied
     * @param copiedUidsTo   List of message uids which was successfully copied
     * @return response code
     */
    private String generateCopyUidResponseCode(ImapSessionFolder currentMailbox,
                                               List<Long> copiedUidsFrom, List<Long> copiedUidsTo) {
        StringBuilder copyuidResponseCode = new StringBuilder("COPYUID").append(SP).
                append(currentMailbox.getUidValidity()).append(SP).
                append(IdRange.uidsToRangeString(copiedUidsFrom)).append(SP).
                append(IdRange.uidsToRangeString(copiedUidsTo));

        return copyuidResponseCode.toString();
    }
}
