/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
*/
package com.icegreen.greenmail.pop3;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry;

import java.io.IOException;

public class Pop3Server extends AbstractServer {
    private Pop3Handler pop3Handler = null;

    public Pop3Server(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }

    public void quit() {
        pop3Handler.quit();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        pop3Handler = new Pop3Handler(new Pop3CommandRegistry(), managers.getUserManager());

        try {
            serverSocket = openServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (keepOn()) {
            try {
                clientSocket = serverSocket.accept();
                pop3Handler.handleConnection(clientSocket);
            } catch (IOException ignored) {
                //ignored
            }
        }
    }
}