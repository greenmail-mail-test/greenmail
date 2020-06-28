/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.InternetPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpConnection {


    private static final Logger log = LoggerFactory.getLogger(SmtpConnection.class);

    // networking/io stuff
    Socket sock;
    InetAddress clientAddress;
    InternetPrintWriter out;
    BufferedReader in;
    SmtpHandler handler;
    String heloName;
    boolean authenticated; // Was there a successful authentication?

    public SmtpConnection(SmtpHandler handler, Socket sock)
            throws IOException {
        this.sock = sock;
        clientAddress = sock.getInetAddress();
        OutputStream o = sock.getOutputStream();
        InputStream i = sock.getInputStream();
        out = InternetPrintWriter.createForEncoding(o, true, EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);
        in = new BufferedReader(new InputStreamReader(i, StandardCharsets.US_ASCII));

        this.handler = handler;
    }

    public void send(String line) {
        log.trace("S: {}", line);
        out.println(line);
    }

    public BufferedReader getReader() {
        return in;
    }

    public String receiveLine()
            throws IOException {
        String line = in.readLine();
        log.trace("C: {}", line);

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

    /**
     * Checks if there was a successful authentication for this connection.
     *
     * @return true, if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Sets the authentication state of this connection.
     *
     * @param authenticated true,
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}