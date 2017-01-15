/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Represents a range of UID values.
 */
public class IdRange implements Serializable {
    /** Matches a sequence of a single id or id range */
    public static final Pattern SEQUENCE = Pattern.compile("\\d+|\\d+\\:\\d+");
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
     * <p/>
     * Example: 1 2:5 8:*
     *
     * @param idRangeSequence the sequence
     * @return a list of ranges, never null.
     */
    public static List<IdRange> parseRangeSequence(String idRangeSequence) {
        StringTokenizer tokenizer = new StringTokenizer(idRangeSequence, " ");
        List<IdRange> ranges = new ArrayList<>();
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

    public static IdRange[] convertUidsToIdRangeArray(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return new IdRange[0];
        }

        List<Long> uidsLocal = new LinkedList<>(uids);
        Collections.sort(uidsLocal);

        List<IdRange> ids = new LinkedList<>();

        IdRange currentIdRange = new IdRange(uidsLocal.get(0));
        for (Long uid : uidsLocal) {
            if (uid == currentIdRange.getHighVal()) {
                // Ignore
            } else if (uid > currentIdRange.getHighVal() && (uid == currentIdRange.getHighVal() + 1)) {
                currentIdRange = new IdRange(currentIdRange.getLowVal(), uid);
            } else {
                ids.add(currentIdRange);
                currentIdRange = new IdRange(uid);
            }
        }

        if (!ids.contains(currentIdRange)) {
            ids.add(currentIdRange);
        }

        return ids.toArray(new IdRange[ids.size()]);
    }

    public static String uidsToRangeString(List<Long> uids) {
        return idRangesToString(convertUidsToIdRangeArray(uids));
    }

    public static String idRangeToString(IdRange idRange) {
        return idRange.getHighVal() == idRange.getLowVal()
                ? Long.toString(idRange.getLowVal())
                : Long.toString(idRange.getLowVal()) + ":" + Long.toString(idRange.getHighVal());
    }

    public static String idRangesToString(IdRange[] idRanges) {
        StringBuilder sb = new StringBuilder();

        for (IdRange idRange : idRanges) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(idRangeToString(idRange));
        }

        return sb.toString();
    }

    private static long parseLong(String value) {
        if (value.length() == 1 && value.charAt(0) == '*') {
            return Long.MAX_VALUE;
        }
        return Long.parseLong(value);
    }

    /**
     * Checks if ranges contain the uid
     *
     * @param idRanges the id ranges
     * @param uid      the uid
     * @return true, if ranges contain given uid
     */
    public static boolean containsUid(IdRange[] idRanges, long uid) {
        if (null != idRanges && idRanges.length > 0) {
            for (IdRange range : idRanges) {
                if (range.includes(uid)) {
                    return true;
                }
            }
        }
        return false;
    }
}
