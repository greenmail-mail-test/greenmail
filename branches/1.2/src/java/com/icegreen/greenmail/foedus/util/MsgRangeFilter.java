/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MsgRangeFilter {
    static final Pattern TWO_PART = Pattern.compile("(\\d+|\\*):(\\d+|\\*)");
    int _top;
    int _bottom;
    boolean _isUID;

    public MsgRangeFilter(String rng, boolean uid) {
        if (rng.indexOf(':') == -1) {
            int value = Integer.parseInt(rng);
            _top = value;
            _bottom = value;
        } else {
            Matcher mat = TWO_PART.matcher(rng);
            mat.matches();
            assert(mat.groupCount() == 2);
            String bot = mat.group(1);
            String top = mat.group(2);
            if (bot.equals("*"))
                _bottom = 0;
            else
                _bottom = Integer.parseInt(bot);

            if (top.equals("*"))
                _top = Integer.MAX_VALUE;
            else
                _top = Integer.parseInt(top);

        }

        _isUID = uid;
    }

    public boolean includes(int seq) {
        return seq >= _bottom && seq <= _top;
    }
}