/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;

import java.io.IOException;


public class InMemoryWorkspace
        implements Workspace {
    public Resource getTmpFile()
            throws IOException {

        return new StringBufferResource();
    }

    public void release(Resource tmpFile) {
    }
}