package com.icegreen.greenmail.imap.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;

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

    private SortCommandParser sortCommandParser = new SortCommandParser();

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
        final SortTerm sortTerm = sortCommandParser.sortTerm(request);

        final MailFolder folder = session.getSelected();

        long[] uids = folder.search(sortTerm.getSearchTerm());
        List<StoredMessage> messages = new ArrayList<>();
        for (long uid : uids) {
            StoredMessage m = folder.getMessage(uid);
            if (m != null) {
                messages.add(m);
            }
        }

        Collections.sort(messages, new StoredMessageSorter(sortTerm));

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
