/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.BindException;
import java.util.Vector;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Feb 2, 2006
 */
public abstract class AbstractServer extends Service {
    protected final InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected Vector handlers = null;
    protected Managers managers;
    protected ServerSetup setup;

    protected AbstractServer(ServerSetup setup, Managers managers) {
        try {
            this.setup = setup;
            bindTo = (setup.getBindAddress() == null) ? InetAddress.getByName("0.0.0.0") : InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.managers = managers;
        handlers = new Vector();
    }

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret = null;
        IOException retEx = null;
        for (int i=0;i<25 && (null == ret);i++) {
            try {
                if (setup.isSecure()) {
                    ret = DummySSLServerSocketFactory.getDefault().createServerSocket(setup.getPort(), 0, bindTo);
                } else {
                    ret = new ServerSocket(setup.getPort(), 0, bindTo);
                }
            } catch (BindException e) {
                try {
                    retEx = e;
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }
        if (null == ret && null != retEx) {
            throw retEx;
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

    public ServerSetup getServerSetup() {
        return setup;
    }

    public String toString() {
        return null!=setup? setup.getProtocol()+':'+setup.getPort() : super.toString();
    }
}
