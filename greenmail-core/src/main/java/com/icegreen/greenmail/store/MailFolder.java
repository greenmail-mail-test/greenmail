/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.mail.MovingMessage;

import jakarta.mail.Flags;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import java.util.Date;
import java.util.List;

/**
 * Represents a mailbox within an {@link com.icegreen.greenmail.store.Store}.
 * May provide storage for MovingMessage objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision: 109034 $
 */
public interface MailFolder {
    String getName();

    String getFullName();

    /**
     * The flags (at a minimum, the system-defined flags) that are applicable for this mailbox.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3501#section-7.2.6">rfc3501#section-7.2.6</a>
     *
     * @return the applicable mailbox flags
     */
    Flags getAvailableFlags();

    /**
     * Permanent flags are those which the client can add or remove from the
     * message flags permanently; that is, concurrent and subsequent
     * sessions will see any change in permanent flags
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3501#page-12">rfc3501#page-12</a>
     *
     * @return the permanent (persisting across sessions) flags
     */
    Flags getPermanentFlags();

    int getMessageCount();

    int getRecentCount(boolean reset);

    long getUidValidity();

    int getFirstUnseen();

    int getUnseenCount();

    boolean isSelectable();

    /**
     * {@inheritDoc}
     */
    long getUIDNext();

    /**
     * Appends a message.
     *
     * @param message the message.
     * @param flags the flags.
     * @param internalDate the received date
     * @return the uid.
     */
    long appendMessage(MimeMessage message, Flags flags, Date internalDate);

    void deleteAllMessages();

    void expunge() throws FolderException;

    /**
     * Expunges flagged for deletion messages in given range.
     *
     * @see com.icegreen.greenmail.imap.commands.ExpungeCommand
     * @param idRanges the ranges. Can be null.
     */
    void expunge(IdRange[] idRanges);

    void addListener(FolderListener listener);

    void removeListener(FolderListener listener);

    void store(MovingMessage mail) throws Exception;
    void store(MimeMessage mail) throws Exception;

    StoredMessage getMessage(long uid);

    long[] getMessageUids();

    long[] search(SearchTerm searchTerm);

    long copyMessage(long uid, MailFolder toFolder) throws FolderException;

    long moveMessage(long uid, MailFolder toFolder) throws FolderException;

    void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException;

    void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException;

    int getMsn(long uid) throws FolderException;

    void signalDeletion();

    List<StoredMessage> getMessages(MsgRangeFilter msgRangeFilter);
    List<StoredMessage> getMessages();
    List<StoredMessage> getNonDeletedMessages();
}
