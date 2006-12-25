/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;

import javax.mail.Flags;


public interface FolderListener {
    void expunged(int msn);

    void added(int msn);

    void flagsUpdated(int msn, Flags flags, Long uid);

    void mailboxDeleted();
}