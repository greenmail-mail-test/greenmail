/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
*/
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.foedus.util.InMemoryWorkspace;

import java.io.IOException;
import java.net.SocketException;
import java.net.Socket;
import java.util.Vector;
import java.util.Iterator;

public class SmtpServer extends AbstractServer {

    public SmtpServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    public synchronized void quit() {
        try {
            for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
                SmtpHandler smtpHandler = (SmtpHandler) iterator.next();
                smtpHandler.quit();
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (keepOn()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    SmtpHandler smtpHandler = new SmtpHandler(new SmtpCommandRegistry(), managers.getSmtpManager(), new InMemoryWorkspace(), clientSocket);
                    handlers.add(smtpHandler);
                    smtpHandler.start();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally{
            quit();
        }
    }
}