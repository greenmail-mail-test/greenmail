/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.foedus.util;

import com.icegreen.greenmail.util.EncodingUtil;

import java.io.*;


public class StringBufferResource
        implements Resource {
    StringWriter currentWriter;
    StringBuilder contentBuffer;

    public StringBufferResource() {
        // Nothing
    }

    public StringBufferResource(String initalValue) {
        contentBuffer = new StringBuilder(initalValue);
    }

    @Override
    public Writer getWriter()
            throws IOException {
        // TODO: Check if always returning new writer is not a bug
        currentWriter = new StringWriter();

        return currentWriter;
    }

    @Override
    public InputStream getInputStream()
            throws IOException {
        closeInput();

        return new ByteArrayInputStream(contentBuffer.toString().getBytes(EncodingUtil.CHARSET_EIGHT_BIT_ENCODING));
    }

    @Override
    public Reader getReader()
            throws IOException {
        closeInput();

        return new StringReader(contentBuffer.toString());
    }

    private void closeInput()
            throws IOException {
        if (currentWriter != null) {
            contentBuffer = new StringBuilder(currentWriter.getBuffer());
            currentWriter = null;
        }

        if (contentBuffer == null)
            throw new IOException("No content has been written");
    }

    @Override
    public long getSize() {
        return contentBuffer.length();
    }

    @Override
    public String getAsString() throws IOException {
        closeInput();
        return contentBuffer.toString();
    }

    @Override
    public void delete() {
        contentBuffer = null;
        currentWriter = null;
    }

}