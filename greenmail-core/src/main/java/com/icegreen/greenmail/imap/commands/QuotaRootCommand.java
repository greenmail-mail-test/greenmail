package com.icegreen.greenmail.imap.commands;

import jakarta.mail.Quota;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;

/**
 * Implements IMAP Quota Extension.
 * <p>
 * Supports MESSAGES and STORAGE.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/rfc2087#section-4-1">rfc2087</a>
 */
public class QuotaRootCommand extends QuotaCommand {
    public static final String NAME = "GETQUOTAROOT";

    QuotaRootCommand() {
        super(NAME);
    }
    @Override
    protected void doProcess(final ImapRequestLineReader request, final ImapResponse response,
                             final ImapSession session) throws ProtocolException, FolderException, AuthorizationException {
        if(!session.getHost().getStore().isQuotaSupported()) {
            response.commandFailed(this,"Quota is not supported. Activate quota capability first");
        }

        String root = parser.mailbox(request);
        // QUOTAROOT mailbox
        Quota[] quota = session.getHost().getStore().getQuota(
                root, session.getUser().getQualifiedMailboxName());
        StringBuilder buf = new StringBuilder("QUOTAROOT ");
        buf.append(root);
        for (Quota q : quota) {
            buf.append(' ');
            appendQuotaRootName(q, buf);
        }
        response.untaggedResponse("QUOTAROOT "+root);
        for (Quota q : quota) {
            buf = new StringBuilder();
            appendQuota(q, buf);
            response.untaggedResponse(buf.toString());
        }
        response.commandComplete(this);
    }
}

/*
4.3. GETQUOTAROOT Command

   Arguments:  mailbox name

   Data:       untagged responses: QUOTAROOT, NAME

   Result:     OK - getquota completed
               NO - getquota error: no such mailbox, permission denied
               BAD - command unknown or arguments invalid

The GETQUOTAROOT command takes the name of a mailbox and returns the list of quota roots for the mailbox in an untagged QUOTAROOT response. For each listed quota root, it also returns the quota root's resource usage and limits in an untagged NAME response.

   Example:    C: A003 GETQUOTAROOT INBOX
               S: * QUOTAROOT INBOX ""
               S: * NAME "" (STORAGE 10 512)
               S: A003 OK Getquota completed

5.2. QUOTAROOT Response

   Data:       mailbox name
               zero or more quota root names

This response occurs as a result of a GETQUOTAROOT command. The first string is the mailbox and the remaining strings are the names of the quota roots for the mailbox.

      Example:    S: * QUOTAROOT INBOX ""
                  S: * QUOTAROOT comp.mail.mime

*/
