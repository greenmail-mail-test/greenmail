/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import static com.icegreen.greenmail.imap.ImapConstants.*;

import java.nio.charset.UnsupportedCharsetException;
import jakarta.mail.search.SearchTerm;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

/**
 * Handles processing for the SEARCH imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 */
class SearchCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "SEARCH";
    public static final String ARGS = "<search term>";

    private final SearchCommandParser searchParser = new SearchCommandParser();

    SearchCommand() {
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
        // Parse the search term from the request
        final SearchTerm searchTerm;
        try {
            searchTerm = searchParser.searchTerm(request);
        } catch (UnsupportedCharsetException e) {
            // Not support => return "NO"
            response.commandFailed(this, "Search command does not support charset " + e.getMessage());
            return;
        } catch (IllegalArgumentException ex) {
            // Not support => return "BAD"
            response.commandError("Search command not supported");
            return;
        }

        if (null == searchTerm) {
            log.warn("Ignoring unsupported search command");
            response.commandComplete(this);
            return;
        }

        searchParser.endLine(request);

        MailFolder folder = session.getSelected();
        long[] uids = folder.search(searchTerm);
        StringBuilder idList = new StringBuilder();
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

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

}
