/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the Apache license 2.0
* This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
*/
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry;
import com.icegreen.greenmail.util.ServerSetup;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

public class Pop3Server extends AbstractServer {
    protected final Vector<Pop3Handler> handlers = new Vector<Pop3Handler>();

    public Pop3Server(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    public synchronized void quit() {

        try {
            synchronized (handlers) {
                for (Pop3Handler handler : handlers) {
                    handler.quit();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            if (null != serverSocket && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {

            try {
                serverSocket = openServerSocket();
                setRunning(true);
                synchronized (this) {
                    this.notifyAll();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (keepOn()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    synchronized (handlers) {
                        if(!keepOn()) {
                            clientSocket.close();
                        } else {
                            Pop3Handler pop3Handler = new Pop3Handler(new Pop3CommandRegistry(), managers.getUserManager(), clientSocket);
                            handlers.add(pop3Handler);
                            pop3Handler.start();
                        }
                    }
                } catch (IOException ignored) {
                    //ignored
                }
            }
        } finally {
            quit();
        }
    }
}