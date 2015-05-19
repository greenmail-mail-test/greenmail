/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.util.InternetPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class SmtpConnection {

    private static final int TIMEOUT_MILLIS = 1000 * 30;
    private static final Logger log = LoggerFactory.getLogger(SmtpConnection.class);

    // networking/io stuff
    Socket sock;
    InetAddress clientAddress;
    InternetPrintWriter out;
    BufferedReader in;
    SmtpHandler handler;
    String heloName;

    public SmtpConnection(SmtpHandler handler, Socket sock)
            throws IOException {
        this.sock = sock;
        sock.setSoTimeout(TIMEOUT_MILLIS);
        clientAddress = sock.getInetAddress();
        OutputStream o = sock.getOutputStream();
        InputStream i = sock.getInputStream();
        out = new InternetPrintWriter(o, true);
        in = new BufferedReader(new InputStreamReader(i));

        this.handler = handler;
    }

    public void send(String line) {
        if (log.isTraceEnabled()) {
            log.trace("S: " + line);
        }
        out.println(line);
    }

    public BufferedReader getReader() {
        return in;
    }

    public String receiveLine()
            throws IOException {
        String line = in.readLine();
        if (log.isTraceEnabled()) {
            log.trace("C: " + line);
        }

        return line;
    }

    public String getClientAddress() {
        return clientAddress.getHostAddress();
    }

    public InetAddress getServerAddress() {
        return sock.getLocalAddress();
    }

    public String getServerGreetingsName() {
        InetAddress address = getServerAddress();
        if (address != null)
            return address.toString();
        else
            return System.getProperty("user.name");
    }

    public String getHeloName() {
        return heloName;
    }

    public void setHeloName(String n) {
        heloName = n;
    }

    public void quit() {
        handler.close();
    }
}