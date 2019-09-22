/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.mail.MovingMessage;

public class SmtpState {
    MovingMessage currentMessage;

    public SmtpState() {
        clearMessage();
    }

    public MovingMessage getMessage() {
        return currentMessage;
    }

    /**
     * To destroy a half-constructed message.
     */
    public void clearMessage() {
        if (currentMessage != null) {
            currentMessage.releaseContent();
        }
        currentMessage = new MovingMessage();
    }
}