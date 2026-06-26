package com.icegreen.greenmail.imap.commands;

import static com.icegreen.greenmail.imap.ImapConstants.*;

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
 * See <a href="https://datatracker.ietf.org/doc/html/rfc2087#section-4-1">rfc2087</a>
 */
public class GetQuotaCommand extends AuthenticatedStateCommand {
    public static final String NAME = "GETQUOTA";

    GetQuotaCommand() {
        super(NAME, null);
    }

    public GetQuotaCommand(String name) {
        super(name, null);
    }

    @Override
    protected void doProcess(final ImapRequestLineReader request, final ImapResponse response,
                             final ImapSession session) throws ProtocolException, FolderException, AuthorizationException {
        if(!session.getHost().getStore().isQuotaSupported()) {
            response.commandFailed(this,"Quota is not supported. Activate quota capability first");
            return;
        }

        String quotaRoot = parser.astring(request);
        // NAME root (name usage limit)
        Quota[] quota = session.getHost().getStore().getQuota(
            quotaRoot, session.getUser().getQualifiedMailboxName());
        if(null==quota||quota.length==0) {
            response.commandFailed(this, "No such quota root: "+quotaRoot);
        } else {
            for (Quota q : quota) {
                StringBuilder buf = new StringBuilder();
                appendQuota(q, buf);
                response.untaggedResponse(buf.toString());
            }
            response.commandComplete(this);
        }
    }

    protected void appendQuota(Quota quota, StringBuilder buf) {
        buf.append("QUOTA ");
        appendQuotaRootName(quota, buf);
        buf.append(SP);
        buf.append('(');
        boolean pad = false;
        for(Quota.Resource resource:quota.resources) {
            if(pad) {
                buf.append(SP);
            }
            buf.append(resource.name);
            buf.append(SP).append(resource.usage);
            buf.append(SP).append(resource.limit);
            pad = true;
        }
        buf.append(')');
    }

    protected void appendQuotaRootName(Quota quota, StringBuilder buf) {
        String rootName = quota.quotaRoot;
        if (null == rootName) {
            rootName = "";
        }
        buf.append(quoteName(rootName));
    }

    /**
     * Renders a client-supplied quota root / mailbox name as an IMAP quoted string,
     * escaping the quoted-specials and dropping CR/LF so it cannot terminate the
     * quoted string early or inject extra response lines.
     */
    protected static String quoteName(String name) {
        String escaped = name
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "");
        return '"' + escaped + '"';
    }
}
