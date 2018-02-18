package com.icegreen.greenmail.imap.commands;

import javax.mail.Quota;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;

/**
 * Implements IMAP Quota Extension.
 *
 * See http://rfc-ref.org/RFC-TEXTS/2087
 *
 * @author mm
 */
public class QuotaCommand extends AuthenticatedStateCommand {
    public static final String NAME = "QUOTA";

    public QuotaCommand() {
        super(NAME, null);
    }

    public QuotaCommand(String name) {
        super(name, null);
    }

    @Override
    protected void doProcess(final ImapRequestLineReader request, final ImapResponse response,
                             final ImapSession session) throws ProtocolException, FolderException, AuthorizationException {
        if(!session.getHost().getStore().isQuotaSupported()) {
            response.commandFailed(this,"Quota is not supported. Activate quota capability first");
        }

        String root = parser.mailbox(request);
        // NAME root (name usage limit)

        Quota[] quota = session.getHost().getStore().getQuota(
                root, session.getUser().getQualifiedMailboxName());
        for(Quota q: quota) {
            StringBuilder buf = new StringBuilder();
            appendQuota(q, buf);
            response.untaggedResponse(buf.toString());
        }

        response.commandComplete(this);
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
        if(null == rootName || rootName.length()==0) {
            rootName = "\"\"";
        }
        buf.append('\"').append(rootName).append('\"');
    }
}

/*
4.2. GETQUOTA Command

   Arguments:  quota root

   Data:       untagged responses: NAME

   Result:     OK - getquota completed
               NO - getquota  error:  no  such  quota  root,  permission
               denied
               BAD - command unknown or arguments invalid

The GETQUOTA command takes the name of a quota root and returns the quota root's resource usage and limits in an untagged NAME response.

   Example:    C: A003 GETQUOTA ""
               S: * NAME "" (STORAGE 10 512)
               S: A003 OK Getquota completed

5.1. NAME Response

   Data:       quota root name
               list of resource names, usages, and limits

This response occurs as a result of a GETQUOTA or GETQUOTAROOT command. The first string is the name of the quota root for which this quota applies.

The name is followed by a S-expression format list of the resource usage and limits of the quota root. The list contains zero or more triplets. Each triplet conatins a resource name, the current usage of the resource, and the resource limit.

Resources not named in the list are not limited in the quota root. Thus, an empty list means there are no administrative resource limits in the quota root.

      Example:    S: * NAME "" (STORAGE 10 512)

*/