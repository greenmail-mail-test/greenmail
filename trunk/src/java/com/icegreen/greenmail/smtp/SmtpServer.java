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

public class SmtpServer extends AbstractServer {
    private SmtpHandler smtpHandler = null;

    public SmtpServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    public void quit() {
        if (null != smtpHandler) {
            smtpHandler.quit();
        }
        
        try {
            if (null != clientSocket) {
                clientSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            if (null != serverSocket) {
                serverSocket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        smtpHandler = new SmtpHandler(new SmtpCommandRegistry(), managers.getSmtpManager(), new InMemoryWorkspace());
        try {
            serverSocket = openServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (keepOn()) {
            try {
                clientSocket = serverSocket.accept();
                smtpHandler.handleConnection(clientSocket);
            } catch (SocketException ignored) {
                //ignored
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}