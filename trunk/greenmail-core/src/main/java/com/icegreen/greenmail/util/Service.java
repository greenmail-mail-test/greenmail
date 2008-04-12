/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail.util;

/**
 * A class that facilitate service implementation
 *
 * @author Wael Chatila
 * @version $id: $
 * @since 2005
 */
abstract public class Service extends Thread {
    public abstract void run();

    public abstract void quit();

    private volatile boolean keepRunning = false;

    //---------
    public void init(Object obj) {
        //empty
    }

    public void destroy(Object obj) {
        //empty
    }

    final protected boolean keepOn() {
        return keepRunning;
    }

    public synchronized void startService(Object obj) {
        if (!keepRunning) {
            keepRunning = true;
            init(obj);
            start();
        }
    }

    /**
     * Stops the service. If a timeout is given and the service has still not
     * gracefully been stopped after timeout ms the service is stopped by force.
     *
     * @param obj
     * @param millis value in ms
     */
    public synchronized final void stopService(Object obj, Long millis) {
        boolean doDestroy = keepRunning;
        try {
            if (keepRunning) {
                keepRunning = false;
                interrupt();
                quit();
                if (null == millis) {
                    join();
                } else {
                    join(millis.longValue());
                }
            }
        } catch (InterruptedException e) {
            //its possible that the thread exits between the lines keepRunning=false and intertupt above
        } finally {
            if (doDestroy) {
                destroy(obj);
            }
        }
    }

    public final void stopService(Object obj) {
        stopService(obj, null);
    }

    public final void stopService(Object obj, long millis) {
        stopService(obj, new Long(millis));
    }
}

