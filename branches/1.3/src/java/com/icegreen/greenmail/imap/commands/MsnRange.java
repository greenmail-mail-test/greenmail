/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.imap.commands;

/**
 * Represents a range of Message Sequence Numbers.
 */
public class MsnRange {

    private int _lowVal;
    private int _highVal;

    public MsnRange(int singleVal) {
        _lowVal = singleVal;
        _highVal = singleVal;
    }

    public MsnRange(int lowVal, int highVal) {
        _lowVal = lowVal;
        _highVal = highVal;
    }

    public int getLowVal() {
        return _lowVal;
    }

    public int getHighVal() {
        return _highVal;
    }

    public boolean includes(int msn) {
        return _lowVal <= msn && msn <= _highVal;
    }

}
