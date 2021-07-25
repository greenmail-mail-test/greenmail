package com.icegreen.greenmail.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Provides shared helpers for handling a client socket connection.
 */
public abstract class AbstractSocketProtocolHandler implements ProtocolHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * The TCP/IP socket over which the IMAP interaction
     * is occurring
     */
    protected final Socket socket;
    protected volatile boolean quitting = false; // Signal that shutdown is initiated
    protected final Object closeMonitor = new Object();

    protected AbstractSocketProtocolHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Preparing for closing handler connection?
     *
     * @return true, if should be closed.
     */
    public boolean isQuitting() {
        return quitting;
    }

    /**
     * Signal for closing handler.
     *
     * @param quitting true, if closing in progress.
     */
    public void setQuitting(boolean quitting) {
        this.quitting = quitting;
    }

    /**
     * Resets the handler data to a basic state.
     */
    @Override
    public void close() {
        setQuitting(true);

        if (log.isTraceEnabled()) {
            log.trace("Closing handler connection {}:{}",
                socket.getInetAddress() ,socket.getPort());
        }

        // Use monitor to avoid race between external close and handler thread run()
        synchronized (closeMonitor) {
            // Close and clear streams, sockets etc.
            if (!socket.isClosed()) {
                try {
                    // Terminates thread blocking on socket read
                    // and automatically closed depending streams
                    socket.close();
                } catch (IOException e) {
                    log.warn("Can not close socket", e);
                }
            }
        }
    }

    /**
     * Gets current socket SO_TIMEOUT
     *
     * @return the timeout or -1 if unable to retrieve.
     */
    protected long getSoTimeout() {
        try {
            return socket.getSoTimeout();
        } catch (SocketException e) {
            log.warn("Can not get socket timeout", e);
            return -1;
        }
    }
}
