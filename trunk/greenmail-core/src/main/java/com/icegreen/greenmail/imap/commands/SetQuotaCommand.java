package com.icegreen.greenmail.imap.commands;

import javax.mail.Quota;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;

/**
 * Implements IMAP Quota Extension.
 * <p/>
 * See http://rfc-ref.org/RFC-TEXTS/2087
 *
 * @author mm
 */
public class SetQuotaCommand extends AuthenticatedStateCommand {
    public static final String NAME = "SETQUOTA";

    @Override
    protected void doProcess(final ImapRequestLineReader request, final ImapResponse response,
                             final ImapSession session) {
        if(!session.getHost().getStore().isQuotaSupported()) {
            response.commandFailed(this,"Quota is not supported. Activate quota capability first");
        }
        try {
            String root = parser.mailbox(request);
            Quota quota = new Quota(root);
            parser.consumeChar(request, ' ');
            parser.consumeChar(request, '(');
            quota.setResourceLimit(parser.astring(request), parser.consumeLong(request));
            char c =request.nextWordChar();
            if(')' != c) {
                quota.setResourceLimit(parser.astring(request), parser.consumeLong(request));
            }
            parser.consumeChar(request, ')');
            session.getHost().getStore().setQuota(
                    quota, session.getUser().getQualifiedMailboxName());
            response.commandComplete(this);
        } catch (ProtocolException e) {
            response.commandFailed(this, "Can not parse command"+e.getMessage());
        }
    }

    @Override
    protected String getArgSyntax() {
        return "quota-root-arg (STORAGE 43008 MESSAGES 10)";
    }

    public String getName() {
        return NAME;
    }
}
/*
4.1. SETQUOTA Command


   Arguments:  quota root
               list of resource limits

   Data:       untagged responses: QUOTA

   Result:     OK - setquota completed
               NO - setquota error: can't set that data
               BAD - command unknown or arguments invalid

      The SETQUOTA command takes the name of a mailbox quota root and a
      list of resource limits.  The resource limits for the named quota
      root are changed to be the specified limits.  Any previous
      resource limits for the named quota root are discarded.

      If the named quota root did not previously exist, an
      implementation may optionally create it and change the quota roots
      for any number of existing mailboxes in an implementation-defined
      manner.

   Example:    C: A001 SETQUOTA "" (STORAGE 512)
               S: * QUOTA "" (STORAGE 10 512)
               S: A001 OK Setquota completed

 */
