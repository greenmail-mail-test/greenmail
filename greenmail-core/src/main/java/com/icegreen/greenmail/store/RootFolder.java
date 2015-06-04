/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import com.icegreen.greenmail.imap.ImapConstants;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
class RootFolder extends HierarchicalFolder {
    RootFolder(final StoredMessageCollectionFactory storedMessageCollectionFactory) {
        super(storedMessageCollectionFactory, null, ImapConstants.USER_NAMESPACE);
    }

    public String getFullName() {
        return name;
    }
}
