package com.icegreen.greenmail.server;

/**
 * Handles a specific protocol, such as IMAP etc. for a given request.
 */
public interface ProtocolHandler extends Runnable {
    /**
     * Close all resources, such as sockets.
     */
    void close();
}
