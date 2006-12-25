/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.foedus.util.Quittable;
import com.icegreen.greenmail.foedus.util.StreamUtils;
import com.icegreen.greenmail.util.InternetPrintWriter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;


public class Pop3Connection {

    // protocol stuff
    Quittable _handler;

    // networking stuff
    private static final int TIMEOUT_MILLIS = 1000 * 30;
    Socket _socket;
    InetAddress _clientAddress;

    // IO stuff
    BufferedReader _in;
    InternetPrintWriter _out;

    public Pop3Connection(Quittable handler, Socket socket)
            throws IOException {
        configureSocket(socket);
        configureStreams();

        _handler = handler;
    }

    private void configureStreams()
            throws IOException {
        OutputStream o = _socket.getOutputStream();
        InputStream i = _socket.getInputStream();
        _out = new InternetPrintWriter(o, true);
        _in = new BufferedReader(new InputStreamReader(i));
    }

    private void configureSocket(Socket socket)
            throws SocketException {
        _socket = socket;
        _socket.setSoTimeout(TIMEOUT_MILLIS);
        _clientAddress = _socket.getInetAddress();
    }

    public void close()
            throws IOException {
        _socket.close();
    }

    public void quit() {
        _handler.quit();
    }

    public void println(String line) {
        System.out.println("S: " + line);
        _out.print(line);
        println();
    }

    public void println() {
        _out.print("\r\n");
        _out.flush();
    }

    public void print(Reader in)
            throws IOException {
        StreamUtils.copy(in, _out);
        _out.flush();
    }

    public String readLine()
            throws IOException {
        String line = _in.readLine();
        System.out.println("C: " + line);

        return line;
    }

    public String getClientAddress() {

        return _clientAddress.toString();
    }
}