package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

import java.util.*;

/**
 * Implements SORT command described in <a href="https://tools.ietf.org/html/rfc5256">RFC5256</a>
 * <br><br>
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortCommand extends SelectedStateCommand implements UidEnabledCommand {

    public static final String NAME = "SORT";
    public static final String ARGS = "(<sort criteria>) <charset specification> <search term>";

    private final SortCommandParser sortCommandParser = new SortCommandParser();

    SortCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        doProcess(request, response, session, false);
    }

    @Override
    public void doProcess(final ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids) throws ProtocolException, FolderException {
        final SortTerm sortTerm;
        try {
            sortTerm = sortCommandParser.sortTerm(request);
        } catch (ProtocolException e) {
            // Not support => return "BAD"
            response.commandError("Sort/search command failed to parse: "+e.getMessage());
            return;
        }

        final MailFolder folder = session.getSelected();

        long[] uids = folder.search(sortTerm.getSearchTerm());
        List<StoredMessage> messages = new ArrayList<>();
        for (long uid : uids) {
            messages.add(folder.getMessage(uid));
        }

        messages.sort(new StoredMessageSorter(sortTerm));

        StringBuilder idList = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                idList.append(SP);
            }
            StoredMessage message = messages.get(i);
            if (useUids) {
                idList.append(message.getUid());
            } else {
                int msn = folder.getMsn(message.getUid());
                idList.append(msn);
            }
        }

        response.commandResponse(this, idList.toString());

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

}
