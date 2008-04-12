/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.store.InMemoryStore;

import java.util.*;

/**
 * An initial implementation of an ImapHost. By default, uses,
 * the {@link com.icegreen.greenmail.store.InMemoryStore} implementation of {@link com.icegreen.greenmail.store.Store}.
 * TODO: Make the underlying store configurable with Phoenix.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ImapHostManagerImpl
        implements ImapHostManager, ImapConstants {
    private Store store;
    private MailboxSubscriptions subscriptions;

    /**
     * Hack constructor which creates an in-memory store, and creates a console logger.
     */
    public ImapHostManagerImpl() {
        store = new InMemoryStore();
        subscriptions = new MailboxSubscriptions();
    }

    public ImapHostManagerImpl(Store store) {
        this.store = store;
        subscriptions = new MailboxSubscriptions();
    }

    public List getAllMessages() {
        List ret = new ArrayList();
        try {
            Collection boxes = store.listMailboxes("*");
            for (Iterator iterator = boxes.iterator(); iterator.hasNext();) {
                MailFolder folder = (MailFolder) iterator.next();
                List messages = folder.getMessages();
                for (int i = 0; i < messages.size(); i++) {
                    ret.add(messages.get(i));
                }
            }
        } catch (FolderException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public char getHierarchyDelimiter() {
        return HIERARCHY_DELIMITER_CHAR;
    }

    /**
     * @see ImapHostManager#getFolder
     */
    public MailFolder getFolder(GreenMailUser user, String mailboxName) {
        String name = getQualifiedMailboxName(user, mailboxName);
        MailFolder folder = store.getMailbox(name);
        return (checkViewable(folder));
    }

    public MailFolder getFolder(GreenMailUser user, String mailboxName, boolean mustExist)
            throws FolderException {
        MailFolder folder = getFolder(user, mailboxName);
        if (mustExist && (folder == null)) {
            throw new FolderException("No such store.");
        }
        return folder;
    }

    private MailFolder checkViewable(MailFolder folder) {
        // TODO implement this.
        return folder;
    }

    /**
     * @see ImapHostManager#getInbox
     */
    public MailFolder getInbox(GreenMailUser user) throws FolderException {
        return getFolder(user, INBOX_NAME);
    }

    /**
     * @see ImapHostManager#createPrivateMailAccount
     */
    public void createPrivateMailAccount(GreenMailUser user) throws FolderException {
        MailFolder root = store.getMailbox(USER_NAMESPACE);
        MailFolder userRoot = store.createMailbox(root, user.getQualifiedMailboxName(), false);
        store.createMailbox(userRoot, INBOX_NAME, true);
    }

    /**
     * @see ImapHostManager#createMailbox
     */
    public MailFolder createMailbox(GreenMailUser user, String mailboxName)
            throws AuthorizationException, FolderException {
        String qualifiedName = getQualifiedMailboxName(user, mailboxName);
        if (store.getMailbox(qualifiedName) != null) {
            throw new FolderException("Mailbox already exists.");
        }

        StringTokenizer tokens = new StringTokenizer(qualifiedName,
                HIERARCHY_DELIMITER);

        if (tokens.countTokens() < 2) {
            throw new FolderException("Cannot create store at namespace level.");
        }

        String namespaceRoot = tokens.nextToken();
        MailFolder folder = store.getMailbox(namespaceRoot);
        if (folder == null) {
            throw new FolderException("Invalid namespace.");
        }

        while (tokens.hasMoreTokens()) {
            // Get the next name from the list, and find the child
            String childName = tokens.nextToken();
            MailFolder child = store.getMailbox(folder, childName);
            // Create if neccessary
            if (child == null) {
                // TODO check permissions.
                boolean makeSelectable = (!tokens.hasMoreTokens());
                child = store.createMailbox(folder, childName, makeSelectable);
            }
            folder = child;
        }

        return folder;
    }

    /**
     * @see ImapHostManager#deleteMailbox
     */
    public void deleteMailbox(GreenMailUser user, String mailboxName)
            throws FolderException, AuthorizationException {
        MailFolder toDelete = getFolder(user, mailboxName, true);
        if (store.getChildren(toDelete).isEmpty()) {
            toDelete.deleteAllMessages();
            toDelete.signalDeletion();
            store.deleteMailbox(toDelete);
        } else {
            if (toDelete.isSelectable()) {
                toDelete.deleteAllMessages();
                store.setSelectable(toDelete, false);
            } else {
                throw new FolderException("Can't delete a non-selectable store with children.");
            }
        }
    }

    /**
     * @see ImapHostManager#renameMailbox
     */
    public void renameMailbox(GreenMailUser user,
                              String oldMailboxName,
                              String newMailboxName)
            throws FolderException, AuthorizationException {

        MailFolder existingFolder = getFolder(user, oldMailboxName, true);

        // TODO: check permissions.

        // Handle case where existing is INBOX
        //          - just create new folder, move all messages,
        //            and leave INBOX (with children) intact.
        String userInboxName = getQualifiedMailboxName(user, INBOX_NAME);
        if (userInboxName.equals(existingFolder.getFullName())) {
            MailFolder inbox = existingFolder;
            MailFolder newBox = createMailbox(user, newMailboxName);
            long[] uids = inbox.getMessageUids();
            for (int i = 0; i < uids.length; i++) {
                long uid = uids[i];
                inbox.copyMessage(uid, newBox);
            }
            inbox.deleteAllMessages();
            return;
        }

        store.renameMailbox(existingFolder, newMailboxName);
    }

    /**
     * @see ImapHostManager#listSubscribedMailboxes
     */
    public Collection listSubscribedMailboxes(GreenMailUser user,
                                              String mailboxPattern)
            throws FolderException {
        return listMailboxes(user, mailboxPattern, true);
    }

    /**
     * @see ImapHostManager#listMailboxes
     */
    public Collection listMailboxes(GreenMailUser user,
                                    String mailboxPattern)
            throws FolderException {
        return listMailboxes(user, mailboxPattern, false);
    }

    /**
     * Partial implementation of list functionality.
     * TODO: Handle wildcards anywhere in store pattern
     * (currently only supported as last character of pattern)
     *
     * @see com.icegreen.greenmail.imap.ImapHostManager#listMailboxes
     */
    private Collection listMailboxes(GreenMailUser user,
                                     String mailboxPattern,
                                     boolean subscribedOnly)
            throws FolderException {
//        System.out.println( "Listing for user: '" + user.getUserName() + "'" +
//                            " pattern:'" + mailboxPattern + "'" );

        ArrayList mailboxes = new ArrayList();
        String qualifiedPattern = getQualifiedMailboxName(user, mailboxPattern);

        Iterator iter = store.listMailboxes(qualifiedPattern).iterator();
        while (iter.hasNext()) {
            MailFolder folder = (MailFolder) iter.next();

            // TODO check subscriptions.
            if (subscribedOnly) {
                if (!subscriptions.isSubscribed(user, folder)) {
                    // if not subscribed
                    folder = null;
                }
            }

            // Sets the store to null if it's not viewable.
            folder = checkViewable(folder);

            if (folder != null) {
                mailboxes.add(folder);
            }
        }

        return mailboxes;
    }

    /**
     * @see ImapHostManager#subscribe
     */
    public void subscribe(GreenMailUser user, String mailboxName)
            throws FolderException {
        MailFolder folder = getFolder(user, mailboxName, true);
        subscriptions.subscribe(user, folder);
    }

    /**
     * @see ImapHostManager#unsubscribe
     */
    public void unsubscribe(GreenMailUser user, String mailboxName)
            throws FolderException {
        MailFolder folder = getFolder(user, mailboxName, true);
        subscriptions.unsubscribe(user, folder);
    }

    /**
     * Convert a user specified store name into a server absolute name.
     * If the mailboxName begins with the namespace token,
     * return as-is.
     * If not, need to resolve the Mailbox name for this user.
     * Example:
     * <br> Convert "INBOX" for user "Fred.Flinstone" into
     * absolute name: "#user.Fred.Flintstone.INBOX"
     *
     * @return String of absoluteName, null if not valid selection
     */
    private String getQualifiedMailboxName(GreenMailUser user, String mailboxName) {
        String userNamespace = user.getQualifiedMailboxName();

        if ("INBOX".equalsIgnoreCase(mailboxName)) {
            return USER_NAMESPACE + HIERARCHY_DELIMITER + userNamespace +
                    HIERARCHY_DELIMITER + INBOX_NAME;
        }

        if (mailboxName.startsWith(NAMESPACE_PREFIX)) {
            return mailboxName;
        } else {
            if (mailboxName.length() == 0) {
                return USER_NAMESPACE + HIERARCHY_DELIMITER + userNamespace;
            } else {
                return USER_NAMESPACE + HIERARCHY_DELIMITER + userNamespace +
                        HIERARCHY_DELIMITER + mailboxName;
            }
        }
    }

    /**
     * Handles all user subscriptions.
     * TODO make this a proper class
     * TODO persist
     */
    private class MailboxSubscriptions {
        private Map userSubs = new HashMap();

        /**
         * Subscribes the user to the store.
         * TODO should this fail if already subscribed?
         *
         * @param user   The user making the subscription
         * @param folder The store to subscribe
         * @throws FolderException ??? doesn't yet.
         */
        void subscribe(GreenMailUser user, MailFolder folder)
                throws FolderException {
            getUserSubs(user).add(folder.getFullName());
        }

        /**
         * Unsubscribes the user from this store.
         * TODO should this fail if not already subscribed?
         *
         * @param user   The user making the request.
         * @param folder The store to unsubscribe
         * @throws FolderException ?? doesn't yet
         */
        void unsubscribe(GreenMailUser user, MailFolder folder)
                throws FolderException {
            getUserSubs(user).remove(folder.getFullName());
        }

        /**
         * Returns whether the user is subscribed to the specified store.
         *
         * @param user   The user to test.
         * @param folder The store to test.
         * @return <code>true</code> if the user is subscribed.
         */
        boolean isSubscribed(GreenMailUser user, MailFolder folder) {
            return getUserSubs(user).contains(folder.getFullName());
        }

        private Collection getUserSubs(GreenMailUser user) {
            Collection subs = (Collection) userSubs.get(user.getLogin());
            if (subs == null) {
                subs = new ArrayList();
                userSubs.put(user.getLogin(), subs);
            }
            return subs;
        }
    }


}
