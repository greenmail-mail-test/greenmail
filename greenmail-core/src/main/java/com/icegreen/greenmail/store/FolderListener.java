/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

import javax.mail.Flags;


public interface FolderListener {
    void expunged(int msn);

    void added(int msn);

    void flagsUpdated(int msn, Flags flags, Long uid);

    void mailboxDeleted();
}