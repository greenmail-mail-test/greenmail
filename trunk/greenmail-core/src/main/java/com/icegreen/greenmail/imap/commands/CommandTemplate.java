/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

/**
 * Base class for all command implementations. This class provides common
 * core functionality useful for all {@link com.icegreen.greenmail.imap.commands.ImapCommand} implementations.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
abstract class CommandTemplate
        implements ImapCommand, ImapConstants {
    protected CommandParser parser = new CommandParser();

    /**
     * By default, valid in any state (unless overridden by subclass.
     *
     * @see com.icegreen.greenmail.imap.commands.ImapCommand#validForState
     */
    public boolean validForState(ImapSessionState state) {
        return true;
    }

    /**
     * Template methods for handling command processing. This method reads
     * argument values (validating them), and checks the request for correctness.
     * If correct, the command processing is delegated to the specific command
     * implemenation.
     *
     * @see ImapCommand#process
     */
    public void process(ImapRequestLineReader request,
                        ImapResponse response,
                        ImapSession session) {
        try {
            doProcess(request, response, session);
        } catch (FolderException e) {
            response.commandFailed(this, e.getResponseCode(), e.getMessage());
        } catch (AuthorizationException e) {
            String msg = "Authorization error: Lacking permissions to perform requested operation.";
            response.commandFailed(this, msg);
        } catch (ProtocolException e) {
            String msg = e.getMessage() + " Command should be '" +
                    getExpectedMessage() + "'";
            response.commandError(msg);
        }
    }

    /**
     * This is the method overridden by specific command implementations to
     * perform commend-specific processing.
     *
     * @param request  The client request
     * @param response The server response
     * @param session  The current client session
     */
    protected abstract void doProcess(ImapRequestLineReader request,
                                      ImapResponse response,
                                      ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException;

    /**
     * Provides a message which describes the expected format and arguments
     * for this command. This is used to provide user feedback when a command
     * request is malformed.
     *
     * @return A message describing the command protocol format.
     */
    protected String getExpectedMessage() {
        StringBuffer syntax = new StringBuffer("<tag> ");
        syntax.append(getName());

        String args = getArgSyntax();
        if (args != null && args.length() > 0) {
            syntax.append(" ");
            syntax.append(args);
        }

        return syntax.toString();
    }

    /**
     * Provides the syntax for the command arguments if any. This value is used
     * to provide user feedback in the case of a malformed request.
     * <p/>
     * For commands which do not allow any arguments, <code>null</code> should
     * be returned.
     *
     * @return The syntax for the command arguments, or <code>null</code> for
     *         commands without arguments.
     */
    protected abstract String getArgSyntax();

    protected MailFolder getMailbox(String mailboxName,
                                    ImapSession session,
                                    boolean mustExist)
            throws FolderException {
        return session.getHost().getFolder(session.getUser(), mailboxName, mustExist);
    }

    protected MailFolder getMailbox(String mailboxName,
                                    ImapSession session) {
        try {
            return session.getHost().getFolder(session.getUser(), mailboxName, false);
        } catch (FolderException e) {
            throw new RuntimeException("Unexpected error in CommandTemplate.java");
        }
    }

    public CommandParser getParser() {
        return parser;
    }
}
