package com.icegreen.greenmail.imap.commands;

import java.net.SocketTimeoutException;

import jakarta.mail.Flags;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ImapSessionFolder;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;

class IdleCommand extends SelectedStateCommand {
    public static final String NAME = "IDLE";
    public static final String ARGS = null;

    IdleCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request, ImapResponse response, ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        parser.endLine(request);
        request.consume(); // TODO should be in eol()
        session.unsolicitedResponses(response);
        request.commandContinuationRequest();
        ImapSessionFolder folder = session.getSelected();
        IdleFolderListener listener = new IdleFolderListener(response);
        try {
            folder.addListener(listener);
            waitForClientDone(request);
            session.unsolicitedResponses(response);
            response.commandComplete(this);
        } finally {
            folder.removeListener(listener);
        }
    }

    private void waitForClientDone(ImapRequestLineReader request) throws ProtocolException {
        while (true) {
            try {
                request.nextChar();
                break;
            } catch (ProtocolException e) {
                if (e.getCause() instanceof SocketTimeoutException) {
                    // ignore
                } else {
                    throw e;
                }
            }
        }
        // TODO validate 'DONE'
    }

    private static class IdleFolderListener implements FolderListener {
        private ImapResponse response;

        private IdleFolderListener(ImapResponse response) {
            this.response = response;
        }

        @Override
        public void mailboxDeleted() {
        }

        @Override
        public void flagsUpdated(int msn, Flags flags, Long uid) {
        }

        @Override
        public void expunged(int msn) {
            response.expungeResponse(msn);
        }

        @Override
        public void added(int msn) {
            response.existsResponse(msn);
        }
    }
}
