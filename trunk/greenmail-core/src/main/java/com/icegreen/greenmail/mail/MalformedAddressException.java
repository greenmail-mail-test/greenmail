/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

public class MalformedAddressException
        extends Exception {
    public MalformedAddressException() {
        super();
    }

    public MalformedAddressException(String s) {
        super(s);
    }

    public MalformedAddressException(String s, Throwable t) {
        super(s, t);
    }

    public MalformedAddressException(Throwable t) {
        super(t);
    }
}