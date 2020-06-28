/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Represents a range of UID values.
 * <p>
 * From https://tools.ietf.org/html/rfc3501 :
 * <p>
 * seq-number      = nz-number / "*"
 * ; message sequence number (COPY, FETCH, STORE
 * ; commands) or unique identifier (UID COPY,
 * ; UID FETCH, UID STORE commands).
 * ; * represents the largest number in use.  In
 * ; the case of message sequence numbers, it is
 * ; the number of messages in a non-empty mailbox.
 * ; In the case of unique identifiers, it is the
 * ; unique identifier of the last message in the
 * ; mailbox or, if the mailbox is empty, the
 * ; mailbox's current UIDNEXT value.
 * ; The server should respond with a tagged BAD
 * ; response to a command that uses a message
 * ; sequence number greater than the number of
 * ; messages in the selected mailbox.  This
 * ; includes "*" if the selected mailbox is empty.
 * <p>
 * seq-range       = seq-number ":" seq-number
 * ; two seq-number values and all values between
 * ; these two regardless of order.
 * ; Example: 2:4 and 4:2 are equivalent and indicate
 * ; values 2, 3, and 4.
 * ; Example: a unique identifier sequence range of
 * ; 3291:* includes the UID of the last message in
 * ; the mailbox, even if that value is less than 3291.
 * <p>
 * sequence-set    = (seq-number / seq-range) *("," sequence-set)
 * ; set of seq-number values, regardless of order.
 * ; Servers MAY coalesce overlaps and/or execute the
 * ; sequence in any order.
 * ; Example: a message sequence number set of
 * ; 2,4:7,9,12:* for a mailbox with 15 messages is
 * ; equivalent to 2,4,5,6,7,9,12,13,14,15
 * ; Example: a message sequence number set of *:4,5:7
 * ; for a mailbox with 10 messages is equivalent to
 * ; 10,9,8,7,6,5,4,5,6,7 and MAY be reordered and
 * ; overlap coalesced to be 4,5,6,7,8,9,10.
 */
public class IdRange {
    private static final String PATTERN_SEQ_NUMBER = "(\\*|\\d+)";
    private static final String PATTERN_SEQ_RANGE = "(" + PATTERN_SEQ_NUMBER + ":" + PATTERN_SEQ_NUMBER + ")";
    private static final String PATTERN_SEQ_NUM_OR_RANGE = "(" + PATTERN_SEQ_NUMBER + "|" + PATTERN_SEQ_RANGE + ")";
    private static final String PATTERN_SEQ_SET = PATTERN_SEQ_NUM_OR_RANGE + "(,(" + PATTERN_SEQ_NUM_OR_RANGE +"))*";


    /**
     * Matches a sequence of a single id or id range
     */
    public static final Pattern SEQUENCE = Pattern.compile(PATTERN_SEQ_SET);
    public static final long VALUE_WILDCARD = Long.MAX_VALUE;
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
     * Note that the wildcard '*' denotes the largest number in use.
     * <p/>
     * Example: 1,2:5,8:*
     *
     * @param idRangeSequence the sequence
     * @return a list of ranges, never null.
     */
    public static List<IdRange> parseRangeSequence(String idRangeSequence) {
        StringTokenizer tokenizer = new StringTokenizer(idRangeSequence, ",");
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
                // two seq-number values and all values between these two regardless of order
                // 2:4 is equivalent to 4:2
                if (lowVal > highVal) {
                    return new IdRange(highVal, lowVal);
                } else {
                    return new IdRange(lowVal, highVal);
                }
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
            if (uid > currentIdRange.getHighVal() && (uid == currentIdRange.getHighVal() + 1)) {
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
            return VALUE_WILDCARD;
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

    @Override
    public String toString() {
        return lowVal == highVal ?
                Long.toString(lowVal)
                : lowVal + ":" + highVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdRange)) return false;
        IdRange idRange = (IdRange) o;
        return lowVal == idRange.lowVal &&
                highVal == idRange.highVal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowVal, highVal);
    }
}
