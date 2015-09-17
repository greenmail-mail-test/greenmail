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

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import java.io.IOException;
import java.net.*;
import java.util.*;

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
        setName(setup.getProtocol() + ':' + setup.getPort());
        try {
            bindTo = (setup.getBindAddress() == null)
                    ? InetAddress.getByName(setup.getDefaultBindAddress())
                    : InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to setup bind address for " + getName(), e);
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
        final ServerSocket socket;
        if (setup.isSecure()) {
            socket = DummySSLServerSocketFactory.getDefault().createServerSocket();
        } else {
            socket = new ServerSocket();
        }
        socket.setReuseAddress(true); // Try to fix TIME_WAIT on Linux when quickly starting/stopping server
        try {
            socket.bind(new InetSocketAddress(bindTo, setup.getPort()));
        } catch (IOException ex) {
            try {
                socket.close(); // Do close if bind failed!
            } catch (IOException nested) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring attempt to close connection", nested);
                }
            }
            throw ex;
        }
        return socket;
    }

    @Override
    public void run() {
        try {
            initServerSocket();

            if (log.isDebugEnabled()) {
                log.debug("Started " + getName());
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
                        log.trace("Error while processing client socket for " + getName(), ignored);
                    }
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    protected synchronized void initServerSocket() {
        try {
            serverSocket = openServerSocket();
            setRunning(true);
        } catch (IOException e) {
            final String msg = "Can not open server socket for " + getName();
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        } finally {
            // Notify everybody that we're ready to accept connections or failed to start.
            // Otherwise will run into startup timeout, see #waitTillRunning(long).
            synchronized (startupMonitor) {
                startupMonitor.notifyAll();
            }
        }
    }

    /**
     * Closes the server socket.
     */
    protected void closeServerSocket() {
        // Close server socket, we do not accept new requests anymore.
        // This also terminates the server thread if blocking on socket.accept.
        if (null != serverSocket) {
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                    if (log.isTraceEnabled()) {
                        log.trace("Closed server socket " + serverSocket + "/ref="
                                + Integer.toHexString(System.identityHashCode(serverSocket))
                                + " for " + getName());
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to successfully quit server " + getName(), e);
            }
        }
    }

    protected void handleClientSocket(Socket clientSocket) {
        final ProtocolHandler handler = createProtocolHandler(clientSocket);
        addHandler(handler);
        String threadName = getName() + "<-" + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
        final Thread thread = new Thread(new Runnable() {
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
        thread.setName(threadName);
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

    /**
     * Quits server by closing server socket and closing client socket handlers.
     */
    protected synchronized void quit() {
        if (log.isDebugEnabled()) {
            log.debug("Stopping " + getName());
        }
        closeServerSocket();

        // Close all handlers. Handler threads terminate if run loop exits
        synchronized (handlers) {
            for (ProtocolHandler handler : handlers) {
                handler.close();
            }
            handlers.clear();
        }
        if (log.isDebugEnabled()) {
            log.debug("Stopped " + getName());
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
        return getName();
    }

    @Override
    public boolean waitTillRunning(long timeoutInMs) throws InterruptedException {
        long t = System.currentTimeMillis();
        synchronized (startupMonitor) {
            // Loop to avoid spurious wakeups, see
            // https://www.securecoding.cert.org/confluence/display/java/THI03-J.+Always+invoke+wait%28%29+and+await%28%29+methods+inside+a+loop
            while (!isRunning() && System.currentTimeMillis() - t < timeoutInMs) {
                startupMonitor.wait(timeoutInMs);
            }
        }

        return isRunning();
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
            log.warn("Got interrupted while stopping " + toString(), e);
        }
    }

    /**
     * Stops the service (without timeout).
     */
    @Override
    public final void stopService() {
        stopService(0L);
    }

    /**
     * Creates a session configured for given server (IMAP, SMTP, ...).
     *
     * @param properties optional session properties, can be null.
     * @return the session.
     */
    public Session createSession(Properties properties) {
        return createSession(properties, false);
    }

    /**
     * Creates a session configured for given server (IMAP, SMTP, ...).
     *
     * @param properties optional session properties, can be null.
     * @param debug      if true enables JavaMail debug settings
     * @return the session.
     */
    public Session createSession(Properties properties, boolean debug) {
        Properties props = setup.configureJavaMailSessionProperties(properties, debug);

        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder("Server Mail session properties are :");
            for (Map.Entry entry : props.entrySet()) {
                if (entry.getKey().toString().contains("imap")) {
                    buf.append("\n\t").append(entry.getKey()).append("\t : ").append(entry.getValue());
                }
            }
            log.debug(buf.toString());
        }

        return Session.getInstance(props, null);
    }

    /**
     * Creates a session configured for given server (IMAP, SMTP, ...).
     *
     * @return the session.
     */
    public Session createSession() {
        return createSession(null);
    }

    /**
     * Creates a new JavaMail store.
     *
     * @return a new store.
     */
    public Store createStore() throws NoSuchProviderException {
        return createSession().getStore(getProtocol());
    }
}
