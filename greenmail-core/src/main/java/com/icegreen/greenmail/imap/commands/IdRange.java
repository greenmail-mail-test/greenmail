/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a range of UID values.
 */
public class IdRange implements Serializable {

    private long lowVal;
    private long highVal;

    public IdRange(long singleVal) {
        lowVal = singleVal;
        highVal = singleVal;
    }

    public IdRange(long lowVal, long highVal) {
        this.lowVal = lowVal;
        this.highVal = highVal;
    }

    public long getLowVal() {
        return lowVal;
    }

    public long getHighVal() {
        return highVal;
    }

    public boolean includes(long uid) {
        return lowVal <= uid && uid <= highVal;
    }

    /**
     * Parses a uid sequence, a comma separated list of uid ranges.
     *
     * Example: 1 2:5 8:*
     * @param idRangeSequence the sequence
     * @return a list of ranges, never null.
     */
    public static List<IdRange> parseRangeSequence(String idRangeSequence) {
        StringTokenizer tokenizer = new StringTokenizer(idRangeSequence, ",");
        List<IdRange> ranges = new ArrayList<IdRange>();
        while (tokenizer.hasMoreTokens()) {
            ranges.add(parseRange(tokenizer.nextToken()));
        }
        return ranges;
    }

    /**
     * Parses a single id range, eg "1" or "1:2" or "4:*".
     *
     * @param range the range.
     * @return the parsed id range.
     */
    public static IdRange parseRange(String range) {
        int pos = range.indexOf(':');
        try {
            if (pos == -1) {
                long value = parseLong(range);
                return new IdRange(value);
            } else {
                long lowVal = parseLong(range.substring(0, pos));
                long highVal = parseLong(range.substring(pos + 1));
                return new IdRange(lowVal, highVal);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid message set " + range);
        }
    }

    private static long parseLong(String value) {
        if (value.length() == 1 && value.charAt(0) == '*') {
            return Long.MAX_VALUE;
        }
        return Long.parseLong(value);
    }
}
