/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.MessagingException;
import javax.mail.Quota;

import com.icegreen.greenmail.imap.ImapConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple in-memory implementation of {@link Store}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class InMemoryStore
        implements Store, ImapConstants {
    final Logger log = LoggerFactory.getLogger(InMemoryStore.class);

    boolean quotaSupported = true;
    private RootFolder rootMailbox = new RootFolder();
    private Map<String, Set<Quota>> quotaMap = new HashMap<>();

    @Override
    public MailFolder getMailbox(String absoluteMailboxName) {
        log.debug("Entering getMailbox with absoluteMailboxName: '" + absoluteMailboxName + "'");
        StringTokenizer tokens = new StringTokenizer(absoluteMailboxName, HIERARCHY_DELIMITER);

        // The first token must be "#mail"
        if (!tokens.hasMoreTokens() ||
                !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE)) {
            log.debug("Leaving getMailbox null");
            return null;
        }

        HierarchicalFolder parent = rootMailbox;
        while (parent != null && tokens.hasMoreTokens()) {
            String childName = tokens.nextToken();
            parent = parent.getChild(childName);
        }
        log.debug("Leaving getMailbox with parent: " + parent);
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
        log.debug("Entering createMailbox with parent: '" + parent + "' and name '" + mailboxName + "', and selectable: " + selectable);

        if (mailboxName.indexOf(HIERARCHY_DELIMITER_CHAR) != -1) {
            throw new FolderException("Invalid mailbox name.");
        }
        HierarchicalFolder castParent = (HierarchicalFolder) parent;
        HierarchicalFolder child = new HierarchicalFolder(castParent, mailboxName);
        castParent.getChildren().add(child);
        child.setSelectable(selectable);
        return child;
    }

    @Override
    public void deleteMailbox(MailFolder folder) throws FolderException {
        HierarchicalFolder toDelete = (HierarchicalFolder) folder;

        if (!toDelete.getChildren().isEmpty()) {
            throw new FolderException("Cannot delete mailbox with children.");
        }

        if (toDelete.getMessageCount() != 0) {
            throw new FolderException("Cannot delete non-empty mailbox");
        }

        HierarchicalFolder parent = toDelete.getParent();
        parent.getChildren().remove(toDelete);
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
            parent.getChildren().remove(toRename);
            HierarchicalFolder userFolder = findParentByName(toRename, ImapConstants.INBOX_NAME).getParent();
            String[] path = newName.split('\\' + ImapConstants.HIERARCHY_DELIMITER);
            HierarchicalFolder newParent = userFolder;
            for (int i = 0; i < path.length - 1; i++) {
                newParent = newParent.getChild(path[i]);
            }
            toRename.moveToNewParent(newParent);
            toRename.setName(newFolderName);
        }
    }

    private HierarchicalFolder findParentByName(HierarchicalFolder folder, String parentName) {
        HierarchicalFolder currentFolder = folder;
        while (null != currentFolder && !parentName.equals(currentFolder.getName())) {
            currentFolder = currentFolder.getParent();
        }
        return currentFolder;
    }

    @Override
    public Collection<MailFolder> getChildren(MailFolder parent) {
        Collection<HierarchicalFolder> children = ((HierarchicalFolder) parent).getChildren();
        return Collections.<MailFolder>unmodifiableCollection(children);
    }

    @Override
    public MailFolder setSelectable(MailFolder folder, boolean selectable) {
        ((HierarchicalFolder) folder).setSelectable(selectable);
        return folder;
    }

    @Override
    public Collection<MailFolder> listMailboxes(String searchPattern)
            throws FolderException {
        log.debug("Entering listMailboxes with searchPattern: '" + searchPattern + "'");
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
                    for (StoredMessage m : folder.getMessageEntries()) {
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

    public void stop() {
        // Do nothing, it is an in-memory store, it will be garbage-collected automaticallys
    }

}
