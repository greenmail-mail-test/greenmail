/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ProtocolException extends Exception {
    static final long serialVersionUID = -8903976326699432941L;
    public ProtocolException(String s) {
        super(s);
    }
    public ProtocolException(String s, Throwable cause) {
        super(s,cause);
    }
}
