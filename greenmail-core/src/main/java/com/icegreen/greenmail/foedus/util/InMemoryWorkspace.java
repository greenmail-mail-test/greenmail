/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
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