/* -------------------------------------------------------------------
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.AbstractServer;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.util.ServerSetup;

import java.io.IOException;
import java.net.BindException;

public final class ImapServer extends AbstractServer {
    private ImapHandler imapHandler = null;

    public ImapServer(ServerSetup setup, Managers managers) {
        super(setup, managers);
    }



    public synchronized void quit() {
        if (null != imapHandler) {
            imapHandler.resetHandler();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        imapHandler = new ImapHandler(managers.getUserManager(), managers.getImapHostManager());

        try {
            serverSocket = openServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (keepOn()) {
            try {
                clientSocket = serverSocket.accept();
                imapHandler.handleConnection(clientSocket);
            } catch (IOException ignored) {
                //ignored
            }
        }
    }
}