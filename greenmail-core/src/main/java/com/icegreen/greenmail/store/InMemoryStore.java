/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.imap.ImapConstants;

import javax.mail.MessagingException;
import javax.mail.Quota;
import java.util.*;

/**
 * A simple in-memory implementation of {@link Store}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class InMemoryStore
        implements Store, ImapConstants {
    boolean quotaSupported = true;
    private RootFolder rootMailbox = new RootFolder();
    private Map<String, Set<Quota>> quotaMap = new HashMap<>();

    @Override
    public MailFolder getMailbox(String absoluteMailboxName) {
        StringTokenizer tokens = new StringTokenizer(absoluteMailboxName, HIERARCHY_DELIMITER);

        // The first token must be "#mail"
        if (!tokens.hasMoreTokens() ||
                !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE)) {
            return null;
        }

        HierarchicalFolder parent = rootMailbox;
        while (parent != null && tokens.hasMoreTokens()) {
            String childName = tokens.nextToken();
            parent = parent.getChild(childName);
        }
        return parent;
    }

    @Override
    public MailFolder getMailbox(MailFolder parent, String name) {
        return ((HierarchicalFolder) parent).getChild(name);
    }

    @Override
    public MailFolder createMailbox(MailFolder parent,
                                    String mailboxName,
                                    boolean selectable)
            throws FolderException {
        if (mailboxName.indexOf(HIERARCHY_DELIMITER_CHAR) != -1) {
            throw new FolderException("Invalid mailbox name "+mailboxName);
        }
        HierarchicalFolder castParent = (HierarchicalFolder) parent;
        HierarchicalFolder child = castParent.createChild(mailboxName);
        child.setSelectable(selectable);
        return child;
    }

    @Override
    public void deleteMailbox(MailFolder folder) throws FolderException {
        HierarchicalFolder toDelete = (HierarchicalFolder) folder;

        if (toDelete.hasChildren()) {
            throw new FolderException("Cannot delete mailbox with children.");
        }

        if (toDelete.getMessageCount() != 0) {
            throw new FolderException("Cannot delete non-empty mailbox");
        }

        HierarchicalFolder parent = toDelete.getParent();
        parent.removeChild(toDelete);
    }

    @Override
    public void renameMailbox(MailFolder existingFolder, String newName) throws FolderException {
        HierarchicalFolder toRename = (HierarchicalFolder) existingFolder;
        HierarchicalFolder parent = toRename.getParent();

        int idx = newName.lastIndexOf(ImapConstants.HIERARCHY_DELIMITER_CHAR);
        String newFolderName;
        String newFolderPathWithoutName;
        if (idx > 0) {
            newFolderName = newName.substring(idx + 1);
            newFolderPathWithoutName = newName.substring(0, idx);
        } else {
            newFolderName = newName;
            newFolderPathWithoutName = "";
        }

        if (parent.getName().equals(newFolderPathWithoutName)) {
            // Simple rename
            toRename.setName(newFolderName);
        } else {
            // Hierarchy change
            parent.removeChild(toRename);
            HierarchicalFolder userFolder = getInboxOrUserRootFolder(toRename);
            String[] path = newName.split('\\' + ImapConstants.HIERARCHY_DELIMITER);
            HierarchicalFolder newParent = userFolder;
            for (int i = 0; i < path.length - 1; i++) {
                newParent = newParent.getChild(path[i]);
            }
            toRename.moveToNewParent(newParent);
            toRename.setName(newFolderName);
        }
    }

    private HierarchicalFolder getInboxOrUserRootFolder(HierarchicalFolder folder) {
        final HierarchicalFolder inboxFolder = findParentByName(folder, ImapConstants.INBOX_NAME);
        if(null==inboxFolder) {
            return folder.getParent();
        }
        return inboxFolder.getParent();
    }

    private HierarchicalFolder findParentByName(HierarchicalFolder folder, String name) {
        HierarchicalFolder currentFolder = folder;
        while (null != currentFolder && !name.equals(currentFolder.getName())) {
            currentFolder = currentFolder.getParent();
        }
        return currentFolder;
    }

    @Override
    public Collection<MailFolder> getChildren(MailFolder parent) {
        return Collections.<MailFolder>unmodifiableCollection(((HierarchicalFolder) parent).getChildren());
    }

    @Override
    public MailFolder setSelectable(MailFolder folder, boolean selectable) {
        ((HierarchicalFolder) folder).setSelectable(selectable);
        return folder;
    }

    @Override
    public Collection<MailFolder> listMailboxes(String searchPattern)
            throws FolderException {
        int starIndex = searchPattern.indexOf('*');
        int percentIndex = searchPattern.indexOf('%');

        // We only handle wildcard at the end of the search pattern.
        if ((starIndex > -1 && starIndex < searchPattern.length() - 1) ||
                (percentIndex > -1 && percentIndex < searchPattern.length() - 1)) {
            throw new FolderException("WIldcard characters are only handled as the last character of a list argument.");
        }

        List<MailFolder> mailboxes = new ArrayList<>();
        if (starIndex != -1 || percentIndex != -1) {
            int lastDot = searchPattern.lastIndexOf(HIERARCHY_DELIMITER);
            String parentName;
            if (lastDot < 0) {
                parentName = USER_NAMESPACE;
            } else {
                parentName = searchPattern.substring(0, lastDot);
            }
            String matchPattern = searchPattern.substring(lastDot + 1, searchPattern.length() - 1);

            HierarchicalFolder parent = (HierarchicalFolder) getMailbox(parentName);
            // If the parent from the search pattern doesn't exist,
            // return empty.
            if (parent != null) {
                for (final Object o : parent.getChildren()) {
                    HierarchicalFolder child = (HierarchicalFolder) o;
                    if (child.getName().startsWith(matchPattern)) {
                        mailboxes.add(child);

                        if (starIndex != -1) {
                            addAllChildren(child, mailboxes);
                        }
                    }
                }
            }

        } else {
            MailFolder folder = getMailbox(searchPattern);
            if (folder != null) {
                mailboxes.add(folder);
            }
        }

        return mailboxes;
    }

    @Override
    public Quota[] getQuota(final String root, final String qualifiedRootPrefix) {
        Set<String> rootPaths = new HashSet<>();
        if (!root.contains(ImapConstants.HIERARCHY_DELIMITER)) {
            rootPaths.add(qualifiedRootPrefix + root);
        } else {
            for (String r : root.split(ImapConstants.HIERARCHY_DELIMITER)) {
                rootPaths.add(qualifiedRootPrefix + r);
            }
        }
        rootPaths.add(qualifiedRootPrefix); // Add default root

        Set<Quota> collectedQuotas = new HashSet<>();
        for (String p : rootPaths) {
            Set<Quota> quotas = quotaMap.get(p);
            if (null != quotas) {
                collectedQuotas.addAll(quotas);
            }
        }
        updateQuotas(collectedQuotas, qualifiedRootPrefix);
        return collectedQuotas.toArray(new Quota[collectedQuotas.size()]);
    }

    private void updateQuotas(final Set<Quota> quotas,
                              final String qualifiedRootPrefix) {
        for (Quota q : quotas) {
            updateQuota(q, qualifiedRootPrefix);
        }
    }

    private void updateQuota(final Quota quota, final String pQualifiedRootPrefix) {
        MailFolder folder = getMailbox(
                ImapConstants.USER_NAMESPACE + ImapConstants.HIERARCHY_DELIMITER +
                        pQualifiedRootPrefix + ImapConstants.HIERARCHY_DELIMITER +
                        quota.quotaRoot);
        try {
            for (Quota.Resource r : quota.resources) {
                if (STORAGE.equals(r.name)) {
                    long size = 0;
                    for (StoredMessage m : folder.getMessages()) {
                        size += m.getMimeMessage().getSize();
                    }
                    r.usage = size;
                } else if (MESSAGES.equals(r.name)) {
                    r.usage = folder.getMessageCount();
                } else {
                    throw new IllegalStateException("Quota " + r.name + " not supported");
                }
            }
        } catch (MessagingException ex) {
            throw new IllegalStateException("Can not update/verify quota " + quota, ex);
        }
    }

    @Override
    public void setQuota(final Quota quota, String qualifiedRootPrefix) {
        // Validate
        for (Quota.Resource r : quota.resources) {
            if (!STORAGE.equals(r.name) && !MESSAGES.equals(r.name)) {
                throw new IllegalStateException("Quota " + r.name + " not supported");
            }
        }

        // Save quota
        Set<Quota> quotas = quotaMap.get(qualifiedRootPrefix + quota.quotaRoot);
        if (null == quotas) {
            quotas = new HashSet<>();
            quotaMap.put(qualifiedRootPrefix + quota.quotaRoot, quotas);
        } else {
            quotas.clear(); // " Any previous resource limits for the named quota root are discarded"
        }
        quotas.add(quota);
    }

    private void addAllChildren(HierarchicalFolder mailbox, Collection<MailFolder> mailboxes) {
        Collection<HierarchicalFolder> children = mailbox.getChildren();
        for (HierarchicalFolder child : children) {
            mailboxes.add(child);
            addAllChildren(child, mailboxes);
        }
    }

    @Override
    public boolean isQuotaSupported() {
        return quotaSupported;
    }

    @Override
    public void setQuotaSupported(final boolean pQuotaSupported) {
        quotaSupported = pQuotaSupported;
    }
}
