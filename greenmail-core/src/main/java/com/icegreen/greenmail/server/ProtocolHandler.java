package com.icegreen.greenmail.server;

/**
 * Handles a specific protocol, such as IMAP etc. for a given request.
 */
public interface ProtocolHandler extends Runnable {
    /**
     * Closes all resources, such as sockets.
     */
    void close();
}
