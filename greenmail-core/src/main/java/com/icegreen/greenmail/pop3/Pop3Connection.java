/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.foedus.util.StreamUtils;
import com.icegreen.greenmail.util.InternetPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;


public class Pop3Connection {
    // Logger.
    protected final Logger log = LoggerFactory.getLogger(getClass());
    // protocol stuff
    Pop3Handler handler;

    // networking stuff
    Socket socket;
    InetAddress clientAddress;

    // IO stuff
    BufferedReader in;
    InternetPrintWriter out;

    public Pop3Connection(Pop3Handler handler, Socket socket)
            throws IOException {
        configureSocket(socket);
        configureStreams();

        this.handler = handler;
    }

    private void configureStreams()
            throws IOException {
        OutputStream o = socket.getOutputStream();
        InputStream i = socket.getInputStream();
        out = new InternetPrintWriter(o, true);
        in = new BufferedReader(new InputStreamReader(i));
    }

    private void configureSocket(Socket socket) {
        this.socket = socket;
        clientAddress = this.socket.getInetAddress();
    }

    public void close()
            throws IOException {
        socket.close();
    }

    public void quit() {
        handler.close();
    }

    public void println(String line) {
        log.debug("S: {}", line);
        out.print(line);
        println();
    }

    public void println() {
        out.print("\r\n");
        out.flush();
    }

    public void print(Reader in)
            throws IOException {
        StreamUtils.copy(in, out);
        out.flush();
    }

    public String readLine()
            throws IOException {
        String line = in.readLine();
        log.debug("C: {}", line);
        return line;
    }

    public String getClientAddress() {
        return clientAddress.toString();
    }
}