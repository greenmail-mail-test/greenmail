/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;

public interface Quittable {
    /**
     * Flags something (probably an ImapHandler)
     * as quitting.  That does not mean that the
     * current transactions (IMAP commands) will
     * be interrupted, but it does mean that no
     * more transactions will be accepted.
     */
    public void quit();
}