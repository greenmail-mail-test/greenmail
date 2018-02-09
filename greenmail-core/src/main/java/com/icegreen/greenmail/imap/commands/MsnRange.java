/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

/**
 * Represents a range of Message Sequence Numbers.
 */
public class MsnRange {

    private int lowVal;
    private int highVal;

    public MsnRange(int singleVal) {
        lowVal = singleVal;
        highVal = singleVal;
    }

    public MsnRange(int lowVal, int highVal) {
        this.lowVal = lowVal;
        this.highVal = highVal;
    }

    public int getLowVal() {
        return lowVal;
    }

    public int getHighVal() {
        return highVal;
    }

    public boolean includes(int msn) {
        return lowVal <= msn && msn <= highVal;
    }

}
