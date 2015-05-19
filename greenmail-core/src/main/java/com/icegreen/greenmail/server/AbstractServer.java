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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private volatile boolean keepRunning = false;
    private volatile boolean running = false;
    private final Object startupMonitor = new Object();

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

    protected ServerSocket openServerSocket() throws IOException {
        if (setup.isSecure()) {
            return DummySSLServerSocketFactory.getDefault().createServerSocket(setup.getPort(), 0, bindTo);
        } else {
            return new ServerSocket(setup.getPort(), 0, bindTo);
        }
    }

    @Override
    public void run() {
        // Open server socket ...
        try {
            serverSocket = openServerSocket();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        setRunning(true);

        // Notify everybody that we're ready to accept connections
        synchronized (startupMonitor) {
            startupMonitor.notifyAll();
        }

        // Handle connections
        while (keepOn()) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (!keepOn()) {
                    clientSocket.close();
                } else {
                    handleClientSocket(clientSocket);
                }
            } catch (IOException ignored) {
                //ignored
                if (log.isTraceEnabled()) {
                    log.trace("Error while processing socket", ignored);
                }
            }
        }
    }

    protected void handleClientSocket(Socket clientSocket) {
        final ProtocolHandler handler = createProtocolHandler(clientSocket);
        addHandler(handler);
        final Thread thread = new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    handler.run(); // NOSONAR
                } finally {
                    // Make sure to de-register, see https://github.com/greenmail-mail-test/greenmail/issues/18
                    removeHandler(handler);
                }
            }
        });
        thread.start();
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
     * Removes protocol handler, eg.  when shutting down.
     *
     * @param handler the handler.
     */
    private void removeHandler(ProtocolHandler handler) {
        handlers.remove(handler);
    }

    protected synchronized void quit() {
        try {
            // Close server socket, we do not accept new requests anymore.
            // This also terminates the server thread if blocking on socket.accept.
            if (null != serverSocket) {
                serverSocket.close();
                serverSocket = null;
            }

            // Close all handlers. Handler threads terminate if run loop exits
            synchronized (handlers) {
                for (ProtocolHandler handler : handlers) {
                    handler.close();
                }
                handlers.clear();
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

    @Override
    public String toString() {
        return null != setup ? setup.getProtocol() + ':' + setup.getPort() : super.toString();
    }

    @Override
    public void waitTillRunning(long timeoutInMs) throws InterruptedException {
        long t = System.currentTimeMillis();
        synchronized (startupMonitor) {
            // Loop to avoid spurious wakeups, see
            // https://www.securecoding.cert.org/confluence/display/java/THI03-J.+Always+invoke+wait%28%29+and+await%28%29+methods+inside+a+loop
            while(!isRunning() && System.currentTimeMillis()-t < timeoutInMs) {
                startupMonitor.wait(timeoutInMs);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    protected void setRunning(boolean r) {
        running = r;
    }

    protected final boolean keepOn() {
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
    public final synchronized void stopService(long millis) {
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
