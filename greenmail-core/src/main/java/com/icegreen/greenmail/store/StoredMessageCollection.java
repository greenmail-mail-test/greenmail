/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;

import java.util.List;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public interface StoredMessageCollection extends Iterable<StoredMessage> {
    int size();

    void add(StoredMessage storedMessage);

    void clear();

    int getFirstUnseen();

    int getMsn(long uid) throws FolderException;

    List<StoredMessage> getMessages(MsgRangeFilter range);

    List<StoredMessage> getMessages();

    long[] getMessageUids();

    void expunge(List<FolderListener> folderListeners);

    StoredMessage get(int i);
}
