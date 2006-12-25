/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.FolderException;


public class QuitCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return true;
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            MailFolder folder = state.getFolder();
            if (folder != null) {
                folder.expunge();

            }

            conn.println("+OK bye see you soon");
            conn.quit();
        } catch (FolderException me) {
            conn.println("+OK Signing off, but message deletion failed");
            conn.quit();
        }
    }
}