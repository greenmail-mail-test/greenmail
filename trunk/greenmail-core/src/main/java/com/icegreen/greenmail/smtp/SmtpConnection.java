/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.util.InternetPrintWriter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class SmtpConnection {

    // TODO: clean up getting localhost name
    private static final int TIMEOUT_MILLIS = 1000 * 30;
    private InetAddress serverAddress;


    {
        try {
            serverAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
        }
    }

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

    /**
     * For testing only
     */
    SmtpConnection() {
    }

    public void println(String line) {

        // System.err.println("S: " + line);
        out.println(line);
    }

    public BufferedReader getReader() {

        return in;
    }

    public String readLine()
            throws IOException {
        String line = in.readLine();

        // System.err.println("C: " + line);
        return line;
    }

    public String getClientAddress() {

        return clientAddress.getHostName();
    }

    public InetAddress getServerAddress() {

        return serverAddress;
    }

    public String getServerGreetingsName() {
        InetAddress serverAddress = getServerAddress();

        if (serverAddress != null)

            return serverAddress.toString();
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
        handler.quit();
    }
}