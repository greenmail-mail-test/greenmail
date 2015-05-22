package com.icegreen.greenmail.store;

import com.icegreen.greenmail.imap.ImapConstants;

class RootFolder extends HierarchicalFolder {
    public RootFolder() {
        super(null, ImapConstants.USER_NAMESPACE);
    }

    public String getFullName() {
        return name;
    }
}
