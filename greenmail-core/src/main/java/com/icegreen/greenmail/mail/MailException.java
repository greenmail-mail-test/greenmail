/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

public class MailException extends Exception {

    public MailException() {
        super();
    }

    public MailException(String s) {
        super(s);
    }

    public MailException(String s, Throwable t) {
        super(s, t);
    }

    public MailException(Throwable t) {
        super(t);
    }

}
