/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.mail;

public class MailException extends Exception {
    static final long serialVersionUID = 7582669663443148456L;

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
