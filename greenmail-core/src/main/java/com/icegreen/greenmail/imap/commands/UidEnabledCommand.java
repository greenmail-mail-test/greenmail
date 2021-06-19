/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ImapResponse;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.FolderException;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface UidEnabledCommand {
    void doProcess(ImapRequestLineReader request,
                   ImapResponse response,
                   ImapSession session,
                   boolean useUids)
            throws ProtocolException, FolderException;
}
