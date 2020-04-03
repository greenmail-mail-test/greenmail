/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3.commands;

import com.icegreen.greenmail.pop3.Pop3Connection;
import com.icegreen.greenmail.pop3.Pop3State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Pop3Command {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public abstract boolean isValidForState(Pop3State state);

    public abstract void execute(Pop3Connection conn, Pop3State state,
                                 String cmd);
}