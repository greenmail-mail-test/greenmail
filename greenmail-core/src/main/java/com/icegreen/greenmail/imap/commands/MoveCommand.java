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
 * Handles MOVE imap command.
 * <p>
 * See https://www.ietf.org/rfc/rfc6851.txt
 * <p>
 * capability     =/ "MOVE"
 * <p>
 * command-select =/ move
 * move           = "MOVE" SP sequence-set SP mailbox
 * uid            = "UID" SP (copy / fetch / search / store / move)
 */
class MoveCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "MOVE";
    public static final String ARGS = "<sequence-set> <mailbox>";
    public static final String CAPABILITY = "MOVE";

    MoveCommand() {
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
                long copiedUid = currentMailbox.moveMessage(uid, toFolder);
                copiedUidsOld.add(uid);
                copiedUidsNew.add(copiedUid);
            }
        }

        // Always send COPYUID, even if not UID MOVE
        response.okResponse(CopyCommand.generateCopyUidResponseCode(toFolder, copiedUidsOld, copiedUidsNew), "");

        session.unsolicitedResponses(response);  // EXPUNGE responses
        response.commandComplete(this);
    }
}
