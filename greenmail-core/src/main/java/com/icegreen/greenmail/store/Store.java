/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import javax.mail.Quota;
import java.util.Collection;

/**
 * Represents the complete mail store for an IMAP server, providing access to
 * and manipulation of all {@link com.icegreen.greenmail.store.MailFolder Mailboxes} stored on this server.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface Store {
    /**
     * Retrieves a mailbox based on a fully qualified name.
     *
     * @param qualifiedMailboxName the fully qualified name.
     * @return The mailbox if present, or <code>null</code> if not.
     */
    MailFolder getMailbox(String qualifiedMailboxName);

    /**
     * Looks up a child mailbox of the supplied parent with the name given.
     *
     * @param parent      The parent mailbox
     * @param mailboxName The name of the child to lookup
     * @return The child mailbox, or <code>null</code> if not found.
     */
    MailFolder getMailbox(MailFolder parent, String mailboxName);

    /**
     * @param parent A mailbox from this store.
     * @return A read-only collection of {@link MailFolder} instances, which
     *         are the children of the supplied parent.
     */
    Collection<MailFolder> getChildren(MailFolder parent);

    /**
     * Creates a mailbox under the supplied parent with the given name.
     * If specified, the mailbox created will be made selectable (able to store messages).
     *
     * @param parent      A mailbox from this store.
     * @param mailboxName The name of the mailbox to create.
     * @param selectable  If <code>true</code>, the mailbox will be created to store messages.
     * @return The created mailbox
     * @throws FolderException If the mailbox couldn't be created.
     */
    MailFolder createMailbox(MailFolder parent,
                             String mailboxName,
                             boolean selectable)
            throws FolderException;

    /**
     * Tells the store to make the supplied mailbox selectable or not (able to store
     * messages). The returned mailbox may be a new instance, and the supplied mailbox
     * may no longer be valid.
     *
     * @param folder     The mailbox to modify.
     * @param selectable Whether this mailbox should be able to store messages.
     * @return The modified mailbox
     */
    MailFolder setSelectable(MailFolder folder, boolean selectable);

    /**
     * Deletes the supplied mailbox from the store. To be deleted, mailboxes
     * must be empty of messages, and not have any children.
     *
     * @param folder A mailbox from this store.
     * @throws FolderException If the mailbox couldn't be deleted.
     */
    void deleteMailbox(MailFolder folder) throws FolderException;

    /**
     * Renames the mailbox with the new name.
     *
     * @param existingFolder A mailbox from this store.
     * @param newName        The new name for the mailbox.
     * @throws FolderException If the mailbox couldn't be renamed
     */
    void renameMailbox(MailFolder existingFolder, String newName)
            throws FolderException;

    /**
     * Lists all of the mailboxes in the store which have a name
     * matching the supplied search pattern.
     * <pre>
     * Valid wildcards are:
     *          '*' - matches any number of characters, including the hierarchy delimiter
     *          '%' - matches any number of characters, but not the hierarchy delimiter
     *
     * @param searchPattern The pattern to match mailboxes
     * @return A read-only collection of mailboxes which match this pattern
     * @throws FolderException If the list operation failed
     */
    Collection<MailFolder> listMailboxes(String searchPattern) throws FolderException;

    /**
     * Gets the quotas.
     *
     * @link http://www.ietf.org/rfc/rfc2087.txt
     * @see com.sun.mail.imap.IMAPStore#getQuota(String)
     * @param root the quota root
     * @param qualifiedRootPrefix the user specific prefix
     * @return the quotas, or an empty array.
     */
    Quota[] getQuota(String root, String qualifiedRootPrefix);

    /**
     * Sets the quota.
     *
     * @link http://www.ietf.org/rfc/rfc2087.txt
     * @see com.sun.mail.imap.IMAPStore#setQuota(javax.mail.Quota)
     * @param quota the quota.
     * @param qualifiedRootPrefix the user specific prefix
     */
    void setQuota(Quota quota, String qualifiedRootPrefix);

    /**
     * Checks if quota capability is activated.
     *
     * @return true, if quota is supported.
     */
    boolean isQuotaSupported();

    /**
     * Toggles quota capability.
     *
     * @param pQuotaSupported true, if supported.
     */
    void setQuotaSupported(boolean pQuotaSupported);
}
