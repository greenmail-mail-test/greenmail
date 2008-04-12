/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;

/**
 * Encapsulates all state held for an ongoing Imap session,
 * which commences when a client first establishes a connection to the Imap
 * server, and continues until that connection is closed.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface ImapSession {
    /**
     * Sends any unsolicited responses to the client, such as EXISTS and FLAGS
     * responses when the selected mailbox is modified by another user.
     *
     * @param response The response to write to
     */
    void unsolicitedResponses(ImapResponse response) throws FolderException;

    /**
     * Closes the connection for this session.
     */
    void closeConnection();

    void closeConnection(String byeMessage);

    /**
     * Provides the Imap host for this server, which is used for all access to mail
     * storage and subscriptions.
     *
     * @return The ImapHost for this server.
     */
    ImapHostManager getHost();

    /**
     * Provides the UserManager for this session, to allow session
     * to validate logins.
     *
     * @return The UserManager for this session.
     */
    UserManager getUserManager();

    /**
     * @return The hostname of the connected client.
     */
    String getClientHostname();

    /**
     * @return The IP address of the connected client.
     */
    String getClientIP();

    /**
     * @return Returns the current state of this session.
     */
    ImapSessionState getState();

    /**
     * Moves the session into {@link ImapSessionState#AUTHENTICATED} state with
     * the supplied user.
     *
     * @param user The user who is authenticated for this session.
     */
    void setAuthenticated(GreenMailUser user);

    /**
     * Provides the authenticated user for this session, or <code>null</code> if this
     * session is not in {@link ImapSessionState#AUTHENTICATED} or
     * {@link ImapSessionState#SELECTED} state.
     *
     * @return The user authenticated for this session
     */
    GreenMailUser getUser();

    /**
     * Moves this session into {@link ImapSessionState#SELECTED} state and sets the
     * supplied mailbox to be the currently selected mailbox.
     *
     * @param folder   The selected mailbox.
     * @param readOnly If <code>true</code>, the selection is set to be read only.
     */
    void setSelected(MailFolder folder, boolean readOnly);

    /**
     * Moves the session out of {@link ImapSessionState#SELECTED} state and back into
     * {@link ImapSessionState#AUTHENTICATED} state. The selected mailbox is cleared.
     */
    void deselect();

    /**
     * Provides the selected mailbox for this session, or <code>null</code> if this
     * session is not in {@link ImapSessionState#SELECTED} state.
     *
     * @return the currently selected mailbox.
     */
    ImapSessionFolder getSelected();

    void unsolicitedResponses(ImapResponse request, boolean omitExpunged) throws FolderException;

}
