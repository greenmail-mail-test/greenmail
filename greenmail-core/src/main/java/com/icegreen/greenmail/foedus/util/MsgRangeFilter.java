/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MsgRangeFilter {
    static final Pattern TWO_PART = Pattern.compile("(\\d+|\\*):(\\d+|\\*)");
    int top;
    int bottom;
    boolean isUid;

    public MsgRangeFilter(String rng, boolean uid) {
        if (rng.indexOf(':') == -1) {
            int value = Integer.parseInt(rng);
            top = value;
            bottom = value;
        } else {
            Matcher mat = TWO_PART.matcher(rng);
            if(mat.matches() &&mat.groupCount() == 2) {
                String botGroup = mat.group(1);
                String topGroup = mat.group(2);
                if ("*".equals(botGroup)) {
                    bottom = 0;
                } else {
                    bottom = Integer.parseInt(botGroup);
                }

                if ("*".equals(topGroup)) {
                    top = Integer.MAX_VALUE;
                } else {
                    top = Integer.parseInt(topGroup);
                }
            } else {
                throw new IllegalStateException("Can not create range filter from "+rng+", uid="+uid);
            }
        }

        isUid = uid;
    }

    public boolean includes(int seq) {
        return seq >= bottom && seq <= top;
    }
}