/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.Service;
import com.icegreen.greenmail.util.ServerSetup;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Feb 2, 2006
 */
public abstract class AbstractServer extends Service {
    protected InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected Socket clientSocket = null;
    protected Managers managers;
    protected ServerSetup setup;

    protected AbstractServer(ServerSetup setup, Managers managers) {
        try {
            this.setup = setup;
            bindTo = InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.managers = managers;
    }

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret;
        if (setup.isSecure()) {
            ret = (SSLServerSocket) DummySSLServerSocketFactory.getDefault().createServerSocket(setup.getPort(), 0, bindTo);
        } else {
            ret = new ServerSocket(setup.getPort(), 0, bindTo);
        }
        return ret;
    }

    public String getBindTo() {
        return bindTo.getHostAddress();
    }

    public int getPort() {
        return setup.getPort();
    }

    public String getProtocol() {
        return setup.getProtocol();
    }
}
