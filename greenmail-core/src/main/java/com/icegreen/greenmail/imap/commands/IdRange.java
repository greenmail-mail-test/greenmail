/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

/**
 * Represents a range of UID values.
 */
public class IdRange {

    private long _lowVal;
    private long _highVal;

    public IdRange(long singleVal) {
        _lowVal = singleVal;
        _highVal = singleVal;
    }

    public IdRange(long lowVal, long highVal) {
        _lowVal = lowVal;
        _highVal = highVal;
    }

    public long getLowVal() {
        return _lowVal;
    }

    public long getHighVal() {
        return _highVal;
    }

    public boolean includes(long uid) {
        return _lowVal <= uid && uid <= _highVal;
    }

}
