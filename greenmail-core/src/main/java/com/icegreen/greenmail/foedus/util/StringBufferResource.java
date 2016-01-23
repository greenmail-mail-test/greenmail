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
    StringWriter _currentWriter;
    StringBuilder _contentBuffer;

    public StringBufferResource() {
        // Nothing
    }

    public StringBufferResource(String initalValue) {
        _contentBuffer = new StringBuilder(initalValue);
    }

    @Override
    public Writer getWriter()
            throws IOException {
        _currentWriter = new StringWriter();

        return _currentWriter;
    }

    @Override
    public InputStream getInputStream()
            throws IOException {
        closeInput();

        return new ByteArrayInputStream(_contentBuffer.toString().getBytes(EncodingUtil.CHARSET_EIGHT_BIT_ENCODING));
    }

    @Override
    public Reader getReader()
            throws IOException {
        closeInput();

        return new StringReader(_contentBuffer.toString());
    }

    private void closeInput()
            throws IOException {
        if (_currentWriter != null) {
            _contentBuffer = new StringBuilder(_currentWriter.getBuffer());
            _currentWriter = null;
        }

        if (_contentBuffer == null)
            throw new IOException("No content has been written");
    }

    @Override
    public long getSize() {
        return _contentBuffer.length();
    }

    @Override
    public String getAsString() throws IOException {
        closeInput();
        return _contentBuffer.toString();
    }

    @Override
    public void delete() {
        _contentBuffer = null;
        _currentWriter = null;
    }

}