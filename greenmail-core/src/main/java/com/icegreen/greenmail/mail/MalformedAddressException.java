/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

public class MalformedAddressException extends Exception {
    static final long serialVersionUID = 5975918967992752579L;

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