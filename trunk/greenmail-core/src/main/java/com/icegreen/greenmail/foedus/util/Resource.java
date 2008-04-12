/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;


public interface Resource {
    public InputStream getInputStream()
            throws IOException;

    public Writer getWriter()
            throws IOException;

    public Reader getReader()
            throws IOException;

    public long getSize()
            throws IOException;

    public void delete()
            throws IOException;

    public String getAsString() throws IOException;
}