/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
*/
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.foedus.util.InMemoryWorkspace;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.util.ServerSetup;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

public class SmtpServer extends AbstractServer {
    protected final Vector<SmtpHandler> handlers = new Vector<SmtpHandler>();

    public SmtpServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    public synchronized void quit() {
        try {
            synchronized (handlers) {
                for (SmtpHandler handler : handlers) {
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
        } catch (Exception e) {
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
                        if (!keepOn()) {
                            clientSocket.close();
                        } else {
                            SmtpHandler smtpHandler = new SmtpHandler(new SmtpCommandRegistry(), managers.getSmtpManager(), new InMemoryWorkspace(), clientSocket);
                            handlers.add(smtpHandler);
                            smtpHandler.start();
                        }
                    }
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            quit();
        }
    }
}