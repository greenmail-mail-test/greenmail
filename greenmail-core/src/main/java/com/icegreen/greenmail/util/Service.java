/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

/**
 * Generic lifecycle handling of a service.
 * <p>
 * Used by email protocol specific services, such as SmtpServer.
 *
 * @author Wael Chatila
 * @since 2005
 */
public interface Service {
    /**
     * Starts the service in the background as a new thread.
     * <p>
     * You can use {@link #isRunning()}  and {@link #waitTillRunning(long)} to check if service is up.
     */
    void startService();

    /**
     * Stops the service.
     * <p>
     * Blocks till the service stopped.
     */
    void stopService();

    /**
     * Stops the service.
     * <p>
     * Blocks till the service stopped.
     * <p>
     * If a timeout is given and the service has still not
     * gracefully been stopped after timeout ms the service is stopped by force.
     *
     * @param timeoutInMs the timeout in milliseconds
     */
    void stopService(long timeoutInMs);

    /**
     * Checks if service is up and running.
     *
     * @return true, if running.
     */
    boolean isRunning();

    /**
     * Waits till service is up or timeout was reached.
     *
     * @param timeoutInMs the timeout in milliseconds
     * @throws InterruptedException if interrupted while waiting.
     * @return true, if running otherwise false if timeout was reached.
     */
    boolean waitTillRunning(long timeoutInMs) throws InterruptedException;
}

