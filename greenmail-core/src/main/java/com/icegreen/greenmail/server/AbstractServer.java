/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.server;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Feb 2, 2006
 */
public abstract class AbstractServer extends Thread implements Service {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected Managers managers;
    protected ServerSetup setup;
    private final List<ProtocolHandler> handlers = Collections.synchronizedList(new ArrayList<ProtocolHandler>());

    protected AbstractServer(ServerSetup setup, Managers managers) {
        this.setup = setup;
        try {
            bindTo = (setup.getBindAddress() == null)
                    ? InetAddress.getByName(setup.getDefaultBindAddress())
                    : InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.managers = managers;
    }

    /**
     * Create a new, specific protocol handler such as for IMAP.
     *
     * @param clientSocket the client socket to use.
     * @return the new protocol handler.
     */
    protected abstract ProtocolHandler createProtocolHandler(Socket clientSocket);

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret = null;
        IOException retEx = null;
        for (int i = 0; i < 25 && (null == ret); i++) {
            try {
                if (setup.isSecure()) {
                    ret = DummySSLServerSocketFactory.getDefault().createServerSocket(setup.getPort(), 0, bindTo);
                } else {
                    ret = new ServerSocket(setup.getPort(), 0, bindTo);
                }
            } catch (BindException e) {
                try {
                    retEx = e;
                    wait(10L);
                } catch (InterruptedException ignored) {
                    if (log.isDebugEnabled()) {
                        log.debug("Can not open port, retrying ...", e);
                    }
                }
            }
        }
        if (null == ret && null != retEx) {
            throw retEx;
        }
        return ret;
    }

    @Override
    public void run() {
        try {
            serverSocket = openServerSocket();
            setRunning(true);
            synchronized (this) {
                this.notifyAll();
            }
            synchronized (startupMonitor) {
                startupMonitor.notifyAll();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (keepOn()) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (!keepOn()) {
                    clientSocket.close();
                } else {
                    final ProtocolHandler handler = createProtocolHandler(clientSocket);
                    addHandler(handler);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            handler.run(); // NOSONAR
                            // Make sure to deregister, see https://github.com/greenmail-mail-test/greenmail/issues/18
                            removeHandler(handler);
                        }
                    }).start();
                }
            } catch (IOException ignored) {
                //ignored
                if (log.isTraceEnabled()) {
                    log.trace("Error while processing socket", ignored);
                }
            }
        }
    }

    /**
     * Adds a protocol handler, for eg. shutting down.
     *
     * @param handler the handler.
     */
    private void addHandler(ProtocolHandler handler) {
        handlers.add(handler);
    }

    /**
     * Adds a protocol handler, for eg. shutting down.
     *
     * @param handler the handler.
     */
    private void removeHandler(ProtocolHandler handler) {
        handlers.remove(handler);
    }

    protected synchronized void quit() {
        try {
            synchronized (handlers) {
                for (ProtocolHandler handler : handlers) {
                    handler.close();
                }
            }
            if (null != serverSocket) {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
                serverSocket = null;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getBindTo() {
        return bindTo.getHostAddress();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public String getProtocol() {
        return setup.getProtocol();
    }

    public ServerSetup getServerSetup() {
        return setup;
    }

    public String toString() {
        return null != setup ? setup.getProtocol() + ':' + setup.getPort() : super.toString();
    }

    private final Object startupMonitor = new Object();
    @Override
    public void waitTillRunning(long timeoutInMs) throws InterruptedException {
        synchronized (startupMonitor) {
            startupMonitor.wait(timeoutInMs);
        }
    }

    private volatile boolean keepRunning = false;
    private volatile boolean running = false;

    public boolean isRunning() {
        return running;
    }

    protected void setRunning(boolean r) {
        this.running = r;
    }

    final protected boolean keepOn() {
        return keepRunning;
    }

    @Override
    public synchronized void startService() {
        if (!keepRunning) {
            keepRunning = true;
            start();
        }
    }


    /**
     * Stops the service. If a timeout is given and the service has still not
     * gracefully been stopped after timeout ms the service is stopped by force.
     *
     * @param millis value in ms
     */
    @Override
    public synchronized final void stopService(long millis) {
        running = false;
        try {
            if (keepRunning) {
                keepRunning = false;
                interrupt();
                quit();
                if (0L == millis) {
                    join();
                } else {
                    join(millis);
                }
            }
        } catch (InterruptedException e) {
            //its possible that the thread exits between the lines keepRunning=false and interrupt above
            log.warn("Got interrupted while stopping", e);
        }
    }

    /**
     * Stops the service (without timeout).
     */
    @Override
    public final void stopService() {
        stopService(0L);
    }
}
