/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.commands.IdRange;

import java.util.List;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public interface StoredMessageCollection extends Iterable<StoredMessage> {
    int size();

    void add(StoredMessage storedMessage);

    StoredMessage remove(long uid);

    void clear();

    int getFirstUnseen();

    int getMsn(long uid) throws FolderException;

    List<StoredMessage> getMessages(MsgRangeFilter range);

    List<StoredMessage> getMessages();

    long[] getMessageUids();

    void expunge(List<FolderListener> folderListeners);

    /**
     * Expunges all messages flagged deleted and with UID in given ranges.
     *
     * @param mailboxListeners folders to notify.
     * @param idRanges the UID message set ranges.
     */
    void expunge(List<FolderListener> mailboxListeners, IdRange[] idRanges);

    StoredMessage get(int i);

}
