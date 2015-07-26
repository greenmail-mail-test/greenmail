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
    @Override
    public Resource getTmpFile()
            throws IOException {

        return new StringBufferResource();
    }

    @Override
    public void release(Resource tmpFile) {
        // Nothing todo
    }
}